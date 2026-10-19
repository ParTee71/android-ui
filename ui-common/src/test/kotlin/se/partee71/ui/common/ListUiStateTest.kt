package se.partee71.ui.common

import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import se.partee71.ui.common.testing.Messages
import se.partee71.ui.common.testing.TestError
import se.partee71.ui.common.testing.TestErrors

/** Listans och detaljens tillstånd – flyttade från apparnas `AsListUiStateTest` och `ListLoaderTest`. */
class ListUiStateTest {

    @Test
    fun `laddar, sedan innehåll eller tomt`() = runTest {
        flowOf(emptyList<String>(), listOf("a")).asListUiState(TestErrors).test {
            assertEquals(ListUiState.Loading, awaitItem())
            assertEquals(ListUiState.Empty, awaitItem())
            assertEquals(ListUiState.Content(persistentListOf("a")), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `fel blir Error med appens fel och text, okända fel som appens okända`() = runTest {
        flow<List<String>> { throw TestError.PermissionDenied }.asListUiState(TestErrors).test {
            assertEquals(ListUiState.Loading, awaitItem())
            val error = assertIs<ListUiState.Error>(awaitItem()).error
            assertEquals(TestError.PermissionDenied, error.cause)
            assertEquals(Messages.PERMISSION_DENIED, error.message)
            awaitComplete()
        }
        flow<List<String>> { error("x") }.asListUiState(TestErrors).test {
            awaitItem()
            assertEquals(TestError.Unknown, assertIs<ListUiState.Error>(awaitItem()).error.cause)
            awaitComplete()
        }
    }

    @Test
    fun `en läst lista blir tomt eller innehåll`() {
        assertEquals(ListUiState.Empty, emptyList<String>().toListUiState())
        assertEquals(ListUiState.Content(persistentListOf("Promenad")), listOf("Promenad").toListUiState())
    }

    @Test
    fun `försök igen börjar om från laddning och läser källan på nytt`() = runTest(UnconfinedTestDispatcher()) {
        var attempts = 0
        val source = flow {
            attempts++
            if (attempts == 1) throw TestError.Offline
            emit(listOf("Solkräm"))
        }
        val loader = ListLoader(source, backgroundScope, TestErrors)
        loader.state.test {
            assertEquals(TestError.Offline, assertIs<ListUiState.Error>(expectMostRecentItem()).error.cause)
            loader.onEvent(ListEvent.Retry)
            assertEquals(ListUiState.Content(persistentListOf("Solkräm")), expectMostRecentItem())
        }
        assertEquals(2, attempts)
    }

    @Test
    fun `utan prenumerant går listan tillbaka till laddning - gammal lista visas aldrig`() = runTest(UnconfinedTestDispatcher()) {
        val source = MutableStateFlow(listOf("Annas solkräm"))
        val loader = ListLoader(source, backgroundScope, TestErrors)
        loader.state.test {
            assertEquals(ListUiState.Content(persistentListOf("Annas solkräm")), expectMostRecentItem())
        }
        advanceTimeBy(STOP_TIMEOUT_MILLIS + 1)
        assertEquals(ListUiState.Loading, loader.state.value)
    }

    @Test
    fun `detaljen laddar, visar värdet, ger fel för ett saknat dokument och försöker igen`() = runTest(UnconfinedTestDispatcher()) {
        var attempts = 0
        val source = flow<String?> {
            attempts++
            when (attempts) {
                1 -> throw TestError.Offline
                2 -> emit(null)
                else -> emit("Promenad")
            }
        }
        val loader = DetailLoader(source, backgroundScope, TestErrors)
        loader.state.test {
            assertEquals(TestError.Offline, assertIs<DetailUiState.Error>(expectMostRecentItem()).error.cause)
            loader.retry()
            val missing = assertIs<DetailUiState.Error>(expectMostRecentItem()).error
            assertEquals(TestError.NotFound, missing.cause)
            assertFalse(missing.retryable, "ett nytt försök hjälper inte")
            loader.retry()
            assertEquals(DetailUiState.Content("Promenad"), expectMostRecentItem())
        }
    }

    @Test
    fun `ett fel med egen text behåller felet`() {
        val error = Result.failure<Unit>(TestError.PermissionDenied).uiErrorOrNull(TestErrors) { if (it == TestError.PermissionDenied) Messages.DUPLICATE else null }
        assertEquals(TestError.PermissionDenied, error?.cause)
        assertEquals(Messages.DUPLICATE, error?.message)
        assertEquals(null, Result.success(Unit).uiErrorOrNull(TestErrors))
    }
}
