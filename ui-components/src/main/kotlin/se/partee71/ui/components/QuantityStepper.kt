package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/** Knappstorlekarna för [QuantityStepper]. */
object QuantityStepperDefaults {
    /** ReseApoteket: knapparna ryms i en lägre stepper. */
    val ButtonSize: Dp = 44.dp

    /** Full tryckyta, 48 dp (Dagboken). */
    val TouchTargetButtonSize: Dp = 48.dp
}

/**
 * Antal ± inom [range]. [label] beskriver vad som räknas ("doser per dag") och läses av
 * TalkBack på knapparna ("Minska doser per dag") och på värdet ("2 doser per dag"). [step] är
 * hur mycket ett tryck ändrar (t.ex. 5 för minuter). [valueText] visar värdet som text med sin
 * enhet ("4 h", "Ingen spärr" för 0); TalkBack läser då [label] och texten ("minsta tid mellan
 * doser 4 h") – [label] ska alltså inte ha någon enhet. Utan [valueText] visas talet och TalkBack
 * läser "2 doser per dag". [buttonSize] är knapparnas storlek ([QuantityStepperDefaults]).
 */
@Composable
fun QuantityStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    range: IntRange = 0..999,
    enabled: Boolean = true,
    step: Int = 1,
    valueText: (@Composable (Int) -> String)? = null,
    buttonSize: Dp = QuantityStepperDefaults.ButtonSize
) {
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = AppTheme.extendedColors.card,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    Row(
        modifier.background(AppTheme.extendedColors.rowTint, AppTheme.shapes.pill).padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        FilledIconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            modifier = Modifier.size(buttonSize),
            enabled = enabled && value > range.first,
            colors = colors
        ) { Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.decrease_format, label)) }
        val shown = valueText?.invoke(value)
        Text(
            shown ?: value.toString(),
            style = AppTheme.typography.quantity,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = VALUE_WIDTH).semantics {
                contentDescription = if (shown == null) "$value $label" else "$label $shown"
                liveRegion = LiveRegionMode.Polite
            }
        )
        FilledIconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            modifier = Modifier.size(buttonSize),
            enabled = enabled && value < range.last,
            colors = colors
        ) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.increase_format, label)) }
    }
}

private val VALUE_WIDTH = 48.dp
