package com.saltplayer.lyric.provider

import com.saltplayer.lyric.provider.hook.SaltPlayerHooker
import com.saltplayer.lyric.provider.hook.XposedBridge
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SaltPlayerModule : IXposedHookLoadPackage {

    companion object {
        private const val SALT_PLAYER_PACKAGE = "com.salt.music"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) return

        try {
            val packageName = lpparam.packageName
            if (packageName != SALT_PLAYER_PACKAGE) {
                return
            }

            val classLoader = lpparam.classLoader
            if (!XposedBridge.initialize(classLoader)) {
                return
            }

            hookMusicService(classLoader)
        } catch (e: Exception) {
            e.printStackTrace()
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
