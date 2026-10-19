package se.partee71.ui.common.testing

import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import kotlin.test.fail
import se.partee71.ui.common.EditorEvents
import se.partee71.ui.common.EditorUiState
import se.partee71.ui.common.ErrorMapper
import se.partee71.ui.common.UiError

/** En apps fel i testerna – som apparnas `DataError`. */
sealed class TestError : Exception() {
    data object Offline : TestError()

    data object PermissionDenied : TestError()

    data object NotFound : TestError()

    data object Unknown : TestError()
}

/** Texterna (strängresurs-id:n i en app). */
object Messages {
    const val OFFLINE = 1
    const val PERMISSION_DENIED = 2
    const val NOT_FOUND = 3
    const val UNKNOWN = 4
    const val DUPLICATE = 5
    const val REQUIRED = 6
}

/** Appens felöversättning, som apparnas `DataError.toMessage()`. */
object TestErrors : ErrorMapper {
    override fun map(error: Throwable): UiError = when (val known = error as? TestError ?: TestError.Unknown) {
        TestError.Offline -> UiError(known, Messages.OFFLINE)
        TestError.PermissionDenied -> UiError(known, Messages.PERMISSION_DENIED)
        TestError.NotFound -> UiError(known, Messages.NOT_FOUND, retryable = false)
        TestError.Unknown -> UiError(known, Messages.UNKNOWN)
    }

    override fun notFound(): UiError = map(TestError.NotFound)
}

/**
 * Väntar i formulärets tillstånd (`editor.state.test { … }`) tills formuläret är klart
 * ([EditorUiState.done]) och kvitterar som ramen gör ([EditorEvents.onDoneHandled]). Ett sparfel
 * under tiden fäller testet.
 */
suspend fun <T> ReceiveTurbine<EditorUiState<T>>.awaitDone(events: EditorEvents): EditorUiState<T> {
    while (true) {
        val state = awaitItem()
        state.failure?.let { fail("sparfel i stället för klart: ${it.cause}") }
        if (state.done) {
            events.onDoneHandled()
            return state
        }
    }
}

/** Väntar tills formuläret har ett sparfel och kvitterar det som ramen gör. Klart under tiden fäller testet. */
suspend fun <T> ReceiveTurbine<EditorUiState<T>>.awaitFailure(events: EditorEvents): UiError {
    while (true) {
        val state = awaitItem()
        if (state.done) fail("klart i stället för sparfel")
        state.failure?.let { failure ->
            events.onFailureShown()
            return failure
        }
    }
}

/** Varken klart eller sparfel i tillstånden som kommit; avslutar `test`-blocket. */
suspend fun <T> ReceiveTurbine<EditorUiState<T>>.expectNoEditorEvents() {
    val states = cancelAndConsumeRemainingEvents().filterIsInstance<Event.Item<EditorUiState<T>>>().map { it.value }
    states.firstOrNull { it.done || it.failure != null }?.let { fail("oväntad händelse: done = ${it.done}, failure = ${it.failure?.cause}") }
}
