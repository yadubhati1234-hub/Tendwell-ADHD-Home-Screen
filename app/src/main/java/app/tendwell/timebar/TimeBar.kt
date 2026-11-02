package app.tendwell.timebar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.background
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * The time bar under the clock: what is next, time until bed, and the day as
 * a bar with past time, events and a "now" marker.
 */
@Composable
fun TimeBar(model: TimeBarModel, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = model.primaryLabel + ". " + model.description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.time))
                Text(
                    model.primaryLabel,
                    style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            model.secondaryLabel?.let {
                Text(it, style = TendwellType.caption, color = c.ink3, maxLines = 1)
            }
        }

        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val trackTop = 7.dp.toPx()
            val trackH = 10.dp.toPx()
            val w = size.width
            val track = Path().apply {
                addRoundRect(RoundRect(0f, trackTop, w, trackTop + trackH, CornerRadius(trackH / 2)))
            }
            clipPath(track) {
                drawRect(c.surface2, Offset(0f, trackTop), Size(w, trackH))
                drawRect(c.past, Offset(0f, trackTop), Size(w * model.nowFraction, trackH))
                val next = model.current ?: model.next
                model.events.forEach { e ->
                    val x0 = w * model.fraction(e.startMin)
                    val x1 = w * model.fraction(e.endMin)
                    if (x1 > x0) {
                        drawRect(
                            c.time,
                            Offset(x0, trackTop),
                            Size((x1 - x0).coerceAtLeast(3.dp.toPx()), trackH),
                            alpha = if (e == next) 1f else 0.45f,
                        )
                    }
                }
            }
            // "Now" marker: a short ink bar with a ring of background around it.
            val x = (w * model.nowFraction).coerceIn(2.dp.toPx(), w - 2.dp.toPx())
            val markerW = 4.dp.toPx()
            val ring = 3.dp.toPx()
            drawRoundRect(
                c.bg,
                Offset(x - markerW / 2 - ring, 0f),
                Size(markerW + 2 * ring, size.height),
                CornerRadius(markerW),
            )
            drawRoundRect(c.ink, Offset(x - markerW / 2, 0f), Size(markerW, size.height), CornerRadius(markerW / 2))
        }

        AxisLabels(model.axis)
    }
}

/** Places each label under its fraction of the bar, kept inside the edges. */
@Composable
private fun AxisLabels(marks: List<Pair<Float, String>>) {
    val c = Tendwell.colors
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            marks.forEach { (_, label) -> Text(label, style = TendwellType.caption.copy(fontSize = TendwellType.eyebrow.fontSize), color = c.ink3, maxLines = 1) }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { i, p ->
                val frac = marks[i].first
                val x = (frac * width - p.width / 2f).toInt().coerceIn(0, (width - p.width).coerceAtLeast(0))
                p.place(x, 0)
            }
        }
    }
}
