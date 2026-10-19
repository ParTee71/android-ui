package se.partee71.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.common.UiError
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.components.testing.sampleOffline

/** Meddelandena: fel, bekräftelser och ångra – vad som visas och när ViewModeln får veta det. */
@RunWith(RobolectricTestRunner::class)
class MessageBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    /** Visar [content] med en snackbarvärd; klockan går bara framåt när testet säger det. */
    private fun show(content: @Composable (SnackbarHostState) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            TestTheme {
                val host = remember { SnackbarHostState() }
                content(host)
                AppSnackbarHost(host)
            }
        }
        settle()
    }

    /**
     * Ändrar tillstånd från testet. Med stillastående klocka skickas ändringen inte alltid vidare till
     * kompositionen av sig själv (den globala snapshotens notiser schemaläggs på huvudtråden) – därför
     * skickas de här, så att nästa bildruta alltid ser ändringen.
     */
    private fun change(block: () -> Unit) {
        block()
        Snapshot.sendApplyNotifications()
    }

    private fun settle(millis: Long = FRAME_MILLIS) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `felet visas med sin text i sitt sammanhang, och ViewModeln får veta när det visats`() {
        var error by mutableStateOf<UiError?>(sampleOffline())
        var shown = 0
        show { host ->
            ErrorSnackbar(error, host, template = R.string.sample_sync_denied_format) {
                shown++
                error = null
            }
        }
        rule.onNodeWithText("Synken nekades: Ingen anslutning just nu. Det du sparar skickas när nätet är tillbaka.").assertExists()
        assertEquals(0, shown)
        settle(SHORT_SNACKBAR_MILLIS)
        assertEquals(1, shown)
    }

    @Test
    fun `samma fel två gånger i rad visas två gånger`() {
        var error by mutableStateOf<UiError?>(sampleOffline())
        var shown = 0
        show { host ->
            ErrorSnackbar(error, host) {
                shown++
                error = null
            }
        }
        settle(SHORT_SNACKBAR_MILLIS)
        assertEquals(1, shown, "det första visades")
        change { error = sampleOffline() }
        settle(SNACKBAR_ANIMATION_MILLIS)
        rule.onNodeWithText("Ingen anslutning just nu. Det du sparar skickas när nätet är tillbaka.").assertExists()
        settle(SHORT_SNACKBAR_MILLIS)
        assertEquals(2, shown)
    }

    @Test
    fun `en bekräftelse visas som meddelande`() {
        var shown = 0
        show { host -> MessageSnackbar("Alvedon 500 mg loggad", host) { shown++ } }
        rule.onNodeWithText("Alvedon 500 mg loggad").assertExists()
        settle(SHORT_SNACKBAR_MILLIS)
        assertEquals(1, shown)
    }

    @Test
    fun `Ångra anropar onUndo och inte onDismissed`() {
        var undone = 0
        var dismissed = 0
        show { host -> UndoSnackbar(UndoRequest("1", "Solkräm"), host, onUndo = { undone++ }, onDismissed = { dismissed++ }) }
        rule.onNodeWithText("Solkräm arkiverad").assertExists()
        rule.onNodeWithText("Ångra").performClick()
        settle()
        assertEquals(1 to 0, undone to dismissed)
    }

    @Test
    fun `utan Ångra går erbjudandet ut efter fem sekunder och säger vilket som gick ut`() {
        val request = UndoRequest("1", "Solkräm")
        var dismissed = 0
        var expired: UndoRequest? = null
        show { host -> UndoSnackbar(request, host, onUndo = {}, onDismissed = { dismissed++ }, onRequestDismissed = { expired = it }) }
        settle(UNDO_MILLIS - 2 * FRAME_MILLIS)
        assertEquals(0, dismissed)
        settle(2 * FRAME_MILLIS + FRAME_MILLIS)
        assertEquals(1, dismissed)
        assertEquals(request, expired)
    }

    @Test
    fun `att lämna skärmen medan ångra erbjuds räknas som att det gick ut`() {
        var visible by mutableStateOf(true)
        var dismissed = 0
        show { host -> if (visible) UndoSnackbar(UndoRequest("1", "Plåster"), host, onUndo = {}, onDismissed = { dismissed++ }) }
        change { visible = false }
        settle()
        assertEquals(1, dismissed)
    }

    private companion object {
        const val FRAME_MILLIS = 16L

        /** Den förra snackbaren hinner glida ut och den nya in. */
        const val SNACKBAR_ANIMATION_MILLIS = 1_000L

        /** `SnackbarDuration.Short` är 4 s; lite marginal för animationen. */
        const val SHORT_SNACKBAR_MILLIS = 5_000L
    }
}
