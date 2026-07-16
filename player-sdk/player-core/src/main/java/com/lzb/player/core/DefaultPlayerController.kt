package com.lzb.player.core

import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerCapability
import com.lzb.player.api.PlayerConfig
import com.lzb.player.api.PlayerController
import com.lzb.player.api.PlayerError
import com.lzb.player.api.PlayerEvent
import com.lzb.player.api.PlayerListener
import com.lzb.player.api.PlayerState
import com.lzb.player.core.capability.CapabilityRegistry
import com.lzb.player.core.engine.FakePlayerEngine
import com.lzb.player.core.engine.PlayerEngine
import com.lzb.player.core.state.PlayerStateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet

/**
 * [PlayerController] 默认实现：生命周期 + 状态机 + Engine 调度 + Capability 管理。
 *
 * 职责边界：
 * - 本类：对外 API、状态迁移、Listener 分发
 * - [PlayerEngine]：实际媒体 I/O（当前为 [FakePlayerEngine]）
 * - [PlayerCapability]：可选插件，不进入本类方法列表
 */
class DefaultPlayerController(
    private val engine: PlayerEngine = FakePlayerEngine(),
    initialConfig: PlayerConfig = PlayerConfig(),
) : PlayerController {

    private val stateMachine = PlayerStateMachine()
    private val capabilityRegistry = CapabilityRegistry()
    private val listeners = CopyOnWriteArraySet<PlayerListener>()

    private val _state = MutableStateFlow(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _currentSource = MutableStateFlow<MediaSource?>(null)
    override val currentSource: StateFlow<MediaSource?> = _currentSource.asStateFlow()

    @Volatile
    private var _config: PlayerConfig = initialConfig
    override val config: PlayerConfig
        get() = _config

    @Volatile
    private var released = false

    private val engineCallback = object : PlayerEngine.Callback {
        override fun onPrepared() {
            if (!moveTo(PlayerState.Prepared)) return
            dispatchEvent(PlayerEvent.Prepared)
            if (_config.autoPlay) {
                play()
            }
        }

        override fun onStarted() {
            if (!moveTo(PlayerState.Playing)) return
            dispatchEvent(PlayerEvent.Started)
        }

        override fun onPaused() {
            if (!moveTo(PlayerState.Paused)) return
            dispatchEvent(PlayerEvent.Paused)
        }

        override fun onStopped() {
            moveToIdle(PlayerEvent.Stopped)
        }

        override fun onCompleted() {
            if (!moveTo(PlayerState.Completed)) return
            dispatchEvent(PlayerEvent.Completed)
        }

        override fun onBufferingStart() {
            if (!moveTo(PlayerState.Buffering)) return
            dispatchEvent(PlayerEvent.BufferingStart)
        }

        override fun onBufferingEnd() {
            // 缓冲结束通常回到 Playing；若当前已暂停则保持 Paused
            val target = if (stateMachine.current == PlayerState.Paused) {
                PlayerState.Paused
            } else {
                PlayerState.Playing
            }
            if (!moveTo(target)) return
            dispatchEvent(PlayerEvent.BufferingEnd)
        }

        override fun onSeekCompleted(positionMs: Long) {
            dispatchEvent(PlayerEvent.SeekCompleted(positionMs))
        }

        override fun onProgress(positionMs: Long, durationMs: Long) {
            listeners.forEach { it.onProgress(positionMs, durationMs) }
        }

        override fun onError(error: PlayerError) {
            dispatchError(error)
        }
    }

    init {
        engine.setCallback(engineCallback)
        applyConfigToEngine(_config)
    }

    override fun setConfig(config: PlayerConfig) {
        ensureNotReleased()
        _config = config
        applyConfigToEngine(config)
    }

    private fun applyConfigToEngine(config: PlayerConfig) {
        engine.setLooping(config.loop)
        engine.setVolume(config.volume.coerceIn(0f, 1f))
    }

    override fun setMediaSource(source: MediaSource) {
        ensureNotReleased()
        _currentSource.value = source
        engine.setMediaSource(source)
    }

    override fun prepare() {
        ensureNotReleased()
        if (_currentSource.value == null) {
            dispatchError(PlayerError(message = "No media source set"))
            return
        }
        // 允许从 Idle / Error / Completed / Prepared / Paused 重新 prepare
        val from = stateMachine.current
        if (from != PlayerState.Idle &&
            from != PlayerState.Error &&
            from != PlayerState.Completed &&
            from != PlayerState.Prepared &&
            from != PlayerState.Paused
        ) {
            if (from == PlayerState.Preparing) return
            dispatchError(PlayerError(message = "Cannot prepare from state: $from"))
            return
        }
        if (!moveTo(PlayerState.Preparing)) {
            dispatchError(PlayerError(message = "Illegal state transition to Preparing"))
            return
        }
        engine.prepare()
    }

    override fun play() {
        ensureNotReleased()
        when (stateMachine.current) {
            PlayerState.Prepared, PlayerState.Paused, PlayerState.Completed -> {
                engine.play()
            }
            PlayerState.Playing, PlayerState.Buffering -> {
                // 已在播：忽略
            }
            PlayerState.Preparing -> {
                // 准备中：忽略，等 onPrepared + autoPlay 或再次 play
            }
            else -> {
                dispatchError(PlayerError(message = "Call prepare() before play()"))
            }
        }
    }

    override fun pause() {
        ensureNotReleased()
        when (stateMachine.current) {
            PlayerState.Playing, PlayerState.Buffering -> engine.pause()
            else -> Unit
        }
    }

    override fun stop() {
        ensureNotReleased()
        engine.stop()
    }

    override fun seekTo(positionMs: Long) {
        ensureNotReleased()
        if (_currentSource.value == null) {
            dispatchError(PlayerError(message = "No media source set"))
            return
        }
        when (stateMachine.current) {
            PlayerState.Idle, PlayerState.Preparing, PlayerState.Error -> {
                dispatchError(PlayerError(message = "Cannot seek in state: ${stateMachine.current}"))
            }
            else -> engine.seekTo(positionMs)
        }
    }

    override fun release() {
        if (released) return
        released = true
        capabilityRegistry.detachAll()
        engine.setCallback(null)
        engine.release()
        _currentSource.value = null
        moveToIdle(event = null)
        listeners.clear()
    }

    override fun addListener(listener: PlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    override fun install(capability: PlayerCapability) {
        ensureNotReleased()
        capabilityRegistry.install(this, capability)
    }

    override fun uninstall(capabilityId: String) {
        ensureNotReleased()
        capabilityRegistry.uninstall(capabilityId)
    }

    private fun moveTo(target: PlayerState): Boolean {
        if (!stateMachine.tryTransition(target)) return false
        if (_state.value == target) return true
        _state.value = target
        listeners.forEach { it.onStateChanged(target) }
        return true
    }

    private fun moveToIdle(event: PlayerEvent?) {
        stateMachine.forceIdle()
        if (_state.value != PlayerState.Idle) {
            _state.value = PlayerState.Idle
            listeners.forEach { it.onStateChanged(PlayerState.Idle) }
        }
        if (event != null) {
            dispatchEvent(event)
        }
    }

    private fun dispatchEvent(event: PlayerEvent) {
        listeners.forEach { it.onEvent(event) }
    }

    private fun dispatchError(error: PlayerError) {
        stateMachine.tryTransition(PlayerState.Error)
        if (_state.value != PlayerState.Error) {
            _state.value = PlayerState.Error
            listeners.forEach { it.onStateChanged(PlayerState.Error) }
        }
        listeners.forEach { it.onError(error) }
        dispatchEvent(PlayerEvent.Error(error))
    }

    private fun ensureNotReleased() {
        check(!released) { "PlayerController already released" }
    }
}

/**
 * Core 层工厂：创建默认控制器。
 *
 * 不属于 Public API；可注入自定义 [PlayerEngine]（如未来的 Media3Engine）。
 */
class DefaultPlayerFactory {
    fun create(
        config: PlayerConfig = PlayerConfig(),
        engine: PlayerEngine = FakePlayerEngine(),
    ): PlayerController {
        return DefaultPlayerController(engine = engine, initialConfig = config)
    }
}