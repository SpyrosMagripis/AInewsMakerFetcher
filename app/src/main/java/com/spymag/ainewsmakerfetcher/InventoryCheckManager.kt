package com.spymag.ainewsmakerfetcher

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class InventoryCheckManager(
    private val storage: CheckSettingsStorage
) {

    fun currentInterval(): Int = storage.getIntervalDays().coerceIn(MIN_CHECK_INTERVAL_DAYS, MAX_CHECK_INTERVAL_DAYS)

    fun updateInterval(days: Int) {
        storage.setIntervalDays(days.coerceIn(MIN_CHECK_INTERVAL_DAYS, MAX_CHECK_INTERVAL_DAYS))
    }

    fun nextCheckDate(): LocalDate? {
        val last = storage.getLastSnapshotDate() ?: return null
        return last.plusDays(currentInterval().toLong())
    }

    fun lastReport(): ShoppingListReport? = storage.getLastShoppingReport()

    fun registerSnapshot(snapshot: InventorySnapshot): ShoppingListReport? {
        val normalizedItems = snapshot.items.mapNotNull { normalizeItem(it) }.toSet()
        val previousFrequencies = storage.getItemFrequencies()
        val lastSnapshotDate = storage.getLastSnapshotDate()
        val interval = currentInterval()

        val shouldEvaluate = lastSnapshotDate?.let {
            ChronoUnit.DAYS.between(it, snapshot.date) >= interval
        } ?: false

        val missingCommonItems = if (shouldEvaluate && previousFrequencies.isNotEmpty()) {
            val commonItems = findMostCommonItems(previousFrequencies)
            commonItems.filterNot { normalizedItems.contains(it) }
        } else {
            emptyList()
        }

        val updatedFrequencies = previousFrequencies.toMutableMap()
        normalizedItems.forEach { item ->
            updatedFrequencies[item] = updatedFrequencies.getOrDefault(item, 0) + 1
        }
        storage.setItemFrequencies(updatedFrequencies)
        storage.setLastSnapshotDate(snapshot.date)

        return if (missingCommonItems.isNotEmpty()) {
            val report = ShoppingListReport(snapshot.date, missingCommonItems, interval)
            storage.setLastShoppingReport(report)
            report
        } else {
            null
        }
    }

    private fun normalizeItem(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.lowercase(Locale.getDefault())
    }

    private fun findMostCommonItems(frequencies: Map<String, Int>): List<String> {
        if (frequencies.isEmpty()) return emptyList()
        val maxCount = frequencies.values.maxOrNull() ?: return emptyList()
        return frequencies.filterValues { it == maxCount }.keys.toList()
    }
}
