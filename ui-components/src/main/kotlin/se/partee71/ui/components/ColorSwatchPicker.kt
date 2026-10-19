package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme

/** Hur [ColorSwatchPicker] markerar den valda färgen. */
@Immutable
sealed interface SwatchMarker {
    /** En ring i `onSurface` runt färgen och en bock i [checkColor] på den (ReseApoteket: vit). */
    data class RingAndCheck(val checkColor: Color) : SwatchMarker

    /** [ChoiceGrid]s ring i primärfärgen (Dagboken). */
    data object GridRing : SwatchMarker
}

/**
 * Välj en av appens färger: [hexes] är det som lagras, [color] ger färgen för en hex och [label]
 * dess namn för TalkBack. Ett lagrat värde med annat skiftläge är samma färg. [marker] har ingen
 * standard: bocken behöver en färg ur appens palett.
 */
@Composable
fun ColorSwatchPicker(
    hexes: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    color: (String) -> Color,
    label: @Composable (String) -> String,
    marker: SwatchMarker,
    modifier: Modifier = Modifier,
    columns: Int = SWATCH_COLUMNS
) {
    val current = hexes.firstOrNull { it.equals(selected, ignoreCase = true) } ?: selected
    val ringColor = if (marker is SwatchMarker.RingAndCheck) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    ChoiceGrid(hexes, current, onSelect, label, modifier, columns, selectionRing = true, ringColor = ringColor) { hex, isSelected ->
        Box(Modifier.fillMaxSize().background(color(hex), AppTheme.shapes.pill), contentAlignment = Alignment.Center) {
            if (isSelected && marker is SwatchMarker.RingAndCheck) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = marker.checkColor, modifier = Modifier.size(CHECK))
            }
        }
    }
}

/** Två rader om fyra – åtta får inte plats på en rad med full tryckyta. */
private const val SWATCH_COLUMNS = 4

private val CHECK = 20.dp
