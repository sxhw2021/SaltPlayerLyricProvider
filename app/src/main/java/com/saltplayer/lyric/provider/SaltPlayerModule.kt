package com.saltplayer.lyric.provider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.saltplayer.lyric.provider.bridge.LyricBridge
import com.saltplayer.lyric.provider.bridge.LyricBridgeManager
import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState
import com.saltplayer.lyric.provider.hook.SaltPlayerHooker
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SaltPlayerModule : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "SaltPlayerLyricProvider"
        private const val SALT_PLAYER_PACKAGE = "com.salt.music"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != SALT_PLAYER_PACKAGE) {
            return
        }

        try {
            hookSaltPlayer(lpparam)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hookSaltPlayer(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            lpparam.classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    SaltPlayerHooker.hookMusicService()
                    registerLyricBridge()
                }
            }
        )

        try {
            val mainActivityClass = XposedHelpers.findClassIfExists(
                "com.salt.music.ui.MainActivity",
                lpparam.classLoader
            )
            if (mainActivityClass != null) {
                XposedHelpers.findAndHookMethod(
                    mainActivityClass,
                    "onCreate",
                    android.os.Bundle::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            SaltPlayerHooker.hookMusicService()
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val musicServiceClass = XposedHelpers.findClassIfExists(
                "com.salt.music.service.MusicService",
                lpparam.classLoader
            )
            if (musicServiceClass != null) {
                XposedHelpers.findAndHookConstructor(
                    musicServiceClass,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            SaltPlayerHooker.hookPlaybackMethods(param.thisObject)
                            SaltPlayerHooker.hookLyricMethods(param.thisObject)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        hookMediaSession(lpparam)
        hookNotification(lpparam)
    }

    private fun hookMediaSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val mediaSessionClass = XposedHelpers.findClassIfExists(
                "android.media.session.MediaSession",
                lpparam.classLoader
            )
            if (mediaSessionClass != null) {
                XposedHelpers.findAndHookMethod(
                    mediaSessionClass,
                    "setCallback",
                    android.media.session.MediaSession.Callback::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val callback = param.args[0] as? android.media.session.MediaSession.Callback
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

    private fun hookNotification(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val notificationClass = XposedHelpers.findClassIfExists(
                "android.app.Notification",
                lpparam.classLoader
            )
            if (notificationClass != null) {
                XposedHelpers.findAndHookMethod(
                    notificationClass,
                    "setLatestEventInfo",
                    Context::class.java,
                    CharSequence::class.java,
                    CharSequence::class.java,
                    android.app.PendingIntent::class.java,
                    android.app.PendingIntent::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
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
