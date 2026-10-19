package se.partee71.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Ett fel som visas – det enda fel-API:t i ramarna (lista, detalj, formulär, `LoadErrorState`,
 * `ErrorSnackbar`). Appen mappar sina egna fel (t.ex. `DataError`) hit en gång, med en [ErrorMapper].
 *
 * - [cause]: appens fel, för det som ska avgöras på felets typ (t.ex. `cause == DataError.Offline`).
 * - [message]: texten som visas.
 * - [retryable]: om "Försök igen" kan hjälpa; `false` för något som inte finns längre.
 *
 * En vanlig klass (inte `data`): samma fel två gånger i rad är två händelser, så att ett
 * `StateFlow` inte slår ihop dem och båda visas.
 */
@Immutable
class UiError(val cause: Throwable, @param:StringRes @get:StringRes val message: Int, val retryable: Boolean = true) {
    /** Samma fel med en annan text – t.ex. en dubblett som datalagret nekar. */
    fun withMessage(@StringRes message: Int): UiError = UiError(cause, message, retryable)

    /** Bara felets typ – aldrig dess meddelande, som kan bära hälsodata eller personuppgifter. */
    override fun toString(): String = "UiError(cause=${cause::class.simpleName}, retryable=$retryable)"
}

/**
 * Appens översättning från fel till [UiError] – en gång per app (t.ex. `object AppErrors : ErrorMapper`
 * bredvid `DataError.toMessage()`).
 */
interface ErrorMapper {
    /** Ett fel från datalagret – appens eget, eller något oväntat (blir appens "okänt fel"). */
    fun map(error: Throwable): UiError

    /** Det som lästes finns inte (längre), t.ex. raderat på en annan enhet. Ett nytt försök hjälper inte. */
    fun notFound(): UiError
}

/**
 * Felet i ett misslyckat [Result] som [UiError], eller `null` om det lyckades. [message] ger en egen
 * text för ett visst fel (t.ex. en dubblett), annars felets egen.
 */
fun Result<*>.uiErrorOrNull(errors: ErrorMapper, message: (Throwable) -> Int? = { null }): UiError? = exceptionOrNull()?.let { error ->
    val mapped = errors.map(error)
    message(error)?.let(mapped::withMessage) ?: mapped
}
