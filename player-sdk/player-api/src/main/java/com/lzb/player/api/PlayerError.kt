package com.lzb.player.api

/**
 * 播放错误信息。
 *
 * @property message 可读错误描述
 * @property code 可选错误码，由具体 Engine 约定
 * @property cause 底层异常，便于日志排查
 */
data class PlayerError(
    val message: String,
    val code: Int = 0,
    val cause: Throwable? = null,
)