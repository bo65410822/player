package com.lzb.player.media3

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.SimpleCache
import com.lzb.player.core.engine.EngineCacheSession

/**
 * Media3 缓存会话：持有 [SimpleCache] 与可注入播放器的 DataSource.Factory。
 */
@OptIn(UnstableApi::class)
class Media3CacheSession(
    val simpleCache: SimpleCache,
    val cacheDataSourceFactory: DataSource.Factory,
) : EngineCacheSession