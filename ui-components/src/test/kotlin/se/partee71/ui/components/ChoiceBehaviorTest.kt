package se.partee71.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleSwatches
import se.partee71.ui.components.testing.TestTheme

/** Valen: filterchip, ett- och flerval, exempel, rutnät och färgväljare – beteende och TalkBack. */
@RunWith(RobolectricTestRunner::class)
class ChoiceBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    private val forms = listOf("Tablett", "Kapsel", "Kräm")

    @Test
    fun `filterchipet läses som valt eller inte och anropar onClick`() {
        var clicks = 0
        show {
            ChipRow {
                AppFilterChip("Tablett", selected = true, onClick = { clicks++ })
                AppFilterChip("Kräm", selected = false, onClick = {})
            }
        }
        rule.onNodeWithText("Tablett").assertIsSelected().performClick()
        rule.onNodeWithText("Kräm").assertIsNotSelected()
        assertEquals(1, clicks)
    }

    @Test
    fun `långtrycket på filterchipet är en TalkBack-åtgärd med sin etikett, även när chipet är inaktivt`() {
        var longClicks = 0
        show { AppFilterChip("Alvedon", selected = false, onClick = {}, enabled = false, onLongClick = { longClicks++ }, onLongClickLabel = "Fler val") }
        val node = rule.onNodeWithText("Alvedon").fetchSemanticsNode()
        assertEquals("Fler val", node.config[SemanticsActions.OnLongClick].label)
        rule.onNodeWithText("Alvedon").performSemanticsAction(SemanticsActions.OnLongClick)
        assertEquals(1, longClicks)
    }

    @Test
    fun `filterchipet med värde visar och läser värdet efter etiketten, och det framhävda är fortfarande valt`() {
        show { AppFilterChip("Huvudvärk", selected = true, onClick = {}, value = "4", emphasized = true) }
        rule.onNodeWithText("Huvudvärk", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("4", useUnmergedTree = true).assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Huvudvärk"), AnnotatedString("4")))).assertIsSelected()
    }

    @Test
    fun `det väntande filterchipet läser beskrivningen i stället för tilläggstexten och går att trycka på`() {
        var clicks = 0
        show { AppFilterChip("Ipren 400 mg", selected = false, onClick = { clicks++ }, pending = ChipPending(R.drawable.ic_clock, "från 15:40", "för tidigt, nästa dos från 15:40")) }
        val node = rule.onNodeWithText("Ipren 400 mg")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "för tidigt, nästa dos från 15:40"))
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Ipren 400 mg"))))
        node.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `filterchipet utan långtryck har ingen långtrycksåtgärd`() {
        show { AppFilterChip("Kapsel", selected = false, onClick = {}) }
        rule.onNodeWithText("Kapsel").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
    }

    @Test
    fun `envalet väljer, och ett tryck på det valda väljer det igen utan onClear`() {
        var selected by mutableStateOf("Tablett")
        var selections = 0
        show {
            ChoiceChips(forms, selected, {
                selected = it
                selections++
            }, label = { it })
        }
        rule.onNodeWithText("Kräm").performClick()
        rule.onNodeWithText("Kräm").assertIsSelected()
        rule.onNodeWithText("Tablett").assertIsNotSelected()
        rule.onNodeWithText("Kräm").performClick()
        assertEquals("Kräm" to 2, selected to selections)
    }

    @Test
    fun `med onClear avmarkerar ett tryck på det valda valet`() {
        var selected by mutableStateOf<String?>("Kapsel")
        show { ChoiceChips(forms, selected, { selected = it }, label = { it.orEmpty() }, onClear = { selected = null }) }
        rule.onNodeWithText("Kapsel").performClick()
        assertEquals(null, selected)
        rule.onNodeWithText("Kapsel").assertIsNotSelected()
    }

    @Test
    fun `flervalet växlar varje alternativ och läser varje chip med namn och valt eller inte`() {
        val places = listOf("Spänd käke", "Hjärtklappning", "Klump i magen")
        var selected by mutableStateOf(setOf("Hjärtklappning"))
        show { ChoiceChips(places, selected = selected, onToggle = { selected = if (it in selected) selected - it else selected + it }, label = { it }) }
        rule.onNodeWithText("Hjärtklappning").assertIsSelected()
        rule.onNodeWithText("Spänd käke").assertIsNotSelected()
        rule.onNodeWithText("Klump i magen").assertIsNotSelected().performClick()
        rule.onNodeWithText("Hjärtklappning").performClick()
        assertEquals(setOf("Klump i magen"), selected)
        rule.onNodeWithText("Klump i magen").assertIsSelected()
        rule.onNodeWithText("Hjärtklappning").assertIsNotSelected()
    }

    @Test
    fun `exemplen väljer sitt eget objekt även med samma text, och det valda har bock`() {
        data class Example(val name: String, val id: Int)
        val options = listOf(Example("Solkräm", 1), Example("Solkräm", 2))
        var picked: Example? = null
        show { ExampleChips(options, { picked = it }, chosen = options[0], label = { it.name }) }
        rule.onAllNodesWithText("Solkräm")[1].assertIsNotSelected().performClick()
        assertSame(options[1], picked)
        rule.onAllNodesWithText("Solkräm")[0].assertIsSelected()
    }

    @Test
    fun `rutnätet läser varje cell som ett radioval med namn och valt läge`() {
        var selected by mutableStateOf("🦊")
        show { ChoiceGrid(listOf("🦊", "🐻", "🐰"), selected, { selected = it }, label = { "Avatar $it" }) { item, _ -> Text(item) } }
        rule.onNodeWithContentDescription("Avatar 🐻")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
            .performClick()
        assertEquals("🐻", selected)
        rule.onNodeWithContentDescription("Avatar 🐻").assertIsSelected()
    }

    @Test
    fun `rutnätet med columns under 1 visar en per rad i stället för att krascha`() {
        show { ChoiceGrid(listOf("🦊", "🐻"), "🦊", {}, label = { "Avatar $it" }, columns = 0) { item, _ -> Text(item) } }
        val first = rule.onNodeWithContentDescription("Avatar 🦊").getBoundsInRoot()
        val second = rule.onNodeWithContentDescription("Avatar 🐻").getBoundsInRoot()
        assertTrue(second.top >= first.bottom, "varsin rad")
    }

    @Test
    fun `färgväljaren känner igen en lagrad färg i annat skiftläge och läser färgens namn`() {
        var selected by mutableStateOf("#3b7ea1")
        show {
            ColorSwatchPicker(
                SampleSwatches.hexes,
                selected,
                { selected = it },
                color = SampleSwatches::color,
                label = { SampleSwatches.label(it) },
                marker = SwatchMarker.GridRing
            )
        }
        rule.onNodeWithContentDescription("Färg: blå").assertIsSelected()
        rule.onNodeWithContentDescription("Färg: grön").performClick()
        assertEquals("#7A9A3C", selected)
    }
}
