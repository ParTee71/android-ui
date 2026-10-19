package se.partee71.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.partee71.ui.components.R
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * "Lägg till ▾": huvudåtgärden och, om [menu] finns, en pil med fler val. Utan meny
 * blir den en vanlig sekundär [AppButton] med plus – samma plats och utseende.
 *
 * [menu] ritar menyn (vanligen `items.asSplitMenu()`, som visar [AppMenuPopup]), förankrad under knappen:
 * den får om menyn är öppen och vad som stänger den.
 */
@Composable
fun AddSplitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit)? = null,
    menuDescription: String = stringResource(R.string.more_options)
) {
    if (menu == null) {
        AppButton(label, onClick, modifier, variant = ButtonVariant.Secondary, icon = R.drawable.ic_add)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pil")
    val colors = ButtonDefaults.filledTonalButtonColors()
    Box(modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = onClick, modifier = Modifier.heightIn(min = HEIGHT), colors = colors) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(ICON))
                    Spacer(Modifier.width(Spacing.s))
                    Text(label, style = AppTheme.typography.button)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    modifier = Modifier.heightIn(min = HEIGHT),
                    colors = colors
                ) {
                    Icon(
                        painterResource(R.drawable.ic_expand_more),
                        contentDescription = menuDescription,
                        modifier = Modifier.size(ICON).rotate(rotation)
                    )
                }
            }
        )
        menu(expanded) { expanded = false }
    }
}

private val HEIGHT = 56.dp
private val ICON = 20.dp
