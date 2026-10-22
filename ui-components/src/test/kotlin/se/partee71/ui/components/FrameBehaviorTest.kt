package se.partee71.ui.components

import android.content.Context
import android.view.View
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.partee71.ui.common.DetailUiState
import se.partee71.ui.common.EditorEvents
import se.partee71.ui.common.EditorState
import se.partee71.ui.common.EditorUiState
import se.partee71.ui.common.ListUiState
import se.partee71.ui.common.Validator
import se.partee71.ui.components.testing.SampleErrors
import se.partee71.ui.components.testing.SampleFailure
import se.partee71.ui.components.testing.SampleSwatches
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.components.testing.sampleOffline
import se.partee71.ui.theme.Spacing

/**
 * Ramarna (EntityListScreen, EntityEditScreen, EntityDetailScreen) utöver kontrakten i `ui-testing`:
 * grupper, kolumner efter bredd, filter, sidfot, arkivering, meny, tangentbord och flikläge.
 */
@RunWith(RobolectricTestRunner::class)
class FrameBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    private val empty = EmptyContent(R.drawable.ic_box, "Inga familjeartiklar än", "Solkräm, plåster – saker hela familjen delar.")
    private val add = AddAction("Lägg till", {})
    private val offlineText get() = ApplicationProvider.getApplicationContext<Context>().getString(R.string.sample_error_offline)

    private fun <T> list(
        state: ListUiState<T>,
        key: (T) -> Any,
        add: AddAction? = this.add,
        group: ((T) -> ListGroup)? = null,
        archive: ListArchive? = null,
        filter: (@Composable () -> Unit)? = null,
        footer: (@Composable () -> Unit)? = null,
        row: @Composable (T) -> Unit
    ) = rule.setContent {
        TestTheme { EntityListScreen("Familjeartiklar", state, empty, add, key, group = group, archive = archive, filter = filter, footer = footer, row = row) }
    }

    @Test
    fun `grupperad lista visar rubrik med antal, en grupp kan sakna räknare och sidfoten står sist`() {
        val rows = listOf("Solkräm" to "Krämer", "Hydrokortison" to "Krämer", "Plåster" to "Förband")
        list(ListUiState.Content(rows.toImmutableList()), key = { it.first }, group = { ListGroup(it.second, showCount = it.second == "Krämer") }, footer = { ItemRow("Visa äldre") }) { ItemRow(it.first) }
        rule.onNodeWithText("Krämer").assertIsDisplayed()
        rule.onNodeWithText("2").assertIsDisplayed()
        rule.onNodeWithText("1").assertDoesNotExist()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Visa äldre"))
        val last = rule.onNodeWithText("Plåster").fetchSemanticsNode().positionInRoot
        val footer = rule.onNodeWithText("Visa äldre").fetchSemanticsNode().positionInRoot
        assertTrue(footer.y > last.y, "sidfoten står efter grupperna")
    }

    @Test
    fun `utan lägg till finns ingen knapp, och sidfoten står under budskapet i tomt läge`() {
        list(ListUiState.Empty, key = { it }, add = null, footer = { ItemRow("Visa äldre") }) { ItemRow(it) }
        rule.onNodeWithText(empty.title).assertIsDisplayed()
        rule.onNodeWithText("Visa äldre").assertIsDisplayed()
        rule.onNodeWithTag(ENTITY_LIST_ADD_TAG).assertDoesNotExist()
    }

    @Test
    fun `lägg till med meny öppnar valen i pilen`() {
        var chosen = 0
        list(ListUiState.Content(persistentListOf("Solkräm")), key = { it }, add = AddAction("Lägg till", {}, listOf(AppMenuItem("Ny vid behov-medicin", { chosen++ })))) { ItemRow(it) }
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Ny vid behov-medicin").performClick()
        assertEquals(1, chosen)
    }

    @Test
    fun `undergrupper, hopfälld grupp och rubrik överst`() {
        val rows = listOf("A1" to "a", "B1" to "b", "D1" to "d")
        rule.setContent {
            TestTheme {
                EntityListScreen(
                    "Gran Canaria",
                    ListUiState.Content(rows.toImmutableList()),
                    empty,
                    add,
                    key = { it.first },
                    group = { if (it.second == "d") ListGroup("Dolda", collapsible = true) else ListGroup("Mediciner", count = "1 / 2") },
                    subtitle = "7 dagar",
                    header = { ItemRow("Rubrikplats") },
                    subgroup = { row -> ListSubgroup(if (row.second == "b") "Erik" else "Anna", SampleSwatches.color(SampleSwatches.hexes[0])).takeIf { row.second != "d" } },
                    archive = ListArchive(showToggle = false)
                ) { ItemRow(it.first) }
            }
        }
        listOf("Rubrikplats", "7 dagar", "1 / 2", "Anna", "Erik").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        rule.onNodeWithContentDescription("Fler val").assertDoesNotExist()
        rule.onNodeWithText("D1").assertDoesNotExist()
        rule.onNodeWithText("Dolda").performClick()
        rule.onNodeWithText("D1").assertIsDisplayed()
    }

    @Test
    fun `en utfälld grupp står kvar efter att skärmen återskapats`() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            TestTheme {
                EntityListScreen("Gran Canaria", ListUiState.Content(persistentListOf("A1", "D1")), empty, add, key = { it }, group = { ListGroup(if (it == "D1") "Dolda" else "Övrigt", collapsible = it == "D1") }) { ItemRow(it) }
            }
        }
        rule.onNodeWithText("Dolda").performClick()
        rule.onNodeWithText("D1").assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("D1").assertIsDisplayed()
    }

    @Test
    fun `på en telefon blir en grupp med flera kolumner en kolumn`() {
        val rows = listOf("Solkräm SPF 50", "Resetabletter")
        list(ListUiState.Content(rows.toImmutableList()), key = { it }, group = { ListGroup("Övrigt", columns = 3) }) { ItemRow(it) }
        val first = rule.onNodeWithText(rows[0]).fetchSemanticsNode().positionInRoot
        val second = rule.onNodeWithText(rows[1]).fetchSemanticsNode().positionInRoot
        assertEquals(first.x, second.x, "en kolumn: samma vänsterkant")
        assertTrue(second.y > first.y, "en kolumn: raderna under varandra")
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp-mdpi")
    fun `på 840 dp bredd får tre kolumner plats, men aldrig fler än gruppens tak`() {
        val rows = listOf("Ö1", "Ö2", "Ö3", "Ö4")
        list(ListUiState.Content(rows.toImmutableList()), key = { it }, group = { ListGroup(if (it == "Ö4") "En" else "Övrigt", columns = if (it == "Ö4") 1 else 4) }) { ItemRow(it) }
        val y = rows.take(3).map { rule.onNodeWithText(it).fetchSemanticsNode().positionInRoot.y }.distinct()
        assertEquals(1, y.size, "tre kolumner: samma rad")
        val x = rows.map { rule.onNodeWithText(it).fetchSemanticsNode().positionInRoot.x }
        assertEquals(x[0], x[3], "taket 1: en kolumn även på bred skärm")
    }

    @Test
    fun `kolumner får plats först när var och en blir minst 260 dp bred`() {
        assertEquals(1, columnsThatFit(358.dp), "telefon")
        assertEquals(1, columnsThatFit(523.dp), "strax under två")
        assertEquals(2, columnsThatFit(524.dp), "precis två")
        assertEquals(3, columnsThatFit(808.dp), "840 dp minus marginaler")
        assertEquals(1, columnsThatFit(0.dp), "alltid minst en")
    }

    @Test
    fun `filter står fast under rubriken i alla lägen`() {
        var state by mutableStateOf<ListUiState<String>>(ListUiState.Empty)
        rule.setContent { TestTheme { EntityListScreen("Listor", state, empty, add, key = { it }, filter = { ItemRow("Filterrad") }) { ItemRow(it) } } }
        rule.onNodeWithText("Filterrad").assertIsDisplayed()
        state = ListUiState.Error(sampleOffline())
        rule.onNodeWithText("Filterrad").assertIsDisplayed()
        state = ListUiState.Content(persistentListOf("Promenad"))
        rule.onNodeWithText("Filterrad").assertIsDisplayed()
        rule.onNodeWithText("Promenad").assertIsDisplayed()
    }

    @Test
    fun `Visa arkiverade i menyn, och samma arkiveringsfel två gånger i rad visas två gånger`() {
        var archive by mutableStateOf(ListArchive())
        var toggles = 0
        var shown = 0
        val events: (ArchiveEvent) -> Unit = {
            if (it == ArchiveEvent.ToggleArchived) toggles++
            if (it == ArchiveEvent.ErrorShown) shown++
        }
        rule.setContent { TestTheme { EntityListScreen("Listor", ListUiState.Content(persistentListOf("Promenad")), empty, add, key = { it }, archive = archive.copy(onEvent = events)) { ItemRow(it) } } }
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Visa arkiverade").performClick()
        assertEquals(1, toggles)
        repeat(2) {
            archive = ListArchive(error = sampleOffline())
            rule.waitForIdle()
            rule.onNodeWithText(offlineText).assertIsDisplayed()
            rule.mainClock.advanceTimeBy(SNACKBAR_GONE_MILLIS)
            rule.waitForIdle()
        }
        assertEquals(2, shown, "samma sorts fel igen visas igen")
    }

    @Test
    fun `radering kräver bekräftelse, arkivera och återställa finns i menyn`() {
        var deleted = 0
        var archived = 0
        var restored = 0
        rule.setContent {
            TestTheme {
                EntityEditScreen("Solkräm", EditorUiState("Solkräm"), EditorEvents.None, {}, {}, onArchive = { archived++ }, onRestore = { restored++ }, delete = DeleteAction("Radera Solkräm?", "Det går inte att ångra.") { deleted++ }) {}
            }
        }
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Radera").performClick()
        assertEquals(0, deleted)
        rule.onNodeWithText("Radera Solkräm?").assertIsDisplayed()
        rule.onNodeWithText("Radera").performClick()
        assertEquals(1, deleted)
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Arkivera").performClick()
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Återställ").performClick()
        assertEquals(1 to 1, archived to restored)
    }

    @Test
    fun `arkivera med osparade ändringar frågar först`() {
        var archived = 0
        rule.setContent {
            TestTheme { EntityEditScreen("Solkräm", EditorUiState("Solkräm 50", isValid = true, isDirty = true), EditorEvents.None, {}, {}, onArchive = { archived++ }) {} }
        }
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Arkivera").performClick()
        rule.onNodeWithText("Fortsätt redigera").performClick()
        assertEquals(0, archived)
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Arkivera").performClick()
        rule.onNodeWithText("Släng").performClick()
        assertEquals(1, archived)
    }

    @Test
    fun `lyckad sparning går till onSaved, bakåt till onClose, och formulärfelet står överst`() {
        var saved = 0
        var closed = 0
        val editor = EditorState("Kreta", Validator { emptyMap() }, SampleErrors)
        rule.setContent {
            TestTheme {
                val state by editor.state.collectAsState()
                EntityEditScreen("Ny resa", state, editor, {}, { closed++ }, onSaved = { saved++ }, formError = "Värdet nekades") { ItemRow("Fält") }
            }
        }
        val error = rule.onNodeWithText("Värdet nekades").fetchSemanticsNode().positionInRoot
        assertTrue(error.y < rule.onNodeWithText("Fält").fetchSemanticsNode().positionInRoot.y, "formulärfelet står överst")
        rule.runOnIdle { runBlocking { editor.run { Result.success(Unit) } } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Tillbaka").performClick()
        assertEquals(1 to 1, saved to closed)
    }

    @Test
    fun `klar och sparfel väntar tills skärmen visas och kommer inte igen efter rotation`() {
        val editor = EditorState("Kreta", Validator { emptyMap() }, SampleErrors)
        var saved = 0
        var shown by mutableStateOf(false)
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            TestTheme {
                val state by editor.state.collectAsState()
                if (shown) EntityEditScreen("Ny resa", state, editor, {}, {}, onSaved = { saved++ }) {}
            }
        }
        rule.runOnIdle { runBlocking { editor.run { Result.failure(SampleFailure("offline")) } } }
        rule.runOnIdle { shown = true }
        rule.onNodeWithText(offlineText).assertIsDisplayed()
        assertNull(editor.state.value.failure, "kvitterat när snackbaren startats")
        rule.runOnIdle { runBlocking { editor.run { Result.success(Unit) } } }
        rule.waitForIdle()
        assertEquals(1, saved)
        assertFalse(editor.state.value.done, "kvitterat när skärmen lämnats")
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(1, saved, "klar upprepas inte efter rotation")
    }

    @Test
    fun `tangentbordet krymper den rullande ytan så att fältet hamnar ovanför det`() {
        var view: View? = null
        rule.setContent {
            view = LocalView.current
            TestTheme { EntityEditScreen("Ny anteckning", EditorUiState("", isValid = true), EditorEvents.None, {}, {}) { repeat(20) { ItemRow("Fält $it") } } }
        }
        val screen = rule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val before = rule.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.bottom
        val keyboard = 300 * rule.density.density
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboard.toInt()))
            .setVisible(WindowInsetsCompat.Type.ime(), true)
            .build()
        rule.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(view), insets) }
        rule.waitForIdle()
        val after = rule.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(before > screen - keyboard, "utan tangentbord når ytan ner under där tangentbordet hamnar")
        assertTrue(after <= screen - keyboard + 1, "med tangentbord slutar ytan ovanför det ($after > ${screen - keyboard})")
    }

    @Test
    fun `detaljen - lägen, försök igen, Redigera och meny`() {
        var state by mutableStateOf<DetailUiState<String>>(DetailUiState.Loading)
        var retries = 0
        var edits = 0
        var archived = 0
        rule.setContent {
            TestTheme {
                EntityDetailScreen(state, header = { DetailHeader(it, "3 recept") }, onBack = {}, onEdit = { edits++ }, menu = { listOf(AppMenuItem("Arkivera", { archived++ })) }, onRetry = { retries++ }) { Text("Recept") }
            }
        }
        rule.onNodeWithContentDescription("Redigera").assertDoesNotExist()
        state = DetailUiState.Error(sampleOffline())
        rule.onNodeWithText("Försök igen").performClick()
        assertEquals(1, retries)
        state = DetailUiState.Content("Anna")
        rule.onNodeWithText("Anna").assertIsDisplayed()
        rule.onNodeWithText("3 recept").assertIsDisplayed()
        rule.onNodeWithContentDescription("Redigera").performClick()
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Arkivera").performClick()
        assertEquals(1 to 1, edits to archived)
    }

    @Test
    fun `detaljen med titel i toppraden har inget centrerat huvud`() {
        var back = 0
        rule.setContent { TestTheme { EntityDetailScreen(DetailUiState.Content("Förkylning"), header = null, onBack = { back++ }, title = "Tema") { ItemRow(it) } } }
        rule.onNodeWithText("Tema").assertIsDisplayed()
        rule.onNodeWithText("Förkylning").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tillbaka").performClick()
        assertEquals(1, back)
    }

    @Test
    fun `detaljen i flikläge - ingen tillbakapil, rubrik, undertitel, egna knappar och egen meddelandeyta`() {
        rule.setContent {
            TestTheme {
                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(Unit) { snackbar.showSnackbar("Alvedon loggad") }
                EntityDetailScreen(
                    DetailUiState.Content("Morgonmedicin"),
                    header = null,
                    onBack = null,
                    title = "Idag",
                    subtitle = "sön 4 okt",
                    actions = { AppIconButton(R.drawable.ic_calendar, "Välj dag", {}) },
                    snackbar = snackbar
                ) { ItemRow(it) }
            }
        }
        rule.onNodeWithContentDescription("Tillbaka").assertDoesNotExist()
        listOf("Idag", "sön 4 okt", "Morgonmedicin", "Alvedon loggad").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        rule.onNodeWithContentDescription("Välj dag").assertIsDisplayed()
    }

    @Test
    fun `detaljen visar ett åtgärdsfel som meddelande och kvitterar det`() {
        var shown = 0
        rule.setContent { TestTheme { EntityDetailScreen(DetailUiState.Content("Anna"), header = { DetailHeader(it) }, onBack = {}, error = sampleOffline(), onErrorShown = { shown++ }) {} } }
        rule.onNodeWithText(offlineText).assertIsDisplayed()
        rule.mainClock.advanceTimeBy(SNACKBAR_GONE_MILLIS)
        rule.waitForIdle()
        assertEquals(1, shown)
    }

    private companion object {
        /** En kort snackbar (4 s) plus animationen ut. */
        const val SNACKBAR_GONE_MILLIS = 6_000L
    }

    @Test
    fun `formulärets sidomarginal är Spacing xl som standard och appens via LocalEditScreenMargin`() {
        var margin: Dp? by mutableStateOf(null)
        rule.setContent {
            TestTheme {
                val form = @Composable { EntityEditScreen("Solkräm", EditorUiState("Solkräm"), EditorEvents.None, {}, {}) { Text("Fält") } }
                margin?.let { CompositionLocalProvider(LocalEditScreenMargin provides it) { form() } } ?: form()
            }
        }
        assertEquals(Spacing.xl, rule.onNodeWithText("Fält").getBoundsInRoot().left)
        margin = Spacing.l
        rule.waitForIdle()
        assertEquals(Spacing.l, rule.onNodeWithText("Fält").getBoundsInRoot().left)
    }

    @Test
    fun `avståndet mellan detaljens sektioner är Spacing m som standard och appens via LocalDetailSectionGap`() {
        var gap: Dp? by mutableStateOf(null)
        fun between() = rule.onNodeWithText("Andra").getBoundsInRoot().top - rule.onNodeWithText("Första").getBoundsInRoot().bottom
        rule.setContent {
            TestTheme {
                val detail = @Composable {
                    EntityDetailScreen(DetailUiState.Content("Förkylning"), header = null, onBack = {}) {
                        Text("Första")
                        Text("Andra")
                    }
                }
                gap?.let { CompositionLocalProvider(LocalDetailSectionGap provides it) { detail() } } ?: detail()
            }
        }
        assertEquals(Spacing.m, between())
        gap = Spacing.l
        rule.waitForIdle()
        assertEquals(Spacing.l, between())
    }
}
