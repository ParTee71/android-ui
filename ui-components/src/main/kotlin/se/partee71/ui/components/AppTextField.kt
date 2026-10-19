package se.partee71.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import se.partee71.ui.theme.AppTheme

/** Standardtaken för [AppTextField] – ReseApotekets `TextLimits`. En app med andra tak (samma som rules) skickar sina. */
object AppTextFieldDefaults {
    /** Namn och andra enradiga fält. */
    const val SHORT_MAX_LENGTH = 200

    /** Anteckningar och förklaringar (flerradiga fält). Dagboken: 5000. */
    const val LONG_MAX_LENGTH = 2000
}

/**
 * Det enda textfältet: etikett, och under fältet antingen [error] (röd, läses av TalkBack som
 * fel) eller [helper]. Fält i formulär får sina fel från `EditorState`. Tangentbordet börjar
 * med stor bokstav; ett fält som anger egna [keyboardOptions], t.ex. siffror, styr själv.
 * Texten växer inte förbi [maxLength] – samma tak som appens rules; en inklistring som skulle gå
 * över ignoreras hellre än kapas mitt i. En lagrad längre text kortas aldrig av, men måste kortas
 * under taket för att gå att spara. Ett flerradigt fält växer med texten, högst [maxLines] rader –
 * sedan scrollar det inuti. [suffix] står efter värdet inne i fältet, nedtonat – t.ex. enheten
 * ("25 mg"). [valueIsPlaceholder] tonar ned värdet som en platshållare – för [PickerField], som visar
 * en text i stället för ett tomt värde.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    maxLength: Int = if (singleLine) AppTextFieldDefaults.SHORT_MAX_LENGTH else AppTextFieldDefaults.LONG_MAX_LENGTH,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    suffix: String? = null,
    valueIsPlaceholder: Boolean = false
) {
    val card = AppTheme.extendedColors.card
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength || it.length < value.length) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = if (valueIsPlaceholder) AppTheme.typography.body.copy(color = muted) else AppTheme.typography.body,
        label = { Text(label) },
        trailingIcon = trailingIcon?.let { { Icon(painterResource(it), contentDescription = null) } },
        suffix = suffix?.let { { Text(it, style = AppTheme.typography.body, color = muted) } },
        supportingText = (error ?: helper)?.let { { Text(it, style = AppTheme.typography.itemSubtitle) } },
        isError = error != null,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = AppTheme.shapes.field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = card,
            unfocusedContainerColor = card,
            disabledContainerColor = card,
            errorContainerColor = card,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}
