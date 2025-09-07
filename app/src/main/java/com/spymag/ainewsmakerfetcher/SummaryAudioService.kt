package com.spymag.ainewsmakerfetcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.speech.tts.UtteranceProgressListener

class SummaryAudioService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_PLAY = "com.spymag.ainewsmakerfetcher.action.PLAY"
        const val ACTION_PAUSE = "com.spymag.ainewsmakerfetcher.action.PAUSE"
        const val ACTION_STOP = "com.spymag.ainewsmakerfetcher.action.STOP"
        const val EXTRA_TEXT = "extra_text"
        private const val CHANNEL_ID = "summary_audio_channel"
        private const val NOTIFICATION_ID = 1
        private const val UTTERANCE_ID = "summary"
    }

    private var tts: TextToSpeech? = null
    private lateinit var mediaSession: MediaSessionCompat
    private var text: String = ""
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        mediaSession = MediaSessionCompat(this, "SummaryAudioService")
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onError(utteranceId: String) {}
            override fun onDone(utteranceId: String) {
                isPlaying = false
                stopForeground(true)
                stopSelf()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                text = intent.getStringExtra(EXTRA_TEXT) ?: text
                if (text.isNotBlank()) speak()
            }
            ACTION_PAUSE -> {
                tts?.stop()
                isPlaying = false
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun speak() {
        isPlaying = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val playIntent = Intent(this, SummaryAudioService::class.java).apply { action = ACTION_PLAY }
        val pauseIntent = Intent(this, SummaryAudioService::class.java).apply { action = ACTION_PAUSE }
        val stopIntent = Intent(this, SummaryAudioService::class.java).apply { action = ACTION_STOP }
        val playPending = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopPending = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.summary_notification_title))
            .setSmallIcon(R.drawable.ic_play)
            .setStyle(MediaNotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        if (isPlaying) {
            builder.addAction(R.drawable.ic_pause, getString(R.string.pause), pausePending)
        } else {
            builder.addAction(R.drawable.ic_play, getString(R.string.play), playPending)
        }
        builder.addAction(R.drawable.ic_stop, getString(R.string.stop), stopPending)
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.summary_playback_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

