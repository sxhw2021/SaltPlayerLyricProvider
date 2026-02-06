package com.saltplayer.lyric.provider

import com.saltplayer.lyric.provider.bridge.LyricBridge
import com.saltplayer.lyric.provider.bridge.LyricBridgeManager
import com.saltplayer.lyric.provider.hook.SaltPlayerHooker
import com.saltplayer.lyric.provider.hook.XposedBridge
import com.saltplayer.lyric.provider.model.LyricInfo
import com.saltplayer.lyric.provider.model.MusicInfo
import com.saltplayer.lyric.provider.model.PlaybackState

object SaltPlayerModule {

    private const val SALT_PLAYER_PACKAGE = "com.salt.music"

    @JvmStatic
    fun handleLoadPackage(lpparam: Any) {
        try {
            val packageName = getPackageName(lpparam)
            if (packageName != SALT_PLAYER_PACKAGE) {
                return
            }

            val classLoader = getClassLoader(lpparam)
            if (!XposedBridge.initialize(classLoader)) {
                return
            }

            hookMusicService(classLoader)
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

    private fun hookMusicService(classLoader: ClassLoader) {
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
    }
}
