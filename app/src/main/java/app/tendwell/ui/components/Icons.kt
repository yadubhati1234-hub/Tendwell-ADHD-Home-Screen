package app.tendwell.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons from the design spec (section 8): 24x24, stroke 1.9, round caps.
 * Circles are written as two arcs so every icon is plain path data. The
 * stroke colour is replaced by the tint at draw time.
 */
object TwIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"

    private fun icon(name: String, vararg paths: String, stroke: Float = 1.9f) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Search = icon("search", circle(11f, 11f, 6.5f), "M20 20l-4.2-4.2")
    val Settings = icon("settings", "M4 7h9M18 7h2M4 17h4M12 17h8", circle(15.5f, 7f, 2.25f), circle(9.5f, 17f, 2.25f))
    val Close = icon("close", "M6 6l12 12M18 6L6 18")
    val ChevronLeft = icon("chevron-left", "M15 5l-7 7 7 7")
    val ChevronUp = icon("chevron-up", "M5 15l7-7 7 7")
    val ChevronDown = icon("chevron-down", "M5 9l7 7 7-7")
    val ChevronRight = icon("chevron-right", "M9 5l7 7-7 7")
    val ArrowRight = icon("arrow-right", "M5 12h14M13 6l6 6-6 6")
    val Bars = icon("bars", "M5 20V11M12 20V5M19 20v-6")
    val Shield = icon("shield", "M12 3.5l7 3v5c0 4.5-3 7.8-7 9-4-1.2-7-4.5-7-9v-5z", "M9 12l2 2 4-4")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val Home = icon("home", "M4.5 11L12 4.5l7.5 6.5V19a1.5 1.5 0 0 1-1.5 1.5h-12A1.5 1.5 0 0 1 4.5 19z")
    val Info = icon("info", circle(12f, 12f, 8f), "M12 11v5", "M12 8h.01")
    val Minus = icon("minus", "M5 12h14")
    val Pause = icon("pause", "M9 6v12M15 6v12", stroke = 2.4f)
    val Clock = icon("clock", circle(12f, 12f, 8f), "M12 8v4.5l3 2")
    val Calendar = icon("calendar", "M7 5.5h10a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-8a3 3 0 0 1 3-3z", "M4 10h16M8.5 3.5v4M15.5 3.5v4")
    val Bell = icon("bell", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2.2 2.2 0 0 0 4 0")
    val Target = icon("target", circle(12f, 12f, 8f), circle(12f, 12f, 3.5f))
    val Hourglass = icon("hourglass", "M7 4h10M7 20h10M8 4v2.5c0 2 4 3.5 4 5.5s-4 3.5-4 5.5V20M16 4v2.5c0 2-4 3.5-4 5.5s4 3.5 4 5.5V20")
    val Undo = icon("undo", "M9 14L4 9l5-5", "M4 9h10a6 6 0 0 1 0 12h-3")
    val Moon = icon("moon", "M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z")
    val Sprout = icon("sprout", "M12 20v-7", "M12 13c0-4 3-6.5 7-6.5 0 4-3 6.5-7 6.5z", "M12 15.5c0-3-2.3-5-5.5-5 0 3 2.3 5 5.5 5z")

    /** The filled play triangle (no stroke). */
    val Play: ImageVector = ImageVector.Builder("play", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes("M8 5.8v12.4a1 1 0 0 0 1.5.86l10-6.2a1 1 0 0 0 0-1.72l-10-6.2A1 1 0 0 0 8 5.8z"),
            fill = SolidColor(Color.Black),
        )
    }.build()
}
