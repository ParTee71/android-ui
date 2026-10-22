package se.partee71.ui.common

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Tillståndet för varje listskärm – samma fyra lägen överallt (`EntityListScreen`). */
@Immutable
sealed interface ListUiState<out T> {
    data object Loading : ListUiState<Nothing>

    data object Empty : ListUiState<Nothing>

    data class Content<T>(val items: ImmutableList<T>) : ListUiState<T>

    data class Error(val error: UiError) : ListUiState<Nothing>
}

/**
 * Som [asListUiState], men börjar om från [Loading][ListUiState.Loading] varje gång [retry]
 * emitterar – "Försök igen" i `EntityListScreen`.
 */
fun <T> Flow<List<T>>.asListUiState(errors: ErrorMapper, retry: Flow<Unit>): Flow<ListUiState<T>> = retry.restarting { asListUiState(errors) }

/** Den enda översättningen från en datalista till [ListUiState]; ett fel blir [errors]s [UiError]. */
fun <T> Flow<List<T>>.asListUiState(errors: ErrorMapper): Flow<ListUiState<T>> = map { items -> items.toListUiState() }
    .onStart { emit(ListUiState.Loading) }
    .catch { emit(ListUiState.Error(errors.map(it))) }

/** En läst lista som tillstånd: tom → [ListUiState.Empty], annars innehåll – för en skärm som själv håller laddning och fel. */
fun <T> List<T>.toListUiState(): ListUiState<T> = if (isEmpty()) ListUiState.Empty else ListUiState.Content(toImmutableList())

/** Händelser som alla listskärmar har gemensamt; skärmens egna händelser läggs i en egen typ. */
sealed interface ListEvent {
    data object Retry : ListEvent
}

/**
 * Listtillståndet för en ViewModel: [state] från [source] och [onEvent] för [ListEvent].
 * Delas av alla listskärmars ViewModels, så att laddning, tomt och fel beter sig likadant.
 */
class ListLoader<T>(source: Flow<List<T>>, scope: CoroutineScope, errors: ErrorMapper) {
    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Utan prenumerant i [STOP_TIMEOUT_MILLIS] stoppas flödet och tillståndet går tillbaka till
     * laddning: listan från förra gången (kanske en annan användares efter kontobyte) visas aldrig
     * ens en bildruta när skärmen kommer tillbaka.
     */
    val state: StateFlow<ListUiState<T>> = source.asListUiState(errors, retries).stateWhileSubscribed(scope, ListUiState.Loading)

    fun onEvent(event: ListEvent) {
        when (event) {
            ListEvent.Retry -> retries.tryEmit(Unit)
        }
    }
}

/** Tillståndet för en detaljskärm (`EntityDetailScreen`): laddning, fel eller innehåll. */
@Immutable
sealed interface DetailUiState<out T> {
    data object Loading : DetailUiState<Nothing>

    data class Content<T>(val value: T) : DetailUiState<T>

    data class Error(val error: UiError) : DetailUiState<Nothing>
}

/**
 * Den enda översättningen från ett dokument till [DetailUiState]; börjar om från laddning när
 * [retry] emitterar. Ett dokument som inte finns (raderat, eller en annans) är [ErrorMapper.notFound].
 */
fun <T : Any> Flow<T?>.asDetailUiState(errors: ErrorMapper, retry: Flow<Unit>): Flow<DetailUiState<T>> = retry.restarting {
    map<T?, DetailUiState<T>> { value -> value?.let { DetailUiState.Content(it) } ?: DetailUiState.Error(errors.notFound()) }
        .onStart { emit(DetailUiState.Loading) }
        .catch { emit(DetailUiState.Error(errors.map(it))) }
}

/** Detaljtillståndet för en ViewModel, med "Försök igen" – som [ListLoader] för listor. */
class DetailLoader<T : Any>(source: Flow<T?>, scope: CoroutineScope, errors: ErrorMapper) {
    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Som [ListLoader.state]: utan prenumerant går tillståndet tillbaka till laddning. */
    val state: StateFlow<DetailUiState<T>> = source.asDetailUiState(errors, retries).stateWhileSubscribed(scope, DetailUiState.Loading)

    fun retry() {
        retries.tryEmit(Unit)
    }
}

/** Kör [block] direkt och börjar om det varje gång flödet emitterar ("Försök igen"). */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <R> Flow<Unit>.restarting(block: () -> Flow<R>): Flow<R> = onStart { emit(Unit) }.flatMapLatest { block() }

/** Delat medan någon prenumererar; efter [STOP_TIMEOUT_MILLIS] utan börjar det om från [initial]. */
private fun <S> Flow<S>.stateWhileSubscribed(scope: CoroutineScope, initial: S): StateFlow<S> = stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0), initial)

/** Hur länge ett flöde lever vidare utan prenumerant – överlever en rotation. */
const val STOP_TIMEOUT_MILLIS = 5_000L
