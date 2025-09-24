package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
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

    private lateinit var tokenWarning: View
    private lateinit var tokenRetryButton: Button
    private lateinit var tokenMessage: TextView

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

        tokenWarning = view.findViewById(R.id.tokenWarning)
        tokenRetryButton = view.findViewById(R.id.btnTokenRetry)
        tokenMessage = view.findViewById(R.id.tokenMessage)
        tokenRetryButton.setOnClickListener { fetchActions() }

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

        if (BuildConfig.GITHUB_PAT_ACTIONS.isBlank()) {
            showTokenWarning(getString(R.string.token_missing_message))
        } else {
            tokenWarning.visibility = View.GONE
        }

        fetchActions()
    }

    fun refreshData() {
        fetchActions()
    }

    private fun fetchActions() {
        if (BuildConfig.GITHUB_PAT_ACTIONS.isBlank()) {
            activity?.runOnUiThread { showTokenWarning(getString(R.string.token_missing_message)) }
            return
        }
        thread {
            try {
                val urlStr = "https://api.github.com/repos/SpyrosMagripis/FilesServer/contents/ActionsForToday"
                val conn = openAuthorizedConnection(urlStr)
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connect()
                val code = conn.responseCode
                if (code in 200..299) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val fetched = parseActions(json)
                    allActions.clear()
                    allActions.addAll(fetched)
                    activity?.runOnUiThread { applyFilter() }
                } else {
                    val msg = when (code) {
                        401, 403 -> {
                            activity?.runOnUiThread { showTokenWarning("Access denied (HTTP $code). Check repoPat_actions token.") }
                            "Unauthorized/Forbidden. Check repoPat_actions token permissions."
                        }
                        404 -> {
                            activity?.runOnUiThread { showTokenWarning("Path not found (HTTP 404). Verify folder and token access.") }
                            "Not found (private repo?)."
                        }
                        else -> "GitHub API error $code"
                    }
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: FileNotFoundException) {
                activity?.runOnUiThread {
                    showTokenWarning("Cannot access FilesServer repo. Check token.")
                    Toast.makeText(requireContext(), "Cannot access FilesServer repo (private?).", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    showTokenWarning("Error fetching actions: ${e.message}")
                    Toast.makeText(requireContext(), "Error fetching actions: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun openAuthorizedConnection(urlStr: String): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        val pat = BuildConfig.GITHUB_PAT_ACTIONS
        if (pat.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $pat")
        }
        conn.setRequestProperty("User-Agent", "AInewsMakerFetcher")
        return conn
    }

    private fun parseActions(json: String): List<Report> {
        val arr = JSONArray(json)
        val list = mutableListOf<Report>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            if (name.endsWith(".md")) {
                val downloadUrl = obj.optString("download_url", "")
                val apiUrl = obj.getString("url")
                val contentUrl = if (downloadUrl.isBlank() || downloadUrl == "null") apiUrl else downloadUrl
                val date = parseDateFromFileName(name) ?: LocalDate.MIN
                list.add(Report(name, date, contentUrl))
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

    private fun showTokenWarning(message: String) {
        tokenMessage.text = message
        tokenWarning.visibility = View.VISIBLE
    }
}