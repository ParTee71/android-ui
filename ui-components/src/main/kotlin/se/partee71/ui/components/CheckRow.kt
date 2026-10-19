package se.partee71.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import se.partee71.ui.theme.AppTheme

/**
 * Kryssrad: en listrad med ett formmorfande kryss ([MorphingCheck]) och dämpad "klar"-stil när den är
 * ikryssad. Hela raden är tryckytan; TalkBack läser den som en kryssruta. Med [onClick] är krysset en egen
 * knapp (läses med titel och undertext, t.ex. "Levaxin 50 µg, 9 tabletter") och resten av raden öppnar
 * detaljer ([onClickLabel] läses upp). [accent] är en färg i vänsterkanten (personens färg, en status).
 *
 * [note] med text ger anteckningsikonen ([NoteIndicator]) efter [trailing]. [menu] är radens övriga
 * åtgärder: de nås med långtryck på raden och med `⋮` sist – där menyn finns går den före bubblan med hela
 * titeln. [below] står under titel och undertext, i linje med titeln. [enabled] = false visar tillståndet
 * utan att det går att växla; TalkBack läser en inaktiv kryssruta, och en [menu] nås ändå.
 */
@Composable
fun CheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    accent: Color? = null,
    note: String = "",
    menu: List<AppMenuItem> = emptyList(),
    below: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    var menuOpen by remember { mutableStateOf(false) }
    val toggle = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    val openMenu = if (menu.isEmpty()) null else ({ menuOpen = true })
    val menuLabel = stringResource(R.string.more_options)
    val interaction = when {
        onClick != null && openMenu == null -> Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick)

        onClick != null -> Modifier.combinedClickable(onClickLabel = onClickLabel, onLongClickLabel = menuLabel, onLongClick = openMenu, onClick = onClick)

        openMenu == null -> toggle

        // Inaktiv med meny: krysset växlar inte, men långtrycket öppnar menyn – samma roll, tillstånd och
        // "inaktiv" för TalkBack som utan meny.
        !enabled ->
            Modifier
                .combinedClickable(role = Role.Checkbox, onLongClickLabel = menuLabel, onLongClick = openMenu) {}
                .semantics {
                    toggleableState = ToggleableState(checked)
                    disabled()
                }

        // Långtrycket kräver en klickyta i stället för toggleable – samma roll och tillstånd för TalkBack.
        else ->
            Modifier
                .combinedClickable(role = Role.Checkbox, onLongClickLabel = menuLabel, onLongClick = openMenu) { onCheckedChange(!checked) }
                .semantics { toggleableState = ToggleableState(checked) }
    }
    val end: (@Composable () -> Unit)? = if (trailing == null && note.isBlank() && menu.isEmpty()) {
        null
    } else {
        {
            trailing?.invoke()
            NoteIndicator(note, title)
            if (menu.isNotEmpty()) {
                Box {
                    AppIconButton(R.drawable.ic_more_vert, menuLabel, onClick = { menuOpen = true })
                    AppMenuPopup(menu, menuOpen, onDismiss = { menuOpen = false })
                }
            }
        }
    }
    ItemRowLayout(
        title = title,
        modifier = modifier,
        interaction = interaction,
        subtitle = subtitle,
        leading = {
            // Samma storlek i båda varianterna, så att titlarna linjerar; med onClick är rutan knappen.
            val label = listOfNotNull(title, subtitle).joinToString(", ")
            val button = if (onClick == null) Modifier else Modifier.clip(AppTheme.shapes.pill).then(toggle).semantics { contentDescription = label }
            LeadingSlot(button) { MorphingCheck(checked) }
        },
        trailing = end,
        tinted = checked,
        done = checked,
        accent = accent,
        below = below,
        belowAlignsWithTitle = true,
        bubble = menu.isEmpty()
    )
}
