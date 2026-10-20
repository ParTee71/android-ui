@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package se.partee71.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import se.partee71.ui.components.R

/** Laddning: Expressive laddningsindikator mitt på ytan. */
@Composable
fun AppLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** Ytan som dras ned för att uppdatera i `EntityListScreen` och `EntityDetailScreen` (finns bara med `onRefresh`). */
const val ENTITY_REFRESH_TAG = "entity-refresh"

/**
 * Dra för att uppdatera runt [content] när [onRefresh] finns – annars bara en `Box` med innehållet.
 * Indikatorn är samma Expressive-laddningsindikator som [AppLoading], i en behållare överst, och syns så
 * länge [refreshing] är sant. Gesten kräver rullbart innehåll (ramarnas lista och sektioner).
 */
@Composable
internal fun RefreshBox(refreshing: Boolean, onRefresh: (() -> Unit)?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    if (onRefresh == null) {
        Box(modifier, content = content)
        return
    }
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.testTag(ENTITY_REFRESH_TAG),
        state = state,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter)) },
        content = content
    )
}
