package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread

class ReportsFragment : Fragment(), TextToSpeech.OnInitListener {

    companion object {
        private const val CHANNEL_ID = "summary_audio"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE = "com.spymag.ainewsmakerfetcher.action.TOGGLE"
        private const val ACTION_STOP = "com.spymag.ainewsmakerfetcher.action.STOP"
    }

    private lateinit var listView: ListView
    private lateinit var adapter: ReportAdapter
    private lateinit var summaryContainer: View
    private lateinit var summaryView: TextView
    private lateinit var listenButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var timeView: TextView
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: java.io.File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                val pos = mp.currentPosition
                val dur = mp.duration
                seekBar.progress = pos
                timeView.text = "${formatTime(pos)} / ${formatTime(dur)}"
                handler.postDelayed(this, 500)
            }
        }
    }
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_TOGGLE -> {
                    togglePlayback()
                    showNotification()
                }
                ACTION_STOP -> {
                    stopPlayback()
                    notificationManager.cancel(NOTIFICATION_ID)
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

        summaryContainer = view.findViewById(R.id.summaryContainer)
        summaryView = view.findViewById(R.id.tvSummary)
        listenButton = view.findViewById(R.id.btnListenSummary)
        seekBar = view.findViewById(R.id.audioSeekBar)
        timeView = view.findViewById(R.id.tvAudioTime)
        tts = TextToSpeech(requireContext(), this)
        notificationManager = NotificationManagerCompat.from(requireContext())
        mediaSession = MediaSessionCompat(requireContext(), "summary_audio_session")
        mediaSession.isActive = true
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                notificationReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            requireContext().registerReceiver(notificationReceiver, filter)
        }
        listenButton.setOnClickListener { togglePlayback() }
        seekBar.isEnabled = false
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    timeView.text = "${formatTime(progress)} / ${formatTime(mediaPlayer?.duration ?: 0)}"
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
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
                    summaryContainer.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                summaryContainer.visibility = View.GONE
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
            summaryContainer.visibility = View.VISIBLE
            summaryView.text = getString(R.string.summary_none)
            listenButton.visibility = View.GONE
            return
        }
        summaryContainer.visibility = View.VISIBLE
        summaryView.text = getString(R.string.summary_loading)
        listenButton.visibility = View.GONE
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
                    listenButton.visibility = if (hide) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    summaryView.text = getString(R.string.summary_failed)
                    listenButton.visibility = View.GONE
                }
            }
        }
    }

    private fun togglePlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                listenButton.text = getString(R.string.play)
            } else {
                mp.start()
                listenButton.text = getString(R.string.pause)
                handler.post(updateRunnable)
            }
            showNotification()
            return
        }
        val text = summaryView.text.toString()
        if (text.isNotBlank()) {
            synthesizeAndPlay(text)
        }
    }

    private fun synthesizeAndPlay(text: String) {
        listenButton.isEnabled = false
        audioFile = java.io.File(requireContext().cacheDir, "summary.wav")
        val file = audioFile!!
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onError(utteranceId: String) {
                activity?.runOnUiThread { listenButton.isEnabled = true }
            }
            override fun onDone(utteranceId: String) {
                activity?.runOnUiThread {
                    prepareMediaPlayer(file)
                    listenButton.isEnabled = true
                    listenButton.text = getString(R.string.pause)
                    mediaPlayer?.start()
                    handler.post(updateRunnable)
                    showNotification()
                }
            }
        })
        tts?.synthesizeToFile(text, null, file, "summary")
    }

    private fun prepareMediaPlayer(file: java.io.File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            seekBar.max = duration
            setOnCompletionListener {
                listenButton.text = getString(R.string.play)
                handler.removeCallbacks(updateRunnable)
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
        seekBar.isEnabled = true
        timeView.text = "0:00 / ${formatTime(mediaPlayer?.duration ?: 0)}"
    }

    private fun showNotification() {
        val playing = mediaPlayer?.isPlaying == true
        val actionIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val actionTitle = if (playing) getString(R.string.pause) else getString(R.string.play)
        val toggleIntent = Intent(ACTION_TOGGLE)
        val togglePending = PendingIntent.getBroadcast(
            requireContext(), 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(ACTION_STOP)
        val stopPending = PendingIntent.getBroadcast(
            requireContext(), 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(getString(R.string.summary_notification_title))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setStyle(MediaNotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .addAction(actionIcon, actionTitle, togglePending)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPending)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(updateRunnable)
        if (this::listenButton.isInitialized) {
            listenButton.text = getString(R.string.play)
            seekBar.progress = 0
            seekBar.isEnabled = false
            timeView.text = "0:00 / 0:00"
        }
        audioFile = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.summary_playback_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
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

    override fun onInit(status: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        context?.unregisterReceiver(notificationReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
        mediaSession.release()
        stopPlayback()
        tts?.shutdown()
        tts = null
    }
}
