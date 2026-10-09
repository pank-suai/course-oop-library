package ui

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class LoanDateFormatterTest {
    @Test fun `deadline is displayed as a readable Russian calendar date in local time`() {
        val epoch = LocalDate.of(2026, 9, 30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals("30.09.2026", formatLoanDate(epoch))
    }
}
