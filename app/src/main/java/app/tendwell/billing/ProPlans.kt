package app.tendwell.billing

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Play Console product ids. They must match what is set up in Play Console > Monetize. */
object ProductIds {
    /** Subscription with two base plans, each with a 7-day free trial offer. */
    const val PRO_SUBSCRIPTION = "tendwell_pro"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"
    /** One-time in-app product. */
    const val PRO_LIFETIME = "tendwell_pro_lifetime"

    val ALL = setOf(PRO_SUBSCRIPTION, PRO_LIFETIME)
}

enum class ProPlan { Yearly, Monthly, Lifetime }

/** What the paywall shows for one plan, filled in from Google Play's product details. */
data class PlanOffer(
    val plan: ProPlan,
    /** Play's formatted price for the paid phase, like "$24.99". */
    val price: String,
    val priceMicros: Long,
    val currencyCode: String,
    /** Free-trial length in days, or 0 if the user is not offered a trial. */
    val trialDays: Int,
    /** Offer token for subscriptions; null for the one-time product. */
    val offerToken: String?,
)

object PlanText {
    /** "$24.99 a year", "$3.99 a month", "$49.99 once". */
    fun priceLine(o: PlanOffer): String = when (o.plan) {
        ProPlan.Yearly -> "${o.price} a year"
        ProPlan.Monthly -> "${o.price} a month"
        ProPlan.Lifetime -> "${o.price} once"
    }

    /** The smaller line under a plan's price. */
    fun subLine(o: PlanOffer, locale: Locale = Locale.getDefault()): String {
        val trial = if (o.trialDays > 0) "${o.trialDays} days free" else null
        return when (o.plan) {
            ProPlan.Yearly -> listOfNotNull("About ${perMonth(o.priceMicros, o.currencyCode, locale)} a month", trial).joinToString(" · ")
            ProPlan.Monthly -> trial ?: "Cancel any time"
            ProPlan.Lifetime -> "Pay once, keep Pro"
        }
    }

    fun headline(o: PlanOffer?): String =
        if (o != null && o.plan != ProPlan.Lifetime && o.trialDays > 0) "Try Pro free for ${o.trialDays} days" else "Get Tendwell Pro"

    fun cta(o: PlanOffer): String = when {
        o.plan == ProPlan.Lifetime -> "Buy lifetime for ${o.price}"
        o.trialDays > 0 -> "Start ${o.trialDays}-day free trial"
        else -> "Subscribe for ${priceLine(o)}"
    }

    /** First line of the terms under the button. */
    fun termsLead(o: PlanOffer): String = when {
        o.plan == ProPlan.Lifetime -> "One payment of ${o.price}. No subscription."
        o.trialDays > 0 -> "${o.trialDays} days free, then ${priceLine(o)}."
        else -> "${priceLine(o).replaceFirstChar { it.uppercase() }}, renewing until you cancel."
    }

    fun trust(o: PlanOffer): String =
        if (o.plan == ProPlan.Lifetime) "No account needed. It all stays on your phone." else "Early subscribers keep this price for good."

    /** The last day to cancel without being charged, like "10 November". */
    fun cancelBy(today: LocalDate, trialDays: Int, locale: Locale = Locale.getDefault()): String =
        today.plusDays(trialDays.toLong()).format(DateTimeFormatter.ofPattern("d MMMM", locale))

    /** A yearly price divided by 12, in the same currency: 24.99 USD gives "$2.08". */
    fun perMonth(yearlyMicros: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
        val digits = currency?.defaultFractionDigits?.takeIf { it >= 0 } ?: 2
        val monthly = BigDecimal(yearlyMicros).divide(BigDecimal(12_000_000), digits, RoundingMode.HALF_UP)
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            if (currency != null) this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return format.format(monthly)
    }

    /** Trial length in days from an ISO-8601 billing period like "P7D" or "P1W". */
    fun trialDays(billingPeriod: String): Int = try {
        val p = Period.parse(billingPeriod)
        p.days + p.months * 30 + p.years * 365
    } catch (e: Exception) {
        0
    }
}
