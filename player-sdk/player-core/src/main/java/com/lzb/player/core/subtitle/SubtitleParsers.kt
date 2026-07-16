package com.lzb.player.core.subtitle

import com.lzb.player.api.SubtitleCue

/**
 * SRT / WebVTT lightweight parser (V1.2).
 */
internal object SubtitleParsers {

    fun parseSrt(content: String): List<SubtitleCue> {
        val blocks = content.replace("\r\n", "\n").trim().split(Regex("\n\\s*\n"))
        val cues = mutableListOf<SubtitleCue>()
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex < 0) continue
            val times = lines[timeLineIndex].split("-->").map { it.trim() }
            if (times.size < 2) continue
            val start = parseSrtTime(times[0]) ?: continue
            val end = parseSrtTime(times[1].substringBefore(' ')) ?: continue
            val text = lines.drop(timeLineIndex + 1).joinToString("\n")
            if (text.isBlank()) continue
            cues += SubtitleCue(startMs = start, endMs = end, text = text)
        }
        return cues.sortedBy { it.startMs }
    }

    fun parseWebVtt(content: String): List<SubtitleCue> {
        val normalized = content.replace("\r\n", "\n")
        val body = normalized.lineSequence().dropWhile { line ->
                val t = line.trim()
                t.isEmpty() || t.startsWith("WEBVTT", ignoreCase = true) || t.startsWith("NOTE")
            }.joinToString("\n")
        val blocks = body.trim().split(Regex("\n\\s*\n"))
        val cues = mutableListOf<SubtitleCue>()
        for (block in blocks) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex < 0) continue
            val times = lines[timeLineIndex].split("-->").map { it.trim() }
            if (times.size < 2) continue
            val start = parseVttTime(times[0]) ?: continue
            val end = parseVttTime(times[1].substringBefore(' ')) ?: continue
            val text = lines.drop(timeLineIndex + 1).joinToString("\n")
            if (text.isBlank()) continue
            cues += SubtitleCue(startMs = start, endMs = end, text = text)
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseSrtTime(raw: String): Long? {
        val value = raw.trim().replace(',', '.')
        return parseClock(value)
    }

    private fun parseVttTime(raw: String): Long? = parseClock(raw.trim())

    private fun parseClock(value: String): Long? {
        val parts = value.split(':')
        if (parts.size !in 2..3) return null
        return try {
            val hours: Long
            val minutes: Long
            val secondsPart: String
            if (parts.size == 3) {
                hours = parts[0].toLong()
                minutes = parts[1].toLong()
                secondsPart = parts[2]
            } else {
                hours = 0
                minutes = parts[0].toLong()
                secondsPart = parts[1]
            }
            val secParts = secondsPart.split('.')
            val seconds = secParts[0].toLong()
            val millis = when {
                secParts.size < 2 -> 0L
                else -> secParts[1].padEnd(3, '0').take(3).toLong()
            }
            ((hours * 3600 + minutes * 60 + seconds) * 1000L) + millis
        } catch (_: NumberFormatException) {
            null
        }
    }
}