package de.j4velin.babyphone

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import de.j4velin.ledclient.lib.LedController
import de.j4velin.ledclient.lib.LedEffect

private const val NOTIFICATION_CHANNEL = "notification"

class RecorderService : Service() {

    companion object {
        var isRecording = false
    }

    private var ledController: LedController? = null
    private var recorder: Stoppable? = null
    private var threshold = THRESHOLD_DEFAULT
    private var ledEffect: LedEffect = EFFECT_DEFAULT // TODO make configurable

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            getString(R.string.recording_active),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.notification_channel_description)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )

        // update settings
        val settings = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        threshold = settings.getInt(THRESHOLD_SETTING_KEY, THRESHOLD_DEFAULT)
        val serverUri = settings.getString(SERVER_URI_KEY, SERVER_URI_DEFAULT)
        if (serverUri == null) {
            Toast.makeText(this, R.string.error_no_server, Toast.LENGTH_LONG).show()
            stopSelf()
        } else {
            ledController = LedController(if (serverUri.startsWith("http://")) serverUri else "http://$serverUri")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (recorder == null) {
            isRecording = true
            recorder = getAmplitude(1000, this::amplitudeUpdate)
            val pendingIntent: PendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }

            val notification: Notification = Notification.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_mic_black_24dp)
                .setContentIntent(pendingIntent)
                .build()
            startForeground(1, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRecording = false
        recorder?.stop()
    }

    private fun amplitudeUpdate(value: Int) {
        if (value > threshold) {
            Log.i(TAG, "Amplitude value $value > $threshold -> sending LED alarm")
            ledController?.trigger(ledEffect)
        }
    }
}