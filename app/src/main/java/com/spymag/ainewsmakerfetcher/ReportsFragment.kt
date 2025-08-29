package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class ReportsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    private lateinit var summaryView: TextView

    private val allReports = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        summaryView = view.findViewById(R.id.tvSummary)
        val spinner: Spinner = view.findViewById(R.id.spinnerSummary)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.summary_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 1) {
                    generateSummary()
                } else {
                    summaryView.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                summaryView.visibility = View.GONE
            }
        }

        listView = view.findViewById(R.id.listReports)
        adapter = ReportAdapter(requireContext(), mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val report = adapter.getItem(position)
            if (report != null) {
                val intent = Intent(requireContext(), ReportActivity::class.java)
                intent.putExtra("url", report.url)
                startActivity(intent)
            }
        }

        view.findViewById<Button>(R.id.btnClearFilter).setOnClickListener {
            fromDate = null
            toDate = null
            applyFilter()
        }
        view.findViewById<Button>(R.id.btnFromDate).setOnClickListener { pickDate { date ->
            fromDate = date
            applyFilter()
        } }
        view.findViewById<Button>(R.id.btnToDate).setOnClickListener { pickDate { date ->
            toDate = date
            applyFilter()
        } }

        fetchReports()
    }

    fun refreshData() {
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
                activity?.runOnUiThread { applyFilter() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseReports(json: String): List<Report> {
        val arr = JSONArray(json)
        val list = mutableListOf<Report>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            if (name.endsWith(".md")) {
                val url = obj.getString("download_url")
                val date = parseDateFromFileName(name) ?: LocalDate.MIN
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
        DatePickerDialog(requireContext(), { _, year, month, day ->
            onDate(LocalDate.of(year, month + 1, day))
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun generateSummary() {
        val recent = allReports.filter { !it.date.isBefore(LocalDate.now().minusDays(3)) }
        if (recent.isEmpty()) {
            summaryView.visibility = View.VISIBLE
            summaryView.text = getString(R.string.summary_none)
            return
        }
        summaryView.visibility = View.VISIBLE
        summaryView.text = getString(R.string.summary_loading)
        thread {
            try {
                val builder = StringBuilder()
                recent.forEach { report ->
                    val text = URL(report.url).readText()
                    builder.append(text).append("\n\n")
                }
                val summary = fetchSummaryFromOpenAI(builder.toString())
                activity?.runOnUiThread { summaryView.text = summary }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread { summaryView.text = getString(R.string.summary_failed) }
            }
        }
    }

    private fun fetchSummaryFromOpenAI(text: String): String {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) {
            return getString(R.string.summary_no_key)
        }
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val payload = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Summarize the following news reports.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
