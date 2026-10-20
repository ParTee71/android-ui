package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/**
 * Ett val i [AppMenu], [AppMenuPopup] eller [AddSplitButton] (via [asSplitMenu]). [destructive] färgar
 * det som radering. [section] är en avdelning med rubrik: där den börjar står en avdelare och rubriken
 * (t.ex. "Recept"); `null` = ingen rubrik. [supporting] är en undertext under etiketten, i [supportingTone]
 * (t.ex. en varning) eller dämpad när den är `null`; [muted] tonar ned etiketten (något som inte går just nu
 * men fortfarande kan väljas).
 */
data class AppMenuItem(
    val label: String,
    val onClick: () -> Unit,
    @param:DrawableRes val icon: Int? = null,
    val destructive: Boolean = false,
    val section: String? = null,
    /**
     * Satt: posten är en **kryssrad** i menyn – bocken visas när `true`, platsen hålls tom när `false`,
     * TalkBack läser den som kryssruta, och menyn står kvar öppen så att flera kan väljas.
     * `null` (standard): ett vanligt val som stänger menyn.
     */
    val checked: Boolean? = null,
    val supporting: String? = null,
    val supportingTone: Tone? = null,
    val muted: Boolean = false
)

/** Menyn bakom "Fler val" (⋮): arkivera, radera, visa arkiverade m.m. */
@Composable
fun AppMenu(items: List<AppMenuItem>, modifier: Modifier = Modifier, contentDescription: String = stringResource(R.string.more_options)) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        AppIconButton(R.drawable.ic_more_vert, contentDescription, onClick = { expanded = true })
        AppMenuPopup(items, expanded, onDismiss = { expanded = false })
    }
}

/** Menyn bakom pilen i [AddSplitButton] (`menu`); `null` utan val – då visas ingen pil. */
fun List<AppMenuItem>.asSplitMenu(): (@Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit)? = if (isEmpty()) null else { expanded, onDismiss -> AppMenuPopup(this, expanded, onDismiss) }

/**
 * Själva menyn, förankrad i omgivande `Box` – delas av [AppMenu], [AddSplitButton], [CheckRow] och appens
 * egna knappar som öppnar en meny.
 */
@Composable
fun AppMenuPopup(items: List<AppMenuItem>, expanded: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = AppTheme.shapes.menu,
        containerColor = AppTheme.extendedColors.card
    ) {
        items.forEachIndexed { index, item ->
            val section = item.section
            if (section != null && section != items.getOrNull(index - 1)?.section) {
                if (index > 0) AppDivider(Modifier.padding(vertical = Spacing.xs))
                GroupLabel(section, Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs))
            }
            val color = when {
                item.destructive -> MaterialTheme.colorScheme.error
                item.muted -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
            val supportingColor = item.supportingTone?.let { AppTheme.tone(it).content } ?: MaterialTheme.colorScheme.onSurfaceVariant
            val checked = item.checked
            // En kryssrad visar bocken (eller håller platsen tom) och stänger inte menyn – flera kan väljas.
            val icon = if (checked == null) item.icon else R.drawable.ic_check.takeIf { checked }
            val semantics = if (checked == null) {
                Modifier
            } else {
                Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                }
            }
            DropdownMenuItem(
                text = {
                    val supporting = item.supporting
                    if (supporting == null) {
                        Text(item.label, style = AppTheme.typography.body)
                    } else {
                        Column {
                            Text(item.label, style = AppTheme.typography.body)
                            Text(supporting, style = AppTheme.typography.caption, color = supportingColor)
                        }
                    }
                },
                onClick = {
                    if (checked == null) onDismiss()
                    item.onClick()
                },
                modifier = semantics,
                leadingIcon = if (icon == null && checked == null) null else ({ icon?.let { Icon(painterResource(it), contentDescription = null) } ?: Spacer(Modifier.size(ICON)) }),
                colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color)
            )
        }
    }
}

/** Menyikonens storlek (Materials standard) – den tomma platsen för en okryssad kryssrad. */
private val ICON = 24.dp
