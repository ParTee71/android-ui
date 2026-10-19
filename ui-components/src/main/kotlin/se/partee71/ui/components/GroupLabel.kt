package se.partee71.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/** Liten etikett över en grupp kort, t.ex. "UTSEENDE" i Inställningar. Läses som rubrik. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = AppTheme.typography.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = Spacing.s).semantics { heading() }
    )
}

/**
 * Ett formulärfält som inte är ett textfält (val, väljare, dosering) med sin [GroupLabel] ovanför
 * och valfri [helper] under, i samma stil som hjälptexten under [AppTextField]; [error] (från
 * formulärets validering) ersätter hjälptexten. [helperAbove] ställer hjälptexten direkt under
 * etiketten i stället, när den säger hur man börjar; felet står alltid under.
 */
@Composable
fun LabeledGroup(
    label: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    helperAbove: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        GroupLabel(label)
        if (helperAbove && helper != null) HelperText(helper)
        content()
        if (error != null) {
            FieldError(error, Modifier.padding(horizontal = Spacing.l))
        } else if (helper != null && !helperAbove) {
            HelperText(helper)
        }
    }
}

/** Ett valideringsfel under ett fält som inte är ett textfält (t.ex. en grupp val), eller ett sparfel i en panel. */
@Composable
fun FieldError(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { error(text) }, style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun HelperText(text: String) {
    Text(text, Modifier.padding(horizontal = Spacing.l), style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
