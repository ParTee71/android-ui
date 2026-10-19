package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone
import se.partee71.ui.theme.ToneColors

/**
 * Etikett eller räknare i en pill, i en av sektionstonerna ([Tone]). [icon] står före texten och
 * [trailingIcon] efter den (t.ex. en rullgardinspil); med [onClick] är pillen en knapp med minst
 * 48 dp tryckyta (t.ex. synkindikatorn i [AppTopBar]), med [role] och [onClickLabel] för TalkBack.
 * [colors] ger en ton utanför [Tone] (t.ex. en känslas egen ton); standard färgerna för [tone].
 */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Primary,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    @DrawableRes trailingIcon: Int? = null,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    colors: ToneColors = AppTheme.tone(tone)
) {
    val click = onClick?.let { Modifier.minimumInteractiveComponentSize().clip(AppTheme.shapes.pill).clickable(onClickLabel = onClickLabel, role = role, onClick = it) } ?: Modifier
    Row(
        modifier.then(click).background(colors.container, AppTheme.shapes.pill).padding(horizontal = Spacing.m, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { PillIcon(it, colors) }
        Text(text, style = AppTheme.typography.pill, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailingIcon?.let { PillIcon(it, colors) }
    }
}

@Composable
private fun PillIcon(@DrawableRes icon: Int, colors: ToneColors) {
    Icon(painterResource(icon), contentDescription = null, tint = colors.content, modifier = Modifier.size(ICON))
}

private val ICON = 16.dp
