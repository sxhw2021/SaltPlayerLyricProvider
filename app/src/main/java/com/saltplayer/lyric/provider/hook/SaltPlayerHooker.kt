package com.saltplayer.lyric.provider.hook

import android.os.Handler
import android.os.Looper
import com.saltplayer.lyric.provider.bridge.LyricBridgeManager
import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
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

    fun hookMusicService() {
        try {
            val musicServiceClass = XposedHelpers.findClassIfExists(
                "com.salt.music.service.MusicService",
                null
            )

            if (musicServiceClass != null) {
                XposedHelpers.findAndHookMethod(
                    musicServiceClass,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            musicServiceReference = WeakReference(param.thisObject)
                            hookPlaybackMethods(param.thisObject)
                            hookLyricMethods(param.thisObject)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookPlaybackMethods(service: Any) {
        try {
            val playMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "play",
                Any::class.java
            )
            if (playMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "play",
                    Any::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            isPlaying = true
                            handler.removeCallbacks(progressRunnable)
                            handler.post(progressRunnable)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val pauseMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "pause"
            )
            if (pauseMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "pause",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            isPlaying = false
                            handler.removeCallbacks(progressRunnable)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val resumeMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "resume"
            )
            if (resumeMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "resume",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            isPlaying = true
                            handler.removeCallbacks(progressRunnable)
                            handler.post(progressRunnable)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val stopMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "stop"
            )
            if (stopMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "stop",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            isPlaying = false
                            handler.removeCallbacks(progressRunnable)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookLyricMethods(service: Any) {
        try {
            val setLyricMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "setLyric",
                String::class.java
            )
            if (setLyricMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "setLyric",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val lyricContent = param.args[0] as? String
                            if (lyricContent != null) {
                                parseAndNotifyLyric(lyricContent)
                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val onLyricChangedMethod = XposedHelpers.findMethodExactIfExists(
                service::class.java,
                "onLyricChanged",
                Any::class.java
            )
            if (onLyricChangedMethod != null) {
                XposedHelpers.findAndHookMethod(
                    service::class.java,
                    "onLyricChanged",
                    Any::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val lyricContent = param.args[0] as? String
                            if (lyricContent != null) {
                                parseAndNotifyLyric(lyricContent)
                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateMusicInfo(title: String, artist: String, album: String, duration: Long, path: String?, artworkUri: String?) {
        currentMusicInfo = MusicInfo(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            path = path,
            artworkUri = artworkUri
        )
        currentDuration = duration
        notifyMusicInfo()
    }

    fun updateLyricContent(lyricContent: String) {
        parseAndNotifyLyric(lyricContent)
    }

    fun updatePosition(position: Long, duration: Long) {
        currentPosition = position
        currentDuration = duration
        updateLyricProgress()
    }

    private fun parseAndNotifyLyric(content: String) {
        try {
            currentLyricInfo = if (content.startsWith("[")) {
                com.saltplayer.lyric.provider.bridge.LyricParser.parseLrc(content)
            } else {
                com.saltplayer.lyric.provider.bridge.LyricParser.parseQrc(content)
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
                musicInfo = currentMusicInfo
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
