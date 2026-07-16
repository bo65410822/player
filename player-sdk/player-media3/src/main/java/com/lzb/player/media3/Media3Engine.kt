package com.lzb.player.media3

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerError
import com.lzb.player.api.PlayerListener
import com.lzb.player.core.engine.EngineCacheSession
import com.lzb.player.core.engine.PlayerEngine

/**
 * Media3 (ExoPlayer) [PlayerEngine] implementation.
 *
 * V1.1: cache via [setCacheSession]
 * V1.3: [setPlaybackSpeed] / [setLooping] / [setVolume]
 */
@OptIn(UnstableApi::class)
class Media3Engine(
    context: Context,
) : PlayerEngine {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var callback: PlayerEngine.Callback? = null
    private var pendingSource: MediaSource? = null
    private var awaitingPrepare = false
    private var isBuffering = false
    private var released = false
    private var cacheSession: Media3CacheSession? = null
    private var boundPlayerView: PlayerView? = null

    private var playbackSpeed: Float = 1f
    private var looping: Boolean = false
    private var volume: Float = 1f

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    if (isBuffering) {
                        isBuffering = false
                        callback?.onBufferingEnd()
                    }
                    if (awaitingPrepare) {
                        awaitingPrepare = false
                        callback?.onPrepared()
                    }
                }
                Player.STATE_BUFFERING -> {
                    if (!awaitingPrepare && !isBuffering) {
                        isBuffering = true
                        callback?.onBufferingStart()
                    }
                }
                Player.STATE_ENDED -> {
                    stopProgress()
                    callback?.onCompleted()
                }
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                callback?.onStarted()
                startProgress()
            } else {
                stopProgress()
                if (exoPlayer.playbackState != Player.STATE_ENDED &&
                    exoPlayer.playbackState != Player.STATE_IDLE &&
                    !exoPlayer.playWhenReady
                ) {
                    callback?.onPaused()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            awaitingPrepare = false
            stopProgress()
            callback?.onError(
                PlayerError(
                    message = error.message ?: "Media3 playback error",
                    code = error.errorCode,
                    cause = error,
                )
            )
        }
    }

    private var exoPlayer: ExoPlayer = buildPlayer(cacheSession = null)

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (released) return
            val player = exoPlayer
            if (player.isPlaying) {
                val duration = player.duration
                val durationMs = if (duration < 0) PlayerListener.DURATION_UNKNOWN else duration
                callback?.onProgress(player.currentPosition, durationMs)
                mainHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
            }
        }
    }

    override fun setCallback(callback: PlayerEngine.Callback?) {
        this.callback = callback
    }

    override fun setMediaSource(source: MediaSource) {
        ensureNotReleased()
        pendingSource = source
    }

    override fun setVideoSurface(surface: Surface?) {
        ensureNotReleased()
        runOnMain {
            exoPlayer.setVideoSurface(surface)
        }
    }

    override fun setCacheSession(session: EngineCacheSession?) {
        ensureNotReleased()
        runOnMain {
            cacheSession = session as? Media3CacheSession
            rebuildPlayerKeepBinding()
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        ensureNotReleased()
        playbackSpeed = speed
        runOnMain {
            exoPlayer.setPlaybackSpeed(speed)
        }
    }

    override fun setLooping(looping: Boolean) {
        ensureNotReleased()
        this.looping = looping
        runOnMain {
            applyLooping(exoPlayer, looping)
        }
    }

    override fun setVolume(volume: Float) {
        ensureNotReleased()
        this.volume = volume.coerceIn(0f, 1f)
        runOnMain {
            exoPlayer.volume = this.volume
        }
    }

    fun bindPlayerView(playerView: PlayerView) {
        ensureNotReleased()
        runOnMain {
            boundPlayerView = playerView
            playerView.player = exoPlayer
        }
    }

    fun unbindPlayerView(playerView: PlayerView) {
        runOnMain {
            if (playerView.player === exoPlayer) {
                playerView.player = null
            }
            if (boundPlayerView === playerView) {
                boundPlayerView = null
            }
        }
    }

    /** Bound [PlayerView] for screenshot (V1.4); null if not attached. */
    fun boundPlayerViewOrNull(): PlayerView? = boundPlayerView

    override fun prepare() {
        ensureNotReleased()
        val source = pendingSource
        if (source == null) {
            callback?.onError(PlayerError(message = "No media source set on engine"))
            return
        }
        runOnMain {
            awaitingPrepare = true
            isBuffering = false
            val mediaItem = MediaSourceMapper.toMediaItem(source)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    override fun play() {
        ensureNotReleased()
        runOnMain {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    override fun pause() {
        ensureNotReleased()
        runOnMain {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    override fun stop() {
        ensureNotReleased()
        runOnMain {
            stopProgress()
            awaitingPrepare = false
            isBuffering = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            callback?.onStopped()
        }
    }

    override fun seekTo(positionMs: Long) {
        ensureNotReleased()
        runOnMain {
            exoPlayer.seekTo(positionMs)
            callback?.onSeekCompleted(positionMs)
            val duration = exoPlayer.duration
            val durationMs = if (duration < 0) PlayerListener.DURATION_UNKNOWN else duration
            callback?.onProgress(positionMs, durationMs)
        }
    }

    override fun release() {
        if (released) return
        released = true
        runOnMain {
            stopProgress()
            boundPlayerView?.player = null
            boundPlayerView = null
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
            cacheSession = null
            callback = null
            pendingSource = null
        }
    }

    private fun rebuildPlayerKeepBinding() {
        stopProgress()
        awaitingPrepare = false
        isBuffering = false
        val view = boundPlayerView
        view?.player = null
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        exoPlayer = buildPlayer(cacheSession)
        view?.player = exoPlayer
    }

    private fun buildPlayer(cacheSession: Media3CacheSession?): ExoPlayer {
        val builder = ExoPlayer.Builder(appContext)
        if (cacheSession != null) {
            builder.setMediaSourceFactory(
                DefaultMediaSourceFactory(appContext)
                    .setDataSourceFactory(cacheSession.cacheDataSourceFactory)
            )
        }
        return builder.build().also { player ->
            player.addListener(playerListener)
            player.setPlaybackSpeed(playbackSpeed)
            applyLooping(player, looping)
            player.volume = volume
        }
    }

    private fun applyLooping(player: ExoPlayer, looping: Boolean) {
        player.repeatMode = if (looping) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    private fun startProgress() {
        stopProgress()
        mainHandler.post(progressRunnable)
    }

    private fun stopProgress() {
        mainHandler.removeCallbacks(progressRunnable)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun ensureNotReleased() {
        check(!released) { "Media3Engine already released" }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
    }
}