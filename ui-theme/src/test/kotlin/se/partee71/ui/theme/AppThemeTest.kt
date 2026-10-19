package se.partee71.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeTest {

    @get:Rule
    val rule = createComposeRule()

    private val green = Tone("green")
    private val primary = ToneColors(Color.White, Color.Black)
    private val greenColors = ToneColors(Color.Green, Color.Black)
    private val extended = ExtendedColors(
        card = Color.White,
        rowTint = Color.LightGray,
        toolbar = Color.Black,
        onToolbar = Color.White,
        tones = mapOf(Tone.Primary to primary, green to greenColors),
        accents = listOf(Color.Red, Color.Blue)
    )
    private val shapes = AppShapes(cardRadius = 22.dp)
    private val scale = Typography()

    @Test
    fun `temat ger komponenterna appens färger, former och stilar`() {
        lateinit var seen: Triple<ExtendedColors, AppShapes, AppTypography>
        var tone: ToneColors? = null
        var unknown: ToneColors? = null
        var materialPrimary: Color? = null
        rule.setContent {
            AppTheme(lightColorScheme(primary = Color.Magenta), extended, shapes, scale) {
                seen = Triple(AppTheme.extendedColors, AppTheme.shapes, AppTheme.typography)
                tone = AppTheme.tone(green)
                unknown = AppTheme.tone(Tone("okänd"))
                materialPrimary = MaterialTheme.colorScheme.primary
            }
        }
        rule.waitForIdle()
        assertSame(extended, seen.first)
        assertSame(shapes, seen.second)
        assertEquals(AppTypography.from(scale), seen.third)
        assertEquals(greenColors, tone)
        assertEquals(primary, unknown, "en ton appen inte gett färger får primärtonen")
        assertEquals(Color.Magenta, materialPrimary)
    }

    @Test
    fun `appen kan ge egna namngivna stilar`() {
        val own = AppTypography.from(scale).copy(button = scale.labelLarge.copy(fontSize = 99.sp))
        var button: Float? = null
        rule.setContent { AppTheme(lightColorScheme(), extended, shapes, scale, typography = own) { button = AppTheme.typography.button.fontSize.value } }
        rule.waitForIdle()
        assertEquals(99f, button)
    }

    @Test
    fun `stilarna kommer ur typskalan som i apparna`() {
        val styles = AppTypography.from(scale)
        assertEquals(scale.displaySmall, styles.screenTitle)
        assertEquals(scale.labelLarge, styles.button)
        assertEquals(scale.labelMedium, styles.pill)
        assertEquals(Hyphens.Auto, styles.itemTitle.hyphens)
    }

    @Test
    fun `kortets bitar är rundade bara i sina ändar`() {
        assertEquals(RoundedCornerShape(22.dp), shapes.card)
        assertEquals(RoundedCornerShape(22.dp, 22.dp, 0.dp, 0.dp), shapes.cardSegment(top = true, bottom = false))
        assertEquals(RoundedCornerShape(0.dp), shapes.cardSegment(top = false, bottom = false))
    }

    @Test
    fun `färgerna kräver primärton och minst en accentfärg`() {
        assertFailsWith<IllegalArgumentException> { extended.copy(tones = mapOf(green to greenColors)) }
        assertFailsWith<IllegalArgumentException> { extended.copy(accents = emptyList()) }
    }

    @Test
    fun `avstånden är samma som i apparna`() {
        assertEquals(listOf(4, 8, 12, 16, 24, 32), listOf(Spacing.xs, Spacing.s, Spacing.m, Spacing.l, Spacing.xl, Spacing.xxl).map { it.value.toInt() })
    }
}
