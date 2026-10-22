package se.partee71.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleRow
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.theme.ToneColors

/** Hopfällbar rubrik, långtryck och svepets bakgrund – delarna apparna bygger egna komponenter av. */
@RunWith(RobolectricTestRunner::class)
class GestureBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { TestTheme(content = content) }

    @Test
    fun `den hopfällbara rubriken växlar på hela raden och läses som knapp med åtgärd och läge`() {
        var expanded by mutableStateOf(false)
        show { CollapsibleHeader(expanded, { expanded = !expanded }) { Text("Recept", Modifier.weight(1f)) } }
        val header = rule.onNodeWithText("Recept")
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Hopfälld"))
            .assertHeightIsAtLeast(48.dp)
        assertEquals("Fäll ut", header.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        header.performClick()
        assertEquals(true, expanded)
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Utfälld"))
        assertEquals("Fäll ihop", header.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    @Test
    fun `etiketterna och chevronens vridning följer läget`() {
        var expanded by mutableStateOf(false)
        var labels: ExpandLabels? = null
        var rotation = -1f
        show {
            labels = expandLabels(expanded)
            rotation = chevronRotation(expanded)
        }
        rule.waitForIdle()
        assertEquals("Fäll ut" to "Hopfälld", labels?.let { it.action to it.state })
        assertEquals(0f, rotation)
        expanded = true
        rule.waitForIdle()
        assertEquals("Fäll ihop" to "Utfälld", labels?.let { it.action to it.state })
        assertEquals(180f, rotation)
    }

    @Test
    fun `långtrycket kör åtgärden i stället för ytans tryck, och ett kort tryck går igenom`() {
        var clicks = 0
        var presses = 0
        show { Box(Modifier.size(SIZE).testTag(TAG).longPress("Fler val", { presses++ }).clickable { clicks++ }) }
        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        assertEquals(1 to 0, presses to clicks)
        rule.onNodeWithTag(TAG).performClick()
        assertEquals(1 to 1, presses to clicks)
    }

    @Test
    fun `en gest som komponenten själv förbrukar blir inget långtryck`() {
        var presses = 0
        val consumeMoves = Modifier.pointerInput(Unit) { awaitEachGesture { while (awaitPointerEvent().changes.onEach { if (it.positionChanged()) it.consume() }.any { it.pressed }) Unit } }
        show { Box(Modifier.size(SIZE).testTag(TAG).longPress("Fler val", { presses++ }).then(consumeMoves)) }
        rule.onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(1f, 0f))
            advanceEventTime(LONG_PRESS_MILLIS)
            up()
        }
        assertEquals(0, presses)
    }

    @Test
    fun `ett finger som rör sig är en rullning, inget långtryck`() {
        var presses = 0
        show { Box(Modifier.size(SIZE).testTag(TAG).longPress("Fler val", { presses++ })) }
        rule.onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(width / 2f, 0f))
            advanceEventTime(LONG_PRESS_MILLIS)
            up()
        }
        assertEquals(0, presses)
    }

    @Test
    fun `långtrycket är TalkBacks långtrycksåtgärd som standard och en anpassad åtgärd på begäran`() {
        var presses = 0
        show {
            Column {
                Box(Modifier.size(SIZE).testTag("lång").longPress("Fler val", { presses++ }))
                Box(Modifier.size(SIZE).testTag("anpassad").longPress("Starkare", { presses++ }, semantics = LongPressSemantics.CustomAction))
            }
        }
        val long = rule.onNodeWithTag("lång").fetchSemanticsNode().config[SemanticsActions.OnLongClick]
        assertEquals("Fler val", long.label)
        long.action?.invoke()
        val custom = rule.onNodeWithTag("anpassad").fetchSemanticsNode().config
        assertEquals(listOf("Starkare"), custom[SemanticsActions.CustomActions].map { it.label })
        assertFalse(SemanticsActions.OnLongClick in custom)
        custom[SemanticsActions.CustomActions].single().action()
        assertEquals(2, presses)
    }

    @Test
    fun `utan åtgärd är ytan som förut`() {
        show { Box(Modifier.size(SIZE).testTag(TAG).longPress("Fler val", null)) }
        rule.onNodeWithTag(TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun `haptiken körs vid långtrycket bara när den är påslagen`() {
        val haptics = mutableListOf<String>()
        val recorder = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType.toString()
            }
        }
        show {
            CompositionLocalProvider(LocalHapticFeedback provides recorder) {
                Column {
                    Box(Modifier.size(SIZE).testTag("tyst").longPress("Fler val", {}))
                    Box(Modifier.size(SIZE).testTag("haptik").longPress("Starkare", {}, haptic = true))
                }
            }
        }
        rule.onNodeWithTag("tyst").performTouchInput { longClick() }
        assertEquals(emptyList(), haptics)
        rule.onNodeWithTag("haptik").performTouchInput { longClick() }
        assertEquals(listOf(HapticFeedbackType.LongPress.toString()), haptics)
    }

    @Test
    fun `en ny lambda mellan långtrycken byter åtgärd utan att starta om gesten`() {
        var round by mutableIntStateOf(0)
        val calls = mutableListOf<Int>()
        show {
            // Lambdan fångar värdet vid kompositionen: en gammal lambda skulle lägga till 0 två gånger.
            val current = round
            Box(
                Modifier.size(SIZE).testTag(TAG).longPress("Fler val", {
                    calls += current
                    round++
                })
            )
        }
        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        assertEquals(listOf(0, 1), calls)
    }

    @Test
    fun `svepets bakgrund visar etiketten, och appen kan dölja bilden för TalkBack`() {
        show {
            Box {
                SwipeBackground("Arkivera", R.drawable.ic_archive)
                SwipeBackground("Radera", R.drawable.ic_delete, Modifier.clearAndSetSemantics {})
            }
        }
        rule.onNodeWithText("Arkivera").assertExists()
        rule.onNodeWithText("Radera").assertDoesNotExist()
    }

    @Test
    fun `svep med egen ikon och ton arkiverar som förut`() {
        var hidden = 0
        show {
            SwipeToHide(
                onHide = { hidden++ },
                label = "Radera",
                icon = R.drawable.ic_delete,
                colors = ToneColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            ) { SampleRow("Solkräm", it, onClick = {}) }
        }
        rule.onNodeWithText("Solkräm").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(1, hidden)
    }

    private companion object {
        const val TAG = "yta"
        const val LONG_PRESS_MILLIS = 1_000L
        val SIZE = 100.dp
    }
}
