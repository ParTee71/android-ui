package se.partee71.ui.components.testing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/** Färgen i nodens bild [x] och [y] från dess övre vänstra hörn – för att se vad en form eller yta ritar. */
fun SemanticsNodeInteraction.pixelAt(density: Density, x: Dp, y: Dp): Color = with(density) {
    captureToImage().toPixelMap()[x.roundToPx(), y.roundToPx()]
}

/** Färgen mitt på nodens höjd, [fromEnd] in från högerkanten – där en rad utan trailing bara har sin bakgrund. */
fun SemanticsNodeInteraction.pixelAtEnd(density: Density, fromEnd: Dp): Color = with(density) {
    val image = captureToImage().toPixelMap()
    image[image.width - fromEnd.roundToPx(), image.height / 2]
}

/** Stilen texten i noden ritas med (färg, storlek, överstrykning) – noden måste vara en `Text` (osammanslagen). */
fun SemanticsNodeInteraction.textStyle(): TextStyle {
    val layouts = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single().layoutInput.style
}

/** Färgerna är lika så när som på avrundningen i ritningen (högst 1/255 per kanal). */
fun assertSameColor(expected: Color, actual: Color, message: String? = null) {
    val close = listOf(expected.red to actual.red, expected.green to actual.green, expected.blue to actual.blue, expected.alpha to actual.alpha)
        .all { (a, b) -> kotlin.math.abs(a - b) <= COLOR_TOLERANCE }
    kotlin.test.assertTrue(close, "${message?.let { "$it: " }.orEmpty()}väntade $expected, fick $actual")
}

private const val COLOR_TOLERANCE = 1.01f / 255f
