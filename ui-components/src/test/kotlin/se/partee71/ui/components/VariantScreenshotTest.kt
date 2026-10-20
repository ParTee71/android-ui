package se.partee71.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
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
 * Skärmdumpar (ljust + mörkt) av varianterna från 0.6.0 – det Fonder behöver för att byta utan att ändra
 * utseende. Testtemat har ReseApotekets palett; appstilen här är Fonders värden (40 dp knappar med
 * formmorf, tonad knapp i en egen ton, kort med kontur och 16 dp luft).
 */
@RunWith(RobolectricTestRunner::class)
class VariantScreenshotTest {

    @Composable
    private fun FonderCards(content: @Composable () -> Unit) {
        val style = AppCardStyle(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), padding = Spacing.l, contentSpacing = 0.dp)
        CompositionLocalProvider(LocalAppCardStyle provides style, content = content)
    }

    @Test
    fun `AppButton - konturerad och beskriven`() = captureLightAndDark("AppButton_konturerad") {
        SampleSheet {
            AppButton("Registrera sälj", {}, variant = ButtonVariant.Outlined)
            AppButton("Öppna bevakning", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Outlined, icon = R.drawable.ic_add)
            AppButton("Följ", {}, variant = ButtonVariant.Outlined, enabled = false)
            AppButton("Avstå", {}, variant = ButtonVariant.Text, contentDescription = "Avstå bytet Global Index till Räntefond")
        }
    }

    @Test
    fun `AppCard - kortstil med kontur, klickbart och tonat`() = captureLightAndDark("AppCard_kontur") {
        FonderCards {
            SampleSheet {
                AppCard {
                    Text("Portföljens värde", style = AppTheme.typography.itemSubtitle)
                    Text("124 560 kr", style = AppTheme.typography.bigNumber)
                }
                AppCard(onClick = {}) { ItemRow("Global Index", subtitle = "12 300 kr · +4,2 %", navigates = true) }
                AppCard(tone = Tone.Green) { ItemRow("Allt är uppdaterat", subtitle = "Kurser från i dag") }
                AppCard(style = AppCardStyle(padding = 0.dp, segmentGap = 0.dp, contentSpacing = 0.dp)) { ItemRow("Utan luft", subtitle = "Raden når kanten") }
            }
        }
    }

    @Test
    fun `CardSegment - bitar med delad kontur`() = captureLightAndDark("CardSegment_kontur") {
        FonderCards {
            SampleSheet {
                Column {
                    CardSegment(top = true, bottom = false) { ItemRow("Köp · Global Index", subtitle = "2 okt · 5 000 kr") }
                    CardSegment(top = false, bottom = false) { ItemRow("Sälj · Räntefond", subtitle = "28 sep · 2 400 kr") }
                    CardSegment(top = false, bottom = true) { ItemRow("Köp · Sverige Small Cap", subtitle = "25 sep · 1 000 kr") }
                }
            }
        }
    }

    @Test
    fun `InfoPill - form och beskrivning`() = captureLightAndDark("InfoPill_form") {
        SampleSheet {
            ChipRow {
                InfoPill("Föreslaget", shape = AppTheme.shapes.smallTile)
                InfoPill("Köpt 7 okt", tone = Tone.Green, shape = AppTheme.shapes.smallTile)
                InfoPill("Risk 5/7", tone = Tone.Blue, shape = AppTheme.shapes.smallTile, contentDescription = "Risknivå 5 av 7")
                InfoPill("Rund")
            }
        }
    }

    @Test
    fun `ItemRow - overline och titelstil`() = captureLightAndDark("ItemRow_overline") {
        SampleSheet {
            AppCard {
                ItemRow(
                    "Global Index → Räntefond",
                    subtitle = "Lägre avgift · 0,20 %",
                    overline = { InfoPill("Föreslaget", shape = AppTheme.shapes.smallTile) },
                    navigates = true,
                    onClick = {}
                )
                ItemRow("Sverige Small Cap", subtitle = "Köp · 1 000 kr", titleStyle = MaterialTheme.typography.titleSmall)
                ItemRow("Global Index", subtitle = "Standardtitel")
            }
        }
    }

    @Test
    fun `CheckRow - ikryssad utan överstrykning`() = captureLightAndDark("CheckRow_utan_overstrykning") {
        SampleSheet {
            AppCard {
                CheckRow("Global Index", checked = true, onCheckedChange = {}, subtitle = "SE0000000001 · innehav.csv", done = false, tinted = false)
                CheckRow("Räntefond", checked = false, onCheckedChange = {}, subtitle = "SE0000000002 · innehav.csv", done = false, tinted = false)
                CheckRow("Packad", checked = true, onCheckedChange = {}, subtitle = "Standard: överstruken och tonad")
            }
        }
    }

    @Test
    fun `SwitchRow - låst`() = captureLightAndDark("SwitchRow_last") {
        SampleSheet {
            AppCard {
                SwitchRow("Bara indexfonder", checked = true, onCheckedChange = {}, subtitle = "Låst under pågående byte", enabled = false)
                SwitchRow("Visa avgifter", checked = false, onCheckedChange = {}, enabled = false)
                SwitchRow("Påminnelser", checked = true, onCheckedChange = {})
            }
        }
    }

    @Test
    fun `AppFloatingToolbar - märke med antal`() = captureLightAndDark("AppFloatingToolbar_marke") {
        SampleSheet {
            AppFloatingToolbar(
                listOf(
                    ToolbarItem("Översikt", R.drawable.ic_clock),
                    ToolbarItem("Beslut", R.drawable.ic_box, badgeCount = 3),
                    ToolbarItem("Händelser", R.drawable.ic_calendar, badgeCount = 12)
                ),
                0,
                {}
            )
        }
    }

    // --- 0.7.0 ---

    @Test
    fun `EmptyState - flera knappar`() = captureLightAndDark("EmptyState_knappar") {
        SampleSheet {
            EmptyState(
                R.drawable.ic_box,
                "Ingen portfölj ännu",
                "Lägg till din första fondtransaktion för att se en översikt här.",
                actions = listOf(
                    EmptyAction("Bevaka en fond", {}, R.drawable.ic_add),
                    EmptyAction("Importera innehav", {}, R.drawable.ic_archive, ButtonVariant.Secondary),
                    EmptyAction("Återställ från säkerhetskopia", {}, R.drawable.ic_unarchive, ButtonVariant.Text)
                )
            )
        }
    }

    @Test
    fun `EntityListScreen - tomt utan lägg till med egna knappar`() = captureLightAndDark("EntityListScreen_tom_knappar") {
        EntityListScreen<String>(
            "Händelser",
            ListUiState.Empty,
            EmptyContent(
                R.drawable.ic_calendar,
                "Inga transaktioner",
                "Registrera köp och sälj för att räkna ut värde.",
                actions = listOf(
                    EmptyAction("Ny transaktion", {}, R.drawable.ic_add),
                    EmptyAction("Importera avräkningsnota", {}, R.drawable.ic_archive, ButtonVariant.Secondary)
                )
            ),
            add = null,
            key = { it },
            filter = { ChoiceChips(listOf("Alla", "Köp", "Sälj"), "Alla", {}, label = { it }) }
        ) {}
    }

    @Test
    fun `AppTopBar - tillbakapilens stil`() = captureLightAndDark("AppTopBar_tillbakapil") {
        SampleSheet {
            AppTopBar("Standard", size = TopBarSize.Small, onBack = {})
            AppTopBar("Lägg till fond", size = TopBarSize.Small, onBack = {}, backStyle = TopBarBackStyle(variant = IconButtonVariant.Plain))
            AppTopBar("Grön ton", size = TopBarSize.Small, onBack = {}, backStyle = TopBarBackStyle(tone = Tone.Green))
            CompositionLocalProvider(LocalTopBarBackStyle provides TopBarBackStyle(variant = IconButtonVariant.Plain, tone = Tone.Blue)) {
                AppTopBar("Blå via appen", size = TopBarSize.Small, onBack = {})
            }
        }
    }
}
