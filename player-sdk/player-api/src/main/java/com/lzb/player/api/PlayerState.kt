package com.lzb.player.api

/**
 * 播放器统一状态机。
 *
 * 禁止用多个 Boolean（isPlaying / isLoading 等）表达状态，避免组合冲突。
 * 由 [PlayerController.state] 与 [PlayerListener.onStateChanged] 对外同步。
 */
enum class PlayerState {
    /** 空闲：未设置媒体或已 stop / release。 */
    Idle,

    /** 准备中：正在加载媒体资源。 */
    Preparing,

    /** 已准备：可以 play / seek。 */
    Prepared,

    /** 播放中。 */
    Playing,

    /** 已暂停。 */
    Paused,

    /** 缓冲中（播放过程中缺少数据）。 */
    Buffering,

    /** 播放完成。 */
    Completed,

    /** 出错；详情见 [PlayerListener.onError] / [PlayerEvent.Error]。 */
    Error,
}