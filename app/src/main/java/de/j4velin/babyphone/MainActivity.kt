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
import de.j4velin.ledclient.lib.Callback
import de.j4velin.ledclient.lib.Flash
import de.j4velin.ledclient.lib.LedEffect
import de.j4velin.ledclient.lib.UiDialog

const val PREFERENCES_NAME = "settings"
const val THRESHOLD_DEFAULT = 5000
const val THRESHOLD_SETTING_KEY = "threshold"
const val SERVER_URI_KEY = "serverUri"
const val SERVER_URI_DEFAULT = "192.168.178.23:5000"
const val EFFECT_JSON_KEY = "effect_json"
const val EFFECT_NAME_KEY = "effect_name"
const val TAG = "Babyphone"

val EFFECT_DEFAULT: LedEffect = Flash(Color.RED, 0.5f, 1)

class MainActivity : Activity() {

    private lateinit var statusImage: ImageView
    private lateinit var currentEffect: LedEffect

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

        val effectName = prefs.getString(EFFECT_NAME_KEY, null)
        val effectJson = prefs.getString(EFFECT_JSON_KEY, null)

        currentEffect = if (effectName != null && effectJson != null) {
            LedEffect.fromJsonString(effectName, effectJson)
        } else {
            EFFECT_DEFAULT
        }

        val threshold = findViewById<EditText>(R.id.threshold)
        threshold.setText(prefs.getInt(THRESHOLD_SETTING_KEY, THRESHOLD_DEFAULT).toString())

        val uri = findViewById<EditText>(R.id.uri)
        uri.setText(prefs.getString(SERVER_URI_KEY, SERVER_URI_DEFAULT))

        val effectView = findViewById<TextView>(R.id.effect)
        effectView.text = currentEffect.name
        effectView.setOnClickListener {
            val dialog = UiDialog(
                object : Callback {
                    override fun configurationResult(effect: LedEffect) {
                        android.util.Log.i(TAG, "Setting effect to: $effect")
                        currentEffect = effect
                        effectView.text = effect.name
                    }
                },
                this,
                currentEffect
            )
            dialog.create()
            dialog.show()
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.edit()
                .putInt(THRESHOLD_SETTING_KEY, Integer.parseInt(threshold.text.toString()))
                .putString(SERVER_URI_KEY, uri.text.toString())
                .putString(EFFECT_JSON_KEY, currentEffect.toJsonString())
                .putString(EFFECT_NAME_KEY, currentEffect.name)
                .apply()
            Log.d(TAG, "Saving: threshold=${threshold.text}, effect=$currentEffect")
            Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show()
        }

        statusImage = findViewById(R.id.status)
        statusImage.setOnClickListener {
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

        getAmplitudeWhile(condition, 500, applicationContext) {
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
        statusImage.setImageResource(
            if (isRecording) {
                R.drawable.ic_mic_off_black_24dp
            } else {
                R.drawable.ic_mic_black_24dp
            }
        )
        statusImage.tag = isRecording
    }
}
