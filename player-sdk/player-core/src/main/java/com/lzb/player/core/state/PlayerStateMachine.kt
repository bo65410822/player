package com.lzb.player.core.state

import com.lzb.player.api.PlayerState

/**
 * 播放器状态机：集中校验合法迁移，避免 Controller 随意赋值。
 *
 * 规则要点：
 * - 任意状态都可进入 [PlayerState.Error]
 * - [forceIdle] 用于 stop / release，强制回到 Idle
 */
class PlayerStateMachine {

    @Volatile
    var current: PlayerState = PlayerState.Idle
        private set

    /**
     * 尝试迁移到 [target]。
     *
     * @return true 表示迁移成功；false 表示非法迁移（保持原状态）
     */
    fun tryTransition(target: PlayerState): Boolean {
        if (current == target) return true
        if (target == PlayerState.Error || isAllowed(current, target)) {
            current = target
            return true
        }
        return false
    }

    /** stop / release 时强制回到 Idle。 */
    fun forceIdle() {
        current = PlayerState.Idle
    }

    /** 重置状态机（例如 Controller 重建场景）。 */
    fun reset() {
        current = PlayerState.Idle
    }

    private fun isAllowed(from: PlayerState, to: PlayerState): Boolean {
        return allowed[from]?.contains(to) == true
    }

    private companion object {
        private val allowed: Map<PlayerState, Set<PlayerState>> = mapOf(
            PlayerState.Idle to setOf(PlayerState.Preparing),
            PlayerState.Preparing to setOf(PlayerState.Prepared, PlayerState.Idle),
            PlayerState.Prepared to setOf(
                PlayerState.Playing,
                PlayerState.Idle,
                PlayerState.Preparing,
            ),
            PlayerState.Playing to setOf(
                PlayerState.Paused,
                PlayerState.Buffering,
                PlayerState.Completed,
                PlayerState.Idle,
            ),
            PlayerState.Paused to setOf(
                PlayerState.Playing,
                PlayerState.Idle,
                PlayerState.Preparing,
                PlayerState.Buffering,
            ),
            PlayerState.Buffering to setOf(
                PlayerState.Playing,
                PlayerState.Paused,
                PlayerState.Completed,
                PlayerState.Idle,
            ),
            PlayerState.Completed to setOf(
                PlayerState.Playing,
                PlayerState.Idle,
                PlayerState.Preparing,
            ),
            PlayerState.Error to setOf(PlayerState.Idle, PlayerState.Preparing),
        )
    }
}