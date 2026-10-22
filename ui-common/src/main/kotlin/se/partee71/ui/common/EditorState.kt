package se.partee71.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Validering av ett formulärvärde: fältnamn → felmeddelande. Tom karta = giltigt. */
fun interface Validator<T> {
    fun validate(value: T): Map<String, Int>
}

/** Det som `EntityEditScreen` visar. [errors] innehåller bara de fel som ska synas. */
@Immutable
data class EditorUiState<T>(
    val value: T,
    val errors: ImmutableMap<String, Int> = persistentMapOf(),
    val isValid: Boolean = true,
    val isDirty: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    /** Det lagrade värdet gick inte att läsa; ramen visar felet och "Försök igen". */
    val loadError: UiError? = null,
    /** Får sparas oförändrat ([EditorState] `saveUnchanged`) – t.ex. ett nytt objekt vars förval är ett riktigt svar. */
    val saveUnchanged: Boolean = false,
    /** Sparat, arkiverat eller raderat: ramen lämnar formuläret och kvitterar med [EditorEvents.onDoneHandled]. */
    val done: Boolean = false,
    /** Sparfelet som ska visas (snackbar eller i ett ark); ramen kvitterar med [EditorEvents.onFailureShown]. */
    val failure: UiError? = null
) {
    /** "Spara" är aktiv först när formuläret är giltigt och ändrat – eller får sparas oförändrat. */
    val canSave: Boolean get() = isValid && (isDirty || saveUnchanged) && !saving && !loading && loadError == null

    /** Felet för [field], om det ska visas. */
    @StringRes
    fun errorFor(field: String): Int? = errors[field]
}

/**
 * Ramens kvittering av händelserna i [EditorUiState] ([EditorUiState.done], [EditorUiState.failure]):
 * en händelse står kvar i tillståndet tills den hanterats – den tappas inte när skärmen inte
 * visas, och den upprepas inte vid rotation.
 */
@Stable
interface EditorEvents {
    fun onDoneHandled()

    fun onFailureShown()

    companion object {
        /** För ett tillstånd utan händelser (förhandsvisning, galleri). */
        val None: EditorEvents = object : EditorEvents {
            override fun onDoneHandled() = Unit

            override fun onFailureShown() = Unit
        }
    }
}

/**
 * Den enda formulärlogiken: värde, ändrat-läge, validering och sparning. Varje `*EditViewModel`
 * exponerar en [EditorState]; `EntityEditScreen` visar den.
 *
 * - Ett fältfel visas när fältet har ändrats eller efter ett sparförsök – aldrig innan
 *   användaren hunnit skriva.
 * - [loading] = true tills [load] anropats (redigering av något som finns).
 * - [errors] översätter läs- och sparfel till det som visas; [errorMessage] ger en egen text för ett
 *   visst fel (t.ex. en dubblett som datalagret nekar).
 * - [saveUnchanged] = true låter ett giltigt formulär sparas utan ändring – ett nytt objekt vars förval
 *   är ett riktigt svar. "Släng ändringar?" gäller ändå bara ändringar. Standard: nej.
 */
