package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class ReportsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    private lateinit var summaryView: TextView
    private lateinit var listenButton: Button
    private lateinit var pauseButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var timeView: TextView
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgress = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                seekBar.progress = mp.currentPosition
                timeView.text = "${formatTime(mp.currentPosition)} / ${formatTime(mp.duration)}"
                if (mp.isPlaying) {
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

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
        listenButton = view.findViewById(R.id.btnListenSummary)
        pauseButton = view.findViewById(R.id.btnPauseSummary)
        seekBar = view.findViewById(R.id.seekSummary)
        timeView = view.findViewById(R.id.tvAudioTime)
        listenButton.setOnClickListener { speakSummary() }
        pauseButton.setOnClickListener { togglePlayback() }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    timeView.text = "${formatTime(progress)} / ${formatTime(mediaPlayer?.duration ?: 0)}"
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}

            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        tts = TextToSpeech(requireContext()) { _ -> }
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
                    hideAudioControls()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                summaryView.visibility = View.GONE
                hideAudioControls()
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
            hideAudioControls()
            return
        }
        summaryView.visibility = View.VISIBLE
        summaryView.text = getString(R.string.summary_loading)
        hideAudioControls()
        thread {
            try {
                val builder = StringBuilder()
                recent.forEach { report ->
                    val text = URL(report.url).readText()
                    builder.append(text).append("\n\n")
                }
                val summary = fetchSummaryFromOpenAI(builder.toString())
                activity?.runOnUiThread {
                    summaryView.text = summary
                    val hide = summary == getString(R.string.summary_no_key) || summary == getString(R.string.summary_failed)
                    if (hide) {
                        hideAudioControls()
                    } else {
                        listenButton.visibility = View.VISIBLE
                    }
                    mediaPlayer?.release()
                    mediaPlayer = null
                    audioFile?.delete()
                    audioFile = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    summaryView.text = getString(R.string.summary_failed)
                    hideAudioControls()
                }
            }
        }
    }

    private fun speakSummary() {
        val text = summaryView.text.toString()
        if (text.isBlank()) return
        audioFile?.let {
            startPlayback(true)
            return
        }
        val file = File(requireContext().cacheDir, "summary.wav")
        audioFile = file
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                activity?.runOnUiThread { startPlayback(true) }
            }
            override fun onError(utteranceId: String?) {
                audioFile = null
            }
        })
        tts?.synthesizeToFile(text, null, file, "summary")
    }

    private fun startPlayback(fromStart: Boolean) {
        val file = audioFile ?: return
        if (fromStart) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    seekBar.max = mp.duration
                    pauseButton.text = getString(R.string.pause_summary)
                    pauseButton.visibility = View.VISIBLE
                    seekBar.visibility = View.VISIBLE
                    timeView.visibility = View.VISIBLE
                    mp.start()
                    handler.post(updateProgress)
                }
                setOnCompletionListener {
                    pauseButton.text = getString(R.string.pause_summary)
                    handler.removeCallbacks(updateProgress)
                }
                prepareAsync()
            }
        } else {
            mediaPlayer?.start()
            pauseButton.text = getString(R.string.pause_summary)
            handler.post(updateProgress)
        }
    }

    private fun togglePlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                pauseButton.text = getString(R.string.resume_summary)
            } else {
                mp.start()
                pauseButton.text = getString(R.string.pause_summary)
                handler.post(updateProgress)
            }
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun hideAudioControls() {
        listenButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        seekBar.visibility = View.GONE
        timeView.visibility = View.GONE
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

    override fun onDestroy() {
        tts?.shutdown()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
