package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/**
 * Det enda kortet: en grupperad yta med rader, rubrik eller innehåll. [tone] färgar ytan i en
 * sektionston – t.ex. grönt när allt är klart. [contentColor] sätts som innehållsfärg i kortet, så
 * att ikoner utan egen färg tar kortets färg även utanför en skärmram (Dagboken: `onSurface`);
 * standard ärvs den omgivande.
 */
@Composable
fun AppCard(modifier: Modifier = Modifier, tone: Tone? = null, contentColor: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    CardSegment(top = true, bottom = true, modifier, tone?.let { AppTheme.tone(it).container } ?: AppTheme.extendedColors.card, contentColor, content)
}

/**
 * En bit av ett [AppCard] – samma yta, hörn och luft. Lata listor (`EntityListScreen`) bygger ett
 * kort av flera bitar, en per rad, så att varje rad är ett eget listobjekt. [contentColor] som i [AppCard].
 */
@Composable
fun CardSegment(
    top: Boolean,
    bottom: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.extendedColors.card,
    contentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val segment: @Composable () -> Unit = {
        Column(
            modifier
                .fillMaxWidth()
                .background(color, AppTheme.shapes.cardSegment(top, bottom))
                .padding(start = Spacing.m, end = Spacing.m, top = if (top) Spacing.m else HALF_GAP, bottom = if (bottom) Spacing.m else HALF_GAP),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            content = content
        )
    }
    if (contentColor == null) segment() else CompositionLocalProvider(LocalContentColor provides contentColor, content = segment)
}

/** Halva radavståndet i ett kort ovanför och under en bit, så att bitarna ser ut som ett kort. */
private val HALF_GAP = 2.dp
