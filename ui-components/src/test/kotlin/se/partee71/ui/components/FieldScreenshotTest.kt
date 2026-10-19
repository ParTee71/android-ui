package se.partee71.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.partee71.ui.components.testing.SampleSheet
import se.partee71.ui.components.testing.captureLightAndDark
import se.partee71.ui.components.testing.captureScreenLightAndDark

/** Skärmdumpar av fälten (ljust + mörkt); där apparna skiljer sig syns båda varianterna. */
@RunWith(RobolectricTestRunner::class)
class FieldScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `AppTextField - lägen`() = captureLightAndDark("AppTextField_lagen") {
        SampleSheet {
            AppTextField("Solkräm", {}, "Namn", helper = "Som det står på förpackningen")
            AppTextField("", {}, "Styrka", error = "Ange en styrka")
            AppTextField("Smörj in var annan timme, och efter bad.", {}, "Anteckning", singleLine = false)
            // Dagboken: enheten efter värdet.
            AppTextField("25", {}, "Doshöjning", suffix = "mg")
        }
    }

    @Test
    fun `PickerField - värde, tomt och platshållare`() = captureLightAndDark("PickerField_lagen") {
        SampleSheet {
            PickerField("Avresa", "lör 19 dec 2026", "Välj datum", R.drawable.ic_clock, {})
            PickerField("Hemresa", "", "Välj datum", R.drawable.ic_clock, {}, error = "Välj ett datum")
            // Dagboken: ett tomt datum betyder något.
            PickerField("Slutdatum", "", "Välj datum", R.drawable.ic_clock, {}, emptyLabel = "Periodens slut")
        }
    }

    @Test
    fun `TimeField - tid`() = captureLightAndDark("TimeField_tid") {
        SampleSheet {
            TimeField("Påminnelse", LocalTime(7, 0), {})
            TimeField("Kvällsdos", LocalTime(21, 30), {}, helper = "Samma tid varje dag")
        }
    }

    @Test
    fun `TimePickerPopup - öppen`() = rule.captureScreenLightAndDark("TimePickerPopup_oppen") {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TimePickerPopup(LocalTime(7, 30), "Välj tid", {}, {})
        }
    }

    @Test
    fun `GroupLabel - etikett`() = captureLightAndDark("GroupLabel_etikett") {
        SampleSheet { GroupLabel("Utseende") }
    }

    @Test
    fun `LabeledGroup - hjälptext under, ovanför och fel`() = captureLightAndDark("LabeledGroup_lagen") {
        SampleSheet {
            LabeledGroup("Form", helper = "Som det står på förpackningen") { ChoiceChips(listOf("Tablett", "Kapsel", "Kräm"), "Kapsel", {}, label = { it }) }
            LabeledGroup("Enhet", error = "Välj en enhet") { ChoiceChips(listOf("st", "ml", "g"), "", {}, label = { it }) }
            // Dagboken: hjälptexten säger hur man börjar.
            LabeledGroup("Känslor", helper = "Börja med den som ligger närmast", helperAbove = true) { ChoiceChips(listOf("Glad", "Ledsen", "Arg"), "Glad", {}, label = { it }) }
        }
    }

    @Test
    fun `FieldError - fel`() = captureLightAndDark("FieldError_fel") {
        SampleSheet { FieldError("Kunde inte spara. Försök igen.") }
    }

    @Test
    fun `QuantityStepper - standard och full tryckyta med värdetext`() = captureLightAndDark("QuantityStepper_varianter") {
        SampleSheet {
            QuantityStepper(2, {}, "doser per dag", range = 1..6)
            QuantityStepper(0, {}, "tabletter", range = 0..99, enabled = false)
            // Dagboken: 48 dp-knappar och värdet som text.
            QuantityStepper(240, {}, "minsta tid mellan doser", step = 30, valueText = { "${it / 60} h" }, buttonSize = QuantityStepperDefaults.TouchTargetButtonSize)
        }
    }
}
