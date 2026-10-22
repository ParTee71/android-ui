package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleCard
import se.partee71.ui.components.testing.SampleRow
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.components.testing.captureScreenLightAndDark
import se.partee71.ui.components.testing.clickWithoutRipple
import se.partee71.ui.components.testing.sampleMenu
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone
import se.partee71.ui.theme.ToneColors

/**
 * Skärmdumpar (ljust + mörkt) av varje komponent med testtemat (ReseApotekets palett).
 * Filnamn `<Komponent>_<variant>_light|dark.png` i `src/test/screenshots` – `UiConsistencyTest`
 * kräver minst ett par per publik komponent.
 */
@RunWith(RobolectricTestRunner::class)
class ComponentScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `AppButton - varianter och lägen`() = captureLightAndDark("AppButton_varianter") {
        SampleSheet {
            AppButton("Logga in", {}, Modifier.fillMaxWidth(), icon = R.drawable.ic_add)
            AppButton("Loggar in …", {}, Modifier.fillMaxWidth(), loading = true)
            AppButton("Lägg till", {}, variant = ButtonVariant.Secondary)
            AppButton("Avbryt", {}, variant = ButtonVariant.Text)
            AppButton("Spara", {}, enabled = false)
            AppButton("Radera", {}, variant = ButtonVariant.Destructive)
            AppButton("Spara", {}, compact = true)
        }
    }

    @Test
    fun `AppIconButton - varianter`() = captureLightAndDark("AppIconButton_varianter") {
        SampleSheet {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                AppIconButton(R.drawable.ic_arrow_back, "Tillbaka", {}, variant = IconButtonVariant.Tonal)
                AppIconButton(R.drawable.ic_archive, "Arkivera", {})
                AppIconButton(R.drawable.ic_add, "Lägg till", {}, variant = IconButtonVariant.Tonal, enabled = false)
            }
        }
    }

    @Test
    fun `AddSplitButton - med och utan meny`() = captureLightAndDark("AddSplitButton_varianter") {
        SampleSheet {
            AddSplitButton("Lägg till", {}, menu = sampleMenu("Räkna om" to {}))
            AddSplitButton("Lägg till person", {})
        }
    }

    @Test
    fun `AddSplitButton - öppen meny`() = rule.captureScreenLightAndDark(
        "AddSplitButton_oppen",
        open = { onNodeWithContentDescription("Fler val").clickWithoutRipple() }
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(Spacing.l)) {
            AddSplitButton("Lägg till", {}, menu = sampleMenu("Räkna om" to {}, "Importera" to {}))
        }
    }

    @Test
    fun `AppDivider - i kort`() = captureLightAndDark("AppDivider_i_kort") {
        SampleSheet {
            SampleCard {
                SampleRow("Solkräm", subtitle = "1 tub per 7 dagar")
                AppDivider()
                SampleRow("Plåster", subtitle = "1 förpackning")
            }
        }
    }

    @Test
    fun `AppLoading - standard`() = captureLightAndDark("AppLoading_standard") {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(Spacing.xxl)) { AppLoading() }
    }

    @Test
    fun `AppSegmentedChoice - tema`() = captureLightAndDark("AppSegmentedChoice_tema") {
        SampleSheet { AppSegmentedChoice(listOf("Ljust", "Mörkt", "System"), 2, {}) }
    }

    @Test
    fun `AppSnackbarHost - meddelande`() = rule.captureLightAndDark("AppSnackbarHost_meddelande") {
        val state = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { state.showSnackbar("Något gick fel. Försök igen.", duration = SnackbarDuration.Indefinite) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
            AppSnackbarHost(state)
        }
    }

    @Test
    fun `AppTopBar - storlekar`() = captureLightAndDark("AppTopBar_storlekar") {
        SampleSheet {
            AppTopBar("Resor", subtitle = "3 kommande") { AppIconButton(R.drawable.ic_add, "Lägg till", {}) }
            AppTopBar("Solkräm", size = TopBarSize.Small, onBack = {}) { AppButton("Spara", {}, compact = true) }
        }
    }

    @Test
    fun `AppTopBar - med appens status`() = captureLightAndDark("AppTopBar_status") {
        val status: @Composable RowScope.() -> Unit = {
            val tone = AppTheme.tone(Tone.Primary)
            Text("Synkas", style = AppTheme.typography.pill, color = tone.content, modifier = Modifier.background(tone.container, AppTheme.shapes.pill).padding(horizontal = Spacing.m, vertical = Spacing.xs))
        }
        CompositionLocalProvider(LocalTopBarStatus provides status) {
            SampleSheet { AppTopBar("Resor") { AppIconButton(R.drawable.ic_add, "Lägg till", {}) } }
        }
    }

    @Test
    fun `ChipRow - bryts och rullar`() = captureLightAndDark("ChipRow_varianter") {
        SampleSheet {
            ChipRow { listOf("Krämer & salvor", "Plåster & förband", "Verktyg", "Övrigt").forEach { AppButton(it, {}, variant = ButtonVariant.Secondary, compact = true) } }
            ChipRow(wrap = false) { listOf("Anna", "Erik", "Lova", "Ture", "Majken").forEach { AppButton(it, {}, variant = ButtonVariant.Secondary, compact = true) } }
        }
    }

    @Test
    fun `SwipeToHide - halvvägs svept`() {
        var swiped = false
        rule.captureLightAndDark("SwipeToHide_svept", settle = {
            // Fingret hålls kvar mitt i svepet, så att bakgrunden syns i båda bilderna.
            if (!swiped) {
                onRoot().performTouchInput {
                    down(Offset(width * 0.8f, height / 2f))
                    repeat(STEPS) { moveBy(Offset(-width * 0.3f / STEPS, 0f)) }
                }
            }
            swiped = true
            waitForIdle()
        }) {
            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(Spacing.l)) {
                SampleCard { SwipeToHide(onHide = {}) { SampleRow("Solkräm", it, subtitle = "1 tub per 7 dagar") } }
            }
        }
    }

    @Test
    fun `SwipeBackground - arkivera och appens radera`() = captureLightAndDark("SwipeBackground_varianter") {
        SampleSheet {
            SwipeBackground("Arkivera", R.drawable.ic_archive, Modifier.height(SWIPE_HEIGHT))
            SwipeBackground(
                "Radera",
                R.drawable.ic_delete,
                Modifier.height(SWIPE_HEIGHT),
                colors = ToneColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                shape = AppTheme.shapes.card
            )
        }
    }

    @Test
    fun `CollapsibleHeader - hopfälld och utfälld`() = captureLightAndDark("CollapsibleHeader_varianter") {
        SampleSheet {
            SampleCard { CollapsibleHeader(false, {}) { SectionHeader("Recept", Modifier.weight(1f), count = "3") } }
            SampleCard { CollapsibleHeader(true, {}) { Text("Anteckningar", Modifier.weight(1f), style = AppTheme.typography.itemTitle) } }
        }
    }

    @Test
    fun `Confetti - mitt i regnet`() {
        rule.mainClock.autoAdvance = false
        rule.captureLightAndDark("Confetti_mitt", settle = {
            mainClock.advanceTimeBy(CONFETTI_MIDDLE)
            waitForIdle()
        }) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { Confetti(Modifier.fillMaxSize()) }
        }
    }

    @Test
    fun `SectionHeader - arbetar`() {
        // Indikatorn snurrar för evigt: klockan står still och går en fast tid före bilden.
        rule.mainClock.autoAdvance = false
        rule.captureLightAndDark("SectionHeader_arbetar", settle = {
            mainClock.advanceTimeBy(CONFETTI_MIDDLE)
            waitForIdle()
        }) {
            SampleSheet { SectionHeader("Byten", count = "2 väntar på dig", working = true) }
        }
    }

    private companion object {
        const val CONFETTI_MIDDLE = 500L
        const val STEPS = 10
        val SWIPE_HEIGHT = 64.dp
    }
}
