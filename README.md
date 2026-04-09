# Fortune Pocket · 口袋占卜屋

A dual-native divination app for iOS and Android.  
一款双端原生占卜 App，完全离线，本地数据驱动。

## Features · 功能

- Tarot · 塔罗牌阵解读
- Astrology · 本命盘占星
- Bazi · 八字性格分析（娱乐向）
- History · 历史记录
- Daily Reminder · 每日提醒
- Share Card · 结果卡片分享

## Architecture · 架构

```
fortune-pocket/
├── shared-content/     # Shared static data (JSON) for both platforms
├── ios/                # Swift + SwiftUI (iOS 17+)
└── android/            # Kotlin + Jetpack Compose (API 26+)
```

## Quick Start · 快速开始

### iOS
See [ios/SETUP.md](ios/SETUP.md)

### Android
```bash
cd android
./gradlew :app:installDebug
```

## Data Management · 数据管理

All content lives in `shared-content/data/`. Both platforms load from this directory.  
See [docs/data-schema.md](docs/data-schema.md) for schema reference and [docs/astrology-engine.md](docs/astrology-engine.md) for natal-chart rules.

## Localization · 多语言

- iOS: `ios/PocketOracle/Resources/*.lproj/Localizable.strings`
- Android: `android/app/src/main/res/values*/strings.xml`
- Content JSON: all text fields have `_zh` and `_en` variants

## Disclaimer · 免责声明

Fortune Pocket is for entertainment and self-exploration only.  
本 App 内容仅供娱乐、放松和自我探索参考，不构成任何专业建议。
