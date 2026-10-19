package se.partee71.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.components.testing.sampleNotFound
import se.partee71.ui.components.testing.sampleOffline
import se.partee71.ui.theme.Spacing

/** Ytorna: kort, rubrik, pill, tomt tillstånd och läsfel – beteende och TalkBack. */
@RunWith(RobolectricTestRunner::class)
class SurfaceBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    @Test
    fun `kortet ärver innehållsfärgen som standard och sätter sin egen när den anges`() {
        var inherited = Color.Unspecified
        var own = Color.Unspecified
        var onSurface = Color.Unspecified
        show {
            onSurface = MaterialTheme.colorScheme.onSurface
            AppCard { inherited = LocalContentColor.current }
            AppCard(contentColor = MaterialTheme.colorScheme.onSurface) { own = LocalContentColor.current }
        }
        rule.waitForIdle()
        assertNotEquals(onSurface, inherited, "utan contentColor ändras inget")
        assertEquals(onSurface, own)
    }

    @Test
    fun `en kortbit sätter sin innehållsfärg och har full luft bara mot kortets kant`() {
        var own = Color.Unspecified
        var accent = Color.Unspecified
        show {
            accent = MaterialTheme.colorScheme.primary
            Column {
                CardSegment(top = true, bottom = false, Modifier.testTag("första"), contentColor = accent) {
                    own = LocalContentColor.current
                    Text("Första")
                }
                CardSegment(top = false, bottom = true, Modifier.testTag("andra")) { Text("Andra") }
            }
        }
        rule.waitForIdle()
        assertEquals(accent, own)
        fun inset(tag: String, text: String) = rule.onNodeWithText(text).getBoundsInRoot().top - rule.onNodeWithTag(tag).getBoundsInRoot().top
        assertEquals(Spacing.m, inset("första", "Första"), "mot kortets överkant: full luft")
        assertEquals(2.dp, inset("andra", "Andra"), "mellan bitarna: halva radavståndet")
    }

    @Test
    fun `sektionsrubriken är en rubrik med sin räknare`() {
        show { SectionHeader("Mediciner", icon = R.drawable.ic_box, count = "4 / 5") }
        rule.onNodeWithText("Mediciner").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("4 / 5").assertExists()
    }

    @Test
    fun `en klickbar pill är en knapp med 48 dp tryckyta och sin etikett`() {
        var clicks = 0
        show { InfoPill("Synkas", onClick = { clicks++ }, onClickLabel = "Visa synkstatus") }
        rule.onNodeWithText("Synkas")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()
        assertEquals(1, clicks)
        assertEquals("Visa synkstatus", rule.onNodeWithText("Synkas").fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    @Test
    fun `en pill utan onClick går inte att trycka på`() {
        show { InfoPill("Krävs") }
        rule.onNodeWithText("Krävs").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `det tomma tillståndet har rubrik, knapp och not`() {
        var added = 0
        show { EmptyState(R.drawable.ic_box, "Inga resor än", "Lägg till en resa så räknar appen ut vad som ska packas.", note = "Du kan ändra allt senare.") { AppButton("Lägg till resa", { added++ }) } }
        rule.onNodeWithText("Inga resor än").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Du kan ändra allt senare.").assertExists()
        rule.onNodeWithText("Lägg till resa").performClick()
        assertEquals(1, added)
    }

    @Test
    fun `det kompakta tomma tillståndet har samma rubrik`() {
        show { EmptyState(R.drawable.ic_box, "För lite data än", "Logga några dagar till.", compact = true) }
        rule.onNodeWithText("För lite data än").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `läsfelet visar felets text och Försök igen`() {
        var retries = 0
        show { LoadErrorState("Kunde inte hämta uppgifterna", sampleOffline(), { retries++ }) }
        rule.onNodeWithText("Ingen anslutning just nu. Det du sparar skickas när nätet är tillbaka.").assertExists()
        rule.onNodeWithText("Försök igen").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `något som inte finns längre har inget Försök igen`() {
        show { LoadErrorState("Kunde inte hämta uppgifterna", sampleNotFound(), {}) }
        rule.onNodeWithText("Det finns inte längre – det kan ha raderats på en annan enhet.").assertExists()
        rule.onNodeWithText("Försök igen").assertDoesNotExist()
    }
}
