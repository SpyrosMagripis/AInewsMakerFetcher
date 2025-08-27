package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONArray
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class ActionsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter

    private val allActions = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listActions)
        adapter = ReportAdapter(requireContext(), mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val action = adapter.getItem(position)
            if (action != null) {
                val intent = Intent(requireContext(), ReportActivity::class.java)
                intent.putExtra("url", action.url)
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

        fetchActions()
    }

    fun refreshData() {
        fetchActions()
    }

    private fun fetchActions() {
        thread {
            try {
                val url = URL("https://api.github.com/repos/SpyrosMagripis/FilesServer/contents/ActionsForToday")
                val conn = url.openConnection() as HttpURLConnection
                
                // Add GitHub authentication if PAT is available
                val githubPat = BuildConfig.GITHUB_PAT
                if (githubPat.isNotEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer $githubPat")
                }
                
                conn.connect()
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val fetched = parseActions(json)
                allActions.clear()
                allActions.addAll(fetched)
                activity?.runOnUiThread { applyFilter() }
            } catch (e: FileNotFoundException) {
                // Repository not found or private repository without authentication
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Cannot access 'SpyrosMagripis/FilesServer'. If it's a private repository, make sure the GitHub PAT is correctly set in local.properties.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            } catch (e: Exception) {
                // Handle other errors
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching actions: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun parseActions(json: String): List<Report> {
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
        val filtered = allActions.filter { action ->
            val afterFrom = fromDate?.let { !action.date.isBefore(it) } ?: true
            val beforeTo = toDate?.let { !action.date.isAfter(it) } ?: true
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
}