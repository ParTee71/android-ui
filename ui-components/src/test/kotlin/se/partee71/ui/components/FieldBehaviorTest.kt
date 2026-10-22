package se.partee71.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import kotlin.test.assertEquals
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.TestTheme

/** Fälten: textfält, väljarfält, tid, grupper och stepper – beteende och TalkBack. */
@RunWith(RobolectricTestRunner::class)
class FieldBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    @Test
    fun `textfältet växer inte förbi taket men går att korta`() {
        var text by mutableStateOf("Solkr")
        show { AppTextField(text, { text = it }, "Namn", maxLength = 6) }
        rule.onNodeWithText("Solkr").performTextInput("äm")
        assertEquals("Solkr", text, "en inmatning över taket ignoreras hellre än kapas")
        rule.onNodeWithText("Solkr").performTextReplacement("Sol")
        assertEquals("Sol", text)
    }

    @Test
    fun `textfältets tak är 200 tecken på en rad och 2000 flerradigt som standard`() {
        assertEquals(200 to 2000, AppTextFieldDefaults.SHORT_MAX_LENGTH to AppTextFieldDefaults.LONG_MAX_LENGTH)
        var text by mutableStateOf("a".repeat(AppTextFieldDefaults.SHORT_MAX_LENGTH))
        show { AppTextField(text, { text = it }, "Namn") }
        rule.onNodeWithText("Namn").performTextInput("b")
        assertEquals(AppTextFieldDefaults.SHORT_MAX_LENGTH, text.length)
    }

    @Test
    fun `textfältets fel läses som fel och visar enheten efter värdet`() {
        show { AppTextField("25", {}, "Dos", error = "Ange en dos", suffix = "mg") }
        rule.onNodeWithText("Dos").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        rule.onNodeWithText("mg", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `väljarfältet läses som en knapp med etikett, värde och fel och öppnar väljaren`() {
        var picks = 0
        show { PickerField("Avresa", "lör 19 dec 2026", "Välj datum", R.drawable.ic_clock, { picks++ }, error = "Välj ett senare datum") }
        rule.onNodeWithContentDescription("Avresa, lör 19 dec 2026, Välj ett senare datum")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, picks)
    }

    @Test
    fun `ett tomt väljarfält läser sin tomma text och kan ha ett eget namn för TalkBack`() {
        show { PickerField("Slutdatum", "", "Välj datum", R.drawable.ic_clock, {}, emptyLabel = "Periodens slut", contentLabel = "Slutdatum, doshöjning 2") }
        rule.onNodeWithContentDescription("Slutdatum, doshöjning 2, Periodens slut").assertExists()
    }

    @Test
    fun `ett tomt väljarfält utan tom text läser vad tryck gör`() {
        show { PickerField("Hemresa", "", "Välj datum", R.drawable.ic_clock, {}) }
        rule.onNodeWithContentDescription("Hemresa, Välj datum").assertExists()
    }

    @Test
    fun `tidsfältet visar tiden med två siffror och OK väljer tiden i väljaren`() {
        var time by mutableStateOf(LocalTime(7, 5))
        var changes = 0
        show {
            TimeField("Påminnelse", time, {
                time = it
                changes++
            })
        }
        rule.onNodeWithContentDescription("Påminnelse, 07:05").performClick()
        rule.onNodeWithText("Välj tid").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, changes)
        assertEquals(LocalTime(7, 5), time)
        rule.onNodeWithText("Välj tid").assertDoesNotExist()
    }

    @Test
    fun `tidsväljaren avbryts utan att tiden ändras`() {
        var picked: LocalTime? = null
        var dismissed = 0
        show { TimePickerPopup(LocalTime(21, 30), "Välj tid", onPick = { picked = it }, onDismiss = { dismissed++ }) }
        rule.onNodeWithText("Avbryt").performClick()
        assertEquals(null to 1, picked to dismissed)
    }

    @Test
    fun `gruppetiketten är en rubrik med versaler`() {
        show { GroupLabel("Utseende") }
        rule.onNodeWithText("UTSEENDE").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `felet ersätter hjälptexten under gruppen och läses som fel`() {
        var error by mutableStateOf<String?>(null)
        show { LabeledGroup("Form", helper = "Välj det som står på förpackningen", error = error) { AppButton("Tablett", {}) } }
        rule.onNodeWithText("Välj det som står på förpackningen").assertExists()
        error = "Välj en form"
        rule.onNodeWithText("Välj det som står på förpackningen").assertDoesNotExist()
        rule.onNodeWithText("Välj en form").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun `hjälptexten kan stå ovanför innehållet`() {
        show { LabeledGroup("Känslor", helper = "Börja i mitten", helperAbove = true) { AppButton("Glad", {}) } }
        val helper = rule.onNodeWithText("Börja i mitten").fetchSemanticsNode().positionInRoot.y
        val content = rule.onNodeWithText("Glad").fetchSemanticsNode().positionInRoot.y
        assertEquals(true, helper < content)
    }

    @Test
    fun `steppern ändrar med steget inom gränserna och läser värdet med etiketten`() {
        var value by mutableIntStateOf(1)
        show { QuantityStepper(value, { value = it }, "doser per dag", range = 1..3) }
        rule.onNodeWithContentDescription("Minska doser per dag").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Öka doser per dag").performClick().performClick()
        assertEquals(3, value)
        rule.onNodeWithContentDescription("3 doser per dag").assertExists()
        rule.onNodeWithContentDescription("Öka doser per dag").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Minska doser per dag").assertIsEnabled().assertTouchHeightIsEqualTo(48.dp)
    }

    @Test
    fun `steppern med värdetext och steg läser etiketten före texten`() {
        var value by mutableIntStateOf(0)
        show {
            QuantityStepper(
                value,
                { value = it },
                "minsta tid mellan doser",
                step = 30,
                valueText = { if (it == 0) "Ingen spärr" else "$it min" },
                buttonSize = QuantityStepperDefaults.TouchTargetButtonSize
            )
        }
        rule.onNodeWithContentDescription("minsta tid mellan doser Ingen spärr").assertExists()
        rule.onNodeWithContentDescription("Öka minsta tid mellan doser").assertTouchHeightIsEqualTo(48.dp).performClick()
        assertEquals(30, value)
        rule.onNodeWithText("30 min").assertExists()
    }

    @Test
    fun `stepperns knappstorlek är 44 dp som standard och appens via LocalQuantityStepperButtonSize`() {
        show {
            Column {
                QuantityStepper(1, {}, "tabletter", Modifier.testTag("standard"))
                CompositionLocalProvider(LocalQuantityStepperButtonSize provides QuantityStepperDefaults.TouchTargetButtonSize) {
                    QuantityStepper(1, {}, "minuter", Modifier.testTag("appens"))
                }
            }
        }
        val standard = rule.onNodeWithTag("standard").getBoundsInRoot().width
        val local = rule.onNodeWithTag("appens").getBoundsInRoot().width
        // Två knappar, var och en 4 dp större.
        assertEquals((QuantityStepperDefaults.TouchTargetButtonSize - QuantityStepperDefaults.ButtonSize) * 2, local - standard)
    }
}
