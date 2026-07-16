package com.lzb.player.api

/**
 * 媒体资源描述。
 *
 * V1.0 支持本地文件、网络 URL、Asset；具体解析由 Engine 实现。
 */
sealed class MediaSource {
    /** 可选展示标题；不影响播放链路。 */
    abstract val title: String?

    /**
     * 网络媒体（HTTP / HTTPS MP4，或 HLS `.m3u8` 等；具体协议由 Engine 支持）。
     *
     * @property url 可访问的媒体地址
     */
    data class Url(
        val url: String,
        override val title: String? = null,
    ) : MediaSource()

    /**
     * 本地文件。
     *
     * @property path 绝对路径或应用可访问路径
     */
    data class LocalFile(
        val path: String,
        override val title: String? = null,
    ) : MediaSource()

    /**
     * APK Asset 内媒体。
     *
     * @property assetPath assets 相对路径，例如 `media/demo.mp4`
     */
    data class Asset(
        val assetPath: String,
        override val title: String? = null,
    ) : MediaSource()
}