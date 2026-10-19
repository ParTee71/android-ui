package se.partee71.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

/**
 * De namngivna textstilarna – det enda sättet komponenter och feature-kod väljer typografi.
 * Appen har typsnitten; [from] ger stilarna ur appens Material 3-typskala med den indelning
 * båda apparna använder.
 */
@Immutable
data class AppTypography(
    /** Stor rubrik överst på en toppnivåskärm. */
    val screenTitle: TextStyle,
    /** Stor siffra. */
    val bigNumber: TextStyle,
    /** Rubrik i dialoger och tomma tillstånd. */
    val headline: TextStyle,
    /** Sektionsrubrik och rubrik på en underskärm. */
    val sectionTitle: TextStyle,
    /** Ett framträdande värde, t.ex. en mängd. */
    val quantity: TextStyle,
    /** Titeln på en listrad. */
    val itemTitle: TextStyle,
    /** Undertext och fotnoter. */
    val itemSubtitle: TextStyle,
    /** Löptext. */
    val body: TextStyle,
    /** Text i pills och chips. */
    val pill: TextStyle,
    /** Liten rubrik över en grupp ("UTSEENDE"). */
    val caption: TextStyle,
    /** Knapptext. */
    val button: TextStyle
) {
    companion object {
        /** Stilarna ur en Material 3-typskala, som i ReseApoteket och Dagboken. */
        fun from(scale: Typography): AppTypography = AppTypography(
            screenTitle = scale.displaySmall,
            bigNumber = scale.headlineMedium,
            headline = scale.headlineSmall,
            sectionTitle = scale.titleLarge,
            quantity = scale.titleLarge,
            // Avstavas i stället för att brytas mitt i ordet i smala kolumner.
            itemTitle = scale.titleMedium.copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph),
            itemSubtitle = scale.bodySmall,
            body = scale.bodyLarge,
            pill = scale.labelMedium,
            caption = scale.labelSmall,
            button = scale.labelLarge
        )
    }
}
