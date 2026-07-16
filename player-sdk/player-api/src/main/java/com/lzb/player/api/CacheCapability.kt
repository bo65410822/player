package com.lzb.player.api

/**
 * 缓存能力插件契约（Public API）。
 *
 * 通过 [PlayerController.install] 安装；不得向 Controller 增加 cacheXxx 业务方法。
 * 具体实现位于 media3 模块（如 Media3CacheCapability）。
 */
interface CacheCapability : PlayerCapability {

    /** 更新磁盘上限；是否立即重建缓存由实现定义。 */
    fun setMaxBytes(maxBytes: Long)

    /**
     * 预加载媒体到磁盘缓存（不占用主播放器窗口）。
     *
     * V1.1 建议仅对 [MediaSource.Url] 生效；其他类型可忽略或回调错误。
     */
    fun preload(source: MediaSource)

    /** 清空全部缓存内容。 */
    fun clear()

    /** 当前已缓存字节数。 */
    fun getCachedBytes(): Long

    companion object {
        /** 固定能力 id，重复安装会替换旧实例。 */
        const val ID: String = "cache"
    }
}