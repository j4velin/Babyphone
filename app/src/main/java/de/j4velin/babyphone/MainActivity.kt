package de.j4velin.babyphone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import de.j4velin.ledclient.lib.Flash
import de.j4velin.ledclient.lib.LedEffect

const val PREFERENCES_NAME = "settings"
const val THRESHOLD_DEFAULT = 5000
const val THRESHOLD_SETTING_KEY = "threshold"
const val SERVER_URI_KEY = "serverUri"
const val SERVER_URI_DEFAULT = "192.168.178.23:5000"
const val TAG = "Babyphone"

val EFFECT_DEFAULT: LedEffect = Flash(Color.RED, 0.5f, 1)

class MainActivity : Activity() {

    private var statusImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        val threshold = findViewById<EditText>(R.id.threshold)
        threshold.setText(prefs.getInt(THRESHOLD_SETTING_KEY, THRESHOLD_DEFAULT).toString())

        val uri = findViewById<EditText>(R.id.uri)
        uri.setText(prefs.getString(SERVER_URI_KEY, SERVER_URI_DEFAULT))

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.edit()
                .putInt(THRESHOLD_SETTING_KEY, Integer.parseInt(threshold.text.toString()))
                .putString(SERVER_URI_KEY, uri.text.toString())
                .apply()
            Log.d(TAG, "Setting threshold to " + threshold.text.toString())
        }

        statusImage = findViewById(R.id.status)
        statusImage?.setOnClickListener {
            val intent = Intent(applicationContext, RecorderService::class.java)
            val isRecording = it.tag as Boolean
            if (isRecording) {
                stopService(intent)
                Toast.makeText(this, R.string.recording_stopped, Toast.LENGTH_SHORT)
                    .show()
            } else {
                startForegroundService(intent)
                Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT)
                    .show()
            }
            updateStatus(!isRecording)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus(RecorderService.isRecording)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Starts the recording and updates the UI with the current value
     */
    private fun startRecording() {
        val current = findViewById<TextView>(R.id.current)
        val handler = Handler()
        val condition = { !isDestroyed && !isFinishing }

        getAmplitudeWhile(condition, 500) {
            handler.post {
                current.text = "(current: $it)"
            }
        }
    }

    /**
     * Update the status image
     * @param isRecording true, to set the status to 'recording'
     */
    private fun updateStatus(isRecording: Boolean) {
        Log.d(TAG, "updateStatus, isRecording=$isRecording")
        statusImage?.setImageResource(
            if (isRecording) {
                R.drawable.ic_mic_off_black_24dp
            } else {
                R.drawable.ic_mic_black_24dp
            }
        )
        statusImage?.tag = isRecording
    }
}
