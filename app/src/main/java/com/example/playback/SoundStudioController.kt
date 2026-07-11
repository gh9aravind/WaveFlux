package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

/**
 * A lightweight "Sound Studio" style bass/treble control.
 *
 * Note: getting the exact ExoPlayer audio session id through MediaController
 * requires extra session-id plumbing between the UI process and
 * PlaybackService. To keep this simple and reliable, the effects here attach
 * to the global output mix (audio session 0), which affects all audio in the
 * app - this works well for a single-player music app like this one.
 */
object SoundStudioController {

    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var initialized = false

    private const val GLOBAL_SESSION = 0

    fun ensureInitialized() {
        if (initialized) return
        try {
            bassBoost = BassBoost(0, GLOBAL_SESSION).apply { enabled = true }
        } catch (e: Exception) {
            bassBoost = null
        }
        try {
            equalizer = Equalizer(0, GLOBAL_SESSION).apply { enabled = true }
        } catch (e: Exception) {
            equalizer = null
        }
        initialized = true
    }

    /** 0-100 */
    fun getBass(): Int {
        ensureInitialized()
        return try {
            ((bassBoost?.roundedStrength ?: 0).toInt() * 100) / 1000
        } catch (e: Exception) {
            0
        }
    }

    /** value 0-100 */
    fun setBass(value: Int) {
        ensureInitialized()
        try {
            val strength = (value.coerceIn(0, 100) * 1000 / 100).toShort()
            bassBoost?.setStrength(strength)
        } catch (e: Exception) {
            // Device doesn't support BassBoost
        }
    }

    /** 0-100, 50 = neutral/no boost */
    fun getTreble(): Int {
        ensureInitialized()
        val eq = equalizer ?: return 50
        return try {
            val band = (eq.numberOfBands - 1).toShort()
            val level = eq.getBandLevel(band)
            val range = eq.bandLevelRange
            val min = range[0]
            val max = range[1]
            if (max == min) 50 else (((level - min).toFloat() / (max - min).toFloat()) * 100).toInt()
        } catch (e: Exception) {
            50
        }
    }

    /** value 0-100, 50 = neutral */
    fun setTreble(value: Int) {
        ensureInitialized()
        val eq = equalizer ?: return
        try {
            val band = (eq.numberOfBands - 1).toShort()
            val range = eq.bandLevelRange
            val min = range[0]
            val max = range[1]
            val level = (min + ((value.coerceIn(0, 100) / 100f) * (max - min))).toInt().toShort()
            eq.setBandLevel(band, level)
        } catch (e: Exception) {
            // Device doesn't support Equalizer
        }
    }

    fun reset() {
        setBass(0)
        setTreble(50)
    }
}
