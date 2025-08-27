package com.spymag.ainewsmakerfetcher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.concurrent.thread

class ActionsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter

    private val allActions = mutableListOf<Report>()

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
                conn.connect()
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val fetched = parseActions(json)
                allActions.clear()
                allActions.addAll(fetched)
                activity?.runOnUiThread { adapter.update(allActions) }
            } catch (e: Exception) {
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
}