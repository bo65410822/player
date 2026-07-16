package com.lzb.player.core.speed

import com.lzb.player.api.AbRepeatRange
import com.lzb.player.api.PlayerController
import com.lzb.player.api.PlayerListener
import com.lzb.player.api.SpeedCapability
import com.lzb.player.api.SpeedConfig
import com.lzb.player.core.engine.PlayerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Speed + AB repeat capability.
 *
 * Speed / loop go through [PlayerEngine]; AB sync uses [PlayerListener.onProgress].
 */
class DefaultSpeedCapability(
    private val engine: PlayerEngine,
    private val config: SpeedConfig = SpeedConfig(),
) : SpeedCapability {

    override val id: String = SpeedCapability.ID

    private val _speed = MutableStateFlow(config.defaultSpeed.coerceIn(config.minSpeed, config.maxSpeed))
    override val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _abRepeat = MutableStateFlow<AbRepeatRange?>(null)
    override val abRepeat: StateFlow<AbRepeatRange?> = _abRepeat.asStateFlow()

    private var controller: PlayerController? = null
    private var seekingForAb = false

    private val progressListener = object : PlayerListener {
        override fun onProgress(positionMs: Long, durationMs: Long) {
            val range = _abRepeat.value ?: return
            // Keep AB priority if host toggles PlayerConfig.loop while AB is active.
            engine.setLooping(false)
            if (seekingForAb) {
                if (positionMs < range.endMs) {
                    seekingForAb = false
                }
                return
            }
            if (positionMs >= range.endMs) {
                seekingForAb = true
                controller?.seekTo(range.startMs)
            }
        }
    }

    override fun onAttach(controller: PlayerController) {
        this.controller = controller
        controller.addListener(progressListener)
        engine.setPlaybackSpeed(_speed.value)
        if (_abRepeat.value != null) {
            engine.setLooping(false)
        }
    }

    override fun onDetach() {
        controller?.removeListener(progressListener)
        controller = null
        seekingForAb = false
        _abRepeat.value = null
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(config.minSpeed, config.maxSpeed)
        _speed.value = clamped
        engine.setPlaybackSpeed(clamped)
    }

    override fun setAbRepeat(startMs: Long, endMs: Long) {
        require(startMs >= 0L) { "startMs must be >= 0" }
        require(endMs > startMs) { "endMs must be > startMs" }
        _abRepeat.value = AbRepeatRange(startMs = startMs, endMs = endMs)
        seekingForAb = false
        // AB takes priority over full-track loop.
        engine.setLooping(false)
    }

    override fun clearAbRepeat() {
        _abRepeat.value = null
        seekingForAb = false
        val loop = controller?.config?.loop == true
        engine.setLooping(loop)
    }
}