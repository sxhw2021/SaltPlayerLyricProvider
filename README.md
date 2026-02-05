# SaltPlayer Lyric Provider

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![LSPosed](https://img.shields.io/badge/Framework-LSPosed-orange.svg)](https://github.com/LSPosed/LSPosed)

A LSPosed module that provides lyrics functionality for SaltPlayer music player, compatible with the Lyricon status bar lyrics extension.

## Features

- Hooks into SaltPlayer to capture music playback information
- Extracts and synchronizes lyrics from multiple formats (LRC, QRC, etc.)
- Provides real-time lyric progress updates
- Compatible with Lyricon status bar lyrics extension

## Requirements

- Android 9.0 (API 26) and above
- Root access
- LSPosed framework installed

## Installation

1. Download the latest release APK from [GitHub Releases](https://github.com/yourusername/SaltPlayerLyricProvider/releases)
2. Install the APK on your Android device
3. Open LSPosed Manager
4. Enable the "SaltPlayer Lyric Provider" module
5. Select "SaltPlayer" as the scope
6. Restart the system UI (or reboot)

## Building from Source

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17 or newer
- Android SDK with API 34
- Gradle 8.2 or newer

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/yourusername/SaltPlayerLyricProvider.git
cd SaltPlayerLyricProvider
```

2. Build the debug APK:
```bash
./gradlew assembleDebug
```

3. Build the release APK:
```bash
./gradlew assembleRelease
```

The APK will be generated at `app/build/outputs/apk/debug/` or `app/build/outputs/apk/release/`.

## Architecture

This project follows the LyricProvider architecture pattern:

```
com.saltplayer.lyric.provider/
├── bridge/                    # LyricProvider bridge implementation
│   ├── LyricBridge.kt        # Bridge interface and manager
│   └── LyricParser.kt        # Lyric format parser (LRC, QRC)
├── hook/                     # LSPosed hooks for SaltPlayer
│   └── SaltPlayerHooker.kt   # Music service and playback hooks
└── model/                    # Data models
    └── LyricModels.kt        # Lyric, Music, PlaybackState models
```

## Supported Lyric Formats

- **LRC**: Standard synchronized lyrics format
- **QRC**: Tencent QMusic lyrics format
- **KRC**: NetEase Cloud Music lyrics format (coming soon)

## Integration with Lyricon

This module is designed to work with [Lyricon](https://github.com/proify/lyricon):

1. Install Lyricon LSPosed module
2. Install this SaltPlayer Lyric Provider module
3. Configure SaltPlayer as the scope in LSPosed
4. Enjoy status bar lyrics while playing music in SaltPlayer

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Lyricon](https://github.com/proify/lyricon) - The status bar lyrics extension this module integrates with
- [SaltPlayer](https://github.com/Moriafly/SaltPlayerSource) - The music player this module targets
- [LSPosed](https://github.com/LSPosed/LSPosed) - The Xposed framework this module uses
