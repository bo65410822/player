package com.lzb.player.core.engine

/**
 * 引擎侧缓存会话标记。
 *
 * 由具体 Engine 实现（如 Media3）解释；core / api 不依赖 Media3 类型。
 * Capability 在 onAttach 时通过 [PlayerEngine.setCacheSession] 注入。
 */
interface EngineCacheSession