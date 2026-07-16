package com.lzb.player.api

/**
 * 播放器事件监听。
 *
 * 默认方法均为空实现，宿主可按需覆写。
 * 也可同时观察 [PlayerController.state]（StateFlow）做 Compose 绑定。
 */
interface PlayerListener {
    /** 状态变化（与 [PlayerController.state] 同步）。 */
    fun onStateChanged(state: PlayerState) = Unit

    /** 一次性事件（Prepared / SeekCompleted 等）。 */
    fun onEvent(event: PlayerEvent) = Unit

    /**
     * 播放进度。
     *
     * @param positionMs 当前进度（毫秒）
     * @param durationMs 总时长（毫秒）；未知时可为 [DURATION_UNKNOWN]
     */
    fun onProgress(positionMs: Long, durationMs: Long) = Unit

    /** 错误回调；[PlayerEvent.Error] 也会同步发出。 */
    fun onError(error: PlayerError) = Unit

    companion object {
        /** 时长未知时的占位值。 */
        const val DURATION_UNKNOWN: Long = -1L
    }
}