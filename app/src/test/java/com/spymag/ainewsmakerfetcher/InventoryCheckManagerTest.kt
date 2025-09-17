package com.spymag.ainewsmakerfetcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryCheckManagerTest {

    private class FakeCheckSettingsStorage : CheckSettingsStorage {
        var interval = MIN_CHECK_INTERVAL_DAYS
        var lastSnapshotDate: LocalDate? = null
        var frequencies: MutableMap<String, Int> = mutableMapOf()
        var lastReport: ShoppingListReport? = null

        override fun getIntervalDays(): Int = interval

        override fun setIntervalDays(days: Int) {
            interval = days
        }

        override fun getLastSnapshotDate(): LocalDate? = lastSnapshotDate

        override fun setLastSnapshotDate(date: LocalDate) {
            lastSnapshotDate = date
        }

        override fun getItemFrequencies(): Map<String, Int> = frequencies.toMap()

        override fun setItemFrequencies(frequencies: Map<String, Int>) {
            this.frequencies = frequencies.toMutableMap()
        }

        override fun getLastShoppingReport(): ShoppingListReport? = lastReport

        override fun setLastShoppingReport(report: ShoppingListReport?) {
            lastReport = report
        }
    }

    @Test
    fun updateInterval_clampsValuesToBounds() {
        val storage = FakeCheckSettingsStorage()
        val manager = InventoryCheckManager(storage)

        manager.updateInterval(3)
        assertEquals(MIN_CHECK_INTERVAL_DAYS, storage.interval)

        manager.updateInterval(14)
        assertEquals(14, storage.interval)

        manager.updateInterval(60)
        assertEquals(MAX_CHECK_INTERVAL_DAYS, storage.interval)
    }

    @Test
    fun registerSnapshot_beforeIntervalDoesNotGenerateReport() {
        val storage = FakeCheckSettingsStorage()
        val manager = InventoryCheckManager(storage)

        val first = InventorySnapshot(LocalDate.of(2024, 1, 1), listOf("Milk", "Eggs"))
        val second = InventorySnapshot(LocalDate.of(2024, 1, 4), listOf("Milk"))

        assertNull(manager.registerSnapshot(first))
        assertNull(manager.registerSnapshot(second))
        assertEquals(LocalDate.of(2024, 1, 4), storage.lastSnapshotDate)
        assertEquals(2, storage.frequencies["milk"])
        assertEquals(1, storage.frequencies["eggs"])
        assertNull(storage.lastReport)
    }

    @Test
    fun registerSnapshot_afterIntervalWithMissingCommonItemsGeneratesReport() {
        val storage = FakeCheckSettingsStorage()
        val manager = InventoryCheckManager(storage)

        val first = InventorySnapshot(LocalDate.of(2024, 1, 1), listOf("Milk", "Eggs"))
        val second = InventorySnapshot(LocalDate.of(2024, 1, 5), listOf("Milk", "Eggs"))
        val third = InventorySnapshot(LocalDate.of(2024, 1, 13), listOf("Milk"))

        assertNull(manager.registerSnapshot(first))
        assertNull(manager.registerSnapshot(second))
        val report = manager.registerSnapshot(third)
        assertNotNull(report)
        assertTrue(report!!.missingItems.contains("eggs"))
        assertEquals(LocalDate.of(2024, 1, 13), report.generatedOn)
        assertEquals(MIN_CHECK_INTERVAL_DAYS, report.intervalDays)
        assertEquals(report, storage.lastReport)
    }

    @Test
    fun registerSnapshot_normalizesItemNames() {
        val storage = FakeCheckSettingsStorage()
        val manager = InventoryCheckManager(storage)

        val first = InventorySnapshot(LocalDate.of(2024, 1, 1), listOf("  Milk  ", "Eggs"))
        val second = InventorySnapshot(LocalDate.of(2024, 1, 9), listOf("eggs"))

        manager.registerSnapshot(first)
        val report = manager.registerSnapshot(second)

        assertNotNull(report)
        assertTrue(report!!.missingItems.contains("milk"))
    }

    @Test
    fun nextCheckDateReflectsLastSnapshotAndInterval() {
        val storage = FakeCheckSettingsStorage()
        val manager = InventoryCheckManager(storage)

        val first = InventorySnapshot(LocalDate.of(2024, 1, 1), listOf("Milk"))
        manager.registerSnapshot(first)
        assertEquals(LocalDate.of(2024, 1, 8), manager.nextCheckDate())

        manager.updateInterval(10)
        assertEquals(LocalDate.of(2024, 1, 11), manager.nextCheckDate())
    }
}
