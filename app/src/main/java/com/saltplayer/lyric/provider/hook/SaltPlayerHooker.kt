package com.saltplayer.lyric.provider.hook

import android.os.Handler
import android.os.Looper
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

    fun hookMusicService() {
        try {
            val classLoader = Thread.currentThread().contextClassLoader
            val musicServiceClass = XposedBridge.findClass(
                "com.salt.music.service.MusicService",
                classLoader
            )

            if (musicServiceClass != null) {
                XposedBridge.hookConstructor(
                    musicServiceClass,
                    arrayOf<Class<*>>(),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
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

    private fun hookPlaybackMethods(service: Any?) {
        if (service == null) return

        try {
            val serviceClass = service.javaClass
            val playMethod = XposedBridge.findMethodExactIfExists(
                serviceClass,
                "play",
                Any::class.java
            )
            if (playMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "play",
                    arrayOf<Class<*>>(Any::class.java),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
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
            val pauseMethod = XposedBridge.findMethodExactIfExists(
                service.javaClass,
                "pause"
            )
            if (pauseMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "pause",
                    arrayOf<Class<*>>(),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
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
            val resumeMethod = XposedBridge.findMethodExactIfExists(
                service.javaClass,
                "resume"
            )
            if (resumeMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "resume",
                    arrayOf<Class<*>>(),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
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
            val stopMethod = XposedBridge.findMethodExactIfExists(
                service.javaClass,
                "stop"
            )
            if (stopMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "stop",
                    arrayOf<Class<*>>(),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
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

    private fun hookLyricMethods(service: Any?) {
        if (service == null) return

        try {
            val setLyricMethod = XposedBridge.findMethodExactIfExists(
                service.javaClass,
                "setLyric",
                String::class.java
            )
            if (setLyricMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "setLyric",
                    arrayOf<Class<*>>(String::class.java),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                            val lyricContent = param.args.getOrNull(0) as? String
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
            val onLyricChangedMethod = XposedBridge.findMethodExactIfExists(
                service.javaClass,
                "onLyricChanged",
                Any::class.java
            )
            if (onLyricChangedMethod != null) {
                XposedBridge.hookMethod(
                    service,
                    "onLyricChanged",
                    arrayOf<Class<*>>(Any::class.java),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                            val lyricContent = param.args.getOrNull(0) as? String
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
