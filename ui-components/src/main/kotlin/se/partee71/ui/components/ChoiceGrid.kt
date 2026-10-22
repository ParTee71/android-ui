package se.partee71.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Ett rutnät av runda val där precis ett är valt – grunden för [ColorSwatchPicker] och appens
 * emojiväljare. TalkBack läser varje cell som ett alternativ ([label]) i en grupp och säger vilket
 * som är valt. [cell] ritar innehållet och får veta om det är valt. Med [selectionRing] äger
 * rutnätet valmarkeringen – en ring i primärfärgen runt den valda cellen, med luft mellan ringen och
 * innehållet (Dagboken); standard ritar [cell] sin egen markering. [ringColor] är ringens färg. [columns]
 * under 1 räknas som 1.
 */
@Composable
fun <T> ChoiceGrid(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    columns: Int = COLUMNS,
    selectionRing: Boolean = false,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    cell: @Composable (item: T, selected: Boolean) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Så många per rad som får plats med full tryckyta, högst [columns].
        val perRow = (maxWidth / TOUCH_TARGET).toInt().coerceIn(1, columns.coerceAtLeast(1))
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            remember(items, perRow) { items.chunked(perRow) }.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { item ->
                        val isSelected = item == selected
                        val description = label(item)
                        // Tryckytan är 48 dp; den synliga cirkeln är mindre.
                        Box(
                            Modifier.size(TOUCH_TARGET)
                                .clip(AppTheme.shapes.pill)
                                .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(item) })
                                .semantics { contentDescription = description },
                            contentAlignment = Alignment.Center
                        ) {
                            val ring = if (selectionRing && isSelected) Modifier.border(RING, ringColor, AppTheme.shapes.pill).padding(RING_GAP) else Modifier
                            Box(Modifier.size(CELL).then(ring), contentAlignment = Alignment.Center) { cell(item, isSelected) }
                        }
                    }
                    // En kortare sista rad följer rutnätet i stället för att spridas ut.
                    repeat(perRow - row.size) { Spacer(Modifier.size(TOUCH_TARGET)) }
                }
            }
        }
    }
}

/** Som mest åtta per rad; på en smal telefon blir det färre, så att tryckytan räcker. */
private const val COLUMNS = 8

private val CELL = 40.dp
private val RING = 2.dp
private val RING_GAP = 4.dp
