package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Tomt tillstånd: ikon, rubrik, en mening och en knapp som löser det. [fullScreen] fyller skärmen
 * med budskapet i mitten och knappen längst ned – för skärmar som bara består av budskapet
 * (inloggning, "Uppdatera appen"); annars står knappen under texten. [isError] tonar ikonrutan som
 * ett fel (listans feltillstånd, [LoadErrorState]). [compact] är samma budskap i mindre format för
 * en yta inuti ett kort – t.ex. ett diagrams tomma läge ("För lite data än").
 * [actions] är en eller flera knappar under [action], i full bredd och i den ordning de står
 * ("Ny transaktion", "Importera avräkningsnota"); standard inga.
 */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    fullScreen: Boolean = false,
    isError: Boolean = false,
    compact: Boolean = false,
    actions: List<EmptyAction> = emptyList(),
    action: (@Composable () -> Unit)? = null
) {
    val footer: @Composable ColumnScope.() -> Unit = {
        action?.let { Box(Modifier.fillMaxWidth()) { it() } }
        EmptyActionButtons(actions)
        note?.let {
            Text(
                it,
                style = AppTheme.typography.itemSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    if (fullScreen) {
        Column(modifier.fillMaxSize().padding(horizontal = Spacing.xl, vertical = Spacing.xxl)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Message(icon, title, message, isError, compact) }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), content = footer)
        }
    } else {
        val gap = if (compact) Spacing.l else Spacing.xl
        Column(
            modifier.fillMaxWidth().padding(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Message(icon, title, message, isError, compact)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), content = footer)
        }
    }
}

/**
 * En knapp i ett tomt tillstånd: text, åtgärd, valfri ikon och knappvariant (standard den fyllda
 * huvudknappen; en andra åtgärd brukar vara [ButtonVariant.Secondary] eller [ButtonVariant.Text]).
 */
data class EmptyAction(
    val label: String,
    val onClick: () -> Unit,
    @param:DrawableRes val icon: Int? = null,
    val variant: ButtonVariant = ButtonVariant.Primary
)

/**
 * Knapparna i [actions] under varandra i full bredd, efter [first] (listramens lägg till-knapp) – samma i
 * [EmptyState] och listramens tomma läge.
 */
@Composable
internal fun EmptyActionButtons(actions: List<EmptyAction>, first: (@Composable () -> Unit)? = null) {
    if (actions.isEmpty() && first == null) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        first?.invoke()
        actions.forEach { AppButton(it.label, it.onClick, Modifier.fillMaxWidth(), variant = it.variant, icon = it.icon) }
    }
}

@Composable
private fun Message(@DrawableRes icon: Int, title: String, message: String, isError: Boolean, compact: Boolean) {
    val tile = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val onTile = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) Spacing.m else Spacing.xl)
    ) {
        Box(
            Modifier.size(if (compact) COMPACT_ICON_TILE else ICON_TILE).background(tile, if (compact) AppTheme.shapes.smallTile else AppTheme.shapes.iconTile),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = onTile,
                modifier = Modifier.size(if (compact) COMPACT_ICON else ICON)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                title,
                style = if (compact) AppTheme.typography.sectionTitle else AppTheme.typography.headline,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                message,
                style = AppTheme.typography.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val ICON_TILE = 120.dp
private val ICON = 56.dp
private val COMPACT_ICON_TILE = 48.dp
private val COMPACT_ICON = 24.dp
