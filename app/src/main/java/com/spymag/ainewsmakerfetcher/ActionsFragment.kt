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
import com.google.android.material.slider.Slider
import org.json.JSONArray
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class ActionsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    private lateinit var checkManager: InventoryCheckManager
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValueView: TextView
    private lateinit var nextCheckView: TextView
    private lateinit var lastReportView: TextView

    private val allActions = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null

    private val dateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkManager = InventoryCheckManager(CheckSettingsRepository(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        intervalSlider = view.findViewById(R.id.sliderCheckInterval)
        intervalValueView = view.findViewById(R.id.tvCheckIntervalValue)
        nextCheckView = view.findViewById(R.id.tvNextCheckDate)
        lastReportView = view.findViewById(R.id.tvLastShoppingReport)

        intervalSlider.apply {
            valueFrom = MIN_CHECK_INTERVAL_DAYS.toFloat()
            valueTo = MAX_CHECK_INTERVAL_DAYS.toFloat()
            stepSize = 1f
            value = checkManager.currentInterval().toFloat()
            addOnChangeListener { _, sliderValue, fromUser ->
                val days = sliderValue.toInt().coerceIn(MIN_CHECK_INTERVAL_DAYS, MAX_CHECK_INTERVAL_DAYS)
                if (fromUser) {
                    checkManager.updateInterval(days)
                }
                updateCheckSummary(days)
            }
        }

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
        updateCheckSummary()
    }

    fun refreshData() {
        fetchActions()
    }

    fun registerInventorySnapshot(date: LocalDate, detectedItems: Collection<String>) {
        val snapshot = InventorySnapshot(date, detectedItems.toList())
        val report = checkManager.registerSnapshot(snapshot)
        if (report != null && isAdded) {
            val message = getString(R.string.shopping_report_toast, formatItems(report.missingItems))
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        if (this::intervalValueView.isInitialized && this::intervalSlider.isInitialized) {
            updateCheckSummary()
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::intervalValueView.isInitialized && this::intervalSlider.isInitialized) {
            updateCheckSummary()
        }
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

    private fun updateCheckSummary(intervalOverride: Int? = null) {
        val interval = intervalOverride ?: checkManager.currentInterval()
        intervalValueView.text = getString(R.string.check_interval_value, interval)

        val nextDate = checkManager.nextCheckDate()
        nextCheckView.text = nextDate?.let {
            getString(R.string.check_next_date_value, dateFormatter.format(it))
        } ?: getString(R.string.check_next_date_unknown)

        checkManager.lastReport()?.let { report ->
            if (report.missingItems.isNotEmpty()) {
                val formattedDate = dateFormatter.format(report.generatedOn)
                val items = formatItems(report.missingItems)
                lastReportView.text = getString(R.string.shopping_report_message, formattedDate, items)
                return
            }
        }
        lastReportView.text = getString(R.string.shopping_report_none)

        val sliderValue = intervalSlider.value.toInt()
        if (sliderValue != interval) {
            intervalSlider.value = interval.toFloat()
        }
    }

    private fun formatItems(items: List<String>): String {
        return items.joinToString(", ") { item ->
            item.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(Locale.getDefault())
                } else {
                    char.toString()
                }
            }
        }
    }
}
