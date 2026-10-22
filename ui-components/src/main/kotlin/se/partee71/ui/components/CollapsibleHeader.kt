package se.partee71.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Titelraden i allt som fälls ihop – hopfällbara grupper i [EntityListScreen] och appens egna
 * hopfällbara sektioner: hela raden växlar, minst 48 dp, läses som knapp med "Fäll ut"/"Fäll ihop"
 * ([ExpandLabels.action]) och läget som `stateDescription` ([ExpandLabels.state]); chevronen sist
 * fjädrar ett halvt varv ([chevronRotation]). [content] står före chevronen i en rad.
 */
@Composable
fun CollapsibleHeader(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val labels = expandLabels(expanded)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = HEADER_MIN_HEIGHT)
            .clip(AppTheme.shapes.row)
            .clickable(onClickLabel = labels.action, role = Role.Button, onClick = onToggle)
            .semantics { stateDescription = labels.state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        content()
        Icon(
            painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevronRotation(expanded))
        )
    }
}

/**
 * Åtgärd ("Fäll ut"/"Fäll ihop") och läge ("Hopfälld"/"Utfälld") för TalkBack – samma i allt som
 * fälls ut, t.ex. en egen chevronknapp på ett kort ([expandLabels]).
 */
@Immutable
class ExpandLabels(val action: String, val state: String)

/** [ExpandLabels] för [expanded], ur bibliotekets strängar (`expand`, `collapse`, `expanded`, `collapsed`). */
@Composable
fun expandLabels(expanded: Boolean): ExpandLabels = ExpandLabels(
    action = stringResource(if (expanded) R.string.collapse else R.string.expand),
    state = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
)

/** Chevronens fjädrande vridning i grader – ett halvt varv i utfällt läge, för `Modifier.rotate`. */
@Composable
fun chevronRotation(expanded: Boolean): Float {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) HALF_TURN else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "chevron"
    )
    return rotation
}

/** En hopfällbar rubrik är minst en tryckyta hög. */
private val HEADER_MIN_HEIGHT = 48.dp

private const val HALF_TURN = 180f
