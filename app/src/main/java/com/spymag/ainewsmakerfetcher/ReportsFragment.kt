package com.spymag.ainewsmakerfetcher

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
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
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media.session.MediaButtonReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Calendar
import kotlin.concurrent.thread
import android.widget.Toast

class ReportsFragment : Fragment(), TextToSpeech.OnInitListener {

    companion object {
        private const val CHANNEL_ID = "summary_audio"
        private const val NOTIFICATION_ID = 1
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
                updatePlaybackState(mp.isPlaying)
                handler.postDelayed(this, 500)
            }
        }
    }
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat
    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            togglePlayback()
        }

        override fun onPause() {
            togglePlayback()
        }

        override fun onStop() {
            stopPlayback()
            notificationManager.cancel(NOTIFICATION_ID)
        }

        override fun onSeekTo(pos: Long) {
            mediaPlayer?.seekTo(pos.toInt())
            updatePlaybackState(mediaPlayer?.isPlaying == true)
        }
    }

    private val allReports = mutableListOf<Report>()
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
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        summaryContainer = view.findViewById(R.id.summaryContainer)
        summaryView = view.findViewById(R.id.tvSummary)
        listenButton = view.findViewById(R.id.btnListenSummary)
        seekBar = view.findViewById(R.id.audioSeekBar)
        timeView = view.findViewById(R.id.tvAudioTime)
        tokenWarning = view.findViewById(R.id.tokenWarning)
        tokenRetryButton = view.findViewById(R.id.btnTokenRetry)
        tokenMessage = view.findViewById(R.id.tokenMessage)
        tts = TextToSpeech(requireContext(), this)
        notificationManager = NotificationManagerCompat.from(requireContext())
        mediaSession = MediaSessionCompat(requireContext(), "summary_audio_session").apply {
            setCallback(sessionCallback)
            isActive = true
        }
        createNotificationChannel()
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

        tokenRetryButton.setOnClickListener { fetchReports() }
        if (BuildConfig.GITHUB_PAT_NEWS.isBlank()) {
            showTokenWarning(getString(R.string.token_missing_message))
        } else {
            tokenWarning.visibility = View.GONE
        }

        fetchReports()
    }

    fun refreshData() {
        fetchReports()
    }

    private fun fetchReports() {
        if (BuildConfig.GITHUB_PAT_NEWS.isBlank()) {
            activity?.runOnUiThread {
                showTokenWarning(getString(R.string.token_missing_message))
            }
            return
        }
        thread {
            try {
                val urlStr = "https://api.github.com/repos/spymag/AInewsMaker/contents/reports"
                val conn = openAuthorizedConnection(urlStr)
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connect()
                val code = conn.responseCode
                if (code in 200..299) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val fetched = parseReports(json)
                    allReports.clear()
                    allReports.addAll(fetched)
                    activity?.runOnUiThread { applyFilter() }
                } else {
                    val msg = when (code) {
                        401, 403 -> {
                            activity?.runOnUiThread {
                                showTokenWarning("Access denied (HTTP $code). Check that your token in local.properties has read access to repo contents.")
                            }
                            "Unauthorized/Forbidden. Provide a valid GitHub token in local.properties (repoPat=...)"
                        }
                        404 -> {
                            activity?.runOnUiThread {
                                showTokenWarning("Repository or path not found (HTTP 404). Ensure the repo name and path are correct and token has access.")
                            }
                            "Not found (private repo?). Ensure token has repo contents read access."
                        }
                        else -> "GitHub API error $code"
                    }
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    showTokenWarning("Failed to fetch reports: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to fetch reports: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openAuthorizedConnection(urlStr: String): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        val pat = BuildConfig.GITHUB_PAT_NEWS
        if (!pat.isNullOrBlank()) {
            // GitHub accepts either token or Bearer; use Bearer for modern style
            conn.setRequestProperty("Authorization", "Bearer $pat")
        }
        conn.setRequestProperty("User-Agent", "AInewsMakerFetcher")
        return conn
    }

    private fun fetchContentWithAuth(rawUrl: String): String {
        val conn = openAuthorizedConnection(rawUrl)
        if (rawUrl.contains("/contents/")) {
            // Request raw file directly via API when using contents endpoint (needed for private repos)
            conn.setRequestProperty("Accept", "application/vnd.github.v3.raw")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code while fetching content")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseReports(json: String): List<Report> {
        val arr = JSONArray(json)
        val list = mutableListOf<Report>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            if (name.endsWith(".md")) {
                val downloadUrl = obj.optString("download_url", "")
                val apiUrl = obj.getString("url") // Always present
                val contentUrl = if (downloadUrl.isBlank() || downloadUrl == "null") apiUrl else downloadUrl
                val date = parseDateFromFileName(name) ?: LocalDate.MIN
                list.add(Report(name, date, contentUrl))
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
                    try {
                        val text = fetchContentWithAuth(report.url)
                        builder.append(text).append("\n\n")
                    } catch (inner: Exception) {
                        inner.printStackTrace()
                    }
                }
                val combined = builder.toString()
                if (combined.isBlank()) throw IllegalStateException("No content fetched")
                val summary = fetchSummaryFromOpenAI(combined)
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
                updatePlaybackState(false)
            } else {
                mp.start()
                listenButton.text = getString(R.string.pause)
                handler.post(updateRunnable)
                updatePlaybackState(true)
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
                    updatePlaybackState(true)
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
                updatePlaybackState(false)
            }
        }
        seekBar.isEnabled = true
        timeView.text = "0:00 / ${formatTime(mediaPlayer?.duration ?: 0)}"
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: 0L)
                .build()
        )
        updatePlaybackState(false)
    }

    private fun showNotification() {
        val playing = mediaPlayer?.isPlaying == true
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // Skip posting notification silently if not granted
                return
            }
        }
        val actionIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val actionTitle = if (playing) getString(R.string.pause) else getString(R.string.play)
        val toggleAction = if (playing) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        val toggleIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            requireContext(), toggleAction
        )
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            requireContext(), PlaybackStateCompat.ACTION_STOP
        )
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(getString(R.string.summary_notification_title))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(actionIcon, actionTitle, toggleIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stopIntent)
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
        updatePlaybackState(false)
    }

    private fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, 1.0f)
                .build()
        )
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
        notificationManager.cancel(NOTIFICATION_ID)
        mediaSession.release()
        stopPlayback()
        tts?.shutdown()
        tts = null
    }

    private fun showTokenWarning(message: String) {
        tokenMessage.text = message
        tokenWarning.visibility = View.VISIBLE
    }
}
