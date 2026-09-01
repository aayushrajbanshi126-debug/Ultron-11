package com.ultron.assistant

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.Locale

/**
 * PHASE 1.1 — switched from android.speech.SpeechRecognizer to Vosk
 * (fully offline, bundled in the app). This is the real fix for the
 * beep/tone problem: there is no OS speech-recognition SERVICE being
 * invoked at all anymore, so there's nothing to make a sound. Everything
 * runs as local audio processing inside the app.
 *
 * The model is bundled as app/src/main/assets/model-en-us.zip and gets
 * unpacked to internal storage automatically on first run.
 */
class UltronForegroundService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var speechService: SpeechService? = null

    private var mode = "sleeping"          // "sleeping" | "active"
    private var activeSince: Long = 0L
    private val activeTimeoutMs = 15_000L

    private val wakePhrases = listOf("hello ultron", "wake up", "morning ultron", "power on", "online ultron")
    private val sleepPhrases = listOf("shutdown", "exit", "quit", "power off")

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForegroundNotification()
        loadModelAndStart()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
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

    private fun loadModelAndStart() {
        StorageService.unpack(
            this, "model-en-us", "model",
            { model -> initRecognizer(model) },
            { exception ->
                // If this fires, the model asset likely didn't get bundled
                // correctly during the build — check the build.yml step
                // that downloads/places model-en-us.zip in assets.
                speak("Model failed to load.")
            }
        )
    }

    private fun initRecognizer(model: Model) {
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {}

                override fun onResult(hypothesis: String?) {
                    val text = extractText(hypothesis)
                    if (text.isNotBlank()) handleHeard(text)
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = extractText(hypothesis)
                    if (text.isNotBlank()) handleHeard(text)
                }

                override fun onError(exception: Exception?) {
                    // Silent — just keep listening, no tone, no popup.
                }

                override fun onTimeout() {}
            })
        } catch (e: Exception) {
            speak("Recognizer failed to start.")
        }
    }

    private fun extractText(hypothesis: String?): String {
        if (hypothesis.isNullOrBlank()) return ""
        return try {
            JSONObject(hypothesis).optString("text", "").lowercase(Locale.US)
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkAutoSleep() {
        if (mode == "active" && System.currentTimeMillis() - activeSince > activeTimeoutMs) {
            mode = "sleeping"
            startForegroundNotification()
        }
    }

    private fun handleHeard(text: String) {
        checkAutoSleep()

        if (mode == "sleeping") {
            if (wakePhrases.any { text.contains(it) }) {
                mode = "active"
                activeSince = System.currentTimeMillis()
                startForegroundNotification()
                // Phase 1: just confirm. Phase 2 wires in real commands here.
                speak("Yes?")
            }
        } else {
            activeSince = System.currentTimeMillis()
            if (sleepPhrases.any { text.contains(it) }) {
                mode = "sleeping"
                startForegroundNotification()
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

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        tts.shutdown()
        super.onDestroy()
    }
}
