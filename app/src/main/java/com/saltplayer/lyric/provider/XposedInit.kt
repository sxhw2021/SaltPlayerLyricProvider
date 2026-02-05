package com.saltplayer.lyric.provider

import android.content.Intent
import com.saltplayer.lyric.provider.hook.SaltPlayerHooker
import com.saltplayer.lyric.provider.hook.XposedBridge

class XposedInit {

    companion object {
        private const val SALT_PLAYER_PACKAGE = "com.salt.music"
    }

    fun initZygote(startupParam: Any?) {
    }

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

            SaltPlayerHooker.hookMusicService()
            hookSaltPlayer(classLoader)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleInitPackageResources(resparam: Any?) {
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

    private fun hookSaltPlayer(classLoader: ClassLoader) {
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
