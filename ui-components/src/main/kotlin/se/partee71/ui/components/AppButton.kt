package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

enum class ButtonVariant {
    Primary,
    Secondary,

    /** Konturerad: ett jämbördigt alternativ bredvid en fylld knapp (Fonder). */
    Outlined,
    Text,

    /** Permanent radering – bara i en `ConfirmDialog` med `destructive = true`. */
    Destructive
}

/**
 * Appens knappstil, som skiljer sig mellan apparna – sätts en gång runt appens innehåll med
 * [LocalAppButtonStyle], så att även ramarnas och dialogernas knappar följer den. Standardvärdena är
 * ReseApotekets och Dagbokens knappar.
 *
 * - [minHeight]: knappens höjd (Fonder: Material 3:s 40 dp). `compact` går före.
 * - [morph]: Expressive-formmorf – rund i vila, fyrkantigare nedtryckt (`ButtonDefaults.shapes()`)
 *   i stället för den fasta pillformen.
 * - [secondaryTone]: färgerna för [ButtonVariant.Secondary]; `null` = Material 3:s tonade
 *   (`secondaryContainer`). Fonder: `Tone.Primary` (primärbehållaren).
 */
@Immutable
data class AppButtonStyle(
    val minHeight: Dp = AppButtonDefaults.MinHeight,
    val morph: Boolean = false,
    val secondaryTone: Tone? = null
)

/** Standardvärden för [AppButton]. */
object AppButtonDefaults {
    /** Tryckyta och höjd enligt mockupen – stor nog för tummen. */
    val MinHeight: Dp = 56.dp

    /** Den lägre knappen för rubrikraden (`compact`). */
    val CompactHeight: Dp = 40.dp
}

/** Knappstilen för [AppButton] i innehållet; standard [AppButtonStyle] (ReseApoteket, Dagboken). */
val LocalAppButtonStyle = staticCompositionLocalOf { AppButtonStyle() }

/**
 * Den enda knappen (skill shared-ui-components). [loading] visar en snurra i stället för
 * ikonen och gör knappen inaktiv – samma "arbetar"-läge överallt. [compact] ger en lägre knapp
 * för rubrikraden (t.ex. "Spara" i `EntityEditScreen`); tryckytan är ändå minst 48 dp.
 * [contentDescription] läses av TalkBack i stället för etiketten – för en knapp som upprepas med samma
 * korta etikett ("Följ") och ska säga vad den gäller; `null` läser etiketten. [style] är appens
 * knappstil (höjd, formmorf, tonad färg), standard [LocalAppButtonStyle].
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
    contentDescription: String? = null,
    style: AppButtonStyle = LocalAppButtonStyle.current
) {
    val described = contentDescription?.let { modifier.semantics { this.contentDescription = it } } ?: modifier
    val sized = described.heightIn(min = if (compact) AppButtonDefaults.CompactHeight else style.minHeight)
    val active = enabled && !loading
    val content: @Composable RowScope.() -> Unit = { ButtonContent(text, icon, loading) }
    // Varianterna är Material 3:s knappar med sina standardvärden – samma som Button, FilledTonalButton,
    // OutlinedButton och TextButton skickar vidare till Button.
    val colors = when (variant) {
        ButtonVariant.Primary -> ButtonDefaults.buttonColors()

        ButtonVariant.Secondary -> style.secondaryTone?.let { AppTheme.tone(it) }
            ?.let { ButtonDefaults.filledTonalButtonColors(containerColor = it.container, contentColor = it.content) }
            ?: ButtonDefaults.filledTonalButtonColors()

        ButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors()

        ButtonVariant.Text -> ButtonDefaults.textButtonColors()

        ButtonVariant.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    }.busy(loading)
    val elevation = when (variant) {
        ButtonVariant.Primary, ButtonVariant.Destructive -> ButtonDefaults.buttonElevation()
        ButtonVariant.Secondary -> ButtonDefaults.filledTonalButtonElevation()
        ButtonVariant.Outlined, ButtonVariant.Text -> null
    }
    val border = if (variant == ButtonVariant.Outlined) ButtonDefaults.outlinedButtonBorder(active) else null
    if (style.morph) {
        // Formmorfens knappar har Expressive-luften efter höjden – samma som Material 3:s standard där.
        Button(onClick, ButtonDefaults.shapes(), sized, active, colors, elevation, border, ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight), content = content)
    } else {
        val padding = if (variant == ButtonVariant.Text) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ContentPadding
        Button(onClick, sized, active, AppTheme.shapes.pill, colors, elevation, border, padding, content = content)
    }
}

/** En knapp som arbetar är inaktiv men behåller sina färger – den ser upptagen ut, inte avstängd. */
private fun ButtonColors.busy(loading: Boolean) = if (loading) copy(disabledContainerColor = containerColor, disabledContentColor = contentColor) else this

@Composable
private fun ButtonContent(text: String, @DrawableRes icon: Int?, loading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(ICON_SIZE),
                color = LocalContentColor.current,
                strokeWidth = 2.5.dp
            )

            icon != null -> Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(ICON_SIZE))
        }
        if (loading || icon != null) Spacer(Modifier.width(Spacing.s))
        Text(text, style = AppTheme.typography.button)
    }
}

private val ICON_SIZE = 20.dp
