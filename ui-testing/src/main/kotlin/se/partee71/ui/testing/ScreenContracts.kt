package se.partee71.ui.testing

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import se.partee71.ui.common.DetailUiState
import se.partee71.ui.common.EditorEvents
import se.partee71.ui.common.EditorState
import se.partee71.ui.common.EditorUiState
import se.partee71.ui.common.ErrorMapper
import se.partee71.ui.common.ListUiState
import se.partee71.ui.components.ENTITY_LIST_ADD_TAG
import se.partee71.ui.components.R

/**
 * Det kontrakten behöver av appen: appens tema ([theme], t.ex. `{ ReseApoteketTheme(content = it) }`),
 * appens felöversättning ([errors], t.ex. `AppErrors`) och ett fel där "Försök igen" hjälper
 * ([retryableFailure], t.ex. `DataError.Offline`). Skrivs en gång i appens testkod.
 */
class ContractEnvironment(
    val errors: ErrorMapper,
    val retryableFailure: Throwable,
    val theme: @Composable (content: @Composable () -> Unit) -> Unit
)

private val context: Context get() = ApplicationProvider.getApplicationContext()

private fun text(id: Int): String = context.getString(id)

/** Texten som visas för [ContractEnvironment.retryableFailure]. */
private val ContractEnvironment.failureText: String get() = text(errors.map(retryableFailure).message)

/**
 * Kontraktet för en skärm på `EntityListScreen`: laddning, tomt med knapp som lägger till, fel med
 * "Försök igen" och innehåll med lägg till ovanför listan. Feature-tester anropar det här och testar
 * sedan bara det som är unikt för skärmen.
 *
 * @param item en rad och [itemText] som ska synas för den.
 * @param addLabel lägg till-knappens text; `null` för en lista utan lägg till – då prövas att ingen
 *   lägg till-knapp finns, varken i det tomma tillståndet eller ovanför listan.
 * @param emptyActions texterna på skärmens egna knappar i det tomma tillståndet (`EmptyContent.actions`),
 *   som ska synas där; standard inga.
 * @param errorTitle läsfelets rubrik om skärmen har en egen (`EntityListScreen.errorTitle`); standard ramens.
 * @param screen skärmens innehåll för ett tillstånd, med callbacks för lägg till och försök igen.
 */
fun <T> ComposeContentTestRule.runListScreenContract(
    environment: ContractEnvironment,
    item: T,
    itemText: String,
    emptyTitle: String,
    addLabel: String?,
    emptyActions: List<String> = emptyList(),
    errorTitle: String = text(R.string.list_error_title),
    screen: @Composable (state: ListUiState<T>, onAdd: () -> Unit, onRetry: () -> Unit) -> Unit
) {
    var state by mutableStateOf<ListUiState<T>>(ListUiState.Loading)
    var adds = 0
    var retries = 0
    setContent { environment.theme { screen(state, { adds++ }, { retries++ }) } }

    onNodeWithContentDescription(text(R.string.loading)).assertExists()

    state = ListUiState.Empty
    onNodeWithText(emptyTitle).assertIsDisplayed()
    if (addLabel != null) {
        onNodeWithText(addLabel).performClick()
        assertEquals("tomt tillstånd: knappen ska lägga till", 1, adds)
    } else {
        onNodeWithTag(ENTITY_LIST_ADD_TAG).assertDoesNotExist()
    }
    emptyActions.forEach { onNodeWithText(it).assertIsDisplayed() }

    state = ListUiState.Error(environment.errors.map(environment.retryableFailure))
    onNodeWithText(errorTitle).assertIsDisplayed()
    onNodeWithText(environment.failureText).assertIsDisplayed()
    onNodeWithText(text(R.string.retry)).performClick()
    assertEquals("fel: Försök igen ska läsa om listan", 1, retries)

    state = ListUiState.Content(persistentListOf(item))
    onNodeWithText(itemText).assertIsDisplayed()
    if (addLabel != null) {
        onNodeWithText(addLabel).performClick()
        assertEquals("innehåll: lägg till ska finnas ovanför listan", 2, adds)
    } else {
        onNodeWithTag(ENTITY_LIST_ADD_TAG).assertDoesNotExist()
    }
}

/**
 * Kontraktet för en skärm på `EntityEditScreen` + `EditorState`: "Spara" inaktiv tills formuläret är
 * giltigt och ändrat, fältfel efter ändring, "Släng ändringar?" vid bakåt (pilen och systemets bakåt),
 * sparfel som snackbar och tillbaka efter lyckad sparning – båda kvitterade i [editor].
 *
 * @param editor formulärets tillstånd; [makeInvalid] och [makeValid] ändrar det som användaren skulle.
 *   Ett formulär där varje värde är giltigt har ingen [makeInvalid]; då prövas bara att "Spara" följer
 *   ändrat-läget.
 * @param invalidMessage felet som ska synas efter [makeInvalid].
 * @param saveLabel "Spara"-knappens text, om skärmen har en egen.
 * @param screen skärmen; `onSave` sparar via [editor] med ett resultat som kontraktet styr.
 */
