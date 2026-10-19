package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.Green
import se.partee71.ui.components.testing.SampleRow
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.components.testing.sampleNotFound
import se.partee71.ui.components.testing.sampleOffline
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/** Skärmdumpar av ytorna (ljust + mörkt); där apparna skiljer sig syns båda varianterna. */
@RunWith(RobolectricTestRunner::class)
class SurfaceScreenshotTest {

    @Test
    fun `AppCard - standard, ton och innehållsfärg`() = captureLightAndDark("AppCard_varianter") {
        SampleSheet {
            AppCard {
                SampleRow("Solkräm", subtitle = "1 tub per 7 dagar")
                SampleRow("Plåster", subtitle = "1 förpackning")
            }
            AppCard(tone = Tone.Green) { SampleRow("Allt är packat", subtitle = "12 av 12") }
            // Dagboken: ikoner utan egen färg tar kortets innehållsfärg.
            AppCard(contentColor = MaterialTheme.colorScheme.onSurface) { Icon(painterResource(R.drawable.ic_clock), contentDescription = null) }
        }
    }

    @Test
    fun `CardSegment - bitar som blir ett kort`() = captureLightAndDark("CardSegment_bitar") {
        SampleSheet {
            Column {
                CardSegment(top = true, bottom = false) { SampleRow("Alvedon 500 mg", subtitle = "2 tabletter per dag") }
                CardSegment(top = false, bottom = false) { SampleRow("Solkräm", subtitle = "1 tub") }
                CardSegment(top = false, bottom = true) { SampleRow("Plåster", subtitle = "1 förpackning") }
            }
        }
    }

    @Test
    fun `SectionHeader - ikon, räknare och räknarens egen ton`() = captureLightAndDark("SectionHeader_varianter") {
        SampleSheet {
            SectionHeader("Mediciner", icon = R.drawable.ic_box, count = "4 / 5")
            SectionHeader("Packat", icon = R.drawable.ic_check, count = "12 / 12", tone = Tone.Green)
            // Dagboken: statuspillen i en annan ton än sektionen.
            SectionHeader("Recept", icon = R.drawable.ic_box, count = "Kontrollerad", countTone = Tone.Green)
            SectionHeader("Övrigt")
        }
    }

    @Test
    fun `InfoPill - toner, ikoner och klickbar`() = captureLightAndDark("InfoPill_varianter") {
        SampleSheet {
            ChipRow {
                InfoPill("3 kvar")
                InfoPill("Packad", tone = Tone.Green, icon = R.drawable.ic_check)
                InfoPill("Synkas", onClick = {})
                // Dagboken: pil efter texten, egen roll och egna färger.
                InfoPill("Vecka", trailingIcon = R.drawable.ic_expand_more, onClick = {}, role = Role.DropdownList)
                InfoPill("Glad", colors = AppTheme.tone(Tone.Green))
            }
        }
    }

    @Test
    fun `EmptyState - standard, fel och kompakt`() = captureLightAndDark("EmptyState_varianter") {
        SampleSheet {
            EmptyState(R.drawable.ic_box, "Inga resor än", "Lägg till en resa så räknar appen ut vad som ska packas.", note = "Du kan ändra allt senare.") {
                AppButton("Lägg till resa", {}, Modifier.padding(horizontal = Spacing.xl))
            }
            // Dagboken: i ett kort, t.ex. ett diagram utan data.
            AppCard { EmptyState(R.drawable.ic_clock, "För lite data än", "Logga några dagar till så syns kurvan.", compact = true) }
        }
    }

    @Test
    fun `EmptyState - helskärm`() = captureLightAndDark("EmptyState_helskarm") {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            EmptyState(R.drawable.ic_wifi_off, "Uppdatera appen", "Hushållets data är sparad med en nyare version av appen.", fullScreen = true, isError = true) {
                AppButton("Uppdatera", {}, Modifier.fillMaxWidth())
            }
        }
    }

    @Test
    fun `LoadErrorState - försök igen och finns inte längre`() = captureLightAndDark("LoadErrorState_varianter") {
        SampleSheet {
            LoadErrorState("Kunde inte hämta uppgifterna", sampleOffline(), {})
            LoadErrorState("Kunde inte hämta uppgifterna", sampleNotFound(), {})
        }
    }
}
