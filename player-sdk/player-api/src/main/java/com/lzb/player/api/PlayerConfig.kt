package com.lzb.player.api

/**
 * 播放器配置。
 *
 * V1.0 仅覆盖基础项；倍速 / 循环增强等后续由 Capability 扩展，避免污染核心配置。
 *
 * @property autoPlay prepare 成功后是否自动开始播放
 * @property loop 是否循环播放
 * @property volume 音量，范围建议 [0f, 1f]
 */
data class PlayerConfig(
    val autoPlay: Boolean = false,
    val loop: Boolean = false,
    val volume: Float = 1f,
)