fun <T> ComposeContentTestRule.runEditScreenContract(
    environment: ContractEnvironment,
    editor: EditorState<T>,
    makeInvalid: (EditorState<T>.() -> Unit)?,
    makeValid: EditorState<T>.() -> Unit,
    invalidMessage: String?,
    saveLabel: String = text(R.string.save),
    screen: @Composable (state: EditorUiState<T>, events: EditorEvents, onSave: () -> Unit, onClose: () -> Unit) -> Unit
) {
    var saveResult: Result<Unit> = Result.failure(environment.retryableFailure)
    var closes = 0
    var systemBack: OnBackPressedDispatcher? = null
    setContent {
        systemBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        environment.theme {
            val state by editor.state.collectAsState()
            val scope = rememberCoroutineScope()
            screen(state, editor, { scope.launch { editor.save { saveResult } } }, { closes++ })
        }
    }
    val saveButton = onNodeWithText(saveLabel)
    saveButton.assertIsNotEnabled()

    if (makeInvalid != null) {
        runOnIdle { editor.makeInvalid() }
        onNodeWithText(checkNotNull(invalidMessage) { "ett ogiltigt värde behöver sitt felmeddelande" }).assertIsDisplayed()
        saveButton.assertIsNotEnabled()
    }

    runOnIdle { editor.makeValid() }
    saveButton.assertIsEnabled()

    onNodeWithContentDescription(text(R.string.back)).performClick()
    onNodeWithText(text(R.string.discard_title)).assertIsDisplayed()
    onNodeWithText(text(R.string.keep_editing)).performClick()
    assertEquals("Fortsätt redigera ska stanna kvar", 0, closes)

    // Systemets bakåt (gest eller knapp) frågar på samma sätt som pilen.
    runOnIdle { checkNotNull(systemBack).onBackPressed() }
    onNodeWithText(text(R.string.discard_title)).assertIsDisplayed()
    onNodeWithText(text(R.string.keep_editing)).performClick()
    assertEquals("systemets bakåt med ändringar ska fråga först", 0, closes)

    saveButton.performClick()
    onNodeWithText(environment.failureText).assertIsDisplayed()
    assertEquals("sparfel ska inte lämna skärmen", 0, closes)
    assertNull("sparfelet ska kvitteras när snackbaren startats", editor.state.value.failure)

    saveResult = Result.success(Unit)
    saveButton.performClick()
    waitForIdle()
    assertEquals("lyckad sparning ska gå tillbaka", 1, closes)
    assertFalse("klar ska kvitteras när skärmen lämnats", editor.state.value.done)

    onNodeWithContentDescription(text(R.string.back)).performClick()
    assertEquals("bakåt utan ändringar ska lämna skärmen direkt", 2, closes)
}

/**
 * Kontraktet för en skärm på `EntityDetailScreen`: laddning utan Redigera, läsfel med "Försök igen", något
 * som inte finns (längre) med sin text och utan "Försök igen", och innehåll med Redigera och tillbakapil.
 *
 * @param value ett laddat värde och [valueText] som ska synas för det.
 * @param editable om skärmen har Redigera i toppraden.
 * @param tab flikläge: ingen tillbakapil (skärmen anropar aldrig `onBack`).
 * @param screen skärmen för ett tillstånd, med callbacks för försök igen, Redigera och tillbaka.
 */
fun <T> ComposeContentTestRule.runDetailScreenContract(
    environment: ContractEnvironment,
    value: T,
    valueText: String,
    editable: Boolean = true,
    tab: Boolean = false,
    screen: @Composable (state: DetailUiState<T>, onRetry: () -> Unit, onEdit: () -> Unit, onBack: () -> Unit) -> Unit
) {
    var state by mutableStateOf<DetailUiState<T>>(DetailUiState.Loading)
    var retries = 0
    var edits = 0
    var backs = 0
    setContent { environment.theme { screen(state, { retries++ }, { edits++ }, { backs++ }) } }

    onNodeWithContentDescription(text(R.string.loading)).assertExists()
    onNodeWithContentDescription(text(R.string.edit)).assertDoesNotExist()

    state = DetailUiState.Error(environment.errors.map(environment.retryableFailure))
    onNodeWithText(text(R.string.load_error_title)).assertIsDisplayed()
    onNodeWithText(environment.failureText).assertIsDisplayed()
    onNodeWithText(text(R.string.retry)).performClick()
    assertEquals("fel: Försök igen ska läsa om", 1, retries)

    val notFound = environment.errors.notFound()
    state = DetailUiState.Error(notFound)
    onNodeWithText(text(notFound.message)).assertIsDisplayed()
    onNodeWithText(text(R.string.retry)).assertDoesNotExist()

    state = DetailUiState.Content(value)
    onNodeWithText(valueText).assertIsDisplayed()
    if (editable) {
        onNodeWithContentDescription(text(R.string.edit)).performClick()
        assertEquals("innehåll: Redigera i toppraden", 1, edits)
    } else {
        onNodeWithContentDescription(text(R.string.edit)).assertDoesNotExist()
    }
    if (tab) {
        onNodeWithContentDescription(text(R.string.back)).assertDoesNotExist()
    } else {
        onNodeWithContentDescription(text(R.string.back)).performClick()
        assertEquals("tillbakapilen stänger detaljen", 1, backs)
    }
}
