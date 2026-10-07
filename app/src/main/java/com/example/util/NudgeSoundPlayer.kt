package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object NudgeSoundPlayer {

    private const val TAG = "NudgeSoundPlayer"
    private var playJob: Job? = null
    private var isPlaying = false

    /**
     * Synthesizes a crisp, brutalist sonar ping / typewriter chime buffer in PCM 16-bit mono at 44.1kHz.
     */
    private val pingPcmData: ByteArray by lazy {
        val sampleRate = 44100
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ByteArray(numSamples * 2)

        val freq1 = 1350.0 // Fundamental sharp ping frequency
        val freq2 = 2700.0 // High overtone
        val decayRate = 12.0 // Exponential decay

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-decayRate * t)
            // Mixed waveform: dominant ping with crisp metallic overtone
            val sampleVal = (sin(2 * PI * freq1 * t) * 0.75 + sin(2 * PI * freq2 * t) * 0.25) * envelope
            val sampleShort = (sampleVal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            val byteIndex = i * 2
            buffer[byteIndex] = (sampleShort.toInt() and 0xFF).toByte()
            buffer[byteIndex + 1] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()
        }
        buffer
    }

    /**
     * Plays the custom ping sound repeatedly until stop() is called, with brutalist haptic vibration.
     */
    fun startAlarmSound(context: Context, scope: CoroutineScope) {
        stopAlarmSound(context)
        isPlaying = true

        playJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackSize = maxOf(minBufSize, pingPcmData.size)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            var audioTrack: AudioTrack? = null
            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(trackSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pingPcmData, 0, pingPcmData.size)

                while (isActive && isPlaying) {
                    vibrateCrisp(context)
                    try {
                        audioTrack.setPlaybackHeadPosition(0)
                        audioTrack.play()
                    } catch (e: Exception) {
                        Log.e(TAG, "AudioTrack play error", e)
                    }

                    // Interval between pings: 1.8 seconds
                    delay(1800)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio loop", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Stops the alarm sound and vibration immediately.
     */
    fun stopAlarmSound(context: Context) {
        isPlaying = false
        playJob?.cancel()
        playJob = null
        stopVibration(context)
    }

    private fun vibrateCrisp(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Short crisp double pulse
                val timings = longArrayOf(0, 120, 80, 120)
                val amplitudes = intArrayOf(0, 255, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 120, 80, 120), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error", e)
        }
    }

    private fun stopVibration(context: Context) {
        try {
            getVibrator(context)?.cancel()
        } catch (ignored: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
