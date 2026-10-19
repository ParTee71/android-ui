package se.partee71.ui.components

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import se.partee71.ui.components.testing.SampleErrors
import se.partee71.ui.components.testing.SampleFailure
import se.partee71.ui.components.testing.sampleOffline

/** Den delade arkiveringslogiken för listor – ren logik, utan Compose. */
@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveActionsTest {

    private val written = mutableListOf<Pair<String, Boolean>>()
    private var result: Result<Unit> = Result.success(Unit)
    private val denied = SampleFailure("nekad")

    private fun actions(undoFormat: Int = R.string.archived_format, errorMessage: (Throwable) -> Int? = { null }) = ArchiveActions(
        CoroutineScope(UnconfinedTestDispatcher()),
        SampleErrors,
        setArchived = { id, archived ->
            written += id to archived
            result
        },
        undoFormat = undoFormat,
        errorMessage = errorMessage
    )

    @Test
    fun `arkivera ger Ångra, och Ångra återställer en gång`() = runTest {
        val archive = actions()
        archive.onEvent(ArchiveEvent.Archive("a", "Anna"))
        assertEquals(UndoRequest("a", "Anna"), archive.undo.value)
        archive.onEvent(ArchiveEvent.Undo)
        archive.onEvent(ArchiveEvent.Undo)
        assertNull(archive.undo.value)
        assertEquals(listOf("a" to true, "a" to false), written)
    }

    @Test
    fun `Ångra kan erbjudas för något annat än arkivering och körs en gång`() = runTest {
        val archive = actions()
        val undone = mutableListOf<String>()
        val request = UndoRequest("tillagt", "6", R.string.archived_format)
        archive.offerUndo(request) {
            undone += "tillagt"
            result
        }
        assertEquals(request, archive.undo.value)
        archive.onEvent(ArchiveEvent.Undo)
        archive.onEvent(ArchiveEvent.Undo)
        assertEquals(listOf("tillagt"), undone)
        assertEquals(emptyList(), written, "inget arkiveras tillbaka")
        result = Result.failure(denied)
        archive.offerUndo(request) { result }
        archive.onEvent(ArchiveEvent.Undo)
        assertSame(denied, archive.error.value?.cause, "ett fel när det ångras visas som meddelande")
    }

    @Test
    fun `en arkivering efter ett eget Ångra ångrar arkiveringen, och avfärdat glömmer åtgärden`() = runTest {
        val archive = actions()
        val undone = mutableListOf<String>()
        archive.offerUndo(UndoRequest("tillagt", "6")) { Result.success(Unit).also { undone += "tillagt" } }
        archive.onEvent(ArchiveEvent.Archive("a", "Anna"))
        archive.onEvent(ArchiveEvent.Undo)
        assertEquals(listOf("a" to true, "a" to false), written)
        archive.offerUndo(UndoRequest("tillagt", "6")) { Result.success(Unit).also { undone += "tillagt" } }
        archive.onEvent(ArchiveEvent.UndoDismissed)
        archive.onEvent(ArchiveEvent.Undo)
        assertEquals(emptyList(), undone)
    }

    @Test
    fun `ett skrivfel visas som meddelande, med egen text om appen ger en, och ger inget Ångra`() = runTest {
        val archive = actions(errorMessage = { if (it === denied) R.string.sample_error_unknown else null })
        result = Result.failure(denied)
        archive.onEvent(ArchiveEvent.Archive("a", "Anna"))
        assertSame(denied, archive.error.value?.cause)
        assertEquals(R.string.sample_error_unknown, archive.error.value?.message)
        assertNull(archive.undo.value)
        archive.onEvent(ArchiveEvent.ErrorShown)
        assertNull(archive.error.value)
    }

    @Test
    fun `arkiverade visas sist och bara när de visas`() = runTest {
        val archive = actions()
        val all = flowOf(listOf("Anna" to true, "Bo" to false))
        assertEquals(listOf("Bo"), archive.visible(all) { it.second }.first().map { it.first })
        archive.onEvent(ArchiveEvent.ToggleArchived)
        assertTrue(archive.showingArchived.value)
        assertEquals(listOf("Bo", "Anna"), archive.visible(all) { it.second }.first().map { it.first })
        archive.onEvent(ArchiveEvent.ToggleArchived)
        assertFalse(archive.showingArchived.value)
    }

    @Test
    fun `samma mekanik döljer en rad med ett eget meddelande, och andra fel visas likadant`() {
        val hiding = actions(R.string.paused)
        hiding.onEvent(ArchiveEvent.Archive("p", "Plåster"))
        assertEquals(UndoRequest("p", "Plåster", R.string.paused), hiding.undo.value)
        val offline = sampleOffline()
        hiding.report(offline)
        assertSame(offline, hiding.error.value)
    }
}
