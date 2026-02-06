package com.saltplayer.lyric.provider

import android.content.Context
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.saltplayer.lyric.provider.hook.SaltPlayerHooker

@InjectYukiHookWithXposed(modulePackageName = "com.saltplayer.lyric.provider")
class SaltPlayerModule : IYukiHookXposedInit {

    companion object {
        const val SALT_PLAYER_PACKAGE = "com.salt.music"
    }

    override fun onHook() {
        YukiHookAPI.encase {
            loadApp(SALT_PLAYER_PACKAGE) {
                SaltPlayerHooker.hook(it)
            }
        }
    }

    override fun onInit() {
        YukiHookAPI.configs {
            debugLog {
                tag = "SaltPlayerLyricProvider"
            }
        }
    }
}
