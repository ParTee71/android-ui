package se.partee71.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import se.partee71.ui.common.ErrorMapper
import se.partee71.ui.common.UiError
import se.partee71.ui.common.uiErrorOrNull

/** Arkivera med svep, Ångra och "Visa arkiverade" – samma händelser i alla listor. */
sealed interface ArchiveEvent {
    data class Archive(val id: String, val name: String) : ArchiveEvent

    data object Undo : ArchiveEvent

    data object UndoDismissed : ArchiveEvent

    data object ToggleArchived : ArchiveEvent

    data object ErrorShown : ArchiveEvent
}

/**
 * Arkivering i en [EntityListScreen], där den finns: "Visa arkiverade" i menyn ([showing]), Ångra efter
 * svep ([undo]) och ett misslyckat arkivera/ångra som meddelande ([error]). Händelserna går till
 * [ArchiveActions] i ViewModeln; [collectAsListArchive] bygger den. [showToggle] = false när dolda rader
 * visas på annat sätt (t.ex. "Dolda" eller "Avslutade").
 */
data class ListArchive(
    val showing: Boolean = false,
    val undo: UndoRequest? = null,
    val error: UiError? = null,
    val showToggle: Boolean = true,
    val onEvent: (ArchiveEvent) -> Unit = {}
) {
    /** Svep på en rad: arkivera den och erbjud Ångra. */
    fun archive(id: String, name: String) = onEvent(ArchiveEvent.Archive(id, name))
}

/**
 * Den enda arkiveringslogiken för en listskärms ViewModel: vad [SwipeToHide], [UndoSnackbar] och "Visa
 * arkiverade" i [EntityListScreen] behöver. [setArchived] skriver; ett fel visas som meddelande ([error])
 * i stället för att tappas, översatt med appens [errors] – [errorMessage] ger en egen text för ett visst
 * fel (t.ex. en dubblett som nekas vid Ångra). Samma mekanik döljer en rad: [setArchived] sätter då t.ex.
 * `hidden` och [undoFormat] är "%s dold". Samma Ångra kan också erbjudas för något annat än arkivering
 * ([offerUndo]), t.ex. rader som just lagts till. ViewModeln exponerar den som `archive`, och skärmen
 * skickar `archive.collectAsListArchive()` till listan.
 */
class ArchiveActions(
    private val scope: CoroutineScope,
    private val errors: ErrorMapper,
    private val setArchived: suspend (id: String, archived: Boolean) -> Result<Unit>,
    @param:StringRes private val undoFormat: Int = R.string.archived_format,
    private val errorMessage: (Throwable) -> Int? = { null }
) {
    private val _showingArchived = MutableStateFlow(false)
    val showingArchived: StateFlow<Boolean> = _showingArchived.asStateFlow()

    private val _undo = MutableStateFlow<UndoRequest?>(null)
    val undo: StateFlow<UndoRequest?> = _undo.asStateFlow()

    /** Vad Ångra gör när det inte är att arkivera tillbaka ([offerUndo]); `null` = arkivera tillbaka. */
    private var undoAction: (suspend () -> Result<Unit>)? = null

    /** Ett misslyckat arkivera/ångra, tills listan visat det. Två fel i rad är två värden ([UiError]). */
    private val _error = MutableStateFlow<UiError?>(null)
    val error: StateFlow<UiError?> = _error.asStateFlow()

    /** Aktiva först; arkiverade ([isArchived]) sist och bara när de visas. Ordningen i övrigt behålls. */
    fun <T> visible(source: Flow<List<T>>, isArchived: (T) -> Boolean): Flow<List<T>> = combine(source, _showingArchived) { all, showArchived -> all.filter { showArchived || !isArchived(it) }.sortedBy(isArchived) }

    /** Ett fel från en annan skrivning i samma lista (t.ex. en avprickning) visas på samma sätt. */
    fun report(error: UiError) {
        _error.value = error
    }

    /** Erbjuder Ångra för något annat än arkivering: [undo] körs om användaren trycker Ångra. */
    fun offerUndo(request: UndoRequest, undo: suspend () -> Result<Unit>) {
        undoAction = undo
        _undo.value = request
    }

    fun onEvent(event: ArchiveEvent) {
        when (event) {
            is ArchiveEvent.Archive -> scope.launch {
                if (!failed(setArchived(event.id, true))) {
                    undoAction = null
                    _undo.value = UndoRequest(event.id, event.name, undoFormat)
                }
            }

            ArchiveEvent.Undo -> {
                val request = _undo.value ?: return
                val action = undoAction
                _undo.value = null
                undoAction = null
                scope.launch { failed(action?.invoke() ?: setArchived(request.id, false)) }
            }

            ArchiveEvent.UndoDismissed -> {
                _undo.value = null
                undoAction = null
            }

            ArchiveEvent.ToggleArchived -> _showingArchived.value = !_showingArchived.value

            ArchiveEvent.ErrorShown -> _error.value = null
        }
    }

    /** Visar felet i [result], om det misslyckades; `true` = misslyckat. */
    private fun failed(result: Result<Unit>): Boolean {
        _error.value = result.uiErrorOrNull(errors, errorMessage) ?: return false
        return true
    }
}

/** Tillståndet som listan visar, med händelserna tillbaka hit; [showToggle] = "Visa arkiverade" i menyn. */
@Composable
fun ArchiveActions.collectAsListArchive(showToggle: Boolean = true): ListArchive {
    val showing by showingArchived.collectAsStateWithLifecycle()
    val request by undo.collectAsStateWithLifecycle()
    val failure by error.collectAsStateWithLifecycle()
    return ListArchive(showing, request, failure, showToggle, ::onEvent)
}
