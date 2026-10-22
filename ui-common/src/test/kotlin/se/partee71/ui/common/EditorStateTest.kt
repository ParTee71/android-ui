package se.partee71.ui.common

import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import se.partee71.ui.common.testing.Messages
import se.partee71.ui.common.testing.TestError
import se.partee71.ui.common.testing.TestErrors
import se.partee71.ui.common.testing.awaitDone
import se.partee71.ui.common.testing.awaitFailure
import se.partee71.ui.common.testing.expectNoEditorEvents

/** Formulärlogiken bakom `EntityEditScreen` – testas en gång här, inte per app eller skärm (flyttad från apparnas `EditorStateTest`). */
class EditorStateTest {

    private data class Form(val name: String = "", val note: String = "")

    private val validator = Validator<Form> { form -> if (form.name.isBlank()) mapOf(NAME to Messages.REQUIRED) else emptyMap() }

    private fun editor(initial: Form = Form()) = EditorState(initial, validator, TestErrors)

    @Test
    fun `ett nytt formulär är oändrat och kan inte sparas, och felet syns inte än`() {
        val state = editor().state.value
        assertFalse(state.isDirty)
        assertFalse(state.isValid)
        assertFalse(state.canSave)
        assertNull(state.errorFor(NAME))
    }

    @Test
    fun `ett fältfel syns först när fältet har ändrats`() {
        val editor = editor()
        editor.update(NOTE) { it.copy(note = "Faktor 50") }
        assertNull(editor.state.value.errorFor(NAME))
        editor.update(NAME) { it.copy(name = "") }
        assertEquals(Messages.REQUIRED, editor.state.value.errorFor(NAME))
    }

    @Test
    fun `spara aktiveras när formuläret är giltigt och ändrat`() {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        assertTrue(editor.state.value.canSave)
        editor.update(NAME) { it.copy(name = "") }
        assertFalse(editor.state.value.canSave)
    }

    @Test
    fun `saveUnchanged låter ett giltigt nytt objekt sparas utan ändring – standard är att det inte går`() {
        assertFalse(EditorState(Form("Solkräm"), validator, TestErrors).state.value.canSave, "standard: oförändrat kan inte sparas")
        val unchanged = EditorState(Form("Solkräm"), validator, TestErrors, saveUnchanged = true)
        assertTrue(unchanged.state.value.canSave)
        assertFalse(unchanged.state.value.isDirty, "oförändrat – ingen fråga om att slänga")
        assertFalse(EditorState(Form(), validator, TestErrors, saveUnchanged = true).state.value.canSave, "ogiltigt sparas ändå inte")
        unchanged.update(NAME) { it.copy(name = "") }
        assertFalse(unchanged.state.value.canSave)
    }

    @Test
    fun `att ändra tillbaka till det sparade gör formuläret oändrat`() {
        val editor = editor(Form("Solkräm"))
        editor.update(NAME) { it.copy(name = "Solkräm 50") }
        editor.update(NAME) { it.copy(name = "Solkräm") }
        assertFalse(editor.state.value.isDirty)
    }

    @Test
    fun `ogiltigt sparförsök visar alla fel och skriver inget`() = runTest {
        val editor = editor()
        var writes = 0
        editor.save {
            writes++
            Result.success(Unit)
        }
        assertEquals(0, writes)
        assertEquals(Messages.REQUIRED, editor.state.value.errorFor(NAME))
    }

    @Test
    fun `lyckad sparning ger Done och ett oändrat formulär`() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        editor.state.test {
            var saved: Form? = null
            editor.save {
                saved = it
                Result.success(Unit)
            }
            awaitDone(editor)
            assertEquals(Form("Solkräm"), saved)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(editor.state.value.isDirty)
        assertFalse(editor.state.value.saving)
    }

