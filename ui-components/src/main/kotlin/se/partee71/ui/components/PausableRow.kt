package se.partee71.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.partee71.ui.theme.Tone

/**
 * En rad för något som kan pausas – recept, kosttillskott, familjeartikel: [detail] (dosering eller
 * mängdregel i klartext) och [kind] (typen) som pill i [pillTone]. Pausad är raden nedtonad med "Pausat"
 * och att den inte räknas med. Öppnar formuläret.
 *
 * [pillTone] saknar standard: tonen är appens (ReseApoteket `Tone.Blue`, Dagboken `Tone.Neutral`), och
 * biblioteket känner bara `Tone.Primary`.
 */
@Composable
fun PausableRow(title: String, detail: String, kind: String?, active: Boolean, onClick: () -> Unit, pillTone: Tone, modifier: Modifier = Modifier) {
    val pill = if (active) kind else stringResource(R.string.paused)
    ItemRow(
        title = title,
        modifier = modifier,
        subtitle = if (active) detail else stringResource(R.string.paused_note),
        trailing = pill?.let { { InfoPill(it, tone = pillTone) } },
        onClick = onClick,
        navigates = true,
        inactive = !active
    )
}
