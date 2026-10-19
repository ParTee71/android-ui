package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.Blue
import se.partee71.ui.components.testing.Green
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.components.testing.captureScreenLightAndDark
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/** Skärmdumpar av listraderna (ljust + mörkt); där apparna skiljer sig syns båda varianterna. */
@RunWith(RobolectricTestRunner::class)
class RowScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `ItemRow - varianter`() = captureLightAndDark("ItemRow_varianter") {
        SampleSheet {
            AppCard {
                ItemRow("Alvedon 500 mg", subtitle = "2 tabletter per dag", onClick = {}, navigates = true)
                ItemRow("Solkräm", subtitle = "1 tub", tinted = true, done = true)
                ItemRow("Plåster", subtitle = "Lisa", accent = AppTheme.extendedColors.accents[1], trailing = { InfoPill("3 st", tone = Tone.Green) })
                ItemRow("Hydrokortison", subtitle = "Pausat – räknas inte med", inactive = true, navigates = true)
                ItemRow("Alvedon 665 mg", subtitle = "Depottablett", titleHighlight = 0..2)
                // Dagboken: ikon i en ledande ruta och en rad under titeln, i linje med den.
                ItemRow("Promenad", subtitle = "45 minuter", icon = R.drawable.ic_clock, below = { InfoPill("Försenat") }, belowAlignsWithTitle = true)
            }
        }
    }

    @Test
    fun `ItemRow - bubbla med hela titeln`() = rule.captureScreenLightAndDark(
        "ItemRow_bubbla",
        open = { onNodeWithText(LONG_TITLE).performTouchInput { longClick() } }
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = BUBBLE_ROOM, start = Spacing.l)) {
            AppCard(Modifier.width(NARROW)) { ItemRow(LONG_TITLE, subtitle = "1 tub per 7 dagar") }
        }
    }

    @Test
    fun `ItemRowText - titel och undertext`() = captureLightAndDark("ItemRowText_texter") {
        SampleSheet {
            AppCard(Modifier.padding(Spacing.m)) {
                ItemRowText("Levaxin 100 µg", "Morgon · 07:00")
                ItemRowText(null, "Bara undertexten")
                ItemRowText("Tagen", "07:05", done = true)
            }
        }
    }

    @Test
    fun `LeadingSlot - ikon och kryss i samma ruta`() = captureLightAndDark("LeadingSlot_ruta") {
        SampleSheet {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                LeadingSlot(Modifier.background(AppTheme.extendedColors.rowTint)) { Text("🦊", style = AppTheme.typography.sectionTitle) }
                LeadingSlot(Modifier.background(AppTheme.extendedColors.rowTint)) { MorphingCheck(true) }
            }
        }
    }

    @Test
    fun `MorphingCheck - av och på`() = captureLightAndDark("MorphingCheck_lagen") {
        SampleSheet {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                MorphingCheck(false)
                MorphingCheck(true)
            }
        }
    }

    @Test
    fun `CheckRow - varianter`() = captureLightAndDark("CheckRow_varianter") {
        SampleSheet {
            AppCard {
                CheckRow("Alvedon 500 mg", false, {}, subtitle = "9 tabletter")
                CheckRow("Solkräm", true, {}, subtitle = "1 tub", accent = AppTheme.extendedColors.accents[0])
                CheckRow("Levaxin 50 µg", false, {}, subtitle = "9 tabletter", onClick = {}, trailing = { InfoPill("Ändrad") })
            }
        }
    }

    @Test
    fun `CheckRow - meny, anteckning, rad under och inaktiv`() = captureLightAndDark("CheckRow_meny") {
        SampleSheet {
            AppCard {
                CheckRow(
                    "D-vitamin 20 µg",
                    false,
                    {},
                    subtitle = "Förmiddag · 10:00",
                    note = "Tas med mat.",
                    menu = listOf(AppMenuItem("Hoppa över", {})),
                    below = { InfoPill("Försenat") }
                )
                CheckRow("Ipren 400 mg", true, {}, subtitle = "Vid behov · 14:20", menu = listOf(AppMenuItem("Radera", {}, destructive = true)), enabled = false)
            }
        }
    }

    @Test
    fun `NoteIndicator - ikon`() = captureLightAndDark("NoteIndicator_ikon") {
        SampleSheet { NoteIndicator("Tas på fastande mage.", "Levaxin") }
    }

    @Test
    fun `PausableRow - aktiv och pausad`() = captureLightAndDark("PausableRow_lagen") {
        SampleSheet {
            AppCard {
                PausableRow("Solkräm SPF 50", "1 tub per 7 dagar", "Per N dagar", active = true, onClick = {}, pillTone = Tone.Blue)
                PausableRow("Hydrokortison 1 %", "1 tub", "Fast antal", active = false, onClick = {}, pillTone = Tone.Blue)
                PausableRow("Loratadin 10 mg", "1 tablett vid behov", "Vid behov", active = true, onClick = {}, pillTone = Tone.Primary)
            }
        }
    }

    @Test
    fun `SwitchRow - varianter`() = captureLightAndDark("SwitchRow_varianter") {
        SampleSheet {
            AppCard {
                SwitchRow("Påminnelser", true, {}, subtitle = "Varje dag 08:00")
                SwitchRow("Kvällsdos", false, {}, onClick = {}, onClickLabel = "Ändra")
                // Dagboken: avstängd påminnelse – texten nedtonad, växeln inte.
                SwitchRow("Mående", false, {}, subtitle = "Av", inactive = true)
            }
        }
    }

    @Test
    fun `SwitchControl - av och på`() = captureLightAndDark("SwitchControl_lagen") {
        SampleSheet {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                SwitchControl(false, onCheckedChange = null)
                SwitchControl(true, onCheckedChange = {}, label = "Påminnelse")
            }
        }
    }

    @Test
    fun `SuggestionField - förslag`() = captureLightAndDark("SuggestionField_forslag") {
        SampleSheet {
            SuggestionField(
                "alv",
                {},
                "Namn",
                listOf(Suggestion("Alvedon 500 mg", "Tablett", 0..2), Suggestion("Alvedon 665 mg", "Depottablett", 0..2)),
                {},
                footer = "Från Läkemedelsverket"
            )
        }
    }

    private companion object {
        const val LONG_TITLE = "Hydrokortison 1 % kräm för eksem och myggbett på armar och ben under resan"
        val NARROW = 240.dp
        val BUBBLE_ROOM = 160.dp
    }
}
