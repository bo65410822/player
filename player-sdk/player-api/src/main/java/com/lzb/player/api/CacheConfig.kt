package com.lzb.player.api

/**
 * 缓存能力配置。
 *
 * @property maxBytes 磁盘缓存上限（字节），超出后按 LRU 淘汰
 * @property directoryName 位于应用缓存目录下的子目录名
 */
data class CacheConfig(
    val maxBytes: Long = DEFAULT_MAX_BYTES,
    val directoryName: String = DEFAULT_DIR_NAME,
) {
    companion object {
        const val DEFAULT_MAX_BYTES: Long = 100L * 1024L * 1024L
        const val DEFAULT_DIR_NAME: String = "player_media_cache"
    }
}