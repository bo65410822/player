package com.lzb.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Playback speed + AB repeat capability (Public API, V1.3).
 *
 * Install via [PlayerController.install]. Loop remains on [PlayerConfig.loop].
 * When AB is active it takes priority over full-track loop (engine loop forced off).
 */
interface SpeedCapability : PlayerCapability {

    val speed: StateFlow<Float>

    val abRepeat: StateFlow<AbRepeatRange?>

    fun setSpeed(speed: Float)

    /**
     * Enable AB repeat. Requires startMs < endMs.
     * Disables engine looping until [clearAbRepeat].
     */
    fun setAbRepeat(startMs: Long, endMs: Long)

    fun clearAbRepeat()

    companion object {
        const val ID: String = "speed"
    }
}