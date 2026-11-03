package app.tendwell.billing

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tendwell.onboarding.FeatureRow
import app.tendwell.onboarding.OnboardingPage
import app.tendwell.onboarding.OnboardingTitle
import app.tendwell.onboarding.PrimaryPill
import app.tendwell.onboarding.QuietButton
import app.tendwell.onboarding.StepDots
import app.tendwell.data.Prefs
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.ProBadge
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.TwSecondaryButton
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

const val PRIVACY_URL = "https://yadubhati1234-hub.github.io/Tendwell-ADHD-Home-Screen/privacy/"
const val TERMS_URL = "https://yadubhati1234-hub.github.io/Tendwell-ADHD-Home-Screen/terms/"

/**
 * The Pro paywall wired to Google Play. [step] is 5 during onboarding (shows
 * the step dots) and null when opened from Settings or a pause limit.
 * [onDone] runs after Close, a purchase or a restore. [onShown] runs once each time the
 * paywall opens (not again when the screen rotates), for analytics.
 */
@Composable
fun PaywallRoute(billing: BillingRepository, step: Int?, onDone: () -> Unit, onShown: () -> Unit = {}) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val store by billing.store.collectAsStateWithLifecycle()
    val event by billing.events.collectAsStateWithLifecycle()
    var restoring by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var shownNoted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!shownNoted) {
            shownNoted = true
            onShown()
        }
        billing.loadOffers()
    }
    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        billing.consumeEvent()
        restoring = false
        when (e) {
            StoreEvent.Purchased, StoreEvent.Restored -> {
                message = if (e == StoreEvent.Purchased) "Pro is on. Thank you." else "Pro restored"
                delay(1200)
                onDone()
            }
            StoreEvent.Pending -> message = "Payment pending. Pro turns on when it clears."
            StoreEvent.RestoredNothing -> message = "No past purchase on this Google account"
            StoreEvent.Failed -> message = "Google Play didn't respond. Try again in a moment."
            StoreEvent.Cancelled -> Unit
        }
    }

    PaywallScreen(
        store = store,
        step = step,
        today = remember { LocalDate.now() },
        restoring = restoring,
        message = message,
        onMessageShown = { message = null },
        onClose = onDone,
        onRestore = {
            if (!restoring) {
                restoring = true
                billing.refreshPurchases(reportRestore = true)
            }
        },
        onRetry = billing::loadOffers,
        onBuy = { offer ->
            val ok = activity != null && billing.launch(activity, offer)
            if (!ok) message = "Google Play didn't respond. Try again in a moment."
        },
        onOpenLink = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        onAccessCode = { code ->
            val ok = billing.redeemAccessCode(code)
            if (ok) {
                message = "Pro is on"
                scope.launch {
                    delay(1200)
                    onDone()
                }
            }
            ok
        },
    )
}

@Composable
fun PaywallScreen(
    store: StoreState,
    step: Int?,
    today: LocalDate,
    restoring: Boolean,
    message: String?,
    onMessageShown: () -> Unit,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onBuy: (PlanOffer) -> Unit,
    onOpenLink: (url: String) -> Unit,
    onAccessCode: suspend (String) -> Boolean,
) {
    val c = Tendwell.colors
    val offers = (store as? StoreState.Ready)?.offers.orEmpty()
    var codeOpen by rememberSaveable { mutableStateOf(false) }
    var chosenName by rememberSaveable { mutableStateOf(ProPlan.Yearly.name) }
    val chosen = offers[ProPlan.valueOf(chosenName)] ?: ProPlan.entries.firstNotNullOfOrNull { offers[it] }

    LaunchedEffect(message) {
        if (message != null) {
            delay(4000)
            onMessageShown()
        }
    }

    Box {
        OnboardingPage {
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                TwIconButton(
                    TwIcons.Close,
                    contentDescription = "Not now",
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-10).dp),
                )
                if (step != null) StepDots(step)
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !restoring, role = Role.Button, onClick = onRestore)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (restoring) "Checking…" else "Restore", style = TendwellType.secondary, color = c.ink2)
                }
            }

            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProBadge()
                OnboardingTitle(PlanText.headline(chosen ?: DEFAULT_YEARLY))
            }

            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(TwIcons.Hourglass, c.peach, "Pause as many apps as you need", "Free covers ${Prefs.MAX_FREE_PAUSED}", bubbleSize = 36.dp, titleSize = 15, supportingColor = c.ink3)
                FeatureRow(TwIcons.Clock, c.lilac, "Focus hours", "Longer pauses while you work, like 9:00 → 17:00", bubbleSize = 36.dp, titleSize = 15, supportingColor = c.ink3)
                FeatureRow(TwIcons.Bars, c.mint, "Weekly insights", "Your opens and the times you drift", bubbleSize = 36.dp, titleSize = 15, supportingColor = c.ink3)
            }

            Spacer(Modifier.weight(1f))

            when (store) {
                is StoreState.Ready -> Column(
                    Modifier.padding(top = 16.dp).selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProPlan.entries.forEach { plan ->
                        offers[plan]?.let { offer ->
                            PlanCard(offer, selected = offer.plan == chosen?.plan, onPick = { chosenName = plan.name })
                        }
                    }
                }
                StoreState.Loading -> StoreNote("Loading plans from Google Play…", onRetry = null)
                StoreState.Unavailable -> StoreNote("Google Play couldn't load the plans. Check your connection and try again.", onRetry = onRetry)
            }

            PrimaryPill(
                text = chosen?.let(PlanText::cta) ?: "Start free trial",
                onClick = { chosen?.let(onBuy) },
                enabled = chosen != null,
                modifier = Modifier.padding(top = 16.dp),
            )

            Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 57.dp).semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.Center,
            ) {
                if (chosen != null) {
                    val terms = TendwellType.caption.copy(lineHeight = 1.45.em, textAlign = TextAlign.Center)
                    Text(PlanText.termsLead(chosen), style = terms, color = c.ink2, modifier = Modifier.fillMaxWidth())
                    if (chosen.plan != ProPlan.Lifetime) {
                        val text = if (chosen.trialDays > 0) {
                            buildAnnotatedString {
                                append("Cancel any time in Google Play ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) {
                                    append("before ${PlanText.cancelBy(today, chosen.trialDays)}")
                                }
                                append(" and you won't be charged.")
                            }
                        } else {
                            buildAnnotatedString { append("Cancel any time in Google Play.") }
                        }
                        Text(text, style = terms, color = c.ink2, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (chosen != null) {
                Row(
                    Modifier.padding(top = 6.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(TwIcons.Shield, contentDescription = null, tint = c.ink2, modifier = Modifier.size(16.dp))
                    Text(PlanText.trust(chosen), style = TendwellType.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FooterLink("Terms", onClick = { onOpenLink(TERMS_URL) })
                FooterLink("Privacy", onClick = { onOpenLink(PRIVACY_URL) })
                FooterLink("Have an access code?", onClick = { codeOpen = true })
            }
        }

        // Short confirmations ("No past purchase on this Google account") under Restore.
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 24.dp, start = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.ink)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(message.orEmpty(), style = TendwellType.caption.copy(fontWeight = FontWeight.SemiBold), color = c.bg)
            }
        }
    }

    if (codeOpen) {
        AccessCodeDialog(
            onDismiss = { codeOpen = false },
            onSubmit = { code -> onAccessCode(code).also { ok -> if (ok) codeOpen = false } },
        )
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TendwellType.caption.copy(fontWeight = FontWeight.SemiBold), color = Tendwell.colors.ink3)
    }
}

