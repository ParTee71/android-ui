package se.partee71.ui.components.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Stand-ins för appernas egna komponenter (rad, kort, meny) som inte finns i biblioteket än –
 * bara så att bibliotekets komponenter kan provas och fotograferas med innehåll.
 */
@Composable
fun SampleRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
    ) {
        Text(title, style = AppTheme.typography.itemTitle)
        subtitle?.let { Text(it, style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Ett kort med appens kortyta och form. */
@Composable
fun SampleCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(AppTheme.extendedColors.card, AppTheme.shapes.card).padding(Spacing.m),
        content = content
    )
}

/** Ytan bilderna tas på: bakgrunden och marginal. */
@Composable
fun SampleSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        content = content
    )
}

/** En meny för [se.partee71.ui.components.AddSplitButton] – som appens `AppMenuPopup`. */
fun sampleMenu(vararg items: Pair<String, () -> Unit>): @Composable (Boolean, () -> Unit) -> Unit = { expanded, onDismiss ->
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = AppTheme.shapes.menu, containerColor = AppTheme.extendedColors.card) {
        items.forEach { (label, action) ->
            DropdownMenuItem(text = { Text(label, style = AppTheme.typography.body) }, onClick = {
                onDismiss()
                action()
            })
        }
    }
}
