package com.saltplayer.lyric.provider.bridge

import com.saltplayer.lyric.provider.model.LyricLine
import com.saltplayer.lyric.provider.model.LyricInfo

object LyricParser {
    fun parseLrc(content: String): LyricInfo {
        val lines = content.lines()
        val lyricLines = mutableListOf<LyricLine>()
        var title = ""
        var artist = ""
        var album = ""
        var offset = 0L

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val tagMatch = Regex("""\[(\w+):([^]]*)]""").find(trimmedLine)
            if (tagMatch != null) {
                val tagName = tagMatch.groupValues[1].lowercase()
                val tagValue = tagMatch.groupValues[2]

                when (tagName) {
                    "ti" -> title = tagValue
                    "ar" -> artist = tagValue
                    "al" -> album = tagValue
                    "offset" -> offset = tagValue.toLongOrNull() ?: 0L
                }
            }

            val timeMatch = Regex("""\[(\d+):(\d+\.?\d*)]""").findAll(trimmedLine)
            for (timeMatchResult in timeMatch) {
                val minutes = timeMatchResult.groupValues[1].toLongOrNull() ?: continue
                val seconds = timeMatchResult.groupValues[2].toDoubleOrNull() ?: continue
                val time = (minutes * 60 + seconds * 1000).toLong()

                val content = trimmedLine
                    .replace(Regex("""\[\d+:\d+\.?\d*]"""), "")
                    .trim()

                if (content.isNotEmpty()) {
                    lyricLines.add(LyricLine(time, content))
                }
            }
        }

        lyricLines.sortBy { it.time }

        return LyricInfo(
            title = title,
            artist = artist,
            album = album,
            lyricist = null,
            composer = null,
            arranger = null,
            lyrics = lyricLines,
            offset = offset,
            isSynced = lyricLines.isNotEmpty()
        )
    }

    fun parseQrc(content: String): LyricInfo {
        return try {
            val lines = content.lines()
            val lyricLines = mutableListOf<LyricLine>()
            var title = ""
            var artist = ""
            var album = ""

            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue

                val timeMatch = Regex("""\[(\d+),(\d+),""").find(trimmedLine)
                if (timeMatch != null) {
                    val time = timeMatch.groupValues[1].toLongOrNull() ?: continue
                    val duration = timeMatch.groupValues[2].toLongOrNull() ?: 0L

                    val contentMatch = Regex("""\](.*)""").find(trimmedLine)
                    val content = contentMatch?.groupValues?.getOrNull(1)?.trim() ?: continue

                    if (content.isNotEmpty()) {
                        lyricLines.add(LyricLine(time, content, duration))
                    }
                }
            }

            lyricLines.sortBy { it.time }

            LyricInfo(
                title = title,
                artist = artist,
                album = album,
                lyricist = null,
                composer = null,
                arranger = null,
                lyrics = lyricLines,
                offset = 0L,
                isSynced = lyricLines.isNotEmpty()
            )
        } catch (e: Exception) {
            LyricInfo(
                title = "",
                artist = "",
                album = "",
                lyricist = null,
                composer = null,
                arranger = null,
                lyrics = emptyList(),
                offset = 0L,
                isSynced = false
            )
        }
    }
}