class EditorState<T>(
    initial: T,
    private val validator: Validator<T>,
    private val errors: ErrorMapper,
    loading: Boolean = false,
    private val errorMessage: (Throwable) -> Int? = { null },
    private val saveUnchanged: Boolean = false
) : EditorEvents {
    private var original: T = initial
    private val touched = mutableSetOf<String>()
    private var attempted = false

    /** Om fält som är ogiltiga i det lagrade värdet ska visa sitt fel ([load]). */
    private var showStoredInvalid = false
    private val _state = MutableStateFlow(compute(initial, loading = loading))

    val state: StateFlow<EditorUiState<T>> = _state.asStateFlow()

    val value: T get() = _state.value.value

    /**
     * Sätter det lagrade värdet – formuläret blir oändrat och fel nollställs. Med [showInvalid] räknas
     * ett fält som är ogiltigt i det lagrade som rört, så att felet syns direkt och kan rättas (se även
     * [revalidate]). Standard: inga fel förrän fältet ändrats.
     */
    fun load(value: T, showInvalid: Boolean = false) {
        original = value
        touched.clear()
        showStoredInvalid = showInvalid
        if (showInvalid) touched += validator.validate(value).keys
        attempted = false
        show(compute(value, loading = false))
    }

    /**
     * Läser det lagrade värdet med [read] – en gång för alla formulär. Finns det inte, eller går
     * det inte att läsa, visas läsfelet; [prepare] fyller i förval för det som saknas. [showInvalid]
     * som i [load].
     */
    suspend fun loadFrom(read: suspend () -> Result<T?>, showInvalid: Boolean = false, prepare: (T) -> T = { it }) {
        val result = read()
        // Avbröts läsningen (t.ex. av "Försök igen") är resultatet inget läsfel, även om [read] gjort
        // avbrottet till ett Result (runCatching): den nyare läsningen gäller.
        currentCoroutineContext().ensureActive()
        result
            .onSuccess { stored -> if (stored == null) loadFailed(errors.notFound()) else load(prepare(stored), showInvalid) }
            .onFailure { loadFailed(it) }
    }

    /** Det lagrade värdet gick inte att läsa – formuläret visar felet i stället för en evig laddning. */
    fun loadFailed(error: Throwable) = loadFailed(errors.map(error))

    /** Som ovan, med ett redan översatt fel. */
    fun loadFailed(error: UiError) {
        _state.update { it.copy(loading = false, loadError = error) }
    }

    /**
     * Läsfelet är åtgärdat utan att något lagrat läses om – t.ex. en lista som valideringen läser och
     * som gick att läsa vid "Försök igen". Det användaren skrivit står kvar.
     */
    fun clearLoadError() {
        _state.update { it.copy(loadError = null) }
    }

    /** Inför ett nytt läsförsök ("Försök igen"). */
    fun reload() {
        _state.update { it.copy(loading = true, loadError = null) }
    }

    /** Ändrar värdet; [fields] markeras som rörda så att deras fel börjar synas. */
    fun update(vararg fields: String, transform: (T) -> T) {
        touched += fields
        // Ett läsfel står kvar: formuläret har inget lagrat värde att spara över.
        _state.update { compute(transform(it.value), loading = it.loading, saving = it.saving).copy(loadError = it.loadError).withEventsOf(it) }
    }

    /**
     * Validerar om utan att något ändrats – när det valideringen läser har kommit eller ändrats (t.ex.
     * listan som dubbletter söks i). Laddning och läsfel står kvar. Efter en [load] med `showInvalid`
     * visas ett fel som först nu uppstår i det oändrade lagrade värdet.
     */
    fun revalidate() {
        if (showStoredInvalid && !_state.value.isDirty) touched += validator.validate(value).keys
        update { it }
    }

    /**
     * Validerar och sparar med [write]. Ogiltigt → alla fel visas och inget sparas.
     * Lyckat → [EditorUiState.done]; fel → [EditorUiState.failure] och formuläret står kvar.
     */
    suspend fun save(write: suspend (T) -> Result<Unit>) {
        if (busy) return
        attempted = true
        val current = compute(value, loading = false)
        show(current)
        if (!current.isValid) return
        // Ändrades formuläret medan det sparades är ändringen inte sparad: formuläret står kvar
        // som ändrat i stället för att stängas och tappa den.
        perform(written = current.value, closeIfChanged = false) { write(current.value) }
    }

    /**
     * Kör en åtgärd som stänger formuläret (arkivera, radera) med samma fel- och klarhantering.
     * Den stänger alltid: en ändring under tiden får inte kunna sparas tillbaka över det som
     * arkiverats eller raderats.
     */
    suspend fun run(action: suspend () -> Result<Unit>) {
        if (busy) return
        perform(written = value, closeIfChanged = true, action)
    }

    /**
     * Ett dubbeltryck sparar inte två gånger, och medan det lagrade värdet laddas (eller inte gick att
     * läsa) finns inget att spara, arkivera eller radera – platshållaren får aldrig skriva över det.
     */
    private val busy: Boolean get() = _state.value.let { it.saving || it.loading || it.loadError != null }

    private suspend fun perform(written: T, closeIfChanged: Boolean, action: suspend () -> Result<Unit>) {
        _state.update { it.copy(saving = true) }
        val failure = action().uiErrorOrNull(errors, errorMessage)
        _state.update { it.copy(saving = false) }
        if (failure == null) {
            original = written
            val after = compute(value, loading = _state.value.loading)
            val close = closeIfChanged || !after.isDirty
            _state.update { after.withEventsOf(it).copy(done = close || it.done) }
        } else {
            _state.update { it.copy(failure = failure) }
        }
    }

    override fun onDoneHandled() = _state.update { it.copy(done = false) }

    override fun onFailureShown() = _state.update { it.copy(failure = null) }

    /** Ett nytt formulärläge; händelser som inte kvitterats än står kvar. */
    private fun show(next: EditorUiState<T>) = _state.update { next.withEventsOf(it) }

    private fun EditorUiState<T>.withEventsOf(old: EditorUiState<T>) = copy(done = old.done, failure = old.failure)

    private fun compute(value: T, loading: Boolean, saving: Boolean = false): EditorUiState<T> {
        val all = validator.validate(value)
        val visible = if (attempted) all else all.filterKeys { it in touched }
        return EditorUiState(value, visible.toImmutableMap(), all.isEmpty(), value != original, loading, saving, saveUnchanged = saveUnchanged)
    }
}
