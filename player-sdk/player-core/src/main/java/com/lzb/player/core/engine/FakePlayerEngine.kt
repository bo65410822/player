package com.lzb.player.core.engine

import android.view.Surface
import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerError
import com.lzb.player.api.PlayerListener

/**
 * Fake engine for wiring tests; speed/loop/volume are no-ops.
 */
class FakePlayerEngine : PlayerEngine {

    private var callback: PlayerEngine.Callback? = null
    private var source: MediaSource? = null
    private var released = false

    override fun setCallback(callback: PlayerEngine.Callback?) {
        this.callback = callback
    }

    override fun setMediaSource(source: MediaSource) {
        ensureNotReleased()
        this.source = source
    }

    override fun setVideoSurface(surface: Surface?) = Unit

    override fun setCacheSession(session: EngineCacheSession?) = Unit

    override fun setPlaybackSpeed(speed: Float) = Unit

    override fun setLooping(looping: Boolean) = Unit

    override fun setVolume(volume: Float) = Unit

    override fun prepare() {
        ensureNotReleased()
        if (source == null) {
            callback?.onError(PlayerError(message = "No media source set on engine"))
            return
        }
        callback?.onPrepared()
    }

    override fun play() {
        ensureNotReleased()
        callback?.onStarted()
    }

    override fun pause() {
        ensureNotReleased()
        callback?.onPaused()
    }

    override fun stop() {
        ensureNotReleased()
        callback?.onStopped()
    }

    override fun seekTo(positionMs: Long) {
        ensureNotReleased()
        callback?.onSeekCompleted(positionMs)
        callback?.onProgress(positionMs, PlayerListener.DURATION_UNKNOWN)
    }

    override fun release() {
        released = true
        callback = null
        source = null
    }

    private fun ensureNotReleased() {
        check(!released) { "FakePlayerEngine already released" }
    }
}