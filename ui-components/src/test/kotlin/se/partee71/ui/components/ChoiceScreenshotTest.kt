package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.SampleSwatches
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.theme.AppTheme

/** Skärmdumpar av valen (ljust + mörkt); där apparna skiljer sig syns båda varianterna. */
@RunWith(RobolectricTestRunner::class)
class ChoiceScreenshotTest {

    @Test
    fun `AppFilterChip - lägen`() = captureLightAndDark("AppFilterChip_lagen") {
        SampleSheet {
            ChipRow {
                AppFilterChip("Tablett", selected = true, onClick = {})
                AppFilterChip("Kapsel", selected = false, onClick = {})
                AppFilterChip("Plåster", selected = false, onClick = {}, icon = R.drawable.ic_add)
                AppFilterChip("Kräm", selected = false, onClick = {}, enabled = false)
            }
        }
    }

    @Test
    fun `ChoiceChips - ett val och flera val`() = captureLightAndDark("ChoiceChips_ett_och_flera") {
        SampleSheet {
            ChoiceChips(listOf("Tablett", "Kapsel", "Kräm", "Droppar"), "Kapsel", {}, label = { it })
            ChoiceChips(
                listOf("Spänd käke", "Hjärtklappning", "Klump i magen", "Tung i bröstet", "Rastlösa ben"),
                selected = setOf("Hjärtklappning", "Klump i magen"),
                onToggle = {},
                label = { it }
            )
        }
    }

    @Test
    fun `ExampleChips - exempel`() = captureLightAndDark("ExampleChips_exempel") {
        SampleSheet { ExampleChips(listOf("Solkräm", "Plåster", "Myggmedel", "Resorb"), {}, chosen = "Plåster") }
    }

    @Test
    fun `ChoiceGrid - egen markering och ring`() = captureLightAndDark("ChoiceGrid_markering") {
        val emojis = listOf("🦊", "🐻", "🐰", "🦉", "🐯", "🐢", "🦄", "🐧", "🐼", "🐨")
        SampleSheet {
            // ReseApoteket: cellen ritar sin egen markering (emojiväljaren).
            ChoiceGrid(emojis, "🐻", {}, label = { it }) { emoji, selected ->
                val marker = if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, AppTheme.shapes.pill).border(2.5.dp, MaterialTheme.colorScheme.primary, AppTheme.shapes.pill)
                } else {
                    Modifier
                }
                Box(marker.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emoji, style = AppTheme.typography.sectionTitle) }
            }
            // Dagboken: rutnätet ritar ringen.
            ChoiceGrid(emojis, "🐻", {}, label = { it }, selectionRing = true) { emoji, _ -> Text(emoji, style = AppTheme.typography.sectionTitle) }
        }
    }

    @Test
    fun `ColorSwatchPicker - ring och bock, och rutnätets ring`() = captureLightAndDark("ColorSwatchPicker_markering") {
        SampleSheet {
            ColorSwatchPicker(SampleSwatches.hexes, "#3B7EA1", {}, SampleSwatches::color, { SampleSwatches.label(it) }, SwatchMarker.RingAndCheck(checkColor = Color.White))
            // Dagboken: rutnätets ring i primärfärgen.
            ColorSwatchPicker(SampleSwatches.hexes, "#3B7EA1", {}, SampleSwatches::color, { SampleSwatches.label(it) }, SwatchMarker.GridRing)
        }
    }
}
