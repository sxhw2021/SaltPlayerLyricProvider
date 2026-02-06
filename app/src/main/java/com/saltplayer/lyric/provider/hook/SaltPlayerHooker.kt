package com.saltplayer.lyric.provider.hook

import android.os.Handler
import android.os.Looper
import com.highcapable.yukihookapi.hook.entity.YukiHookBill
import com.highcapable.yukihookapi.hook.entity.YukiModuleHook
import com.saltplayer.lyric.provider.bridge.LyricBridgeManager
import com.saltplayer.lyric.provider.bridge.LyricParser
import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState
import java.lang.ref.WeakReference

object SaltPlayerHooker {
    private const val TAG = "SaltPlayerHooker"
    private var musicServiceReference: WeakReference<Any>? = null
    private var isPlaying = false
    private var currentPosition = 0L
    private var currentDuration = 0L
    private var currentMusicInfo: MusicInfo? = null
    private var currentLyricInfo: LyricInfo? = null
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && currentMusicInfo != null) {
                currentPosition += 100L
                if (currentPosition > currentDuration) {
                    currentPosition = currentDuration
                }
                notifyPlaybackState()
                updateLyricProgress()
                handler.postDelayed(this, 100L)
            }
        }
    }

    fun hook(YukiHookBill: YukiHookBill) {
        try {
            hookConstructor(YukiHookBill)
            hookPlaybackMethods(YukiHookBill)
            hookLyricMethods(YukiHookBill)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookConstructor(YukiHookBill: YukiHookBill) {
        YukiHookBill.fromClass("com.salt.music.service.MusicService")
            .hookConstructor {
                after {
                    musicServiceReference = WeakReference(thisObject)
                    hookPlaybackMethods(thisObject)
                    hookLyricMethods(thisObject)
                }
            }
    }

    private fun hookPlaybackMethods(YukiHookBill: YukiHookBill) {
        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("play") {
                    after {
                        isPlaying = true
                        handler.removeCallbacks(progressRunnable)
                        handler.post(progressRunnable)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("pause") {
                    after {
                        isPlaying = false
                        handler.removeCallbacks(progressRunnable)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("resume") {
                    after {
                        isPlaying = true
                        handler.removeCallbacks(progressRunnable)
                        handler.post(progressRunnable)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("stop") {
                    after {
                        isPlaying = false
                        handler.removeCallbacks(progressRunnable)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookLyricMethods(YukiHookBill: YukiHookBill) {
        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("setLyric") {
                    after {
                        val lyricContent = args.getOrNull(0) as? String
                        if (lyricContent != null) {
                            parseAndNotifyLyric(lyricContent)
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            YukiHookBill.fromClass("com.salt.music.service.MusicService")
                .hookMethod("onLyricChanged") {
                    after {
                        val lyricContent = args.getOrNull(0) as? String
                        if (lyricContent != null) {
                            parseAndNotifyLyric(lyricContent)
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookPlaybackMethods(service: Any?) {
    }

    private fun hookLyricMethods(service: Any?) {
    }

    private fun parseAndNotifyLyric(content: String) {
        try {
            currentLyricInfo = if (content.startsWith("[")) {
                LyricParser.parseLrc(content)
            } else {
                LyricParser.parseQrc(content)
            }
            currentLyricInfo?.let { LyricBridgeManager.notifyLyricInfo(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyPlaybackState() {
        if (currentMusicInfo != null) {
            val playbackState = PlaybackState(
                isPlaying = isPlaying,
                position = currentPosition,
                duration = currentDuration,
                musicInfo = currentMusicInfo!!
            )
            LyricBridgeManager.notifyPlaybackState(playbackState)
        }
    }

    private fun notifyMusicInfo() {
        currentMusicInfo?.let { LyricBridgeManager.notifyMusicInfo(it) }
    }

    private fun updateLyricProgress() {
        val lyricInfo = currentLyricInfo ?: return
        val lyrics = lyricInfo.lyrics

        var currentLineIndex = -1
        var currentLineContent = ""

        for (i in lyrics.indices) {
            val line = lyrics[i]
            if (currentPosition >= line.time) {
                currentLineIndex = i
                currentLineContent = line.content

                if (i < lyrics.size - 1 && currentPosition >= lyrics[i + 1].time) {
                    break
                }
            }
        }

        if (currentLineIndex >= 0) {
            LyricBridgeManager.notifyLyricProgress(currentLineIndex, currentLineContent, currentPosition)
        }
    }

    fun cleanup() {
        handler.removeCallbacks(progressRunnable)
        musicServiceReference?.clear()
        musicServiceReference = null
        currentMusicInfo = null
        currentLyricInfo = null
    }
}
