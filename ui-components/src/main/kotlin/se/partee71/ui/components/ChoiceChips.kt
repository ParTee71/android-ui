package se.partee71.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Ett val bland några få: en [ChipRow] med en [AppFilterChip] per alternativ, det valda markerat
 * (form, enhet, kategori). [label] ger alternativets text. Med [onClear] avmarkerar ett tryck på det
 * valda alternativet valet (anropar [onClear] i stället för [onSelect]) – för val som får vara ej
 * angivna; standard väljer ett tryck alltid.
 */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    ChipRow(modifier) {
        options.forEach { option ->
            val isSelected = option == selected
            AppFilterChip(label(option), selected = isSelected, onClick = { if (isSelected && onClear != null) onClear() else onSelect(option) })
        }
    }
}

/**
 * Flera val bland några få (t.ex. var i kroppen det sitter): samma utseende som envalet, men varje
 * alternativ i [selected] är markerat och ett tryck växlar det ([onToggle]). TalkBack läser varje
 * chip med namn och om det är valt. Anropa med namngivet `onToggle`, så att envalet inte väljs.
 */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    ChipRow(modifier) {
        options.forEach { option -> AppFilterChip(label(option), selected = option in selected, onClick = { onToggle(option) }) }
    }
}
