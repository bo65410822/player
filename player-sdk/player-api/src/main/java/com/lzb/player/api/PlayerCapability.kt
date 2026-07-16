package com.lzb.player.api

/**
 * 播放器能力插件接口。
 *
 * 新增缓存 / 字幕 / 倍速等能力时实现本接口，并通过 [PlayerController.install] 安装，
 * 避免向 Controller 继续堆砌业务方法。
 *
 * V1.0 仅提供安装机制；具体能力实现从 V1.1 开始交付。
 */
interface PlayerCapability {
    /** 能力唯一标识；同 id 重复安装时将先卸载旧实例。 */
    val id: String

    /** 安装成功后回调；可持有 [controller] 引用（勿泄漏 Context）。 */
    fun onAttach(controller: PlayerController)

    /** 卸载或 [PlayerController.release] 时回调，用于释放资源。 */
    fun onDetach()
}