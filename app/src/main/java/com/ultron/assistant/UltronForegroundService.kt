package com.ultron.assistant

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * PHASE 1 — core loop only.
 *
 * Uses SpeechRecognizer directly (not the RecognizerIntent popup that
 * termux-speech-to-text relies on), so there's no system UI flash and no
 * forced earcon tied to your ringer volume — that whole class of problem
 * goes away by building a real app.
 *
 * Wake phrases / sleep phrases / auto-timeout mirror the Termux version so
 * behavior is familiar. Command handling (open app, call contact, etc.) gets
 * wired in during Phase 2 — this file currently just proves the listening
 * loop, wake/sleep state, and TTS work end-to-end.
 */
class UltronForegroundService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())

    private var mode = "sleeping"          // "sleeping" | "active"
    private var activeSince: Long = 0L
    private val activeTimeoutMs = 15_000L

    private val wakePhrases = listOf("hello ultron", "wake up", "morning ultron", "power on", "online ultron")
    private val sleepPhrases = listOf("shutdown", "exit", "quit", "power off")

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        startForegroundNotification()
        startListening()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?) = null

    private fun startForegroundNotification() {
        val channelId = "ultron_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ultron", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ultron")
            .setContentText(if (mode == "active") "Active — listening for commands" else "Sleeping — say a wake phrase")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startListening() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.US) ?: ""
                handleHeard(text)
                relisten()
            }

            override fun onError(error: Int) {
                // No speech / timeout / etc. — just try again. No tone plays here
                // because we never launched the popup UI in the first place.
                relisten()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
        })

        speechRecognizer.startListening(recognizerIntent)
    }

    private fun relisten() {
        val delay = if (mode == "active") 300L else 800L
        handler.postDelayed({ startListening() }, delay)
        checkAutoSleep()
    }

    private fun checkAutoSleep() {
        if (mode == "active" && System.currentTimeMillis() - activeSince > activeTimeoutMs) {
            mode = "sleeping"
            updateNotification()
        }
    }

    private fun handleHeard(text: String) {
        if (text.isBlank()) return

        if (mode == "sleeping") {
            if (wakePhrases.any { text.contains(it) }) {
                mode = "active"
                activeSince = System.currentTimeMillis()
                updateNotification()
                // Phase 1: just confirm. Phase 2 wires in real commands here.
                speak("Yes?")
            }
        } else {
            activeSince = System.currentTimeMillis()
            if (sleepPhrases.any { text.contains(it) }) {
                mode = "sleeping"
                updateNotification()
                speak("Going to sleep.")
            } else {
                // Phase 2: route `text` to command handling / Accessibility actions here.
                speak("You said: $text")
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ultron_utterance")
    }

    private fun updateNotification() {
        startForegroundNotification()
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
