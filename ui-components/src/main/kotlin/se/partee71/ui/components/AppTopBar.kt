@file:OptIn(ExperimentalMaterial3Api::class)

package se.partee71.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import se.partee71.ui.components.R
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Tone

enum class TopBarSize {
    /** Toppnivåskärm: stor rundad rubrik som krymper vid rullning. */
    Large,

    /** Underskärm: tillbakaknapp och liten rubrik. */
    Small
}

/**
 * Statusen till höger i toppraden, före [AppTopBar]s actions – t.ex. appens synkindikator.
 * Sätts av appens navigation; utan den visas ingenting (t.ex. i tester).
 */
val LocalTopBarStatus = compositionLocalOf<@Composable RowScope.() -> Unit> { {} }

/**
 * Tillbakapilens utseende i [AppTopBar]: knappens [variant] och [tone] (se [AppIconButton]). Standard är
 * den tonade runda plattan i Material-färgerna (ReseApoteket, Dagboken); Fonder har en genomskinlig pil,
 * `TopBarBackStyle(variant = IconButtonVariant.Plain)`.
 */
data class TopBarBackStyle(val variant: IconButtonVariant = IconButtonVariant.Tonal, val tone: Tone? = null)

/** Appens tillbakapil, satt en gång i appens tema – gäller också ramarnas topprad. */
val LocalTopBarBackStyle = staticCompositionLocalOf { TopBarBackStyle() }

/**
 * Skärmrubriken. [onBack] ger en tillbakaknapp; [actions] står till höger (t.ex. `AppMenu`).
 * [scrollBehavior] kopplas av ramarna (`EntityListScreen`, `EntityEditScreen`).
 * Före [actions] står [status] – standard det appen satt i [LocalTopBarStatus]. Pilens utseende är
 * [backStyle] – standard det appen satt i [LocalTopBarBackStyle].
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    size: TopBarSize = TopBarSize.Large,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    status: @Composable RowScope.() -> Unit = LocalTopBarStatus.current,
    backStyle: TopBarBackStyle = LocalTopBarBackStyle.current,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val background = MaterialTheme.colorScheme.background
    val colors = TopAppBarDefaults.topAppBarColors(containerColor = background, scrolledContainerColor = background)
    val navigation: @Composable () -> Unit = {
        onBack?.let { AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), it, variant = backStyle.variant, tone = backStyle.tone) }
    }
    val heading = Modifier.semantics { heading() }
    val allActions: @Composable RowScope.() -> Unit = {
        status()
        actions()
    }
    when (size) {
        TopBarSize.Large -> LargeFlexibleTopAppBar(
            title = { Text(title, style = AppTheme.typography.screenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = heading) },
            modifier = modifier,
            subtitle = subtitle?.let { { Text(it, style = AppTheme.typography.itemSubtitle) } },
            navigationIcon = navigation,
            actions = allActions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )

        TopBarSize.Small -> TopAppBar(
            title = { Text(title, style = AppTheme.typography.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = heading) },
            modifier = modifier,
            navigationIcon = navigation,
            actions = allActions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}
