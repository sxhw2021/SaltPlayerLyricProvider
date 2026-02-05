package com.saltplayer.lyric.provider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.saltplayer.lyric.provider.bridge.LyricBridge
import com.saltplayer.lyric.provider.bridge.LyricBridgeManager
import com.saltplayer.lyric.provider.hook.XposedBridge
import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState
import com.saltplayer.lyric.provider.hook.SaltPlayerHooker
import java.lang.reflect.Method

class SaltPlayerModule {

    companion object {
        private const val TAG = "SaltPlayerLyricProvider"
        private const val SALT_PLAYER_PACKAGE = "com.salt.music"
    }

    fun handleLoadPackage(lpparam: Any) {
        try {
            val classLoader = getClassLoader(lpparam)
            if (!XposedBridge.initialize(classLoader)) {
                return
            }

            val packageName = getPackageName(lpparam)
            if (packageName != SALT_PLAYER_PACKAGE) {
                return
            }

            hookSaltPlayer(lpparam, classLoader)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getClassLoader(lpparam: Any): ClassLoader {
        return try {
            val method = lpparam.javaClass.getMethod("getClassLoader")
            method.invoke(lpparam) as ClassLoader
        } catch (e: Exception) {
            Thread.currentThread().contextClassLoader
        }
    }

    private fun getPackageName(lpparam: Any): String {
        return try {
            val field = lpparam.javaClass.getField("packageName")
            field.get(lpparam) as String
        } catch (e: Exception) {
            ""
        }
    }

    private fun getClassLoaderFromAny(obj: Any): ClassLoader {
        return try {
            obj.javaClass.getMethod("getClassLoader").invoke(obj) as ClassLoader
        } catch (e: Exception) {
            Thread.currentThread().contextClassLoader
        }
    }

    private fun hookSaltPlayer(lpparam: Any, classLoader: ClassLoader) {
        try {
            XposedBridge.hookMethod(
                "android.app.Application",
                getClassLoaderFromAny(lpparam),
                arrayOf<Class<*>>(),
                object : XposedBridge.MethodHookCallback() {
                    override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                    }

                    override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                        SaltPlayerHooker.hookMusicService()
                        registerLyricBridge()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val mainActivityClass = XposedBridge.findClass(
                "com.salt.music.ui.MainActivity",
                classLoader
            )
            if (mainActivityClass != null) {
                XposedBridge.hookMethod(
                    mainActivityClass,
                    "onCreate",
                    arrayOf<Class<*>>(Bundle::class.java),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                            SaltPlayerHooker.hookMusicService()
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
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
                            SaltPlayerHooker.hookPlaybackMethods(param.thisObject)
                            SaltPlayerHooker.hookLyricMethods(param.thisObject)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        hookMediaSession(lpparam, classLoader)
        hookNotification(lpparam, classLoader)
    }

    private fun hookMediaSession(lpparam: Any, classLoader: ClassLoader) {
        try {
            val mediaSessionClass = XposedBridge.findClass(
                "android.media.session.MediaSession",
                classLoader
            )
            if (mediaSessionClass != null) {
                XposedBridge.hookMethod(
                    mediaSessionClass,
                    "setCallback",
                    arrayOf<Class<*>>(android.media.session.MediaSession.Callback::class.java),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                            val callback = param.args.getOrNull(0) as? android.media.session.MediaSession.Callback
                            hookMediaSessionCallback(callback)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookMediaSessionCallback(callback: android.media.session.MediaSession.Callback?) {
        if (callback == null) return
    }

    private fun hookNotification(lpparam: Any, classLoader: ClassLoader) {
        try {
            val notificationClass = XposedBridge.findClass(
                "android.app.Notification",
                classLoader
            )
            if (notificationClass != null) {
                XposedBridge.hookMethod(
                    notificationClass,
                    "setLatestEventInfo",
                    arrayOf<Class<*>>(
                        Context::class.java,
                        CharSequence::class.java,
                        CharSequence::class.java,
                        android.app.PendingIntent::class.java,
                        android.app.PendingIntent::class.java
                    ),
                    object : XposedBridge.MethodHookCallback() {
                        override fun beforeHookedMethod(param: XposedBridge.MethodHookParam) {
                        }

                        override fun afterHookedMethod(param: XposedBridge.MethodHookParam) {
                            val notification = param.thisObject as? android.app.Notification
                            extractMediaInfoFromNotification(notification)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractMediaInfoFromNotification(notification: android.app.Notification?) {
        if (notification == null) return
    }

    private fun registerLyricBridge() {
        val bridge = object : LyricBridge {
            override fun onLyricInfoReceived(lyricInfo: LyricInfo) {
            }

            override fun onPlaybackStateChanged(playbackState: PlaybackState) {
            }

            override fun onMusicInfoChanged(musicInfo: MusicInfo) {
            }

            override fun onLyricProgressChanged(currentLineIndex: Int, currentLineContent: String, progress: Long) {
            }
        }
        LyricBridgeManager.registerBridge(bridge)
    }
}
