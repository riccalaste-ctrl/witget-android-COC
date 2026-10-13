# Clashboard Android

Android port of the iOS Clashboard app (Clash of Clans widget tracker) from [Zacatac3/clash_widgets](https://github.com/Zacatac3/clash_widgets).

## Features

- **3 Widgets**: Main builders grid, Closest Upgrade (lock screen), Clan War (lock screen)
- **Companion App**: Full Material 3 UI for managing profiles, imports, boosts, notifications
- **Boost-Aware Timers**: Accurate countdown with Builder Potion, Research Potion, Clock Tower, Helpers
- **Multi-Profile Support**: Switch between multiple Clash of Clans accounts
- **JSON Import**: Paste Clash of Clans export data to populate upgrades
- **Quick Settings Tile**: Import from clipboard with one tap
- **WorkManager Updates**: Efficient background widget updates (15min default, 1-5min with active boosts)

## Architecture

```
app/
├── data/
│   ├── model/          # BuildingUpgrade, PlayerAccount, ActiveBoost, etc.
│   ├── database/       # Room database, DAOs, TypeConverters
│   ├── parser/         # CoC JSON export parser
│   ├── repository/     # ProfileRepository, UpgradeRepository
│   └── boost/          # Boost calculation engine
├── widget/
│   ├── main/           # Main builders grid widget (Glance)
│   ├── closest/        # Closest upgrade widget (Glance)
│   ├── war/            # Clan war widget (Glance)
│   └── config/         # Widget configuration activity
├── ui/
│   ├── dashboard/      # Main dashboard screen
│   ├── profiles/       # Profile management
│   ├── importdata/     # JSON import screen
│   ├── settings/       # Settings & backup
│   ├── boosts/         # Active boost management
│   └── notifications/  # Notification preferences
├── util/               # WidgetConfigStore, NotificationBootReceiver
└── di/                 # Hilt modules
```

## Building

### Prerequisites
- Android Studio Koala (2024.1.1) or later
- JDK 17
- Android SDK 34

### Steps

1. **Open in Android Studio**
   ```bash
   # Open the clashboard-android folder in Android Studio
   ```

2. **Extract Assets from iOS Repo**
   ```bash
   # Clone the iOS repo alongside this project
   git clone https://github.com/Zacatac3/clash_widgets.git ../clash_widgets
   
   # Run asset extraction script
   python3 extract_assets.py
   ```

3. **Build & Run**
   - Connect Android device with USB debugging enabled
   - Click Run (Shift+F10) in Android Studio

## Asset Licensing

The iOS repo contains Supercell game assets (building icons, etc.). These are **copyrighted by Supercell**.

- **Personal use**: OK to use extracted assets
- **Play Store release**: You MUST recreate all icons or obtain Supercell permission
- See Supercell's [Fan Content Policy](https://www.supercell.com/fan-content-policy/)

## Key Implementation Details

### Boost Calculation Engine
Ported from `Models.swift:1210-1355`:
- Segment-based timeline calculation for overlapping boosts
- Clock Tower boosts don't stack (extend duration instead)
- Targeted boosts (Builder Apprentice, Lab Assistant) apply to specific upgrades
- Projects completion date accounting for future boost expiry

### Widget Update Strategy
- **Standard**: 15 minutes
- **Active boosts**: 1-5 minutes based on urgency
- **Near completion (<5min)**: 1 minute
- **At boost expiry**: Scheduled exactly at boost end time

### Data Flow
```
CoC JSON Export → CoCExportParser → BuildingUpgrade[] → Room DB
                                              ↓
                                    Glance Widgets ← WorkManager
                                              ↓
                                    Companion App (Compose)
```

## Project Structure

### Gradle Dependencies
- Jetpack Glance 1.0.0 (Widgets)
- Room 2.6.1 (Database)
- DataStore Preferences (Widget config)
- WorkManager 2.9.0 (Background updates)
- Hilt 2.48 (DI)
- Navigation Compose 2.7.6
- Coil 2.5.0 (Image loading)
- Material 3 / Compose

### Min SDK
- API 26 (Android 8.0) - 95%+ device coverage
- Glance supports API 26+

## Configuration

### Widget Setup
1. Long-press home screen → Widgets → Clashboard
2. Choose widget type (Builders, Closest Upgrade, War)
3. Configure profile and upgrade type
4. Resize as needed (Main widget supports 2×2, 4×2, 4×3)

### Quick Settings Tile
1. Swipe down twice for Quick Settings
2. Tap pencil/edit → Add "Import Clipboard" tile
3. Copy CoC JSON export → Tap tile → Profile created automatically

## TODO for Production

- [ ] Replace Supercell assets with custom icons
- [ ] Add official Supercell API integration (requires API key)
- [ ] Implement push notifications (FCM)
- [ ] Add backup/restore to Google Drive
- [ ] Tablet/foldable layout optimization
- [ ] Accessibility improvements
- [ ] Unit/integration tests
- [ ] CI/CD pipeline

## Credits

- Original iOS app: [Zacatac3/clash_widgets](https://github.com/Zacatac3/clash_widgets)
- Clash of Clans data: Supercell
- Android port: This project