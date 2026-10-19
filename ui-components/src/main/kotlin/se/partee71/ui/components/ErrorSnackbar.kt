package se.partee71.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import se.partee71.ui.common.UiError

/**
 * Ett fel från en åtgärd i ramen (t.ex. arkivera, spara) som meddelande i [hostState], med felets
 * text ([UiError.message]) – samma texter överallt. [template] (`%1$s` = felets text) sätter felet i
 * ett sammanhang, t.ex. en nekad synk. [key] gör samma fel två gånger i rad till två meddelanden
 * (standard: felet självt, och varje [UiError] är ett eget). [onShown] anropas när det visats, så att
 * ViewModeln släpper det.
 */
@Composable
fun ErrorSnackbar(
    error: UiError?,
    hostState: SnackbarHostState,
    @StringRes template: Int? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    key: Any? = error,
    onShown: () -> Unit
) {
    val text = error?.let { stringResource(it.message) }?.let { text -> template?.let { stringResource(it, text) } ?: text }
    MessageSnackbar(text, hostState, key = key, duration = duration, onShown = onShown)
}

/**
 * Ett meddelande ([text]) i [hostState] – det enda sättet att visa en text som snackbar: fel via
 * [ErrorSnackbar], bekräftelser direkt (t.ex. "Alvedon 500 mg loggad"). [key] gör samma text två
 * gånger i rad till två meddelanden; [onShown] anropas när det visats, så att ViewModeln släpper det.
 */
@Composable
fun MessageSnackbar(
    text: String?,
    hostState: SnackbarHostState,
    key: Any? = text,
    duration: SnackbarDuration = SnackbarDuration.Short,
    onShown: () -> Unit
) {
    val shown by rememberUpdatedState(onShown)
    val current by rememberUpdatedState(text)
    LaunchedEffect(key) {
        val message = current ?: return@LaunchedEffect
        hostState.showSnackbar(message, duration = duration)
        shown()
    }
}
