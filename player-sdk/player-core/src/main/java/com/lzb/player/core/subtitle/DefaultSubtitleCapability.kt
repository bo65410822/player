package com.lzb.player.core.subtitle

import com.lzb.player.api.PlayerController
import com.lzb.player.api.PlayerListener
import com.lzb.player.api.SubtitleCapability
import com.lzb.player.api.SubtitleConfig
import com.lzb.player.api.SubtitleCue
import com.lzb.player.api.SubtitleTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * External subtitle capability: parse SRT/WebVTT and emit [currentCue] by playback progress.
 */
class DefaultSubtitleCapability(
    private val config: SubtitleConfig = SubtitleConfig(),
) : SubtitleCapability {

    override val id: String = SubtitleCapability.ID

    private val _enabled = MutableStateFlow(config.enabledByDefault)
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _currentCue = MutableStateFlow<SubtitleCue?>(null)
    override val currentCue: StateFlow<SubtitleCue?> = _currentCue.asStateFlow()

    private val _activeTrack = MutableStateFlow<SubtitleTrack?>(null)
    override val activeTrack: StateFlow<SubtitleTrack?> = _activeTrack.asStateFlow()

    private var controller: PlayerController? = null
    private var lastPositionMs: Long = 0L

    private val progressListener = object : PlayerListener {
        override fun onProgress(positionMs: Long, durationMs: Long) {
            lastPositionMs = positionMs
            refreshCue(positionMs)
        }
    }

    override fun onAttach(controller: PlayerController) {
        this.controller = controller
        controller.addListener(progressListener)
        refreshCue(lastPositionMs)
    }

    override fun onDetach() {
        controller?.removeListener(progressListener)
        controller = null
        _currentCue.value = null
    }

    override fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        if (!enabled) {
            _currentCue.value = null
        } else {
            refreshCue(lastPositionMs)
        }
    }

    override fun loadSrt(
        content: String,
        trackId: String,
        label: String,
        language: String?,
    ) {
        val cues = SubtitleParsers.parseSrt(content)
        setTrack(
            SubtitleTrack(
                id = trackId,
                label = label,
                language = language,
                cues = cues,
            )
        )
    }

    override fun loadWebVtt(
        content: String,
        trackId: String,
        label: String,
        language: String?,
    ) {
        val cues = SubtitleParsers.parseWebVtt(content)
        setTrack(
            SubtitleTrack(
                id = trackId,
                label = label,
                language = language,
                cues = cues,
            )
        )
    }

    override fun clear() {
        _activeTrack.value = null
        _currentCue.value = null
    }

    private fun setTrack(track: SubtitleTrack) {
        _activeTrack.value = track
        refreshCue(lastPositionMs)
    }

    private fun refreshCue(positionMs: Long) {
        if (!_enabled.value) {
            _currentCue.value = null
            return
        }
        val track = _activeTrack.value
        if (track == null) {
            _currentCue.value = null
            return
        }
        val cue = track.cues.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }
        _currentCue.value = cue
    }
}