package se.partee71.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleRow
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.components.testing.sampleMenu

/** Beteende och tillgänglighet i bibliotekets komponenter – flyttade från ReseApotekets `ComponentBehaviorTest`/`ComponentsTest`. */
@RunWith(RobolectricTestRunner::class)
class ComponentBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    private companion object {
        const val SETTLE_MILLIS = 2_000L
    }

    @Test
    fun `knappen anropar onClick`() {
        var clicks = 0
        show { AppButton("Spara", onClick = { clicks++ }) }
        rule.onNodeWithText("Spara").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `knappen som laddar är inaktiv och anropar inte onClick`() {
        var clicks = 0
        show { AppButton("Loggar in …", onClick = { clicks++ }, loading = true) }
        rule.onNodeWithText("Loggar in …").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun `den kompakta knappen har ändå 48 dp tryckyta`() {
        show { AppButton("Spara", {}, compact = true) }
        rule.onNodeWithText("Spara").assertTouchHeightIsEqualTo(48.dp)
    }

    @Test
    fun `ikonknappen har sin beskrivning, 48 dp och anropar onClick`() {
        var clicks = 0
        show { AppIconButton(R.drawable.ic_arrow_back, "Tillbaka", { clicks++ }) }
        rule.onNodeWithContentDescription("Tillbaka").assertTouchHeightIsEqualTo(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `laddningen läses som Laddar`() {
        show { AppLoading() }
        rule.onNodeWithContentDescription("Laddar").assertExists()
    }

    @Test
    fun `segmentvalet rapporterar valet`() {
        var segment by mutableIntStateOf(2)
        show { AppSegmentedChoice(listOf("Ljust", "Mörkt", "System"), segment, { segment = it }) }
        rule.onNodeWithText("Mörkt").performClick()
        assertEquals(1, segment)
        rule.onNodeWithText("Mörkt").assertIsSelected()
    }

    @Test
    fun `split-knappen lägger till och pilen öppnar fler val`() {
        var added = 0
        var recalculated = 0
        show { AddSplitButton("Lägg till", { added++ }, menu = sampleMenu("Räkna om" to { recalculated++ })) }
        rule.onNodeWithText("Lägg till").performClick()
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Räkna om").performClick()
        assertEquals(1 to 1, added to recalculated)
        rule.onNodeWithText("Räkna om").assertDoesNotExist()
    }

    @Test
    fun `split-knappen utan meny är en vanlig knapp utan pil`() {
        var added = 0
        show { AddSplitButton("Lägg till person", { added++ }) }
        rule.onNodeWithContentDescription("Fler val").assertDoesNotExist()
        rule.onNodeWithText("Lägg till person").performClick()
        assertEquals(1, added)
    }

    @Test
    fun `toppraden har tillbakaknapp och rubrik som rubrik`() {
        var back = 0
        show { AppTopBar("Solkräm", size = TopBarSize.Small, onBack = { back++ }) }
        rule.onNodeWithText("Solkräm").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Tillbaka").performClick()
        assertEquals(1, back)
    }

    @Test
    fun `toppraden visar appens status före åtgärderna, bara när den är satt`() {
        var pending by mutableStateOf(false)
        show {
            val status: @Composable RowScope.() -> Unit = { if (pending) Text("Synkas") }
            CompositionLocalProvider(LocalTopBarStatus provides status) {
                AppTopBar("Resor") { AppIconButton(R.drawable.ic_arrow_back, "Fler val", {}) }
            }
        }
        rule.onNodeWithText("Synkas").assertDoesNotExist()
        pending = true
        rule.onNodeWithText("Synkas").assertIsDisplayed()
        rule.onNodeWithContentDescription("Fler val").assertIsDisplayed()
    }

    @Test
    fun `chipraden behåller innehållets tillstånd när radbrytningen byts`() {
        var wrap by mutableStateOf(true)
        show {
            ChipRow(wrap = wrap) {
                var clicks by remember { mutableIntStateOf(0) }
                AppButton("Klick $clicks", { clicks++ })
            }
        }
        rule.onNodeWithText("Klick 0").performClick()
        wrap = false
        rule.onNodeWithText("Klick 1").assertIsDisplayed()
    }

    @Test
    fun `konfettin säger till när regnet är över`() {
        var finished = 0
        show { Confetti(onFinished = { finished++ }) }
        rule.waitForIdle()
        assertEquals(1, finished)
    }

    @Test
    fun `konfettin tar egna färger`() {
        var finished = 0
        show { Confetti(colors = listOf(Color.Red), onFinished = { finished++ }) }
        rule.waitForIdle()
        assertEquals(1, finished)
    }

    @Test
    fun `en rad utan svep har ingen arkivera-åtgärd`() {
        show { SwipeToHide(onHide = {}, enabled = false) { SampleRow("Solkräm", it, onClick = {}) } }
        rule.onNodeWithText("Solkräm").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun `en rad behåller sitt tillstånd när den arkiveras och inte går att svepa`() {
        var enabled by mutableStateOf(true)
        var hidden = 0
        show {
            SwipeToHide(onHide = { hidden++ }, enabled = enabled) {
                var clicks by remember { mutableIntStateOf(0) }
                SampleRow("Klick $clicks", it, onClick = { clicks++ })
            }
        }
        rule.onNodeWithText("Klick 0").performClick()
        enabled = false
        rule.onNodeWithText("Klick 1").assertIsDisplayed().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(0, hidden)
    }

    @Test
    fun `svep åt vänster arkiverar`() {
        var hidden = 0
        show { SwipeToHide(onHide = { hidden++ }) { SampleRow("Solkräm", it, onClick = {}) } }
        rule.onNodeWithText("Solkräm").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(1, hidden)
    }

    @Test
    fun `en svept rad som står kvar glider tillbaka`() {
        show { SwipeToHide(onHide = {}) { SampleRow("Solkräm", it, onClick = {}) } }
        val x = { rule.onNodeWithText("Solkräm").fetchSemanticsNode().positionInRoot.x }
        val start = x()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Solkräm").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(RESET_DELAY_MILLIS / 2) // undan, men inte tillbaka än
        assertTrue(x() < start, "raden ska ha svepts åt vänster innan den glider tillbaka")
        rule.mainClock.advanceTimeBy(RESET_DELAY_MILLIS + SETTLE_MILLIS)
        rule.waitForIdle()
        assertEquals(start, x())
    }

    @Test
    fun `arkivera-åtgärden för TalkBack ligger på raden som TalkBack läser`() {
        var hidden = 0
        show { SwipeToHide(onHide = { hidden++ }) { SampleRow("Solkräm", it, subtitle = "1 tub", onClick = {}) } }
        // Den sammanslagna noden är den TalkBack fokuserar; åtgärden ska finnas just där.
        val actions = rule.onNodeWithText("Solkräm").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        actions.single { it.label == "Arkivera" }.action()
        assertEquals(1, hidden)
    }

    @Test
    fun `svepets etikett går att byta`() {
        show { SwipeToHide(onHide = {}, label = "Dölj") { SampleRow("Solkräm", it, onClick = {}) } }
        val actions = rule.onNodeWithText("Solkräm").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Dölj"), actions.map { it.label })
    }

    @Test
    fun `snackbarvärden visar meddelandet`() {
        show {
            val state = remember { SnackbarHostState() }
            LaunchedEffect(Unit) { state.showSnackbar("Något gick fel. Försök igen.", duration = SnackbarDuration.Indefinite) }
            AppSnackbarHost(state)
        }
        rule.onNodeWithText("Något gick fel. Försök igen.").assertIsDisplayed()
    }
}
