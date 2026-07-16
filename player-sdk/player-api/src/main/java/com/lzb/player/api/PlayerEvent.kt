package com.lzb.player.api

/**
 * 播放器一次性事件。
 *
 * 与 [PlayerState] 互补：状态表达「现在怎样」，事件表达「刚刚发生了什么」。
 */
sealed class PlayerEvent {
    /** 媒体准备完成，可播放。 */
    data object Prepared : PlayerEvent()

    /** 开始播放。 */
    data object Started : PlayerEvent()

    /** 暂停。 */
    data object Paused : PlayerEvent()

    /** 停止。 */
    data object Stopped : PlayerEvent()

    /** 播放完成。 */
    data object Completed : PlayerEvent()

    /** 开始缓冲。 */
    data object BufferingStart : PlayerEvent()

    /** 缓冲结束。 */
    data object BufferingEnd : PlayerEvent()

    /** Seek 完成。 */
    data class SeekCompleted(val positionMs: Long) : PlayerEvent()

    /** 发生错误；同时会回调 [PlayerListener.onError]。 */
    data class Error(val error: PlayerError) : PlayerEvent()
}