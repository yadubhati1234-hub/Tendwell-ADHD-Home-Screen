package app.tendwell.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Fraunces
import app.tendwell.ui.theme.Tendwell

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** Step 1: what Tendwell does. */
@Composable
fun WelcomeStep(onStart: () -> Unit) {
    val c = Tendwell.colors
    OnboardingPage {
        OnboardingTopBar(step = 1, onBack = null)
        Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
            SproutClock(Modifier.size(216.dp))
        }
        OnboardingTitle("A calm home screen for ADHD brains.", Modifier.padding(top = 20.dp), size = 39)
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureRow(TwIcons.Clock, c.lilac, "See your time", "A big clock and the time left until your next event.")
            FeatureRow(TwIcons.Target, c.mint, "Do one thing", "One task for right now, with a visual timer.")
            FeatureRow(TwIcons.Hourglass, c.peach, "Scroll less", "A 10-second pause before the apps that eat your day.")
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryPill("Get started", onClick = onStart, trailing = TwIcons.ArrowRight)
            Footnote("Takes about 2 minutes. Everything stays on your phone.")
        }
    }
}

/** Step 4: ask Android to make Tendwell the Home app. */
@Composable
fun SetHomeStep(isDefaultHome: Boolean, onBack: () -> Unit, onSetHome: () -> Unit, onNext: () -> Unit) {
    val c = Tendwell.colors
    OnboardingPage {
        OnboardingTopBar(step = 4, onBack = onBack)
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            PhoneWithHome(Modifier.size(width = 240.dp, height = 232.dp))
        }
        if (isDefaultHome) {
            OnboardingTitle("It's your home screen", Modifier.padding(top = 20.dp))
            OnboardingLead("Tendwell is set as your Home screen. Press Home any time to come back here.")
        } else {
            OnboardingTitle("Make it your home screen", Modifier.padding(top = 20.dp))
            OnboardingLead("Android will ask which app to use as your Home screen. Choose this one, then tap “Set as default”.")
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureRow(TwIcons.Undo, c.mint, "Switch back any time", "Settings has a one-tap way back to your old launcher.")
            FeatureRow(TwIcons.Shield, c.sky, "Nothing leaves your phone", "No account, no tracking of what you do in apps.")
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isDefaultHome) {
                PrimaryPill("Continue", onClick = onNext)
            } else {
                PrimaryPill("Set as home screen", onClick = onSetHome, leading = TwIcons.Home)
                QuietButton("Not now", onClick = onNext)
            }
        }
    }
}

/** The welcome illustration: a sprout inside a time ring that sweeps in. Decorative. */
@Composable
private fun SproutClock(modifier: Modifier) {
    val c = Tendwell.colors
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1400, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f))) }
    val stem = remember { path("M120 160V114") }
    val leafRight = remember { path("M120 116C120 92 138 76 162 76C162 100 144 116 120 116Z") }
    val leafLeft = remember { path("M120 130C120 112 107 100 89 100C89 118 102 130 120 130Z") }
    val veins = remember { path("M125 111L152 86M115 125L96 107") }
    Canvas(modifier) {
        val u = size.width / 240f
        scale(u, u, pivot = Offset.Zero) {
            drawCircle(c.peach, 6f, Offset(18f, 36f))
            drawCircle(c.butter, 4.5f, Offset(224f, 42f))
            drawCircle(c.sky, 6.5f, Offset(214f, 212f))
            drawCircle(c.timeSoft, 104f, Offset(120f, 120f), style = Stroke(6f))
            // 444 of the ring's 653.5 length, from the top, clockwise.
            drawArc(c.time, -90f, 244.6f * sweep.value, false, Offset(16f, 16f), Size(208f, 208f), style = Stroke(6f, cap = StrokeCap.Round))
            if (sweep.value > 0.8f) {
                drawCircle(c.bg, 9.25f, Offset(25.9f, 164.3f))
                drawCircle(c.time, 5.75f, Offset(25.9f, 164.3f))
            }
            drawCircle(c.primarySoft, 86f, Offset(120f, 120f))
            drawOval(c.primaryLine, Offset(86f, 155f), Size(68f, 12f))
            drawPath(stem, c.primary, style = Stroke(5f, cap = StrokeCap.Round))
            drawPath(leafRight, c.primary)
            drawPath(leafLeft, c.primary)
            drawPath(veins, c.primarySoft, alpha = 0.7f, style = Stroke(2f, cap = StrokeCap.Round))
        }
    }
}

