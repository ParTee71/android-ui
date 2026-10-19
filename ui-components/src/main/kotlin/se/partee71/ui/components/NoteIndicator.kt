package se.partee71.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Anteckningsikonen i en rads eller ett korts trailing-del ([CheckRow] med `note`): syns bara när [note]
 * har text; ett tryck visar anteckningen att läsa, med [title] som rubrik och en Stäng-knapp. Den
 * redigeras i postens formulär, aldrig här.
 */
@Composable
fun NoteIndicator(note: String, title: String, modifier: Modifier = Modifier) {
    if (note.isBlank()) return
    var open by rememberSaveable { mutableStateOf(false) }
    AppIconButton(R.drawable.ic_note, stringResource(R.string.note_show), { open = true }, modifier)
    if (open) {
        val close = { open = false }
        ConfirmDialog(title, note, stringResource(R.string.close), onConfirm = close, onDismiss = close, dismissLabel = null)
    }
}
