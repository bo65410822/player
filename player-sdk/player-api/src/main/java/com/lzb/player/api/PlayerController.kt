package com.lzb.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * 播放器对外控制入口（Public API）。
 *
 * 宿主只依赖本接口与同包公开模型；不感知 Engine / Media3 等实现细节。
 *
 * 状态与事件双通道：
 * - [state] / [currentSource]：适合 Compose / Flow 订阅
 * - [PlayerListener]：适合传统回调与一次性事件
 *
 * 扩展能力通过 [install] 安装 [PlayerCapability]，而不是修改本接口方法列表。
 */
interface PlayerController {
    /** 当前播放状态，初始为 [PlayerState.Idle]。 */
    val state: StateFlow<PlayerState>

    /** 当前媒体；未 [setMediaSource] 时为 null。 */
    val currentSource: StateFlow<MediaSource?>

    /** 当前生效配置。 */
    val config: PlayerConfig

    /**
     * 更新配置。
     *
     * 具体何时生效（即时 / 下次 prepare）由实现定义；V1.0 实现即时保存。
     */
    fun setConfig(config: PlayerConfig)

    /**
     * 设置媒体资源。
     *
     * 不会自动 prepare；需显式调用 [prepare]。
     */
    fun setMediaSource(source: MediaSource)

    /**
     * 准备媒体。
     *
     * 成功后进入 [PlayerState.Prepared]（若 [PlayerConfig.autoPlay] 为 true 可直接进入 Playing）；
     * 失败进入 [PlayerState.Error] 并回调 [PlayerListener.onError]。
     */
    fun prepare()

    /** 开始或恢复播放。 */
    fun play()

    /** 暂停播放。 */
    fun pause()

    /**
     * 停止播放。
     *
     * 回到 [PlayerState.Idle]，媒体资源是否保留由实现定义；V1.0 实现保留 [currentSource]。
     */
    fun stop()

    /**
     * 跳转到指定位置。
     *
     * @param positionMs 目标进度（毫秒）
     */
    fun seekTo(positionMs: Long)

    /**
     * 释放资源。
     *
     * 会卸载全部 Capability，释放 Engine，清空媒体并回到 [PlayerState.Idle]。
     */
    fun release()

    /** 注册监听；同一实例重复添加时实现应去重或忽略。 */
    fun addListener(listener: PlayerListener)

    /** 移除监听。 */
    fun removeListener(listener: PlayerListener)

    /**
     * 安装能力插件。
     *
     * 相同 [PlayerCapability.id] 已存在时，先 [uninstall] 再安装新实例。
     */
    fun install(capability: PlayerCapability)

    /** 按 id 卸载能力插件；不存在时忽略。 */
    fun uninstall(capabilityId: String)
}