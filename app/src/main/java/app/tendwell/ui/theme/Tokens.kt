package app.tendwell.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from the design spec (section 5). The names match the
 * mockups' tokens() so screens can be built straight from the canvas.
 * Default accent is sage; the time colour is lavender.
 */
@Immutable
data class TendwellColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val past: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val cardLine: Color,
    val scrim: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val primaryLine: Color,
    val time: Color,
    val timeSoft: Color,
    val onTimeSoft: Color,
    val peach: Color,
    val lilac: Color,
    val mint: Color,
    val sky: Color,
    val butter: Color,
)

val LightColors = TendwellColors(
    isDark = false,
    bg = Color(0xFFF3F5F1),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE8ECE6),
    line = Color(0xFFDCE2DA),
    past = Color(0xFFCBD3C9),
    ink = Color(0xFF17201A),
    ink2 = Color(0xFF44514A),
    ink3 = Color(0xFF5F6B64),
    cardLine = Color(0x0D17201A),
    scrim = Color(0x6617201A),
    primary = Color(0xFF2E6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFDDEBE2),
    onPrimarySoft = Color(0xFF1E4D37),
    primaryLine = Color(0x422E6A4F),
    time = Color(0xFF6B58D6),
    timeSoft = Color(0xFFE9E5FB),
    onTimeSoft = Color(0xFF4433A6),
    peach = Color(0xFFFBE3D2),
    lilac = Color(0xFFE8E2FB),
    mint = Color(0xFFDDF0E4),
    sky = Color(0xFFDCEBFA),
    butter = Color(0xFFF7ECC6),
)

val DarkColors = TendwellColors(
    isDark = true,
    bg = Color(0xFF0D1210),
    surface = Color(0xFF161D19),
    surface2 = Color(0xFF202923),
    line = Color(0xFF29332D),
    past = Color(0xFF34403A),
    ink = Color(0xFFECF1ED),
    ink2 = Color(0xFFB7C3BB),
    ink3 = Color(0xFF8E9B93),
    cardLine = Color(0xFF222B26),
    scrim = Color(0x99000000),
    primary = Color(0xFF8FCDAA),
    onPrimary = Color(0xFF0D1A13),
    primarySoft = Color(0xFF1C3327),
    onPrimarySoft = Color(0xFFA9DEC0),
    primaryLine = Color(0x478FCDAA),
    time = Color(0xFFA99BFF),
    timeSoft = Color(0xFF262044),
    onTimeSoft = Color(0xFFCEC6FF),
    peach = Color(0xFF3A2A20),
    lilac = Color(0xFF2A2546),
    mint = Color(0xFF1D3328),
    sky = Color(0xFF1B2A3D),
    butter = Color(0xFF36311E),
)

val LocalTendwellColors = staticCompositionLocalOf { LightColors }