/**
 * Where Google Play's reviewers type the access code from Play Console's
 * "Sign in details" (see [AccessCode]). Anyone else simply has no code.
 */
@Composable
private fun AccessCodeDialog(onDismiss: () -> Unit, onSubmit: suspend (String) -> Boolean) {
    val c = Tendwell.colors
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val submit: () -> Unit = {
        if (code.isNotBlank() && !checking) {
            checking = true
            scope.launch {
                val ok = onSubmit(code)
                checking = false
                if (!ok) wrong = true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.bg)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Access code", style = TendwellType.cardTitle, color = c.ink, modifier = Modifier.semantics { heading() })
            Text("If you were given a code, enter it to turn on Pro.", style = TendwellType.small, color = c.ink2)
            val shape = RoundedCornerShape(16.dp)
            val fieldStyle = TendwellType.body.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(shape)
                    .background(c.surface)
                    .border(1.5.dp, if (wrong) c.ink else c.line, shape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (code.isEmpty()) Text("XXXX-XXXX-XXXX-XXXX", style = fieldStyle, color = c.ink3)
                BasicTextField(
                    value = code,
                    onValueChange = {
                        code = it.take(32)
                        wrong = false
                    },
                    singleLine = true,
                    textStyle = fieldStyle.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .semantics { contentDescription = "Access code" },
                )
            }
            if (wrong) {
                Text(
                    "That code didn't work. Check it and try again.",
                    style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PrimaryPill(
                text = if (checking) "Checking…" else "Turn on Pro",
                onClick = submit,
                enabled = code.isNotBlank() && !checking,
                modifier = Modifier.padding(top = 4.dp),
            )
            QuietButton("Cancel", onClick = onDismiss)
        }
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
    }
}

/** Shown in the headline before Play answers, so it does not jump from a blank title. */
private val DEFAULT_YEARLY = PlanOffer(ProPlan.Yearly, "", 0, "USD", 7, null)

@Composable
private fun PlanCard(offer: PlanOffer, selected: Boolean, onPick: () -> Unit) {
    val c = Tendwell.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.primarySoft else c.surface)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) c.primary else c.line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onPick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .border(2.dp, if (selected) c.primary else c.ink3, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.primary))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(PlanText.priceLine(offer), style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.25.em), color = c.ink)
                Text(PlanText.subLine(offer), style = TendwellType.caption.copy(lineHeight = 1.35.em), color = c.ink2)
            }
        }
        if (offer.plan == ProPlan.Yearly) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.primary)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("BEST VALUE", style = TendwellType.eyebrow.copy(fontSize = 11.sp, letterSpacing = 0.8.sp), color = c.onPrimary)
            }
        }
    }
}

@Composable
private fun StoreNote(text: String, onRetry: (() -> Unit)?) {
    val c = Tendwell.colors
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.5.dp, c.line, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, style = TendwellType.small, color = c.ink2, textAlign = TextAlign.Center)
        if (onRetry != null) TwSecondaryButton("Try again", onClick = onRetry)
    }
}
