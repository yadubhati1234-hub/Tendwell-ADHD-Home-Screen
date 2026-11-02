package app.tendwell.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

const val ONBOARDING_STEPS = 5

/** Row of 5 step indicators: current is a 22dp pill, done and upcoming are 8dp dots. */
@Composable
fun StepDots(step: Int, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Row(
        modifier.semantics { contentDescription = "Step $step of $ONBOARDING_STEPS" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 1..ONBOARDING_STEPS) {
            Box(
                Modifier
                    .size(width = if (i == step) 22.dp else 8.dp, height = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (i <= step) c.primary else c.line),
            )
        }
    }
}

/** The 44dp top bar of an onboarding step: back on the left, step dots in the middle. */
@Composable
fun OnboardingTopBar(step: Int, onBack: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
        if (onBack != null) {
            TwIconButton(TwIcons.ChevronLeft, contentDescription = "Back", onClick = onBack, modifier = Modifier.offset(x = (-10).dp))
        }
        StepDots(step, Modifier.align(Alignment.Center))
    }
}

/**
 * Full-screen onboarding page. The body scrolls on short screens, and on
 * tall ones a weighted spacer in [content] pushes the footer to the bottom.
 */
@Composable
fun OnboardingPage(content: @Composable ColumnScope.() -> Unit) {
    val c = Tendwell.colors
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding(),
    ) {
        val minHeight = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            content = content,
        )
    }
}

@Composable
fun OnboardingTitle(text: String, modifier: Modifier = Modifier, size: Int = 34) {
    Text(
        text,
        style = TendwellType.h1.copy(
            fontSize = size.sp,
            lineHeight = if (size > 36) 1.08.em else 1.1.em,
            letterSpacing = if (size > 36) (-0.6).sp else (-0.5).sp,
        ),
        color = Tendwell.colors.ink,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun OnboardingLead(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TendwellType.body.copy(fontWeight = FontWeight.Medium),
        color = Tendwell.colors.ink2,
        modifier = modifier.padding(top = 10.dp),
    )
}

/** An icon in a soft coloured square, then a bold title and a supporting line. */
@Composable
fun FeatureRow(
    icon: ImageVector,
    bubble: Color,
    title: String,
    supporting: String,
    bubbleSize: Dp = 40.dp,
    titleSize: Int = 16,
    supportingColor: Color = Tendwell.colors.ink2,
) {
    val c = Tendwell.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = if (bubbleSize < 40.dp) Alignment.CenterVertically else Alignment.Top) {
        Box(
            Modifier
                .size(bubbleSize)
                .clip(RoundedCornerShape(if (bubbleSize < 40.dp) 12.dp else 14.dp))
                .background(bubble),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(if (bubbleSize < 40.dp) 18.dp else 20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = TendwellType.body.copy(fontSize = titleSize.sp, fontWeight = if (titleSize < 16) FontWeight.SemiBold else FontWeight.Bold, lineHeight = 1.35.em),
                color = c.ink,
            )
            Text(
                supporting,
                style = TendwellType.small.copy(fontSize = if (titleSize < 16) 13.sp else 14.sp, lineHeight = 1.45.em),
                color = supportingColor,
            )
        }
    }
}

/** 56dp primary pill. When [enabled] is false it turns grey and ignores taps. */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val c = Tendwell.colors
    val fg = if (enabled) c.onPrimary else c.ink3
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) c.primary else c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (leading != null) Icon(leading, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = TendwellType.button.copy(fontSize = 16.sp), color = fg)
        if (trailing != null) Icon(trailing, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

/** 44dp text-only button, like "Not now". */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TendwellType.secondary, color = Tendwell.colors.ink2)
    }
}

@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TendwellType.caption.copy(lineHeight = 1.4.em),
        color = Tendwell.colors.ink3,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The small uppercase "Pro" badge in the time colour. */
@Composable
fun ProBadge(background: Color = Tendwell.colors.timeSoft) {
    val c = Tendwell.colors
    Box(
        Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("PRO", style = TendwellType.eyebrow.copy(fontSize = 11.sp, letterSpacing = 0.8.sp), color = c.onTimeSoft)
    }
}
