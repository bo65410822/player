package com.lzb.player.common

/**
 * SDK 元信息（可选公共模块）。
 *
 * 存放与具体业务无关的常量，例如版本号、展示名称等。
 * 当前未被其他模块强依赖，可按需引入。
 */
object PlayerSdk {
    /** 当前 SDK 版本号。 */
    const val VERSION = "0.1.0"

    /** SDK 展示名称。 */
    const val NAME = "Player SDK"
}