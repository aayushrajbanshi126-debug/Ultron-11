package com.ultron.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNeededPermissions()

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val intent = Intent(this, UltronForegroundService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, UltronForegroundService::class.java))
        }

        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            // Android requires the user to manually flip this on — no app can
            // silently grant itself Accessibility access. This just jumps you
            // to the right settings screen.
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun requestNeededPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }
}
