package com.spymag.ainewsmakerfetcher

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun parsesHyphenSeparatedDates() {
        val date = parseDateFromFileName("report-2024-05-10.md")
        assertEquals(LocalDate.of(2024, 5, 10), date)
    }

    @Test
    fun parsesUnderscoreSeparatedDates() {
        val date = parseDateFromFileName("report_2024_05_10.md")
        assertEquals(LocalDate.of(2024, 5, 10), date)
    }

    @Test
    fun parsesCompactDates() {
        val date = parseDateFromFileName("report_20250827.md")
        assertEquals(LocalDate.of(2025, 8, 27), date)
    }
}
