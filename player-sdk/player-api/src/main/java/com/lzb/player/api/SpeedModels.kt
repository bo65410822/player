package com.lzb.player.api

/**
 * AB repeat range [startMs, endMs).
 */
data class AbRepeatRange(
    val startMs: Long,
    val endMs: Long,
)

/**
 * Speed capability config.
 *
 * @property defaultSpeed initial rate applied on attach
 * @property minSpeed inclusive lower bound
 * @property maxSpeed inclusive upper bound
 */
data class SpeedConfig(
    val defaultSpeed: Float = 1f,
    val minSpeed: Float = 0.25f,
    val maxSpeed: Float = 4f,
)