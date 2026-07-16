package com.lzb.player.api

import android.graphics.Bitmap

/**
 * Screenshot capability (Public API, V1.4).
 *
 * Captures the current video frame as a [Bitmap].
 * Install via [PlayerController.install]. Host owns Bitmap lifecycle (recycle when done).
 */
interface ScreenshotCapability : PlayerCapability {

    /**
     * Capture the current video frame.
     *
     * @return frame bitmap, or null if surface is unavailable / copy failed
     */
    suspend fun capture(): Bitmap?

    companion object {
        const val ID: String = "screenshot"
    }
}
