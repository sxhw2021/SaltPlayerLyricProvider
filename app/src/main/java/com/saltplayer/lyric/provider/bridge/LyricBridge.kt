package com.saltplayer.lyric.provider.bridge

import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState

interface LyricBridge {
    fun onLyricInfoReceived(lyricInfo: LyricInfo)
    fun onPlaybackStateChanged(playbackState: PlaybackState)
    fun onMusicInfoChanged(musicInfo: MusicInfo)
    fun onLyricProgressChanged(currentLineIndex: Int, currentLineContent: String, progress: Long)
}

object LyricBridgeManager {
    private val bridges = mutableListOf<LyricBridge>()

    @Synchronized
    fun registerBridge(bridge: LyricBridge) {
        if (!bridges.contains(bridge)) {
            bridges.add(bridge)
        }
    }

    @Synchronized
    fun unregisterBridge(bridge: LyricBridge) {
        bridges.remove(bridge)
    }

    fun notifyLyricInfo(lyricInfo: LyricInfo) {
        bridges.forEach { it.onLyricInfoReceived(lyricInfo) }
    }

    fun notifyPlaybackState(playbackState: PlaybackState) {
        bridges.forEach { it.onPlaybackStateChanged(playbackState) }
    }

    fun notifyMusicInfo(musicInfo: MusicInfo) {
        bridges.forEach { it.onMusicInfoChanged(musicInfo) }
    }

    fun notifyLyricProgress(currentLineIndex: Int, currentLineContent: String, progress: Long) {
        bridges.forEach { it.onLyricProgressChanged(currentLineIndex, currentLineContent, progress) }
    }
}
