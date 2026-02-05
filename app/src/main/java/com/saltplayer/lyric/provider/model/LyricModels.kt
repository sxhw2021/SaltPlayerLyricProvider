package com.saltplayer.lyric.provider.model

data class LyricLine(
    val time: Long,
    val content: String,
    val duration: Long = 0
)

data class LyricInfo(
    val title: String,
    val artist: String,
    val album: String,
    val lyricist: String?,
    val composer: String?,
    val arranger: String?,
    val lyrics: List<LyricLine>,
    val offset: Long = 0,
    val isSynced: Boolean = false
)

data class MusicInfo(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String?,
    val artworkUri: String?
)

data class PlaybackState(
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val musicInfo: MusicInfo?
)