/** The set-home illustration: a small Tendwell home screen above a pulsing Home button. Decorative. */
@Composable
private fun PhoneWithHome(modifier: Modifier) {
    val c = Tendwell.colors
    val measurer = rememberTextMeasurer()
    val pulse = rememberInfiniteTransition(label = "ripple")
    val ripple by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "ripple")
    val homeIcon = remember { path("M4.5 11L12 4.5l7.5 6.5V19a1.5 1.5 0 0 1-1.5 1.5h-12A1.5 1.5 0 0 1 4.5 19z") }
    val clockStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(300), fontSize = 26.sp, letterSpacing = (-1).sp)
    Canvas(modifier) {
        val u = size.width / 240f
        val clock = measurer.measure("10:24", clockStyle.copy(fontSize = (26f * u).toSp(), letterSpacing = (-1f * u).toSp()))
        scale(u, u, pivot = Offset.Zero) {
            drawCircle(c.peach, 6f, Offset(38f, 52f))
            drawCircle(c.butter, 4.5f, Offset(206f, 34f))
            drawCircle(c.sky, 6.5f, Offset(200f, 150f))
            roundRect(c.surface, 74f, 2f, 92f, 184f, 22f)
            roundRect(c.line, 74f, 2f, 92f, 184f, 22f, stroke = 3f)
        }
        // Text is measured in real pixels, so it is drawn outside the scaled block.
        drawText(clock, color = c.ink, topLeft = Offset(85f * u, 40f * u - clock.firstBaseline))
        scale(u, u, pivot = Offset.Zero) {
            roundRect(c.surface2, 85f, 49f, 70f, 4f, 2f)
            roundRect(c.past, 85f, 49f, 15f, 4f, 2f)
            drawRect(c.time, Offset(100f, 49f), Size(3.5f, 4f))
            drawRect(c.time, Offset(111f, 49f), Size(4.5f, 4f), alpha = 0.45f)
            drawRect(c.time, Offset(133f, 49f), Size(4.5f, 4f), alpha = 0.45f)
            roundRect(c.ink, 99f, 46f, 2f, 10f, 1f)
            roundRect(c.primarySoft, 83f, 63f, 74f, 38f, 10f)
            roundRect(c.primaryLine, 83f, 63f, 74f, 38f, 10f, stroke = 1.5f)
            drawCircle(c.primaryLine, 6.5f, Offset(96f, 82f), style = Stroke(3f))
            drawArc(c.primary, -90f, 264f, false, Offset(89.5f, 75.5f), Size(13f, 13f), style = Stroke(3f, cap = StrokeCap.Round))
            roundRect(c.ink, 108f, 76f, 38f, 5f, 2.5f, alpha = 0.85f)
            roundRect(c.onPrimarySoft, 108f, 85f, 24f, 4f, 2f, alpha = 0.6f)
            roundRect(c.ink, 85f, 116f, 30f, 6f, 3f, alpha = 0.6f)
            roundRect(c.ink, 85f, 129f, 44f, 6f, 3f, alpha = 0.6f)
            roundRect(c.ink, 85f, 142f, 26f, 6f, 3f, alpha = 0.6f)
            roundRect(c.surface2, 83f, 162f, 74f, 12f, 6f)
            drawCircle(c.primary, 18f * (1f + 0.6f * ripple), Offset(120f, 211f), alpha = 0.6f * (1f - ripple), style = Stroke(2f))
            drawCircle(c.surface, 18f, Offset(120f, 211f))
            drawCircle(c.primary, 18f, Offset(120f, 211f), style = Stroke(2.5f))
            translate(110.5f, 201.5f) {
                scale(0.79f, 0.79f, pivot = Offset.Zero) {
                    drawPath(homeIcon, c.primary, style = Stroke(2.6f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
        }
    }
}

private fun DrawScope.roundRect(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float, stroke: Float = 0f, alpha: Float = 1f) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
        alpha = alpha,
        style = if (stroke > 0f) Stroke(stroke) else androidx.compose.ui.graphics.drawscope.Fill,
    )
}
