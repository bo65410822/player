package com.lzb.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * 字幕能力插件契约（Public API，V1.2）。
 *
 * 外挂 SRT / WebVTT；通过 [PlayerController.install] 安装。
 * UI 展示由宿主完成，SDK 只提供当前 cue 与开关。
 */
interface SubtitleCapability : PlayerCapability {

    /** 是否启用字幕显示/匹配。 */
    val enabled: StateFlow<Boolean>

    /** 当前时间点应对应的字幕；无匹配时为 null。 */
    val currentCue: StateFlow<SubtitleCue?>

    /** 当前激活的字幕轨。 */
    val activeTrack: StateFlow<SubtitleTrack?>

    fun setEnabled(enabled: Boolean)

    /** 加载 SRT 文本并设为当前轨。 */
    fun loadSrt(
        content: String,
        trackId: String = "srt-default",
        label: String = "SRT",
        language: String? = null,
    )

    /** 加载 WebVTT 文本并设为当前轨。 */
    fun loadWebVtt(
        content: String,
        trackId: String = "vtt-default",
        label: String = "WebVTT",
        language: String? = null,
    )

    /** 清空字幕轨与当前 cue。 */
    fun clear()

    companion object {
        const val ID: String = "subtitle"
    }
}