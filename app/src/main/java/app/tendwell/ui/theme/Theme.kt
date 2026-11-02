package app.tendwell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun TendwellTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    // Material is only used for a few widgets (bottom sheet, ripples); map it to our tokens.
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, background = c.bg, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2, onSurfaceVariant = c.ink2,
            outline = c.line, scrim = c.scrim, surfaceContainerLow = c.surface,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, background = c.bg, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2, onSurfaceVariant = c.ink2,
            outline = c.line, scrim = c.scrim, surfaceContainerLow = c.surface,
        )
    }
    CompositionLocalProvider(LocalTendwellColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object Tendwell {
    val colors: TendwellColors
        @Composable @ReadOnlyComposable get() = LocalTendwellColors.current
}
