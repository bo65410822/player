package com.lzb.player.core.engine

import android.view.Surface
import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerError

/**
 * Playback engine abstraction.
 *
 * Core schedules this interface only; not part of Public API.
 */
interface PlayerEngine {

    fun setCallback(callback: Callback?)

    fun setMediaSource(source: MediaSource)

    fun setVideoSurface(surface: Surface?)

    fun prepare()

    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun release()

    /**
     * Inject / clear cache session (V1.1). Pass null to disable engine-side cache.
     */
    fun setCacheSession(session: EngineCacheSession?)

    /**
     * Playback rate (V1.3). Typical range 0.25f..4f.
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Single-item loop (V1.3). Driven by [com.lzb.player.api.PlayerConfig.loop].
     */
    fun setLooping(looping: Boolean)

    /**
     * Optional volume [0f, 1f].
     */
    fun setVolume(volume: Float)

    interface Callback {
        fun onPrepared()
        fun onStarted()
        fun onPaused()
        fun onStopped()
        fun onCompleted()
        fun onBufferingStart()
        fun onBufferingEnd()
        fun onSeekCompleted(positionMs: Long)
        fun onProgress(positionMs: Long, durationMs: Long)
        fun onError(error: PlayerError)
    }
}