package se.partee71.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Temat som komponenterna läser: Material 3 Expressive med appens [colorScheme], [typeScale] och
 * [motionScheme], plus appens [extendedColors], [shapes] och namngivna [typography]. Appen väljer
 * ljust eller mörkt och skickar in rätt färger; biblioteket har inga egna färger eller typsnitt.
 */
@Composable
fun AppTheme(
    colorScheme: ColorScheme,
    extendedColors: ExtendedColors,
    shapes: AppShapes,
    typeScale: Typography,
    typography: AppTypography = remember(typeScale) { AppTypography.from(typeScale) },
    motionScheme: MotionScheme = MotionScheme.expressive(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalAppShapes provides shapes,
        LocalAppTypography provides typography
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            typography = typeScale,
            content = content
        )
    }
}

/** Det aktuella temat, för komponenter och feature-kod. */
object AppTheme {
    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable
        get() = LocalAppShapes.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    /** Färgerna för en [Tone] i aktuellt tema. */
    @Composable
    @ReadOnlyComposable
    fun tone(tone: Tone): ToneColors = extendedColors.tone(tone)
}

private fun missing(what: String): Nothing = error("$what saknas – lägg innehållet i AppTheme(…)")

internal val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { missing("ExtendedColors") }
internal val LocalAppShapes = staticCompositionLocalOf<AppShapes> { missing("AppShapes") }
internal val LocalAppTypography = staticCompositionLocalOf<AppTypography> { missing("AppTypography") }
