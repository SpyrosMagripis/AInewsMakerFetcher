package com.spymag.ainewsmakerfetcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class CheckSettingsRepository(context: Context) : CheckSettingsStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("inventory_checks", Context.MODE_PRIVATE)

    override fun getIntervalDays(): Int {
        val stored = prefs.getInt(KEY_INTERVAL_DAYS, MIN_CHECK_INTERVAL_DAYS)
        return stored.coerceIn(MIN_CHECK_INTERVAL_DAYS, MAX_CHECK_INTERVAL_DAYS)
    }

    override fun setIntervalDays(days: Int) {
        val clamped = days.coerceIn(MIN_CHECK_INTERVAL_DAYS, MAX_CHECK_INTERVAL_DAYS)
        prefs.edit().putInt(KEY_INTERVAL_DAYS, clamped).apply()
    }

    override fun getLastSnapshotDate(): LocalDate? {
        return prefs.getString(KEY_LAST_SNAPSHOT_DATE, null)?.let { LocalDate.parse(it) }
    }

    override fun setLastSnapshotDate(date: LocalDate) {
        prefs.edit().putString(KEY_LAST_SNAPSHOT_DATE, date.toString()).apply()
    }

    override fun getItemFrequencies(): Map<String, Int> {
        val raw = prefs.getString(KEY_ITEM_FREQUENCIES, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                val iterator = json.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    put(key, json.getInt(key))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun setItemFrequencies(frequencies: Map<String, Int>) {
        val json = JSONObject()
        frequencies.forEach { (key, value) ->
            json.put(key, value)
        }
        prefs.edit().putString(KEY_ITEM_FREQUENCIES, json.toString()).apply()
    }

    override fun getLastShoppingReport(): ShoppingListReport? {
        val raw = prefs.getString(KEY_LAST_SHOPPING_REPORT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val date = LocalDate.parse(json.getString("date"))
            val interval = json.getInt("interval")
            val itemsArray = json.getJSONArray("items")
            val items = mutableListOf<String>()
            for (i in 0 until itemsArray.length()) {
                items += itemsArray.getString(i)
            }
            ShoppingListReport(date, items, interval)
        } catch (_: Exception) {
            null
        }
    }

    override fun setLastShoppingReport(report: ShoppingListReport?) {
        val editor = prefs.edit()
        if (report == null) {
            editor.remove(KEY_LAST_SHOPPING_REPORT)
        } else {
            val json = JSONObject().apply {
                put("date", report.generatedOn.toString())
                put("interval", report.intervalDays)
                put("items", JSONArray(report.missingItems))
            }
            editor.putString(KEY_LAST_SHOPPING_REPORT, json.toString())
        }
        editor.apply()
    }

    companion object {
        private const val KEY_INTERVAL_DAYS = "interval_days"
        private const val KEY_LAST_SNAPSHOT_DATE = "last_snapshot_date"
        private const val KEY_ITEM_FREQUENCIES = "item_frequencies"
        private const val KEY_LAST_SHOPPING_REPORT = "last_shopping_report"
    }
}
