package se.partee71.ui.common

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class UiErrorTest {

    private class LeakyError(message: String) : Exception(message)

    @Test
    fun `toString visar bara felets typ, aldrig dess meddelande`() {
        val text = UiError(LeakyError("Recept: Waran 5 mg till Anna"), message = 1).toString()
        assertEquals("UiError(cause=LeakyError, retryable=true)", text)
        assertFalse(text.contains("Waran"))
    }
}
