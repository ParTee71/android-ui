package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.partee71.ui.components.R
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.ToneColors

/**
 * Arkivera/dölj med svep åt vänster: raden döljs direkt och [onHide] anropas; ramen visar
 * sedan `UndoSnackbar`. Permanent radering sker aldrig med svep.
 *
 * Samma åtgärd finns för TalkBack ("Arkivera"), eftersom svep inte går att göra med
 * skärmläsare: [content] får en modifier som raden ska använda, så att åtgärden hamnar på
 * den nod som TalkBack läser. Står raden kvar efter svepet (t.ex. när arkiverade visas)
 * glider den tillbaka. Med [enabled] = `false` (en redan arkiverad rad) visas raden utan svep.
 * Bakgrunden är [SwipeBackground] med [label], [icon] och [colors] (standard arkivera i primärtonen).
 */
@Composable
fun SwipeToHide(
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.archive),
    enabled: Boolean = true,
    @DrawableRes icon: Int = R.drawable.ic_archive,
    colors: ToneColors = AppTheme.extendedColors.primaryTone,
    content: @Composable (Modifier) -> Unit
) {
    val hide by rememberUpdatedState(onHide)
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Utan svep syns aldrig bakgrunden, inte heller genom en nedtonad rad.
            if (enabled) SwipeBackground(label, icon, colors = colors)
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        // Samma gren med och utan svep, så att radens tillstånd överlever när den arkiveras.
        gesturesEnabled = enabled,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                hide()
                // Försvinner raden avbryts detta med den; står den kvar glider den tillbaka.
                scope.launch {
                    delay(RESET_DELAY_MILLIS)
                    state.reset()
                }
            }
        }
    ) {
        // Ogenomskinlig, så att bakgrunden bara syns där raden svepts undan. En arkiverad rad
        // (utan svep) är nedtonad och ska synas mot listans bakgrund, inte mot ett kort.
        Box(if (enabled) Modifier.clip(AppTheme.shapes.row).background(AppTheme.extendedColors.card) else Modifier) {
            content(
                if (enabled) {
                    Modifier.semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(label) {
                                hide()
                                true
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
        }
    }
}

/**
 * Det som syns bakom en rad som sveps åt vänster: [icon] och [label] i högerkanten på en yta i [colors]
 * med formen [shape] (standard radens). Bara en bild av svepet – åtgärden ger svepkomponenten
 * ([SwipeToHide]) eller appens egen; en app som har åtgärden på annat ställe för TalkBack kan dölja
 * bilden med `Modifier.clearAndSetSemantics {}` i [modifier]. Standardvärdena är [SwipeToHide]s arkivera.
 */
@Composable
fun SwipeBackground(
    label: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    colors: ToneColors = AppTheme.extendedColors.primaryTone,
    shape: Shape = AppTheme.shapes.row
) {
    Row(
        modifier.fillMaxSize().clip(shape).background(colors.container).padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.content)
        Text(label, style = AppTheme.typography.button, color = colors.content)
    }
}

/** Hur länge en svept rad som inte försvinner ligger undan innan den glider tillbaka. */
internal const val RESET_DELAY_MILLIS = 1_000L