    @Test
    fun `sparfel ger Failed och formuläret står kvar ändrat`() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        editor.state.test {
            editor.save { Result.failure(TestError.Offline) }
            awaitFailure(editor).let {
                assertEquals(TestError.Offline, it.cause)
                assertEquals(Messages.OFFLINE, it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(editor.state.value.isDirty)
        assertTrue(editor.state.value.canSave)
    }

    @Test
    fun `klar och sparfel står kvar tills de kvitterats, även när formuläret ändras, och kommer inte igen`() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        editor.save { Result.failure(TestError.Offline) }
        editor.update(NOTE) { it.copy(note = "SPF 50") }
        assertEquals(TestError.Offline, editor.state.value.failure?.cause, "ett okvitterat fel tappas inte av en ändring")
        editor.onFailureShown()
        assertNull(editor.state.value.failure)

        editor.save { Result.success(Unit) }
        editor.load(Form("Solkräm", "SPF 50"))
        assertTrue(editor.state.value.done, "ett okvitterat klar tappas inte av load")
        editor.onDoneHandled()
        assertFalse(editor.state.value.done)
        assertNull(editor.state.value.failure, "det kvitterade felet visas inte igen")

        editor.update(NOTE) { it.copy(note = "SPF 30") }
        assertFalse(editor.state.value.done, "klar visas inte igen")
    }

    @Test
    fun `samma fel två gånger i rad är två sparfel`() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        editor.state.test {
            editor.save { Result.failure(TestError.Offline) }
            val first = awaitFailure(editor)
            editor.save { Result.failure(TestError.Offline) }
            val second = awaitFailure(editor)
            assertTrue(first !== second, "ett nytt fel, inte det kvitterade")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load sätter det lagrade värdet och avslutar laddningen`() {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        assertTrue(editor.state.value.loading)
        assertFalse(editor.state.value.canSave)
        editor.update(NAME) { it.copy(name = "") }
        editor.load(Form("Plåster"))
        val state = editor.state.value
        assertFalse(state.loading)
        assertFalse(state.isDirty)
        assertEquals(Form("Plåster"), state.value)
        assertNull(state.errorFor(NAME))
    }

    @Test
    fun `loadFrom laddar det lagrade, och ett saknat eller oläsbart värde blir läsfel`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        editor.loadFrom({ Result.success(Form("Solkräm")) }) { it.copy(note = "förval") }
        assertEquals(Form("Solkräm", "förval"), editor.state.value.value)
        assertFalse(editor.state.value.loading)

        val missing = EditorState(Form(), validator, TestErrors, loading = true)
        missing.loadFrom({ Result.success(null) })
        assertEquals(TestError.NotFound, missing.state.value.loadError?.cause)

        val offline = EditorState(Form(), validator, TestErrors, loading = true)
        offline.loadFrom({ Result.failure(TestError.Offline) })
        assertEquals(TestError.Offline, offline.state.value.loadError?.cause)
    }

    @Test
    fun `run för arkivera och radera ger Done eller Failed`() = runTest {
        val editor = editor(Form("Solkräm"))
        editor.state.test {
            editor.run { Result.success(Unit) }
            awaitDone(editor)
            editor.run { Result.failure(TestError.PermissionDenied) }
            awaitFailure(editor).let {
                assertEquals(TestError.PermissionDenied, it.cause)
                assertEquals(Messages.PERMISSION_DENIED, it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett andra sparförsök medan det första pågår skriver inte två gånger`() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val first = launch {
            editor.save {
                writes++
                gate.await()
                Result.success(Unit)
            }
        }
        testScheduler.runCurrent()
        assertTrue(editor.state.value.saving)
        editor.save {
            writes++
            Result.success(Unit)
        }
        gate.complete(Unit)
        first.join()
        assertEquals(1, writes)
    }

    @Test
    fun `spara medan det lagrade värdet laddas eller inte gick att läsa skriver inget`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        editor.update(NAME) { it.copy(name = "Platshållare") }
        var writes = 0
        editor.save {
            writes++
            Result.success(Unit)
        }
        assertTrue(editor.state.value.loading, "laddningen avbryts inte av ett sparförsök")
        editor.loadFailed(TestError.Offline)
        editor.save {
            writes++
            Result.success(Unit)
        }
        assertEquals(TestError.Offline, editor.state.value.loadError?.cause)
        assertEquals(0, writes)
    }

    @Test
    fun `text som skrivs medan sparningen pågår är osparad och stänger inte formuläret `() = runTest {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Solkräm") }
        val gate = CompletableDeferred<Unit>()
        editor.state.test {
            val saving = launch {
                editor.save {
                    gate.await()
                    Result.success(Unit)
                }
            }
            testScheduler.runCurrent()
            editor.update(NOTE) { it.copy(note = "SPF 50") }
            gate.complete(Unit)
            saving.join()

            assertTrue(editor.state.value.isDirty, "anteckningen är inte sparad")
            assertTrue(editor.state.value.canSave)
            expectNoEditorEvents()
        }
    }

