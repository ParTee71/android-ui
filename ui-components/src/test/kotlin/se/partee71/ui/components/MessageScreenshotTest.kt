package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.components.testing.sampleOffline

/**
 * Skärmdumpar av meddelandena (ljust + mörkt) i [AppSnackbarHost]. Klockan står still och går en
 * fast tid före varje bild – annars spolar `waitForIdle` fram tills meddelandet försvunnit.
 */
@RunWith(RobolectricTestRunner::class)
class MessageScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun capture(name: String, message: @Composable (SnackbarHostState) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.captureLightAndDark(name, settle = {
            mainClock.advanceTimeBy(SHOWN_MILLIS)
            waitForIdle()
        }) {
            val host = remember { SnackbarHostState() }
            message(host)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) { AppSnackbarHost(host) }
        }
    }

    @Test
    fun `ErrorSnackbar - fel i sitt sammanhang`() = capture("ErrorSnackbar_sammanhang") { host ->
        ErrorSnackbar(remember { sampleOffline() }, host, template = R.string.sample_sync_denied_format) {}
    }

    @Test
    fun `MessageSnackbar - bekräftelse`() = capture("MessageSnackbar_bekraftelse") { host ->
        MessageSnackbar("Alvedon 500 mg loggad", host) {}
    }

    @Test
    fun `UndoSnackbar - arkiverad med Ångra`() = capture("UndoSnackbar_angra") { host ->
        UndoSnackbar(UndoRequest("1", "Solkräm"), host, onUndo = {}, onDismissed = {})
    }

    private companion object {
        /** Inne i bild, långt före meddelandets fyra (ångra: fem) sekunder – också i den andra bilden. */
        const val SHOWN_MILLIS = 600L
    }
}
