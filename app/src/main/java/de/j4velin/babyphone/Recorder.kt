package de.j4velin.babyphone

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * An object, which can be stopped
 */
interface Stoppable {
    /**
     * Stops this Stoppable
     */
    fun stop()
}

/**
 * Continuously gets the max amplitude between two measurements
 *
 * @param delayMs the delay between to measurements
 * @param context the Android context
 * @param action the action to invoke when a value was measured
 * @return a stoppable to stop the measurements
 */
fun getAmplitude(delayMs: Long, context: Context, action: (Int) -> Unit): Stoppable {
    val keepRunning = AtomicBoolean(true)
    getAmplitudeWhile({ keepRunning.get() }, delayMs, context, action)
    return object : Stoppable {
        override fun stop() {
            keepRunning.set(false)
        }
    }
}

private val recorder: MediaRecorder by lazy { MediaRecorder() }
private val listener = AtomicInteger(0)

/**
 * Continuously gets the max amplitude between two measurements as long as the condition is fulfilled
 *
 * @param condition the amplitude is measured until this condition produces false
 * @param delayMs the delay between to measurements
 * @param context the Android context
 * @param action the action to invoke when a value was measured
 */
fun getAmplitudeWhile(
    condition: () -> Boolean,
    delayMs: Long,
    context: Context,
    action: (Int) -> Unit
) {
    if (listener.getAndIncrement() == 0) {
        // first listener? then start recoding
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(File(context.externalCacheDir, "babyphone"))
        recorder.prepare()
        try {
            recorder.start()
        } catch (e: Exception) {
            Log.e(TAG, "Can not start recorder", e)
            listener.getAndDecrement()
            return
        }
        recorder.maxAmplitude // ignore first value
        Log.i(TAG, "recoding started")
    }

    Thread {
        while (condition.invoke()) {
            action.invoke(recorder.maxAmplitude)
            Thread.sleep(delayMs)
        }
        if (listener.decrementAndGet() == 0) {
            // no component needs amplitude updates any more -> stop recording
            recorder.stop()
            Log.i(TAG, "all listener expired -> recoding stopped")
        } else {
            Log.i(TAG, "listener expired but recording still active")
        }
    }.start()
}