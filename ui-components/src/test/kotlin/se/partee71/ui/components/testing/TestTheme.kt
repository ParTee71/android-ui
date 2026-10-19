package se.partee71.ui.components.testing

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import se.partee71.ui.theme.AppShapes
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.ExtendedColors
import se.partee71.ui.theme.Tone
import se.partee71.ui.theme.ToneColors

/** En ton som bara testtemat har – visar att en app kan lägga till egna toner. */
val Tone.Companion.Green get() = Tone("green")

val Tone.Companion.Blue get() = Tone("blue")

/**
 * Testtemat: ReseApotekets palett, former och typskala (med systemtypsnittet – typsnitten stannar i
 * appen). Bara för bibliotekets tester och skärmdumpar; varje app har sitt eget tema.
 */
object TestPalette {
    val light: ColorScheme = lightColorScheme(
        primary = Color(0xFFBF4A2A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE6DA),
        onPrimaryContainer = Color(0xFF7A2E18),
        inversePrimary = Color(0xFFFFB59C),
        secondary = Color(0xFF6B5A50),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE6DA),
        onSecondaryContainer = Color(0xFF7A2E18),
        tertiary = Color(0xFF2C6584),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE2EFF6),
        onTertiaryContainer = Color(0xFF1F4C66),
        background = Color(0xFFFFF4EE),
        onBackground = Color(0xFF241B16),
        surface = Color(0xFFFFF4EE),
        onSurface = Color(0xFF241B16),
        surfaceVariant = Color(0xFFF6E6DD),
        onSurfaceVariant = Color(0xFF6B5A50),
        surfaceTint = Color(0xFFBF4A2A),
        inverseSurface = Color(0xFF3A2E28),
        inverseOnSurface = Color(0xFFFBEEE7),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF8C1D18),
        outline = Color(0xFF8C7B71),
        outlineVariant = Color(0xFFEADDD5),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFF4EE),
        surfaceDim = Color(0xFFEBDDD5),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFDF1EA),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFFFFFFF),
        surfaceContainerHighest = Color(0xFFF6E6DD)
    )

    val dark: ColorScheme = darkColorScheme(
        primary = Color(0xFFFFB59C),
        onPrimary = Color(0xFF5E1703),
        primaryContainer = Color(0xFF6E2A15),
        onPrimaryContainer = Color(0xFFFFDBCD),
        inversePrimary = Color(0xFFBF4A2A),
        secondary = Color(0xFFCDBBB1),
        onSecondary = Color(0xFF33271F),
        secondaryContainer = Color(0xFF6E2A15),
        onSecondaryContainer = Color(0xFFFFDBCD),
        tertiary = Color(0xFF9BCBE8),
        onTertiary = Color(0xFF0E3448),
        tertiaryContainer = Color(0xFF1D3A4B),
        onTertiaryContainer = Color(0xFFC6E5F7),
        background = Color(0xFF1C1512),
        onBackground = Color(0xFFF3E6DF),
        surface = Color(0xFF1C1512),
        onSurface = Color(0xFFF3E6DF),
        surfaceVariant = Color(0xFF33271F),
        onSurfaceVariant = Color(0xFFCDBBB1),
        surfaceTint = Color(0xFFFFB59C),
        inverseSurface = Color(0xFFF3E6DF),
        inverseOnSurface = Color(0xFF33271F),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF601410),
        onErrorContainer = Color(0xFFF9DEDC),
        outline = Color(0xFF9E8C82),
        outlineVariant = Color(0xFF4A3B33),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF3A2E28),
        surfaceDim = Color(0xFF1C1512),
        surfaceContainerLowest = Color(0xFF17110E),
        surfaceContainerLow = Color(0xFF33271F),
        surfaceContainer = Color(0xFF2A201B),
        surfaceContainerHigh = Color(0xFF2A201B),
        surfaceContainerHighest = Color(0xFF3A2E28)
    )

    private val accents = listOf(
        Color(0xFFD97757),
        Color(0xFF3B7EA1),
        Color(0xFF7A9A3C),
        Color(0xFF8E5BA8),
        Color(0xFFC9892A),
        Color(0xFF3E8E7E),
        Color(0xFFB8527A),
        Color(0xFF5B6BB5)
    )

    val lightExtended = ExtendedColors(
        card = Color(0xFFFFFFFF),
        rowTint = Color(0xFFFDF1EA),
        toolbar = Color(0xFF241B16),
        onToolbar = Color(0xFFF3E6DF),
        tones = mapOf(
            Tone.Primary to ToneColors(Color(0xFFFFE6DA), Color(0xFF7A2E18)),
            Tone.Green to ToneColors(Color(0xFFE3F0D6), Color(0xFF3F5A1C)),
            Tone.Blue to ToneColors(Color(0xFFE2EFF6), Color(0xFF1F4C66))
        ),
        accents = accents
    )

    val darkExtended = ExtendedColors(
        card = Color(0xFF2A201B),
        rowTint = Color(0xFF33271F),
        toolbar = Color(0xFFF3E6DF),
        onToolbar = Color(0xFF33271F),
        tones = mapOf(
            Tone.Primary to ToneColors(Color(0xFF6E2A15), Color(0xFFFFDBCD)),
            Tone.Green to ToneColors(Color(0xFF34451F), Color(0xFFD2EDB0)),
            Tone.Blue to ToneColors(Color(0xFF1D3A4B), Color(0xFFC6E5F7))
        ),
        accents = accents
    )

    val shapes = AppShapes(cardRadius = 28.dp)

    private fun heading(size: Int, line: Int, spacing: Double = 0.0) = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.em)

    private fun sans(size: Int, line: Int, weight: FontWeight) = TextStyle(fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

    val typeScale = Typography(
        displaySmall = heading(34, 38, -0.015),
        headlineMedium = heading(28, 32),
        headlineSmall = heading(22, 28),
        titleLarge = heading(18, 24),
        titleMedium = sans(16, 22, FontWeight.Bold),
        titleSmall = sans(14, 20, FontWeight.Bold),
        bodyLarge = sans(16, 24, FontWeight.Normal),
        bodyMedium = sans(14, 20, FontWeight.Normal),
        bodySmall = sans(13, 18, FontWeight.SemiBold),
        labelLarge = heading(15, 20),
        labelMedium = sans(13, 16, FontWeight.Bold),
        labelSmall = sans(12, 16, FontWeight.Bold).copy(letterSpacing = 0.05.em)
    )
}

/** [AppTheme] med testpaletten, ljust eller mörkt. */
@Composable
fun TestTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    AppTheme(
        colorScheme = if (darkTheme) TestPalette.dark else TestPalette.light,
        extendedColors = if (darkTheme) TestPalette.darkExtended else TestPalette.lightExtended,
        shapes = TestPalette.shapes,
        typeScale = TestPalette.typeScale,
        content = content
    )
}
