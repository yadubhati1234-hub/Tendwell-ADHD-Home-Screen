package app.tendwell.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import app.tendwell.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/** Where the paywall stands with Google Play. */
sealed interface StoreState {
    data object Loading : StoreState
    /** Play is not reachable, or the products are not set up (for example a sideloaded debug build). */
    data object Unavailable : StoreState
    data class Ready(val offers: Map<ProPlan, PlanOffer>) : StoreState
}

/** Result of a purchase attempt or restore, for a short message on the paywall. */
enum class StoreEvent { Purchased, Pending, Cancelled, Failed, RestoredNothing, Restored }

/**
 * Google Play Billing: loads the Pro plans, starts purchases, acknowledges
 * them and keeps the Pro flag in Prefs (cached so Pro works offline).
 */
class BillingRepository(context: Context, private val prefs: Prefs) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectLock = Mutex()
    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val details = mutableMapOf<String, ProductDetails>()

    private val _store = MutableStateFlow<StoreState>(StoreState.Loading)
    val store: StateFlow<StoreState> = _store.asStateFlow()

    private val _events = MutableStateFlow<StoreEvent?>(null)
    /** The latest purchase result; the paywall shows it once and calls [consumeEvent]. */
    val events: StateFlow<StoreEvent?> = _events.asStateFlow()

    fun consumeEvent() { _events.value = null }

    private suspend fun connect(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // Report failure if setup never finished; the next call reconnects.
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private var lastRefreshMs = 0L

    /**
     * Re-checks purchases if the last check is more than a few hours old, so
     * an ended subscription or a refund turns Pro off. Called from Home on resume.
     */
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshMs < REFRESH_EVERY_MS) return
        lastRefreshMs = now
        refreshPurchases()
    }

    /** Re-checks purchases (cheap; Play caches them). [reportRestore] reports the result for the Restore button. */
    fun refreshPurchases(reportRestore: Boolean = false) {
        scope.launch {
            if (!connect()) {
                if (reportRestore) _events.value = StoreEvent.Failed
                return@launch
            }
            val subs = queryPurchases(BillingClient.ProductType.SUBS)
            val inapp = queryPurchases(BillingClient.ProductType.INAPP)
            if (subs == null || inapp == null) {
                if (reportRestore) _events.value = StoreEvent.Failed
                return@launch
            }
            val owned = (subs + inapp).filter { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.any { it in ProductIds.ALL }
            }
            owned.forEach { acknowledge(it) }
            prefs.setPro(owned.isNotEmpty())
            lastRefreshMs = System.currentTimeMillis()
            if (reportRestore) _events.value = if (owned.isNotEmpty()) StoreEvent.Restored else StoreEvent.RestoredNothing
        }
    }

    /** Loads the plans and prices for the paywall. */
    fun loadOffers() {
        scope.launch {
            _store.value = StoreState.Loading
            if (!connect()) {
                _store.value = StoreState.Unavailable
                return@launch
            }
            val sub = queryDetails(ProductIds.PRO_SUBSCRIPTION, BillingClient.ProductType.SUBS)
            val life = queryDetails(ProductIds.PRO_LIFETIME, BillingClient.ProductType.INAPP)
            val offers = buildMap {
                sub?.let { d ->
                    details[d.productId] = d
                    offerFor(d, ProductIds.BASE_PLAN_YEARLY, ProPlan.Yearly)?.let { put(ProPlan.Yearly, it) }
                    offerFor(d, ProductIds.BASE_PLAN_MONTHLY, ProPlan.Monthly)?.let { put(ProPlan.Monthly, it) }
                }
                life?.let { d ->
                    details[d.productId] = d
                    @Suppress("DEPRECATION")
                    d.oneTimePurchaseOfferDetails?.let { o ->
                        put(ProPlan.Lifetime, PlanOffer(ProPlan.Lifetime, o.formattedPrice, o.priceAmountMicros, o.priceCurrencyCode, 0, null))
                    }
                }
            }
            _store.value = if (offers.isEmpty()) StoreState.Unavailable else StoreState.Ready(offers)
        }
    }

    /** Picks the best offer on a base plan: the free trial if the user is eligible, else the plain base plan. */
    private fun offerFor(d: ProductDetails, basePlanId: String, plan: ProPlan): PlanOffer? {
        val candidates = d.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == basePlanId }
        if (candidates.isEmpty()) return null
        fun trialOf(o: ProductDetails.SubscriptionOfferDetails): Int =
            o.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }?.let { PlanText.trialDays(it.billingPeriod) } ?: 0
        val chosen = candidates.maxByOrNull { trialOf(it) } ?: return null
        val paid = chosen.pricingPhases.pricingPhaseList.lastOrNull { it.priceAmountMicros > 0L } ?: return null
        return PlanOffer(plan, paid.formattedPrice, paid.priceAmountMicros, paid.priceCurrencyCode, trialOf(chosen), chosen.offerToken)
    }

    /** Opens Google Play's purchase sheet. Returns false if the plan is not loaded. */
    fun launch(activity: Activity, offer: PlanOffer): Boolean {
        val productId = if (offer.plan == ProPlan.Lifetime) ProductIds.PRO_LIFETIME else ProductIds.PRO_SUBSCRIPTION
        val d = details[productId] ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).apply {
            offer.offerToken?.let { setOfferToken(it) }
        }.build()
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val list = purchases.orEmpty().filter { p -> p.products.any { it in ProductIds.ALL } }
                val done = list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                done.forEach { acknowledge(it) }
                if (done.isNotEmpty()) {
                    prefs.setPro(true)
                    _events.value = StoreEvent.Purchased
                } else if (list.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    _events.value = StoreEvent.Pending
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.value = StoreEvent.Cancelled
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases(reportRestore = true)
            else -> _events.value = StoreEvent.Failed
        }
    }

    private suspend fun acknowledge(p: Purchase) {
        if (p.isAcknowledged) return
        suspendCancellableCoroutine { cont ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private suspend fun queryPurchases(type: String): List<Purchase>? = suspendCancellableCoroutine { cont ->
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, list ->
            if (cont.isActive) cont.resume(if (result.responseCode == BillingClient.BillingResponseCode.OK) list else null)
        }
    }

    private suspend fun queryDetails(id: String, type: String): ProductDetails? = suspendCancellableCoroutine { cont ->
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, found ->
            val d = if (result.responseCode == BillingClient.BillingResponseCode.OK) found.productDetailsList.firstOrNull() else null
            if (cont.isActive) cont.resume(d)
        }
    }

    companion object {
        private const val REFRESH_EVERY_MS = 6 * 60 * 60 * 1000L

        fun manageSubscriptionUrl(packageName: String) =
            "https://play.google.com/store/account/subscriptions?sku=${ProductIds.PRO_SUBSCRIPTION}&package=$packageName"
    }
}
