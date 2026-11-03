package app.tendwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tendwell.ui.theme.Tendwell

/**
 * Tablet and landscape layout rules, matching the store's tablet screenshots.
 *
 * Home, the pause, the focus timer and insights switch to two columns when
 * the window is at least 600 dp wide and wider than it is tall: a tablet or
 * an unfolded foldable held sideways, or a phone turned sideways. Everywhere
 * else screens keep one column, no wider than [ReadableWidth] and centred.
 */
object LayoutRules {
    const val WIDE_MIN_WIDTH_DP = 600f

    fun isWide(widthDp: Float, heightDp: Float): Boolean = widthDp >= WIDE_MIN_WIDTH_DP && widthDp > heightDp
}

fun isWide(width: Dp, height: Dp): Boolean = LayoutRules.isWide(width.value, height.value)

/** The widest a single column gets, so one-column screens don't stretch across a tablet. */
val ReadableWidth: Dp = 600.dp

/** The widest the two columns get together, so they stay close on big tablets. */
val WideMaxWidth: Dp = 1000.dp

/** Centres a one-column screen at [ReadableWidth], with the page colour either side. */
@Composable
fun CenteredPage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Tendwell.colors.bg), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ReadableWidth).fillMaxHeight()) { content() }
    }
}
