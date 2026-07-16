package com.lzb.player.media3

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.lzb.player.api.CacheCapability
import com.lzb.player.api.CacheConfig
import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerController
import com.lzb.player.core.engine.PlayerEngine
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Media3 缓存能力实现（V1.1）。
 *
 * - 边播边缓存：attach 后通过 [PlayerEngine.setCacheSession] 注入主播放器
 * - 预加载：使用 [CacheWriter] 在后台写入（不占用主播放窗口）
 * - 仅对 [MediaSource.Url] 生效
 *
 * @param engine 需为可识别 [Media3CacheSession] 的引擎（通常是 [Media3Engine]）
 */
@OptIn(UnstableApi::class)
class Media3CacheCapability(
    context: Context,
    private val engine: PlayerEngine,
    private val config: CacheConfig = CacheConfig(),
) : CacheCapability {

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.cacheDir, config.directoryName)
    private val databaseProvider = StandaloneDatabaseProvider(appContext)
    private val preloadExecutor = Executors.newSingleThreadExecutor()

    private val maxBytesRef = AtomicReference(config.maxBytes)
    private var simpleCache: SimpleCache = createCache(maxBytesRef.get())
    private var attachedController: PlayerController? = null

    override val id: String = CacheCapability.ID

    override fun onAttach(controller: PlayerController) {
        attachedController = controller
        engine.setCacheSession(buildSession())
    }

    override fun onDetach() {
        attachedController = null
        engine.setCacheSession(null)
    }

    override fun setMaxBytes(maxBytes: Long) {
        require(maxBytes > 0L) { "maxBytes must be > 0" }
        maxBytesRef.set(maxBytes)
        // 重建 SimpleCache（先解绑会话）
        val wasAttached = attachedController != null
        if (wasAttached) {
            engine.setCacheSession(null)
        }
        releaseCache()
        simpleCache = createCache(maxBytes)
        if (wasAttached) {
            engine.setCacheSession(buildSession())
        }
    }

    override fun preload(source: MediaSource) {
        val url = (source as? MediaSource.Url)?.url
            ?: return
        val uri = Uri.parse(url)
        val sessionFactory = buildCacheDataSourceFactory()
        preloadExecutor.execute {
            try {
                val dataSource = sessionFactory.createDataSource() as CacheDataSource
                val dataSpec = DataSpec.Builder().setUri(uri).build()
                CacheWriter(dataSource, dataSpec, /* temporaryBuffer= */ null, /* listener= */ null)
                    .cache()
            } catch (_: Exception) {
                // V1.1：预加载失败静默；后续可通过 Listener/Event 扩展
            }
        }
    }

    override fun clear() {
        val keys = simpleCache.keys.toList()
        keys.forEach { key ->
            simpleCache.removeResource(key)
        }
    }

    override fun getCachedBytes(): Long = simpleCache.cacheSpace

    /** Sample / 宿主在不再需要缓存能力时可主动释放底层缓存。 */
    fun release() {
        onDetach()
        preloadExecutor.shutdownNow()
        releaseCache()
    }

    private fun buildSession(): Media3CacheSession {
        return Media3CacheSession(
            simpleCache = simpleCache,
            cacheDataSourceFactory = buildCacheDataSourceFactory(),
        )
    }

    private fun buildCacheDataSourceFactory(): CacheDataSource.Factory {
        val upstream = DefaultHttpDataSource.Factory()
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createCache(maxBytes: Long): SimpleCache {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(maxBytes),
            databaseProvider,
        )
    }

    private fun releaseCache() {
        try {
            simpleCache.release()
        } catch (_: Exception) {
            // ignore
        }
    }
}