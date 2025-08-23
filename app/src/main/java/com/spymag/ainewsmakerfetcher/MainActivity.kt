package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter

    private val allReports = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listReports)
        adapter = ReportAdapter(this, mutableListOf())
        listView.adapter = adapter

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { fetchReports() }
        findViewById<Button>(R.id.btnClearFilter).setOnClickListener {
            fromDate = null
            toDate = null
            applyFilter()
        }
        findViewById<Button>(R.id.btnFromDate).setOnClickListener { pickDate { date ->
            fromDate = date
            applyFilter()
        } }
        findViewById<Button>(R.id.btnToDate).setOnClickListener { pickDate { date ->
            toDate = date
            applyFilter()
        } }

        fetchReports()
    }

    private fun fetchReports() {
        thread {
            try {
                val url = URL("https://api.github.com/repos/spymag/AInewsMaker/contents/reports")
                val conn = url.openConnection() as HttpURLConnection
                conn.connect()
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val fetched = parseReports(json)
                allReports.clear()
                allReports.addAll(fetched)
                runOnUiThread { applyFilter() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseReports(json: String): List<Report> {
        val arr = JSONArray(json)
        val list = mutableListOf<Report>()
        val dateRegex = Regex("(\\d{4}-\\d{2}-\\d{2})")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            if (name.endsWith(".md")) {
                val url = obj.getString("download_url")
                val match = dateRegex.find(name)
                val date = match?.let { LocalDate.parse(it.value) } ?: LocalDate.MIN
                list.add(Report(name, date, url))
            }
        }
        return list.sortedByDescending { it.date }
    }

    private fun applyFilter() {
        val filtered = allReports.filter { report ->
            val afterFrom = fromDate?.let { !report.date.isBefore(it) } ?: true
            val beforeTo = toDate?.let { !report.date.isAfter(it) } ?: true
            afterFrom && beforeTo
        }
        adapter.update(filtered)
    }

    private fun pickDate(onDate: (LocalDate) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            onDate(LocalDate.of(year, month + 1, day))
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }
}
