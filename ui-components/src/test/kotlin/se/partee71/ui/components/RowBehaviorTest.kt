package se.partee71.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.Blue
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/** Listraderna: ItemRow, CheckRow, SwitchRow, PausableRow, förslagsfältet och anteckningsikonen. */
@RunWith(RobolectricTestRunner::class)
class RowBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    private fun count(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun `en kapad titel visar hela titeln och undertexten i en bubbla vid långtryck`() {
        show { ItemRow(LONG_TITLE, Modifier.width(NARROW), subtitle = "2 tabletter per dag") }
        assertEquals(1, count(LONG_TITLE))
        rule.onNodeWithText(LONG_TITLE).performTouchInput { longClick() }
        assertEquals(2, count(LONG_TITLE), "bubblan visar hela titeln")
        assertEquals(2, count("2 tabletter per dag"), "och undertexten")
    }

    @Test
    fun `en titel som får plats har ingen bubbla`() {
        show { ItemRow("Alvedon", subtitle = "1 tablett") }
        rule.onNodeWithText("Alvedon").performTouchInput { longClick() }
        assertEquals(1, count("Alvedon"))
    }

    @Test
    fun `en navigerande rad anropar onClick och visar pilen efter trailing`() {
        var clicks = 0
        show { ItemRow("Resor", onClick = { clicks++ }, navigates = true, trailing = { InfoPill("3") }) }
        rule.onNodeWithText("Resor").performClick()
        assertEquals(1, clicks)
        rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `below linjerar med titeln efter en ikon när belowAlignsWithTitle`() {
        show {
            ItemRow("Promenad", icon = R.drawable.ic_clock, below = { Text("Försenat") }, belowAlignsWithTitle = true)
        }
        val title = rule.onNodeWithText("Promenad").getBoundsInRoot().left
        val below = rule.onNodeWithText("Försenat").getBoundsInRoot().left
        assertEquals(title, below)
    }

    @Test
    fun `avståndet mellan titel och undertext är 2 dp som standard och appens via LocalItemRowTextGap`() {
        fun gap() = rule.onNodeWithText("1 tablett").getBoundsInRoot().top - rule.onNodeWithText("Alvedon").getBoundsInRoot().bottom
        rule.setContent {
            TestTheme {
                Column {
                    ItemRow("Alvedon", subtitle = "1 tablett")
                    CompositionLocalProvider(LocalItemRowTextGap provides Spacing.xs) { ItemRow("Ipren", subtitle = "2 tabletter") }
                }
            }
        }
        assertEquals(2.dp, gap())
        val ipren = rule.onNodeWithText("2 tabletter").getBoundsInRoot().top - rule.onNodeWithText("Ipren").getBoundsInRoot().bottom
        assertEquals(Spacing.xs, ipren)
    }

    @Test
    fun `kryssraden är en kryssruta som växlar på hela raden`() {
        val changes = mutableListOf<Boolean>()
        show { CheckRow("Solkräm", false, { changes += it }, subtitle = "1 tub") }
        rule.onNodeWithText("Solkräm").assertIsOff().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun `med onClick är krysset en egen knapp och raden öppnar detaljer`() {
        val changes = mutableListOf<Boolean>()
        var opened = 0
        show { CheckRow("Levaxin 50 µg", false, { changes += it }, subtitle = "9 tabletter", onClick = { opened++ }, onClickLabel = "Ändra mängd") }
        rule.onNodeWithContentDescription("Levaxin 50 µg, 9 tabletter").performClick()
        rule.onNodeWithText("Levaxin 50 µg").performClick()
        assertEquals(listOf(true), changes)
        assertEquals(1, opened)
    }

    @Test
    fun `kryssraden med meny - tryck växlar, långtryck och ⋮ öppnar menyn och anteckningen går att läsa`() {
        val changes = mutableListOf<Boolean>()
        var skipped = 0
        show {
            CheckRow(
                "Levaxin 100 µg",
                false,
                { changes += it },
                note = "Tas på fastande mage.",
                menu = listOf(AppMenuItem("Hoppa över", { skipped++ }, R.drawable.ic_remove)),
                below = { InfoPill("Försenat") }
            )
        }
        rule.onNodeWithText("Levaxin 100 µg").assertIsOff().performClick()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("Försenat").assertIsDisplayed()

        rule.onNodeWithText("Levaxin 100 µg").performTouchInput { longClick() }
        assertEquals(listOf(true), changes, "långtrycket växlar inte")
        rule.onNodeWithText("Hoppa över").performClick()
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Hoppa över").performClick()
        assertEquals(2, skipped)

        rule.onNodeWithContentDescription("Visa anteckning").performClick()
        rule.onNodeWithText("Tas på fastande mage.").assertIsDisplayed()
        rule.onNodeWithText("Stäng").performClick()
        rule.onNodeWithText("Tas på fastande mage.").assertDoesNotExist()
    }

    @Test
    fun `med meny går menyn före bubblan på en kapad titel`() {
        show { CheckRow(LONG_TITLE, false, {}, Modifier.width(NARROW), menu = listOf(AppMenuItem("Hoppa över", {}))) }
        rule.onNodeWithText(LONG_TITLE).performTouchInput { longClick() }
        rule.onNodeWithText("Hoppa över").assertIsDisplayed()
        assertEquals(1, count(LONG_TITLE), "ingen bubbla")
    }

    @Test
    fun `utan meny visar en kapad kryssrad bubblan`() {
        show { CheckRow(LONG_TITLE, false, {}, Modifier.width(NARROW)) }
        rule.onNodeWithText(LONG_TITLE).performTouchInput { longClick() }
        assertEquals(2, count(LONG_TITLE))
    }

    @Test
    fun `en inaktiv kryssrad med meny växlar inte men menyn nås`() {
        val changes = mutableListOf<Boolean>()
        var deleted = 0
        show { CheckRow("Ipren 400 mg", true, { changes += it }, menu = listOf(AppMenuItem("Radera", { deleted++ }, destructive = true)), enabled = false) }
        rule.onNodeWithText("Ipren 400 mg").assertIsOn().assertIsNotEnabled().performClick()
        rule.onNodeWithText("Ipren 400 mg").performTouchInput { longClick() }
        rule.onNodeWithText("Radera").performClick()
        assertEquals(1, deleted)
        assertEquals(emptyList(), changes)
    }

    @Test
    fun `en inaktiv kryssrad utan meny växlar inte`() {
        val changes = mutableListOf<Boolean>()
        show { CheckRow("Ipren 400 mg", true, { changes += it }, enabled = false) }
        rule.onNodeWithText("Ipren 400 mg").assertIsNotEnabled().performClick()
        assertEquals(emptyList(), changes)
    }

    @Test
    fun `växelraden växlar på hela raden och läses som en växel`() {
        val changes = mutableListOf<Boolean>()
        show { SwitchRow("Påminnelser", false, { changes += it }, inactive = true) }
        rule.onNodeWithText("Påminnelser").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun `med onClick är växeln en egen kontroll med titeln och raden öppnar detaljer`() {
        val changes = mutableListOf<Boolean>()
        var opened = 0
        show { SwitchRow("Morgondos", true, { changes += it }, onClick = { opened++ }) }
        rule.onNodeWithContentDescription("Morgondos").performClick()
        rule.onNodeWithText("Morgondos").performClick()
        assertEquals(listOf(false), changes)
        assertEquals(1, opened)
    }

    @Test
    fun `en pausad rad säger Pausat och att den inte räknas med, en aktiv visar typen`() {
        var opened = 0
        show {
            Column {
                PausableRow("Solkräm", "1 tub per 7 dagar", "Per N dagar", active = true, onClick = { opened++ }, pillTone = Tone.Blue)
                PausableRow("Hydrokortison", "1 tub", "Fast antal", active = false, onClick = {}, pillTone = Tone.Blue)
            }
        }
        rule.onNodeWithText("Per N dagar", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Pausat", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Pausat – räknas inte med", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Fast antal", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Solkräm").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `förslagen läser antalet och ett tryck väljer förslaget`() {
        var picked = -1
        show {
            SuggestionField(
                "alv",
                {},
                "Namn",
                listOf(Suggestion("Alvedon 500 mg", "Tablett", 0..2), Suggestion("Alvedon 665 mg", "Depottablett", 0..2)),
                { picked = it },
                footer = "Från Läkemedelsverket"
            )
        }
        rule.onNodeWithContentDescription("2 förslag").assertExists()
        rule.onNodeWithText("Alvedon 665 mg").performClick()
        assertEquals(1, picked)
        rule.onNodeWithText("Från Läkemedelsverket", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `utan förslag visas bara fältet`() {
        show { SuggestionField("x", {}, "Namn", emptyList(), {}) }
        rule.onNodeWithContentDescription("0 förslag").assertDoesNotExist()
        rule.onNodeWithText("Namn").assertExists()
    }

    @Test
    fun `anteckningsikonen syns bara med text`() {
        show { NoteIndicator(" ", "Levaxin") }
        rule.onNodeWithContentDescription("Visa anteckning").assertDoesNotExist()
    }

    private companion object {
        const val LONG_TITLE = "Hydrokortison 1 % kräm för eksem och myggbett på armar och ben under resan"
        val NARROW = 220.dp
    }
}
