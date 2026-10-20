package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Väntande läge på ett [AppFilterChip]: något som inte går just nu (t.ex. en kylperiod eller en nådd
 * dagsgräns). Chipet tonas med streckad kant, [icon] står före texten och [text] efter den (t.ex.
 * "från 15:40"). TalkBack läser [description] (t.ex. "för tidigt, nästa dos från 15:40") efter
 * etiketten i stället för [text]; läget bärs av ikon och text, inte bara av färg.
 */
@Immutable
data class ChipPending(@param:DrawableRes val icon: Int, val text: String, val description: String)

/**
 * Valchip: bock och temats `primaryContainer` när det är valt. Används ensamt eller i en [ChipRow].
 * [icon] står före texten när chipet inte är valt. Med [onLongClick] öppnar ett långtryck något mer
 * (t.ex. en meny) och TalkBack erbjuder det som åtgärd med [onLongClickLabel]; tryck är fortfarande
 * [onClick]. Långtrycket fungerar också när chipet är inaktivt ([enabled] = false).
 *
 * [value] står efter etiketten (t.ex. styrkan "4" i "Huvudvärk 4") och läses som en egen del av TalkBack.
 * [emphasized] är det aktiva bland flera valda: fylld primärfärg i stället för `primaryContainer`.
 * [pending] ger det väntande läget ([ChipPending]). Standardvärdena bevarar det vanliga chipet.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    value: String? = null,
    emphasized: Boolean = false,
    pending: ChipPending? = null
) {
    val leading = if (selected) R.drawable.ic_check else pending?.icon ?: icon
    // Gesten startas en gång (nyckel Unit) och läser alltid den senaste callbacken.
    val longClick by rememberUpdatedState(onLongClick)
    val dashColor = MaterialTheme.colorScheme.outline
    val shape = AppTheme.shapes.pill
    val chipModifier = Modifier
        .heightIn(min = CHIP_HEIGHT)
        .then(if (onLongClick != null) Modifier.longPress({ longClick?.invoke() }, onLongClickLabel) else Modifier)
    val chip = @Composable { chipModifier: Modifier ->
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = AppTheme.typography.pill)
                    value?.let { Text(it, style = AppTheme.typography.pill) }
                    // Den synliga tilläggstexten ersätts för TalkBack av den fullständiga beskrivningen.
                    pending?.let { Text(it.text, style = AppTheme.typography.pill, modifier = Modifier.clearAndSetSemantics { }) }
                }
            },
            modifier = chipModifier,
            enabled = enabled,
            leadingIcon = leading?.let { { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(ICON)) } },
            shape = shape,
            colors = when {
                pending != null -> pendingChipColors()
                emphasized -> emphasizedChipColors()
                else -> chipColors()
            },
            border = if (pending != null) null else FilterChipDefaults.filterChipBorder(enabled = enabled, selected = selected)
        )
    }
    if (pending == null) {
        chip(modifier.then(chipModifier))
    } else {
        // Tryckytan (48 dp) läggs runt chipet här i stället för inuti FilterChip, så att den streckade kanten
        // ritas runt själva chipet och inte runt tryckytan.
        Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                chip(chipModifier.semantics { stateDescription = pending.description }.dashedOutline(shape, dashColor))
            }
        }
    }
}

/** Samma färger för alla chips – även appens egna (t.ex. en personchip). */
@Composable
fun chipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    labelColor = MaterialTheme.colorScheme.onSurface
)

/** Det aktiva chipet bland flera valda ([AppFilterChip] `emphasized`): fylld primärfärg. */
@Composable
private fun emphasizedChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    labelColor = MaterialTheme.colorScheme.onSurface
)

/** Det väntande chipet ([ChipPending]): dämpad yta och text. */
@Composable
private fun pendingChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** Streckad kant längs [shape] – `BorderStroke` kan inte strecka. */
private fun Modifier.dashedOutline(shape: Shape, color: Color): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(width = DASH_WIDTH.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx())))
    onDrawWithContent {
        drawContent()
        drawOutline(outline, color, style = stroke)
    }
}

/**
 * Långtryck på en yta vars klick ägs av en Material-komponent utan eget långtryck. `FilterChip` tar bara
 * `onClick`, och `combinedClickable` går inte att lägga ovanpå utan att chipets egen klickyta (ripple,
 * vald-semantik, inaktivt läge) försvinner – därför en egen gest. Den läses före komponenten
 * ([PointerEventPass.Initial]) och avbryts, som `detectTapGestures`, när fingret släpps, rör sig mer än
 * touch slop (en rullning som börjar på chipet går vidare till listan) eller när någon annan förbrukat
 * gesten. Först efter ett långtryck förbrukas resten av gesten, så att komponentens klick inte också körs.
 * TalkBack får långtrycket som åtgärd.
 */
private fun Modifier.longPress(onLongClick: () -> Unit, label: String?): Modifier = semantics {
    onLongClick(label) {
        onLongClick()
        true
    }
}
    .pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                    val moved = change != null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    if (change == null || !change.pressed || change.isConsumed || moved) break
                }
                true
            }
            if (cancelled == true) return@awaitEachGesture
            onLongClick()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }

private val CHIP_HEIGHT = 36.dp
private val ICON = 18.dp
private val DASH = 4.dp
private val DASH_WIDTH = 1.dp
