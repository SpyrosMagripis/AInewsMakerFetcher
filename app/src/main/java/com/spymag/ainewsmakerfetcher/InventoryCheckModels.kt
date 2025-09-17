package com.spymag.ainewsmakerfetcher

import java.time.LocalDate

/**
 * Snapshot of detected pantry or inventory items captured on a given [date].
 */
data class InventorySnapshot(
    val date: LocalDate,
    val items: List<String>
)

/**
 * Recommendation generated when common items are missing from a snapshot.
 */
data class ShoppingListReport(
    val generatedOn: LocalDate,
    val missingItems: List<String>,
    val intervalDays: Int
)
