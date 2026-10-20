package se.partee71.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.common.DetailUiState
import se.partee71.ui.common.ListUiState
import se.partee71.ui.components.testing.Green
import se.partee71.ui.components.testing.TestPalette
import se.partee71.ui.components.testing.TestTheme
import se.partee71.ui.components.testing.assertSameColor
import se.partee71.ui.components.testing.pixelAt
import se.partee71.ui.components.testing.pixelAtEnd
import se.partee71.ui.components.testing.sampleOffline
import se.partee71.ui.components.testing.textStyle
import se.partee71.ui.theme.AppTheme
import se.partee71.ui.theme.Spacing
import se.partee71.ui.theme.Tone

/**
 * Parametrarna som gör att Fonder kan byta till biblioteket utan att ändra utseende (0.6.0): knappens och
 * kortets appstil, konturerad knapp, klickbart kort, radernas overline/titelstil/överstrykning, låst växel,
 * pillens form och beskrivning, flikens märke och dra för att uppdatera i ramarna. Från 0.7.0 också tomma
 * lägets egna knappar, listans felrubrik och tillbakapilens stil.
 */
@RunWith(RobolectricTestRunner::class)
class VariantBehaviorTest {

    @get:Rule
    val rule = createComposeRule()

    /** Innehållet på bakgrunden – det en form inte ritar har bakgrundens färg ([background]). */
    private fun show(content: @Composable () -> Unit) = rule.setContent {
        TestTheme { Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() } }
    }

    private val background = TestPalette.light.background

    private fun height(text: String) = rule.onNodeWithText(text).getBoundsInRoot().let { it.bottom - it.top }

    // --- AppButton ---

    @Test
    fun `knappen läser sin beskrivning i stället för etiketten`() {
        var clicks = 0
        show { AppButton("Följ", { clicks++ }, contentDescription = "Följ Global Index") }
        rule.onNodeWithContentDescription("Följ Global Index").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `knappens höjd följer appens knappstil, och compact går före`() {
        show {
            Column {
                AppButton("Standard", {})
                AppButton("Kompakt", {}, compact = true)
                CompositionLocalProvider(LocalAppButtonStyle provides AppButtonStyle(minHeight = AppButtonDefaults.CompactHeight)) {
                    AppButton("Appens", {})
                }
                AppButton("Egen", {}, style = AppButtonStyle(minHeight = AppButtonDefaults.CompactHeight))
            }
        }
        assertEquals(AppButtonDefaults.MinHeight, height("Standard"))
        assertEquals(height("Kompakt"), height("Appens"))
        assertEquals(height("Kompakt"), height("Egen"))
    }

    @Test
    fun `den tonade knappen har Material 3-färgen som standard och appens ton när den anges`() {
        show {
            Column {
                AppButton("Standard", {}, variant = ButtonVariant.Secondary)
                AppButton("Grön", {}, variant = ButtonVariant.Secondary, style = AppButtonStyle(secondaryTone = Tone.Green))
            }
        }
        val inside = 12.dp
        assertSameColor(TestPalette.light.secondaryContainer, rule.onNodeWithText("Standard").pixelAt(rule.density, inside, AppButtonDefaults.MinHeight / 2))
        assertSameColor(TestPalette.lightExtended.tone(Tone.Green).container, rule.onNodeWithText("Grön").pixelAt(rule.density, inside, AppButtonDefaults.MinHeight / 2))
    }

    @Test
    fun `med formmorf blir knappen fyrkantigare när den trycks ned, annars behåller den pillformen`() {
        show {
            Column {
                AppButton("Morf", {}, style = AppButtonStyle(morph = true))
                AppButton("Pill", {})
            }
        }
        val corner = 4.dp
        for (label in listOf("Morf", "Pill")) {
            assertSameColor(background, rule.onNodeWithText(label).pixelAt(rule.density, corner, corner), "$label i vila: hörnet är runt")
        }
        rule.onNodeWithText("Morf").performTouchInput { down(center) }
        assertNotEquals(background, rule.onNodeWithText("Morf").pixelAt(rule.density, corner, corner), "nedtryckt: hörnet fylls")
        rule.onNodeWithText("Morf").performTouchInput { up() }
        rule.onNodeWithText("Pill").performTouchInput { down(center) }
        assertSameColor(background, rule.onNodeWithText("Pill").pixelAt(rule.density, corner, corner), "utan morf: hörnet förblir runt")
    }

    @Test
    fun `den konturerade knappen har kontur, genomskinlig yta och anropar onClick`() {
        var clicks = 0
        show { AppButton("Registrera sälj", { clicks++ }, variant = ButtonVariant.Outlined) }
        val button = rule.onNodeWithText("Registrera sälj")
        assertNotEquals(background, button.pixelAt(rule.density, 0.dp, AppButtonDefaults.MinHeight / 2), "konturen")
        assertSameColor(background, button.pixelAt(rule.density, 12.dp, AppButtonDefaults.MinHeight / 2), "ytan innanför")
        button.performClick()
        assertEquals(1, clicks)
    }

    // --- AppCard ---

    @Test
    fun `ett klickbart kort är en nod som anropar onClick, ett vanligt kort går inte att trycka på`() {
        var clicks = 0
        show {
            Column {
                AppCard(onClick = { clicks++ }) {
                    Text("Global Index")
                    Text("12 300 kr")
                }
                AppCard { Text("Utan klick") }
            }
        }
        rule.onNodeWithText("Global Index").assertHasClickAction().performClick()
        rule.onNodeWithText("12 300 kr").assertHasClickAction()
        rule.onNodeWithText("Utan klick").assertHasNoClickAction()
        assertEquals(1, clicks)
    }

    @Test
    fun `kortstilen ger kortets luft och avstånd`() {
        show {
            CompositionLocalProvider(LocalAppCardStyle provides AppCardStyle(padding = Spacing.l, contentSpacing = 0.dp)) {
                AppCard(Modifier.testTag("kort")) {
                    Text("Första")
                    Text("Andra")
                }
            }
        }
        assertEquals(Spacing.l, rule.onNodeWithText("Första").getBoundsInRoot().top - rule.onNodeWithTag("kort").getBoundsInRoot().top)
        assertEquals(rule.onNodeWithText("Första").getBoundsInRoot().bottom, rule.onNodeWithText("Andra").getBoundsInRoot().top)
    }

    @Test
    fun `med kontur delar bitarna en skiljelinje, och ett tonat kort har ingen kontur`() {
        val border = BorderStroke(1.dp, TestPalette.light.outline)
        show {
            CompositionLocalProvider(LocalAppCardStyle provides AppCardStyle(border = border)) {
                Column {
                    CardSegment(top = true, bottom = false, Modifier.testTag("första")) { Text("Första") }
                    CardSegment(top = false, bottom = true, Modifier.testTag("andra")) { Text("Andra") }
                    AppCard(Modifier.testTag("kontur")) { Text("Kontur") }
                    AppCard(Modifier.testTag("tonat"), tone = Tone.Green) { Text("Tonat") }
                }
            }
        }
        assertEquals(rule.onNodeWithTag("första").getBoundsInRoot().bottom - 1.dp, rule.onNodeWithTag("andra").getBoundsInRoot().top)
        val edge = 0.dp
        val middle = 24.dp
        assertNotEquals(TestPalette.lightExtended.card, rule.onNodeWithTag("kontur").pixelAt(rule.density, edge, middle), "konturen ritas i kanten")
        assertSameColor(TestPalette.lightExtended.tone(Tone.Green).container, rule.onNodeWithTag("tonat").pixelAt(rule.density, edge, middle))
    }

    // --- Rader ---

    @Test
    fun `overline står ovanför titeln, och titelns stil går att byta`() {
        var itemTitle = TextUnit.Unspecified
        var caption = TextUnit.Unspecified
        show {
            itemTitle = AppTheme.typography.itemTitle.fontSize
            caption = AppTheme.typography.caption.fontSize
            Column {
                ItemRow("Global Index", overline = { InfoPill("Föreslaget") })
                ItemRow("Räntefond", titleStyle = AppTheme.typography.caption)
            }
        }
        assertTrue(rule.onNodeWithText("Föreslaget").getBoundsInRoot().bottom <= rule.onNodeWithText("Global Index").getBoundsInRoot().top)
        assertEquals(itemTitle, rule.onNodeWithText("Global Index", useUnmergedTree = true).textStyle().fontSize)
        assertEquals(caption, rule.onNodeWithText("Räntefond", useUnmergedTree = true).textStyle().fontSize)
    }

    @Test
    fun `en ikryssad kryssrad stryks över och tonas som standard, men kan låta bli`() {
        show {
            Column {
                CheckRow("Klar", checked = true, onCheckedChange = {})
                CheckRow("Ta med", checked = true, onCheckedChange = {}, done = false, tinted = false)
            }
        }
        assertEquals(TextDecoration.LineThrough, rule.onNodeWithText("Klar", useUnmergedTree = true).textStyle().textDecoration)
        assertNotEquals(TextDecoration.LineThrough, rule.onNodeWithText("Ta med", useUnmergedTree = true).textStyle().textDecoration)
        assertSameColor(TestPalette.lightExtended.rowTint, rule.onNodeWithText("Klar").pixelAtEnd(rule.density, Spacing.s))
        assertSameColor(background, rule.onNodeWithText("Ta med").pixelAtEnd(rule.density, Spacing.s))
        rule.onNodeWithText("Ta med").assertHasClickAction()
    }

    @Test
    fun `en låst växelrad växlar inte, och med onClick går raden ändå att öppna`() {
        var changes = 0
        var opened = 0
        show {
            Column {
                SwitchRow("Bara indexfonder", checked = false, onCheckedChange = { changes++ }, enabled = false)
                SwitchRow("Påminnelse", checked = true, onCheckedChange = { changes++ }, onClick = { opened++ }, enabled = false)
            }
        }
        rule.onNodeWithText("Bara indexfonder").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Påminnelse").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Påminnelse").performClick()
        assertEquals(0 to 1, changes to opened)
    }

    // --- InfoPill ---

    @Test
    fun `pillen läses med sin beskrivning och tar en egen form`() {
        show {
            Column {
                InfoPill("Risk 5/7", contentDescription = "Risknivå 5 av 7")
                InfoPill("Rund", Modifier.testTag("rund"))
                InfoPill("Kantig", Modifier.testTag("kantig"), shape = RectangleShape)
            }
        }
        rule.onNodeWithContentDescription("Risknivå 5 av 7").assertExists()
        assertSameColor(background, rule.onNodeWithTag("rund").pixelAt(rule.density, 0.dp, 0.dp), "standard: rund")
        assertSameColor(TestPalette.lightExtended.primaryTone.container, rule.onNodeWithTag("kantig").pixelAt(rule.density, 0.dp, 0.dp))
    }

    // --- Flikraden ---

    @Test
    fun `en ovald flik med märke läser antalet med namnet, den valda och en utan antal inget märke`() {
        var selected by mutableIntStateOf(0)
        show {
            AppFloatingToolbar(
                listOf(ToolbarItem("Översikt", R.drawable.ic_clock), ToolbarItem("Beslut", R.drawable.ic_box, badgeCount = 3), ToolbarItem("Händelser", R.drawable.ic_add, badgeCount = 0)),
                selected,
                { selected = it }
            )
        }
        rule.onNodeWithContentDescription("Beslut, 3 nya").performClick()
        rule.onNodeWithContentDescription("Beslut").assertIsSelected()
        rule.onNodeWithContentDescription("Händelser").assertExists()
        rule.onNodeWithText("3").assertDoesNotExist()
    }

    // --- Ramarna ---

    @Test
    fun `listan dras ned för att uppdatera med onRefresh, och utan finns ingen gest`() {
        var refreshes = 0
        var withRefresh by mutableIntStateOf(1)
        rule.setContent {
            TestTheme {
                EntityListScreen(
                    "Fondsök",
                    ListUiState.Content(persistentListOf("Global Index", "Räntefond")),
                    EmptyContent(R.drawable.ic_box, "Inga fonder hittades", "Sök på ett annat namn."),
                    add = null,
                    key = { it },
                    onRefresh = if (withRefresh == 1) ({ refreshes++ }) else null
                ) { ItemRow(it) }
            }
        }
        rule.onNodeWithTag(ENTITY_REFRESH_TAG).performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertEquals(1, refreshes)
        withRefresh = 0
        rule.onNodeWithTag(ENTITY_REFRESH_TAG).assertDoesNotExist()
    }

    @Test
    fun `detaljen dras ned för att uppdatera med onRefresh, och utan finns ingen gest`() {
        var refreshes = 0
        var withRefresh by mutableIntStateOf(1)
        rule.setContent {
            TestTheme {
                EntityDetailScreen(
                    DetailUiState.Content("Global Index"),
                    header = null,
                    onBack = {},
                    title = "Global Index",
                    onRefresh = if (withRefresh == 1) ({ refreshes++ }) else null
                ) { ItemRow("Kurs 312,40 kr") }
            }
        }
        rule.onNodeWithTag(ENTITY_REFRESH_TAG).performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertEquals(1, refreshes)
        withRefresh = 0
        rule.onNodeWithTag(ENTITY_REFRESH_TAG).assertDoesNotExist()
    }

    // --- 0.7.0: tomma lägets knappar, felrubriken och tillbakapilen ---

    private val back get() = ApplicationProvider.getApplicationContext<Context>().getString(R.string.back)

    @Test
    fun `tomt läge visar egna knappar under varandra och de anropar sin åtgärd`() {
        var created = 0
        var imported = 0
        show {
            EmptyState(
                R.drawable.ic_box,
                "Inga innehav ännu",
                "Lägg till din första fondtransaktion.",
                actions = listOf(EmptyAction("Ny transaktion", { created++ }, R.drawable.ic_add), EmptyAction("Importera avräkningsnota", { imported++ }, variant = ButtonVariant.Secondary))
            )
        }
        rule.onNodeWithText("Ny transaktion").performClick()
        rule.onNodeWithText("Importera avräkningsnota").performClick()
        assertEquals(1, created)
        assertEquals(1, imported)
        val first = rule.onNodeWithText("Ny transaktion").getBoundsInRoot()
        val second = rule.onNodeWithText("Importera avräkningsnota").getBoundsInRoot()
        assertTrue(second.top > first.bottom, "den andra knappen står under den första")
    }

    @Test
    fun `listan utan lägg till visar de tomma knapparna, med lägg till står de under den`() {
        var withAdd by mutableIntStateOf(0)
        var created = 0
        val empty = EmptyContent(R.drawable.ic_box, "Inga transaktioner", "Registrera köp och sälj.", actions = listOf(EmptyAction("Ny transaktion", { created++ })))
        rule.setContent {
            TestTheme {
                EntityListScreen<String>("Händelser", ListUiState.Empty, empty, if (withAdd == 1) AddAction("Lägg till", {}) else null, key = { it }) {}
            }
        }
        rule.onNodeWithTag(ENTITY_LIST_ADD_TAG).assertDoesNotExist()
        rule.onNodeWithText("Ny transaktion").performClick()
        assertEquals(1, created)
        withAdd = 1
        val add = rule.onNodeWithTag(ENTITY_LIST_ADD_TAG).getBoundsInRoot()
        assertTrue(rule.onNodeWithText("Ny transaktion").getBoundsInRoot().top > add.bottom, "egna knappar står under lägg till")
    }

    @Test
    fun `listans läsfel får en egen rubrik`() {
        val empty = EmptyContent(R.drawable.ic_box, "Inga transaktioner", "Registrera köp och sälj.")
        rule.setContent {
            TestTheme {
                EntityListScreen<String>("Händelser", ListUiState.Error(sampleOffline()), empty, null, key = { it }, errorTitle = "Kunde inte läsa händelserna") {}
            }
        }
        rule.onNodeWithText("Kunde inte läsa händelserna").assertExists()
        rule.onNodeWithText(ApplicationProvider.getApplicationContext<Context>().getString(R.string.list_error_title)).assertDoesNotExist()
    }

    @Test
    fun `tillbakapilen är en tonad platta som standard, genomskinlig eller i en ton när appen väljer det`() {
        var style by mutableIntStateOf(0)
        show {
            val backStyle = when (style) {
                0 -> TopBarBackStyle()
                1 -> TopBarBackStyle(variant = IconButtonVariant.Plain)
                else -> TopBarBackStyle(tone = Tone.Green)
            }
            CompositionLocalProvider(LocalTopBarBackStyle provides backStyle) {
                EntityListScreen<String>("Lägg till fond", ListUiState.Loading, EmptyContent(R.drawable.ic_box, "Inga", "Inga"), null, key = { it }, onBack = {}, topBarSize = TopBarSize.Small) {}
            }
        }
        // Ovanför ikonen men inuti den runda plattan.
        fun plate() = rule.onNodeWithContentDescription(back).pixelAt(rule.density, 24.dp, 6.dp)
        assertSameColor(TestPalette.light.secondaryContainer, plate(), "standard")
        style = 1
        rule.waitForIdle()
        assertSameColor(background, plate(), "genomskinlig")
        style = 2
        rule.waitForIdle()
        assertSameColor(TestPalette.lightExtended.tone(Tone.Green).container, plate(), "grön ton")
    }
}
