package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class RepositoryNewsFragment : Fragment() {
    
    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    
    private val allReports = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_repository_news, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
        
        view.findViewById<Button>(R.id.btnFromDate).setOnClickListener { 
            pickDate { date ->
                fromDate = date
                applyFilter()
            }
        }
        
        view.findViewById<Button>(R.id.btnToDate).setOnClickListener { 
            pickDate { date ->
                toDate = date
                applyFilter()
            }
        }
        
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
        val dateRegex = Regex("(\\d{4}-\\d{2}-\\d{2})")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            val match = dateRegex.find(name)
            if (match != null) {
                val dateStr = match.groupValues[1]
                val date = LocalDate.parse(dateStr)
                val url = obj.getString("download_url")
                list.add(Report(name, date, url))
            }
        }
        return list.sortedByDescending { it.date }
    }
    
    private fun applyFilter() {
        val filtered = allReports.filter { report ->
            (fromDate == null || !report.date.isBefore(fromDate)) &&
            (toDate == null || !report.date.isAfter(toDate))
        }
        adapter.update(filtered)
    }
    
    private fun pickDate(onDatePicked: (LocalDate) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            onDatePicked(LocalDate.of(year, month + 1, day))
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }
}