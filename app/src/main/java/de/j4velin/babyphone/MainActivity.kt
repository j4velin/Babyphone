package de.j4velin.babyphone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.widget.*

const val THRESHOLD_DEFAULT = 5000
const val THRESHOLD_SETTING_KEY = "threshold"

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val threshold = findViewById<EditText>(R.id.threshold)
        threshold.setText(prefs.getInt(THRESHOLD_SETTING_KEY, THRESHOLD_DEFAULT).toString())

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.edit().putInt(THRESHOLD_SETTING_KEY, Integer.parseInt(threshold.text.toString()))
                .apply()
        }

        findViewById<ImageView>(R.id.status).setOnClickListener {
            startService(Intent(applicationContext, RecorderService::class.java))
            // TODO: stop
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            Toast.makeText(this, "Permission is required!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startRecording() {
        val mRecorder = MediaRecorder()
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mRecorder.setOutputFile("/dev/null")
        mRecorder.prepare()
        mRecorder.start()

        val current = findViewById<TextView>(R.id.current)

        val handler = Handler()
        Thread {
            while (!isDestroyed && !isFinishing) {
                handler.post {
                    current.text = "(current: " + mRecorder.maxAmplitude + ")"
                }
                Thread.sleep(500)
            }
        }.start()
    }
}
