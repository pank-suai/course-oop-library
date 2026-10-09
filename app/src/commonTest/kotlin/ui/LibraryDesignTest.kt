package ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryDesignTest {
    @Test
    fun `catalog uses the approved neutral Pay-inspired palette`() {
        assertEquals(Color(0xFF242432), LibraryDesign.primary)
        assertEquals(Color(0xFFF7F7FA), LibraryDesign.background)
        assertEquals(Color(0xFFEBEBF2), LibraryDesign.primaryContainer)
    }

    @Test
    fun `navigation exposes all four Russian sections`() {
        assertEquals(
            listOf("Читатели", "Издания", "Выдачи", "Архитектура"),
            LibraryDesign.navigationLabels,
        )
    }

    @Test
    fun `desktop window has spacious material layout`() {
        assertEquals(1180, LibraryDesign.windowWidthDp)
        assertEquals(780, LibraryDesign.windowHeightDp)
    }
}
