package de.j4velin.babyphone

import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

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
 * @param action the action to invoke when a value was measured
 * @return a stoppable to stop the measurements
 */
fun getAmplitude(delayMs: Long, action: (Int) -> Unit): Stoppable {
    val keepRunning = AtomicBoolean(true)
    getAmplitudeWhile({ keepRunning.get() }, delayMs, action)
    return object : Stoppable {
        override fun stop() {
            keepRunning.set(false)
        }
    }
}

/**
 * Continuously gets the max amplitude between two measurements as long as the condition is fulfilled
 *
 * @param condition the amplitude is measured until this condition produces false
 * @param delayMs the delay between to measurements
 * @param action the action to invoke when a value was measured
 */
fun getAmplitudeWhile(condition: () -> Boolean, delayMs: Long, action: (Int) -> Unit) {
    val mRecorder = MediaRecorder()
    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
    mRecorder.setOutputFile("/dev/null")
    mRecorder.prepare()
    mRecorder.start()

    mRecorder.maxAmplitude // ignore first value

    Thread {
        while (condition.invoke()) {
            action.invoke(mRecorder.maxAmplitude)
            Thread.sleep(delayMs)
        }
    }.start()
}