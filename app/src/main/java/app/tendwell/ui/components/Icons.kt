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
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val Home = icon("home", "M4.5 11L12 4.5l7.5 6.5V19a1.5 1.5 0 0 1-1.5 1.5h-12A1.5 1.5 0 0 1 4.5 19z")
    val Info = icon("info", circle(12f, 12f, 8f), "M12 11v5", "M12 8h.01")
    val Minus = icon("minus", "M5 12h14")
}
