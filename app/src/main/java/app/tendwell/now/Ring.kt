package app.tendwell.now

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** A countdown ring: the coloured arc is the time left, starting at 12 o'clock. */
@Composable
fun TimerRing(
    fractionLeft: Float,
    track: Color,
    progress: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    trackAlpha: Float = 1f,
    progressAlpha: Float = 1f,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, alpha = trackAlpha, style = Stroke(stroke))
        val sweep = 360f * fractionLeft.coerceIn(0f, 1f)
        if (sweep > 0f) {
            drawArc(progress, -90f, sweep, false, Offset(inset, inset), arcSize, alpha = progressAlpha, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}
