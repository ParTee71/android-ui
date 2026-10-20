package se.partee71.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/**
 * Appens kortstil, som skiljer sig mellan apparna – sätts en gång runt appens innehåll med
 * [LocalAppCardStyle], så att även ramarnas kort (`EntityListScreen`) följer den. Standardvärdena är
 * ReseApotekets och Dagbokens kort.
 *
 * - [border]: kontur runt kortet (Fonder: 1 dp `outlineVariant`); `null` = ingen. En tonad yta
 *   (`AppCard(tone = …)`) har ingen kontur. Bitar ([CardSegment]) under varandra delar kontur.
 * - [padding]: luft mellan kortets kant och innehållet (Fonder `Spacing.l`).
 * - [segmentGap]: luft ovanför och under en bit mot nästa bit i samma kort.
 * - [contentSpacing]: avståndet mellan innehållets delar (Fonder 0).
 */
@Immutable
data class AppCardStyle(
    val border: BorderStroke? = null,
    val padding: Dp = Spacing.m,
    val segmentGap: Dp = AppCardDefaults.SegmentGap,
    val contentSpacing: Dp = Spacing.xs
)

/** Standardvärden för [AppCard] och [CardSegment]. */
object AppCardDefaults {
    /** Halva radavståndet i ett kort ovanför och under en bit, så att bitarna ser ut som ett kort. */
    val SegmentGap: Dp = 2.dp
}

/** Kortstilen för [AppCard] och [CardSegment] i innehållet; standard [AppCardStyle] (ReseApoteket, Dagboken). */
val LocalAppCardStyle = staticCompositionLocalOf { AppCardStyle() }

/**
 * Det enda kortet: en grupperad yta med rader, rubrik eller innehåll. [tone] färgar ytan i en
 * sektionston – t.ex. grönt när allt är klart. [contentColor] sätts som innehållsfärg i kortet, så
 * att ikoner utan egen färg tar kortets färg även utanför en skärmram (Dagboken: `onSurface`);
 * standard ärvs den omgivande. Med [onClick] är hela kortet en klickbar yta som TalkBack läser som
 * en nod. [style] är appens kortstil (kontur, luft), standard [LocalAppCardStyle].
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: Tone? = null,
    contentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    style: AppCardStyle = LocalAppCardStyle.current,
    content: @Composable ColumnScope.() -> Unit
) {
    CardSegment(
        top = true,
        bottom = true,
        modifier = modifier,
        color = tone?.let { AppTheme.tone(it).container } ?: AppTheme.extendedColors.card,
        contentColor = contentColor,
        onClick = onClick,
        style = if (tone == null) style else style.copy(border = null),
        content = content
    )
}

/**
 * En bit av ett [AppCard] – samma yta, hörn och luft. Lata listor (`EntityListScreen`) bygger ett
 * kort av flera bitar, en per rad, så att varje rad är ett eget listobjekt. [contentColor], [onClick]
 * och [style] som i [AppCard]; med en kontur dras en bit under en annan upp konturens bredd, så att
 * bitarna delar en skiljelinje i stället för att rita den dubbelt.
 */
@Composable
fun CardSegment(
    top: Boolean,
    bottom: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.extendedColors.card,
    contentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    style: AppCardStyle = LocalAppCardStyle.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = AppTheme.shapes.cardSegment(top, bottom)
    val border = style.border
    val segment: @Composable () -> Unit = {
        Column(
            (if (border != null && !top) Modifier.sharedEdge(border.width) else Modifier)
                .then(modifier)
                .fillMaxWidth()
                .background(color, shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
                .padding(
                    start = style.padding,
                    end = style.padding,
                    top = if (top) style.padding else style.segmentGap,
                    bottom = if (bottom) style.padding else style.segmentGap
                ),
            verticalArrangement = Arrangement.spacedBy(style.contentSpacing),
            content = content
        )
    }
    if (contentColor == null) segment() else CompositionLocalProvider(LocalContentColor provides contentColor, content = segment)
}

/** Drar upp biten [width] så att dess övre kontur ligger på bitens ovanför. Ytterst, så att anroparens mått mäts innanför. */
private fun Modifier.sharedEdge(width: Dp): Modifier = layout { measurable, constraints ->
    val overlap = width.roundToPx()
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height - overlap).coerceAtLeast(0)) { placeable.place(0, -overlap) }
}
