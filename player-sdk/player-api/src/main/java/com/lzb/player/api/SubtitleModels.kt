package com.lzb.player.api

/**
 * 单条字幕。
 *
 * @property startMs 开始时间（毫秒，含）
 * @property endMs 结束时间（毫秒，不含）
 * @property text 展示文本（可含换行）
 */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * 字幕轨。
 *
 * @property id 轨唯一 id
 * @property label 展示名称
 * @property language 可选语言标签，如 zh / en
 * @property cues 按时间排序的字幕条目
 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String? = null,
    val cues: List<SubtitleCue>,
)

/**
 * 字幕能力配置。
 *
 * @property enabledByDefault 安装后是否默认开启显示
 */
data class SubtitleConfig(
    val enabledByDefault: Boolean = true,
)