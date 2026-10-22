package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Tone

enum class IconButtonVariant { Tonal, Plain }

/**
 * Den enda ikonknappen: minst 48 dp och alltid en [contentDescription] för TalkBack. [tone] färgar knappen
 * med temats ton – plattan och ikonen för [IconButtonVariant.Tonal], ikonen för [IconButtonVariant.Plain];
 * standard `null` = Material-standarden (`secondaryContainer` respektive omgivande innehållsfärg).
 */
@Composable
fun AppIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.Plain,
    enabled: Boolean = true,
    tone: Tone? = null
) {
    val sized = modifier.size(TOUCH_TARGET)
    val content: @Composable () -> Unit = { Icon(painterResource(icon), contentDescription) }
    val colors = tone?.let { AppTheme.tone(it) }
    when (variant) {
        IconButtonVariant.Tonal -> FilledTonalIconButton(
            onClick,
            sized,
            enabled,
            colors = colors?.let { IconButtonDefaults.filledTonalIconButtonColors(containerColor = it.container, contentColor = it.content) }
                ?: IconButtonDefaults.filledTonalIconButtonColors(),
            content = content
        )

        IconButtonVariant.Plain -> IconButton(
            onClick,
            sized,
            enabled,
            colors = colors?.let { IconButtonDefaults.iconButtonColors(contentColor = it.content) } ?: IconButtonDefaults.iconButtonColors(),
            content = content
        )
    }
}
