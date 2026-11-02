@file:OptIn(ExperimentalTextApi::class)

package app.tendwell.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tendwell.R

private fun fraunces(weight: Int, opticalSize: Float) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
        FontVariation.Setting("SOFT", 100f),
    ),
)

private fun jakarta(weight: Int) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Display serif. Weight 300 is used for the big clock and stats, so it gets the largest optical size. */
val Fraunces = FontFamily(
    fraunces(300, 144f),
    fraunces(400, 36f),
    fraunces(500, 36f),
)

/** UI sans, inherited by everything that is not a display serif. */
val Jakarta = FontFamily(
    jakarta(400),
    jakarta(500),
    jakarta(600),
    jakarta(700),
    jakarta(800),
)

object TendwellType {
    val clock = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(300), fontSize = 84.sp, lineHeight = 0.95.em, letterSpacing = (-3).sp)
    val h1 = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 1.1.em, letterSpacing = (-0.5).sp)
    val cardTitle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 1.2.em, letterSpacing = (-0.2).sp)
    val homeApp = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 26.sp, letterSpacing = (-0.3).sp)
    val body = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 1.5.em)
    val rowLabel = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val listApp = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    val secondary = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val small = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val caption = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val eyebrow = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp)
    val button = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}
