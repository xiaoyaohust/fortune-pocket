# iOS Project Setup

All Swift source files are already written. Follow these steps to create the Xcode project.

## Requirements

- Xcode 15.0+
- iOS 17.0+ deployment target
- Recommended: [XcodeGen](https://github.com/yonaskolb/XcodeGen) for one-command setup

---

## Option A: XcodeGen (Recommended)

### 1. Install XcodeGen
```bash
brew install xcodegen
```

### 2. Create `ios/project.yml`

Create `ios/project.yml` with the content below, then run:
```bash
cd ios
xcodegen generate
open PocketOracle.xcodeproj
```

### project.yml content
```yaml
name: PocketOracle
options:
  bundleIdPrefix: com.fortunepocket
  deploymentTarget:
    iOS: "17.0"
  xcodeVersion: "15.0"

targets:
  PocketOracle:
    type: application
    platform: iOS
    deploymentTarget: "17.0"
    sources:
      - path: PocketOracle
    resources:
      - path: ../shared-content/data
        buildPhase: resources
    info:
      path: PocketOracle/Resources/Info.plist
      properties:
        CFBundleDisplayName: Fortune Pocket
        CFBundleShortVersionString: "1.0"
        CFBundleVersion: "1"
        LSRequiresIPhoneOS: true
        UILaunchStoryboardName: ""
        UIApplicationSceneManifest:
          UIApplicationSupportsMultipleScenes: false
        NSUserNotificationsUsageDescription: "Fortune Pocket 需要发送每日提醒通知"
        CFBundleLocalizations:
          - en
          - zh-Hans
    settings:
      SWIFT_VERSION: "5.9"
      DEVELOPMENT_TEAM: ""   # Set your team ID here
```

---

## Option B: Manual Xcode Setup

### 1. Create Xcode Project
- Open Xcode → File → New → Project
- Template: **App**
- Product Name: `PocketOracle`
- Organization Identifier: `com.fortunepocket`
- Bundle Identifier: `com.fortunepocket.ios`
- Interface: **SwiftUI**
- Language: **Swift**
- Storage: **SwiftData** ✓
- Min Deployment: **iOS 17.0**
- Save in: `fortune-pocket/ios/`

### 2. Remove generated files
Delete all auto-generated files that were replaced by ours:
- `ContentView.swift` (we have our own)
- `Item.swift` (replaced by our models)

### 3. Add source files
Right-click the `PocketOracle` group → Add Files → select all `.swift` files in:
- `ios/PocketOracle/App/`
- `ios/PocketOracle/Core/`
- `ios/PocketOracle/Models/`
- `ios/PocketOracle/Features/`
- `ios/PocketOracle/SharedContent/`

### 4. Add shared-content as Folder Reference
- Right-click the `PocketOracle` group → Add Files
- Navigate to `fortune-pocket/shared-content/data/`
- Select the `data` folder
- **IMPORTANT**: Choose "Create folder references" (NOT "Create groups")
- This creates a **blue** folder reference in Xcode

### 5. Add Localizations
- Project settings → Info tab → Localizations
- Click + and add **Chinese (Simplified)**
- Xcode will find the `.strings` files in `en.lproj/` and `zh-Hans.lproj/`

### 6. Info.plist additions
Add to your `Info.plist`:
```xml
<key>NSUserNotificationsUsageDescription</key>
<string>Fortune Pocket 需要发送每日提醒通知</string>
<key>CFBundleLocalizations</key>
<array>
    <string>en</string>
    <string>zh-Hans</string>
</array>
```

### 7. Build & Run
- Select iPhone 17 simulator
- Cmd+R

---

## Project Structure in Xcode

```
PocketOracle/
├── App/
│   ├── FortunePocketApp.swift
│   └── ContentView.swift
├── Core/
│   ├── Theme/
│   │   ├── AppColors.swift
│   │   └── AppFonts.swift
│   └── Extensions/
│       └── View+Theme.swift
├── Models/
│   ├── ReadingRecord.swift
│   ├── TarotCard.swift
│   ├── ZodiacSign.swift
│   └── BaziModels.swift
├── Features/
│   ├── Home/HomeView.swift
│   ├── Tarot/TarotView.swift
│   ├── Astrology/AstrologyView.swift
│   ├── Bazi/BaziView.swift
│   ├── History/HistoryView.swift
│   └── Settings/SettingsView.swift
├── SharedContent/
│   └── ContentLoader.swift
└── Resources/
    ├── en.lproj/Localizable.strings
    ├── zh-Hans.lproj/Localizable.strings
    └── data/  ← Folder Reference to shared-content/data/
```
