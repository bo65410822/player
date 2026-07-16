package com.lzb.player.media3

import android.content.Context
import com.lzb.player.api.PlayerConfig
import com.lzb.player.api.PlayerController
import com.lzb.player.core.DefaultPlayerFactory

/**
 * Media3 组装结果：Controller（公开）+ Engine（Sample 绑定画面用）。
 */
data class Media3PlayerHandle(
    val controller: PlayerController,
    val engine: Media3Engine,
)

/**
 * Media3 播放器工厂（非 Public API）。
 *
 * Sample / 宿主通过本工厂注入 [Media3Engine]，替换 Core 默认的 FakeEngine。
 */
class Media3PlayerFactory(
    private val context: Context,
) {
    fun create(config: PlayerConfig = PlayerConfig()): Media3PlayerHandle {
        val engine = Media3Engine(context)
        val controller = DefaultPlayerFactory().create(config = config, engine = engine)
        return Media3PlayerHandle(controller = controller, engine = engine)
    }
}