    @Test
    fun `arkivera eller radera stänger alltid, även om något ändrats under tiden`() = runTest {
        val editor = editor(Form("Solkräm"))
        val gate = CompletableDeferred<Unit>()
        editor.state.test {
            val deleting = launch {
                editor.run {
                    gate.await()
                    Result.success(Unit)
                }
            }
            testScheduler.runCurrent()
            editor.update(NOTE) { it.copy(note = "skrivet under raderingen") }
            gate.complete(Unit)
            deleting.join()
            awaitDone(editor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett läsfel avslutar laddningen och Försök igen laddar om`() {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        editor.loadFailed(TestError.Offline)
        assertFalse(editor.state.value.loading)
        assertEquals(TestError.Offline, editor.state.value.loadError?.cause)
        assertFalse(editor.state.value.canSave)
        editor.reload()
        assertTrue(editor.state.value.loading)
        assertNull(editor.state.value.loadError)
        editor.load(Form("Plåster"))
        assertNull(editor.state.value.loadError)
    }

    private companion object {
        const val NAME = "name"
        const val NOTE = "note"
    }

    @Test
    fun `Försök igen för något nytt läser inget och laddar inte för evigt`() = runTest {
        val editor = editor()
        val loader = EditorLoader(editor, backgroundScope, read = null)
        loader.retry()
        assertFalse(editor.state.value.loading)
        assertNull(loader.stored.value)
    }

    @Test
    fun `omvalidering och ändringar tar inte bort ett läsfel`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        editor.loadFailed(TestError.Offline)

        editor.revalidate()
        assertEquals(TestError.Offline, editor.state.value.loadError?.cause)
        editor.update("name") { it.copy(name = "Promenad") }
        assertEquals(TestError.Offline, editor.state.value.loadError?.cause)

        var writes = 0
        editor.save {
            writes++
            Result.success(Unit)
        }
        assertEquals(0, writes, "inget lagrat värde att spara över")
    }

    @Test
    fun `run gör inget medan det lagrade laddas eller inte gick att läsa`() = runTest {
        var calls = 0
        val loading = EditorState(Form(), validator, TestErrors, loading = true)
        loading.run {
            calls++
            Result.success(Unit)
        }
        val failed = EditorState(Form(), validator, TestErrors, loading = true)
        failed.loadFailed(TestError.Offline)
        failed.run {
            calls++
            Result.success(Unit)
        }
        assertEquals(0, calls)
        assertFalse(loading.state.value.done)
        assertFalse(failed.state.value.done)
    }

    @Test
    fun `en laddning som börjar medan run pågår står kvar efteråt`() = runTest {
        val editor = editor()
        editor.load(Form("Solkräm"))
        editor.run {
            editor.reload()
            Result.success(Unit)
        }
        assertTrue(editor.state.value.loading, "laddningen pågår fortfarande")
        assertTrue(editor.state.value.done)
    }

    @Test
    fun `EditorLoader nollställer stored när en omläsning visar att det lagrade inte finns längre`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        var result: Result<Form?> = Result.success(Form("Promenad"))
        val loader = EditorLoader(editor, backgroundScope, read = { result })
        testScheduler.runCurrent()
        assertEquals(Form("Promenad"), loader.stored.value)

        result = Result.success(null)
        loader.retry()
        testScheduler.runCurrent()
        assertNull(loader.stored.value)
        assertEquals(TestError.NotFound, editor.state.value.loadError?.cause)
    }

    @Test
    fun `Försök igen medan en läsning pågår visar inte den avbrutna läsningen som läsfel`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        val slow = CompletableDeferred<Form>()
        var reads = 0
        // Som datalagret brukar läsa: runCatching gör även avbrottet till ett Result.
        val loader = EditorLoader(editor, backgroundScope, read = { if (++reads == 1) runCatching { slow.await() } else runCatching { awaitCancellation() } })
        testScheduler.runCurrent()
        loader.retry()
        testScheduler.runCurrent()
        assertNull(editor.state.value.loadError)
        assertTrue(editor.state.value.loading, "den nya läsningen pågår")
    }

    @Test
    fun `EditorLoader utan projektion lägger det lagrade i formuläret och i stored`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        val loader = EditorLoader(editor, backgroundScope, read = { Result.success(Form("Promenad")) })
        testScheduler.runCurrent()
        assertEquals(Form("Promenad"), editor.state.value.value)
        assertEquals(Form("Promenad"), loader.stored.value)
    }

    @Test
    fun `EditorLoader gör ändringen efter varje lyckad läsning, också efter Försök igen, och inte efter ett läsfel`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        var result: Result<Form?> = Result.failure(TestError.Offline)
        val loader = EditorLoader(editor, backgroundScope, read = { result }, afterLoad = { editor.update { it.copy(note = "ändrad") } })
        testScheduler.runCurrent()
        assertEquals(TestError.Offline, editor.state.value.loadError?.cause)
        assertEquals(Form(), editor.state.value.value)

        result = Result.success(Form("Promenad"))
        loader.retry()
        testScheduler.runCurrent()
        assertEquals(Form("Promenad", note = "ändrad"), editor.state.value.value)
        assertTrue(editor.state.value.isDirty, "ändringen kan sparas")
    }

    @Test
    fun `Försök igen avbryter en pågående läsning – den sena laddas aldrig över den nya och efterarbetet körs en gång`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, loading = true)
        val slow = CompletableDeferred<Result<Form?>>()
        var reads = 0
        var afterLoads = 0
        val loader = EditorLoader(
            editor,
            backgroundScope,
            read = { if (++reads == 1) slow.await() else Result.success(Form("Ny")) },
            afterLoad = { afterLoads++ }
        )
        testScheduler.runCurrent()
        loader.retry()
        testScheduler.runCurrent()
        assertEquals(Form("Ny"), editor.state.value.value)

        editor.update { it.copy(note = "användarens") }
        slow.complete(Result.success(Form("Gammal")))
        testScheduler.runCurrent()
        assertEquals(Form("Ny", note = "användarens"), editor.state.value.value, "den första läsningen avbröts")
        assertEquals(1, afterLoads)
    }

    @Test
    fun `ett ogiltigt lagrat värde visar sitt fel direkt bara när formuläret ber om det`() {
        val editor = editor()
        editor.load(Form())
        assertTrue(editor.state.value.errors.isEmpty(), "standard: som förut")
        editor.load(Form(), showInvalid = true)
        assertTrue(editor.state.value.errors.isNotEmpty())
    }

    @Test
    fun `ett fel som uppstår först vid omvalidering syns när det lagrade ska visa sina fel`() {
        var taken = emptySet<String>()
        val editor = EditorState(Form(), Validator<Form> { if (it.name in taken) mapOf(NAME to Messages.REQUIRED) else emptyMap() }, TestErrors)
        editor.load(Form("Promenad"), showInvalid = true)
        assertTrue(editor.state.value.errors.isEmpty())
        taken = setOf("Promenad")
        editor.revalidate()
        assertEquals(Messages.REQUIRED, editor.state.value.errorFor(NAME))
    }

    @Test
    fun `errorMessage ger en egen text för ett visst fel, och felet behålls`() = runTest {
        val editor = EditorState(Form(), validator, TestErrors, errorMessage = { if (it == TestError.PermissionDenied) Messages.DUPLICATE else null })
        editor.update(NAME) { it.copy(name = "Solkräm") }
        editor.state.test {
            editor.save { Result.failure(TestError.PermissionDenied) }
            awaitFailure(editor).let {
                assertEquals(TestError.PermissionDenied, it.cause)
                assertEquals(Messages.DUPLICATE, it.message)
            }
            editor.save { Result.failure(TestError.Offline) }
            assertEquals(Messages.OFFLINE, awaitFailure(editor).message, "andra fel har sin egen text")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ett saknat lagrat värde går inte att försöka läsa igen, ett oläsbart går`() = runTest {
        val missing = EditorState(Form(), validator, TestErrors, loading = true)
        missing.loadFrom({ Result.success(null) })
        assertFalse(missing.state.value.loadError!!.retryable)
        val offline = EditorState(Form(), validator, TestErrors, loading = true)
        offline.loadFrom({ Result.failure(TestError.Offline) })
        assertTrue(offline.state.value.loadError!!.retryable)
    }

    @Test
    fun `clearLoadError tar bort läsfelet och behåller det användaren skrivit`() {
        val editor = editor()
        editor.update(NAME) { it.copy(name = "Promenad") }
        editor.loadFailed(TestError.Offline)
        editor.clearLoadError()
        assertNull(editor.state.value.loadError)
        assertEquals(Form("Promenad"), editor.state.value.value)
        assertTrue(editor.state.value.canSave)
    }

    @Test
    fun `EditorLoader med projektion lägger det lagrade i stored och projektionen i formuläret`() = runTest {
        val editor = EditorState("", { if (it.isBlank()) mapOf(NAME to Messages.REQUIRED) else emptyMap() }, TestErrors, loading = true)
        val loader = EditorLoader(editor, backgroundScope, read = { Result.success(Form("Promenad", "30 min")) }, project = { it.name })
        testScheduler.runCurrent()
        assertEquals("Promenad", editor.state.value.value)
        assertEquals(Form("Promenad", "30 min"), loader.stored.value)
    }
}
