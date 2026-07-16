package com.lzb.player.core.capability

import com.lzb.player.api.PlayerCapability
import com.lzb.player.api.PlayerController
import java.util.concurrent.ConcurrentHashMap

/**
 * Capability 注册表：按 [PlayerCapability.id] 管理安装与卸载。
 */
class CapabilityRegistry {

    private val capabilities = ConcurrentHashMap<String, PlayerCapability>()

    fun install(controller: PlayerController, capability: PlayerCapability) {
        uninstall(capability.id)
        capabilities[capability.id] = capability
        capability.onAttach(controller)
    }

    fun uninstall(capabilityId: String) {
        capabilities.remove(capabilityId)?.onDetach()
    }

    /** 释放时卸载全部能力。 */
    fun detachAll() {
        val snapshot = capabilities.values.toList()
        capabilities.clear()
        snapshot.forEach { it.onDetach() }
    }

    fun get(capabilityId: String): PlayerCapability? = capabilities[capabilityId]
}