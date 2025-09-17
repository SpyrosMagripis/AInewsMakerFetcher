package com.spymag.ainewsmakerfetcher

import java.time.LocalDate

interface CheckSettingsStorage {
    fun getIntervalDays(): Int
    fun setIntervalDays(days: Int)

    fun getLastSnapshotDate(): LocalDate?
    fun setLastSnapshotDate(date: LocalDate)

    fun getItemFrequencies(): Map<String, Int>
    fun setItemFrequencies(frequencies: Map<String, Int>)

    fun getLastShoppingReport(): ShoppingListReport?
    fun setLastShoppingReport(report: ShoppingListReport?)
}
