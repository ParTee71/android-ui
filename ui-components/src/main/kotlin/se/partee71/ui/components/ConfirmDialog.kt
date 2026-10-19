package se.partee71.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Bekräftelse. Permanent radering bekräftas alltid här med [destructive] = true ([DeleteConfirmDialog]) –
 * aldrig med svep. [onDecline] är nej-knappen när den betyder något annat än att stänga (t.ex. "Behåll
 * listan"); tryck utanför är alltid [onDismiss]. Med [dismissLabel] = `null` visas bara en knapp – för ett
 * meddelande som bara ska läsas ([NoteIndicator]). [content] står under texten – ett fält som frågan
 * behöver (t.ex. ett slutdatum); `null` (standard) = bara texten.
 *
 * **Bredden med [content]**: i stället för plattformens fönsterbredd fyller dialogen skärmen minus
 * `Spacing.xl` på var sida, högst 560 dp (M3:s tak). Ett textfält i ett fönster med plattformens
 * WRAP_CONTENT-bredd mäts annars med flera bredder per layout, och Robolectric-testernas väntan på vila tar
 * aldrig slut. Utan [content] är dialogen som förut.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    dismissLabel: String? = stringResource(R.string.cancel),
    onDecline: () -> Unit = onDismiss,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppButton(confirmLabel, onConfirm, variant = if (destructive) ButtonVariant.Destructive else ButtonVariant.Primary)
        },
        modifier = if (content == null) modifier else modifier.padding(horizontal = Spacing.xl).widthIn(max = DIALOG_MAX_WIDTH),
        dismissButton = dismissLabel?.let { { AppButton(it, onDecline, variant = ButtonVariant.Text) } },
        title = { Text(title, style = AppTheme.typography.headline) },
        text = {
            val text = @Composable { Text(message, style = AppTheme.typography.body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (content == null) {
                text()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                    text()
                    content()
                }
            }
        },
        shape = AppTheme.shapes.card,
        containerColor = AppTheme.extendedColors.card,
        properties = if (content == null) DialogProperties() else DialogProperties(usePlatformDefaultWidth = false)
    )
}

/**
 * En permanent radering som väntar på bekräftelse: [title] (t.ex. "Radera Alvedon?" –
 * `R.string.delete_title_format`), [message] och vad "Radera" gör ([onConfirm]).
 */
data class DeleteAction(val title: String, val message: String, val onConfirm: () -> Unit)

/**
 * Permanent radering efter bekräftelse: [action]s titel och text och "Radera" i felfärg; "Radera" stänger
 * dialogen ([onDismiss]) och raderar. En gång för redigeraskärmen, kort och rader.
 */
@Composable
fun DeleteConfirmDialog(action: DeleteAction, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ConfirmDialog(
        action.title,
        action.message,
        stringResource(R.string.delete),
        onConfirm = {
            onDismiss()
            action.onConfirm()
        },
        onDismiss = onDismiss,
        modifier = modifier,
        destructive = true
    )
}

/**
 * "Släng ändringar?" – den enda frågan när ett formulär med osparade ändringar ska stängas (redigeraskärmen
 * och [AppBottomSheet] med `dirty`). [onConfirm] slänger och går vidare; [onDismiss] är "Fortsätt redigera".
 */
@Composable
fun DiscardChangesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ConfirmDialog(
        title = stringResource(R.string.discard_title),
        message = stringResource(R.string.discard_message),
        confirmLabel = stringResource(R.string.discard_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        dismissLabel = stringResource(R.string.keep_editing)
    )
}

private val DIALOG_MAX_WIDTH = 560.dp
