package de.j4velin.babyphone

import android.app.Service
import android.content.Intent

class RecorderService : Service() {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO
        return START_STICKY
    }

}