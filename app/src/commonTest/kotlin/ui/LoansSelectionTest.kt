package ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoansSelectionTest {
    @Test
    fun `available selection remains valid`() {
        assertEquals(
            "publication-1",
            resolveAvailableSelection(
                selectedId = "publication-1",
                availableIds = setOf("publication-1", "publication-2"),
            ),
        )
    }

    @Test
    fun `selection is cleared when publication becomes unavailable`() {
        assertNull(
            resolveAvailableSelection(
                selectedId = "publication-1",
                availableIds = setOf("publication-2"),
            ),
        )
    }

    @Test
    fun `missing selection stays empty`() {
        assertNull(resolveAvailableSelection(selectedId = null, availableIds = setOf("publication-1")))
    }
}
