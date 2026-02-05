-keep class com.saltplayer.lyric.provider.** { *; }
-keep class com.salt.music.** { *; }

-keepclassmembers class com.saltplayer.lyric.provider.** {
    <init>(...);
}

-dontwarn com.saltplayer.lyric.provider.**
