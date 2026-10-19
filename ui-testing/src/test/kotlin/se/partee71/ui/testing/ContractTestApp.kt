package se.partee71.ui.testing

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import se.partee71.ui.common.ErrorMapper
import se.partee71.ui.common.UiError
import se.partee71.ui.theme.AppShapes
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.ExtendedColors
import se.partee71.ui.theme.Tone
import se.partee71.ui.theme.ToneColors

/** En apps fel i testerna – som apparnas `DataError`. */
sealed class ContractError : Exception() {
    data object Offline : ContractError()

    data object NotFound : ContractError()

    data object Unknown : ContractError()
}

/** Appens felöversättning, som apparnas `AppErrors`. */
object ContractErrors : ErrorMapper {
    override fun map(error: Throwable): UiError = when (val known = error as? ContractError ?: ContractError.Unknown) {
        ContractError.Offline -> UiError(known, R.string.contract_error_offline)
        ContractError.NotFound -> UiError(known, R.string.contract_error_not_found, retryable = false)
        ContractError.Unknown -> UiError(known, R.string.contract_error_unknown)
    }

    override fun notFound(): UiError = map(ContractError.NotFound)
}

/** Ett tema som en app skulle ha det – Material 3:s standardfärger; skärmdumparna tas i ui-components. */
@Composable
fun ContractTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme()
    AppTheme(
        colorScheme = scheme,
        extendedColors = ExtendedColors(
            card = scheme.surfaceContainerLowest,
            rowTint = scheme.surfaceContainerLow,
            toolbar = scheme.inverseSurface,
            onToolbar = scheme.inverseOnSurface,
            tones = mapOf(Tone.Primary to ToneColors(scheme.primaryContainer, scheme.onPrimaryContainer)),
            accents = listOf(scheme.primary, scheme.tertiary)
        ),
        shapes = AppShapes(cardRadius = 28.dp),
        typeScale = Typography(),
        content = content
    )
}

/** Det apparna skriver en gång i sin testkod. */
val contractEnvironment = ContractEnvironment(ContractErrors, ContractError.Offline) { ContractTheme(it) }
