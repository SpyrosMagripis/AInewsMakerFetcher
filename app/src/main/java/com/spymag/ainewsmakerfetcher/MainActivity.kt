package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    private lateinit var layoutNews: View
    private lateinit var layoutDaily: View
    private lateinit var folderText: TextView

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            folderText.text = uri.toString()
        }
    }

    private val allReports = mutableListOf<Report>()
    private var fromDate: LocalDate? = null
    private var toDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        val isLightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = isLightMode

        val root: View = findViewById(R.id.rootLayout)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        layoutNews = findViewById(R.id.layoutNews)
        layoutDaily = findViewById(R.id.layoutDaily)
        folderText = findViewById(R.id.tvFolder)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    layoutNews.visibility = View.VISIBLE
                    layoutDaily.visibility = View.GONE
                } else {
                    layoutNews.visibility = View.GONE
                    layoutDaily.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        val typedArray = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = typedArray.getDimensionPixelSize(0, 0)
        typedArray.recycle()
        val start = root.paddingLeft
        val end = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                start + systemBars.left,
                systemBars.top + actionBarHeight,
                end + systemBars.right,
                bottom + systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        listView = findViewById(R.id.listReports)
        adapter = ReportAdapter(this, mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val report = adapter.getItem(position)
            if (report != null) {
                val intent = Intent(this, ReportActivity::class.java)
                intent.putExtra("url", report.url)
                startActivity(intent)
            }
        }
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

        findViewById<Button>(R.id.btnSelectFolder).setOnClickListener {
            folderPicker.launch(null)
        }

        fetchReports()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                fetchReports()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
