package se.partee71.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.partee71.ui.common.UiError

/**
 * Läsfelet i ramarna (lista, redigering, detalj): samma ikon, text och "Försök igen" överallt – utom
 * för något som inte finns längre ([UiError.retryable] = false), som inte blir bättre av ett nytt
 * försök och då bara visar felet.
 */
@Composable
fun LoadErrorState(title: String, error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = if (error.retryable) R.drawable.ic_wifi_off else R.drawable.ic_box,
        title = title,
        message = stringResource(error.message),
        modifier = modifier,
        isError = true,
        action = if (error.retryable) {
            { AppButton(stringResource(R.string.retry), onRetry, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary) }
        } else {
            null
        }
    )
}
