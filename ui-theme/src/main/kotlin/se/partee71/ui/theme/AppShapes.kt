package se.partee71.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Appens former. Bara kortens hörnradie skiljer sig mellan apparna (ReseApoteket 28 dp, Dagboken 22 dp);
 * övriga har apparnas gemensamma värden som standard.
 */
@Immutable
data class AppShapes(
    /** Kortens hörnradie – ger [card] och [cardSegment]. */
    val cardRadius: Dp,
    /** Rader och snackbars. */
    val row: Shape = RoundedCornerShape(18.dp),
    /** Pills, chips och knappar – helt rundade. */
    val pill: Shape = CircleShape,
    /** Textfält – rundare än Material-standard men tydligt ett fält, inte en knapp. */
    val field: Shape = RoundedCornerShape(14.dp),
    /** Menyer. */
    val menu: Shape = RoundedCornerShape(16.dp),
    /** Bottom sheet: rundat upptill. */
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    /** Liten ikonruta, t.ex. i en sektionsrubrik. */
    val smallTile: Shape = RoundedCornerShape(10.dp),
    /** Ikonrutan i ett tomt tillstånd – rundare än ett kort, för att den är större. */
    val iconTile: Shape = RoundedCornerShape(40.dp)
) {
    /** Kortet. */
    val card: Shape get() = RoundedCornerShape(cardRadius)

    /** En bit av ett kort som delas upp i rader (lat lista): rundad bara upptill och/eller nedtill. */
    fun cardSegment(top: Boolean, bottom: Boolean): Shape = RoundedCornerShape(
        topStart = if (top) cardRadius else 0.dp,
        topEnd = if (top) cardRadius else 0.dp,
        bottomStart = if (bottom) cardRadius else 0.dp,
        bottomEnd = if (bottom) cardRadius else 0.dp
    )
}
