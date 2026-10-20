package se.partee71.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.common.DetailUiState
import se.partee71.ui.common.ListUiState
import se.partee71.ui.components.testing.Blue
import se.partee71.ui.components.testing.Green
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/**
 * Varianterna från 0.6.0 med något som rör sig (snurran i knappen, uppdateringsindikatorn) – i en
 * compose-regel, som håller oändliga animationer stilla så att bilden blir densamma varje gång.
 */
@RunWith(RobolectricTestRunner::class)
class VariantMotionScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `AppButton - appens knappstil`() = rule.captureLightAndDark("AppButton_appstil") {
        CompositionLocalProvider(LocalAppButtonStyle provides AppButtonStyle(minHeight = AppButtonDefaults.CompactHeight, morph = true, secondaryTone = Tone.Blue)) {
            SampleSheet {
                AppButton("Spara", {})
                AppButton("Exportera", {}, variant = ButtonVariant.Secondary)
                AppButton("Registrera sälj", {}, variant = ButtonVariant.Outlined)
                AppButton("Stäng", {}, variant = ButtonVariant.Text)
                AppButton("Töm databasen", {}, variant = ButtonVariant.Destructive)
                AppButton("Importerar …", {}, loading = true)
            }
        }
    }

    @Test
    fun `EntityListScreen - uppdaterar`() = rule.captureLightAndDark("EntityListScreen_uppdaterar") {
        EntityListScreen(
            "Fondsök",
            ListUiState.Content(persistentListOf("Global Index", "Räntefond", "Sverige Small Cap")),
            EmptyContent(R.drawable.ic_box, "Inga fonder hittades", "Sök på ett annat namn."),
            add = null,
            key = { it },
            onBack = {},
            topBarSize = TopBarSize.Small,
            refreshing = true,
            onRefresh = {}
        ) { ItemRow(it, subtitle = "Aktiefond", navigates = true, onClick = {}) }
    }

    @Test
    fun `EntityDetailScreen - uppdaterar`() = rule.captureLightAndDark("EntityDetailScreen_uppdaterar") {
        EntityDetailScreen(
            DetailUiState.Content("Global Index"),
            header = null,
            onBack = {},
            title = "Global Index",
            refreshing = true,
            onRefresh = {}
        ) {
            AppCard {
                ItemRow("Senaste kurs", subtitle = "312,40 kr · 20 okt")
                ItemRow("Innehav", subtitle = "39 andelar · 12 300 kr")
            }
        }
    }
}
