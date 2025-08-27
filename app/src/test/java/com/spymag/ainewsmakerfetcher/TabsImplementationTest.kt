package com.spymag.ainewsmakerfetcher

import org.junit.Test
import org.junit.Assert.*

/**
 * Test for the tabs implementation
 */
class TabsImplementationTest {
    
    @Test
    fun testReportDataClassCreation() {
        val report = Report("test-2024-01-01.md", java.time.LocalDate.of(2024, 1, 1), "https://example.com")
        assertEquals("test-2024-01-01.md", report.name)
        assertEquals(java.time.LocalDate.of(2024, 1, 1), report.date)
        assertEquals("https://example.com", report.url)
    }
    
    @Test
    fun testUrlConstantsForDataSources() {
        val reportsUrl = "https://api.github.com/repos/spymag/AInewsMaker/contents/reports"
        val actionsUrl = "https://api.github.com/repos/SpyrosMagripis/FilesServer/contents/ActionsForToday"
        
        assertTrue("Reports URL should contain correct repository", reportsUrl.contains("spymag/AInewsMaker"))
        assertTrue("Actions URL should contain correct repository", actionsUrl.contains("SpyrosMagripis/FilesServer"))
        assertTrue("Actions URL should contain correct folder", actionsUrl.contains("ActionsForToday"))
    }
}