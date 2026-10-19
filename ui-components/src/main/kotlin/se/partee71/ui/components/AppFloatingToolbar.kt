package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/** En flik i [AppFloatingToolbar]. */
data class ToolbarItem(val label: String, @param:DrawableRes val icon: Int)

/** Huvudknappen bredvid flikarna i [AppFloatingToolbar], t.ex. plusknappen som loggar. */
data class ToolbarAction(val label: String, @param:DrawableRes val icon: Int, val onClick: () -> Unit)

/** Plats som verktygsraden tar inklusive marginal. */
val FloatingToolbarClearance: Dp = 96.dp

/** Ingen plats längst ned – verktygsraden syns inte (undersidor). */
val NoBottomClearance: Dp = 0.dp

/**
 * Hur mycket plats den flytande verktygsraden tar längst ned. Navigationen sätter värdet när
 * raden syns; ramarna lägger till det under listan, knappen och snackbaren.
 */
val LocalBottomClearance = compositionLocalOf { NoBottomClearance }

/**
 * Toppnivånavigeringen: en flytande verktygsrad där vald flik visar ikon och namn och övriga bara ikon
 * (med namnet för TalkBack). [action] lägger en gör-något-knapp i `secondary` till höger om flikarna;
 * utan den (standard) visas bara flikarna.
 */
@Composable
fun AppFloatingToolbar(
    items: List<ToolbarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    action: ToolbarAction? = null
) {
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = AppTheme.extendedColors.toolbar,
        toolbarContentColor = AppTheme.extendedColors.onToolbar
    )
    val tabs: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, item ->
            ToolbarTab(item, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
    if (action == null) {
        HorizontalFloatingToolbar(expanded = true, modifier = modifier, colors = colors, content = tabs)
    } else {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.StandardFloatingActionButton(
                    onClick = action.onClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) { Icon(painterResource(action.icon), contentDescription = action.label) }
            },
            modifier = modifier,
            colors = colors,
            content = tabs
        )
    }
}

@Composable
private fun ToolbarTab(item: ToolbarItem, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else AppTheme.extendedColors.toolbar
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else AppTheme.extendedColors.onToolbar
    Row(
        Modifier
            .heightIn(min = TAB_HEIGHT)
            .widthIn(min = TAB_HEIGHT)
            .clip(AppTheme.shapes.pill)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = item.label }
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally)
    ) {
        Icon(painterResource(item.icon), contentDescription = null, tint = content, modifier = Modifier.size(ICON))
        if (selected) Text(item.label, style = AppTheme.typography.button, color = content, maxLines = 1)
    }
}

private val TAB_HEIGHT = 48.dp
private val ICON = 22.dp
