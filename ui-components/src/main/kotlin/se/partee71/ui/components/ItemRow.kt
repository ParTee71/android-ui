@file:OptIn(ExperimentalMaterial3Api::class)

package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing

/**
 * Avståndet mellan titel och undertext i en listrad ([ItemRow], [CheckRow], [SwitchRow], [ItemRowText]).
 * Standard 2 dp (ReseApoteket); Dagboken sätter `Spacing.xs` en gång i sitt tema.
 */
val LocalItemRowTextGap = compositionLocalOf { ItemRowDefaults.TextGap }

/** Standardvärden för listraden. */
object ItemRowDefaults {
    /** Avståndet mellan titel och undertext (ReseApoteket). */
    val TextGap: Dp = 2.dp
}

/**
 * Den enda listraden: valfri [leading] (avatar, kryss), titel, undertext och [trailing] (pill, pil,
 * växel). [done] ger den dämpade "klar"-stilen, [tinted] den tonade bakgrunden och [accent] en färgad
 * vänsterkant (personens färg, en status). [navigates] visar en pil längst till höger (efter [trailing])
 * – raden öppnar en annan skärm. [inactive] tonar ner hela raden, för det som finns men inte räknas med
 * (ett pausat recept). [below] står under titel och undertext över hela radens bredd; med
 * [belowAlignsWithTitle] börjar den i linje med titeln efter en [leading] i [LeadingSlot].
 * [titleHighlight] färgmarkerar en del av titeln – det man sökt på i ett [SuggestionField].
 * [icon] är en dekorativ ikon först i raden i en [LeadingSlot] (val i ark och kort) – i stället för [leading].
 *
 * Kapas titeln visar ett långtryck hela titeln och undertexten i en bubbla; annars är långtrycket radens.
 */
@Composable
fun ItemRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    tinted: Boolean = false,
    done: Boolean = false,
    accent: Color? = null,
    navigates: Boolean = false,
    inactive: Boolean = false,
    below: (@Composable () -> Unit)? = null,
    titleHighlight: IntRange? = null,
    @DrawableRes icon: Int? = null,
    belowAlignsWithTitle: Boolean = false
) {
    val start: (@Composable () -> Unit)? = leading ?: icon?.let { { LeadingSlot { Icon(painterResource(it), contentDescription = null) } } }
    val end: (@Composable () -> Unit)? = if (navigates) {
        {
            trailing?.invoke()
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        trailing
    }
    ItemRowLayout(
        title = title,
        modifier = modifier.inactive(inactive),
        interaction = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        subtitle = subtitle,
        leading = start,
        trailing = end,
        tinted = tinted,
        done = done,
        accent = accent,
        below = below,
        titleHighlight = titleHighlight,
        belowAlignsWithTitle = belowAlignsWithTitle
    )
}

/**
 * Gemensam layout för [ItemRow], [CheckRow] och [SwitchRow] – de skiljer sig bara i [interaction].
 * [textInactive] tonar ned bara titel och undertext, så att en kontroll i [trailing] behåller full kontrast.
 * [bubble] = false stänger av bubblan med hela titeln (när långtrycket öppnar radens meny).
 */
@Composable
internal fun ItemRowLayout(
    title: String,
    modifier: Modifier = Modifier,
    interaction: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    tinted: Boolean = false,
    done: Boolean = false,
    accent: Color? = null,
    below: (@Composable () -> Unit)? = null,
    titleHighlight: IntRange? = null,
    textInactive: Boolean = false,
    belowAlignsWithTitle: Boolean = false,
    bubble: Boolean = true
) {
    val tint = AppTheme.extendedColors.rowTint
    // Bubblan finns bara när titeln faktiskt kapas – annars är långtrycket radens som vanligt.
    var truncated by remember { mutableStateOf(false) }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { FullText(title, subtitle) } },
        state = rememberTooltipState(),
        enableUserInput = bubble && truncated
    ) {
        // Anroparens modifierare (svep och dess TalkBack-åtgärd) ska ligga på raden som TalkBack läser.
        Column(
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_HEIGHT)
                .clip(AppTheme.shapes.row)
                .then(if (tinted) Modifier.background(tint) else Modifier)
                .then(interaction)
                .accentBar(accent)
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                leading?.invoke()
                RowText(title, subtitle, done, titleHighlight, onTitleOverflow = { truncated = it }, modifier = Modifier.weight(1f).inactive(textInactive))
                trailing?.invoke()
            }
            below?.let { content ->
                if (belowAlignsWithTitle) Box(Modifier.padding(start = TOUCH_TARGET + Spacing.m)) { content() } else content()
            }
        }
    }
}

/**
 * Titel och undertext i en listrad – en gång, för [ItemRow] och appens egna kort med samma text: titeln
 * högst två rader, undertexten hela. `null` som titel ger bara undertexten. [done] stryker över och dämpar,
 * [titleHighlight] färgmarkerar en del av titeln.
 */
@Composable
fun ItemRowText(title: String?, subtitle: String?, modifier: Modifier = Modifier, done: Boolean = false, titleHighlight: IntRange? = null) {
    RowText(title, subtitle, done, titleHighlight, onTitleOverflow = {}, modifier = modifier)
}

@Composable
private fun RowText(title: String?, subtitle: String?, done: Boolean, titleHighlight: IntRange?, onTitleOverflow: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val decoration = if (done) TextDecoration.LineThrough else null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalItemRowTextGap.current)) {
        if (title != null) {
            Text(
                highlighted(title, titleHighlight),
                style = AppTheme.typography.itemTitle,
                color = if (done) muted else MaterialTheme.colorScheme.onSurface,
                textDecoration = decoration,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { onTitleOverflow(it.hasVisualOverflow) }
            )
        }
        subtitle?.let { Text(it, style = AppTheme.typography.itemSubtitle, color = muted, textDecoration = decoration) }
    }
}

/** Hela titeln och undertexten, i långtrycksbubblan på en rad med kapad titel. */
@Composable
private fun FullText(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalItemRowTextGap.current)) {
        Text(title, style = AppTheme.typography.itemTitle)
        subtitle?.let { Text(it, style = AppTheme.typography.itemSubtitle) }
    }
}

/** En färgad vänsterkant (personens färg, en status) – för listraden och appens kort; `null` ritar inget. */
fun Modifier.accentBar(color: Color?): Modifier = if (color == null) {
    this
} else {
    drawBehind {
        val width = ACCENT_WIDTH.toPx()
        drawRoundRect(color, size = Size(width, size.height), cornerRadius = CornerRadius(width / 2))
    }
}

/**
 * Den enda nedtoningen: det som finns men inte räknas med (pausat recept, avslutad post), det som inte
 * går att välja och en avstängd kontroll.
 */
const val INACTIVE_ALPHA = 0.55f

/** Tonar ned med [INACTIVE_ALPHA] när [inactive] – samma nedtoning överallt. */
fun Modifier.inactive(inactive: Boolean): Modifier = if (inactive) alpha(INACTIVE_ALPHA) else this

private val MIN_HEIGHT = 56.dp
private val ACCENT_WIDTH = 4.dp
private const val TITLE_MAX_LINES = 2

/** [text] med [range] i primärfärgen; utan (eller med ett ogiltigt) intervall bara texten. */
@Composable
private fun highlighted(text: String, range: IntRange?): AnnotatedString {
    val clipped = range?.let { it.first.coerceAtLeast(0)..it.last.coerceAtMost(text.lastIndex) }?.takeUnless { it.isEmpty() }
        ?: return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary), clipped.first, clipped.last + 1)
    }
}
