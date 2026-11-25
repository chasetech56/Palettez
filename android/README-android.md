# Design Kit for Android

A free, local-first design toolkit built with Kotlin, Jetpack Compose and Material 3.

| Tab | What it does |
|---|---|
| **Palette** | Pick a photo, take one with the camera, or share an image to the app from your gallery. Extracts 2–12 dominant colors on-device (k-means), showing coverage %, HEX, RGB, CMYK and HSL. Tap any value to copy it. Copy everything as HEX, CSS variables or a full list, or save a palette PNG. |
| **Grid** | Layout grids for presets (Instagram, Story, slides, web, phone, A4, Letter, poster) or any custom size. Columns, rows, gutter, baseline, margins, colors (including your last palette), fill opacity, center lines, rule of thirds and an image underlay. Export PNG or SVG, or copy a CSS grid. Settings are remembered. |
| **Fonts** | The full Google Fonts catalog, refreshed each time the app opens so new releases appear automatically (marked NEW for 60 days). Search, filter by category, sort by newest/popular/A–Z, preview your own text, see every weight, copy the `<link>`/CSS, or save the .ttf files. |

## Privacy
- Images never leave your phone. The photo picker means the app needs no storage permission.
- The only network access is to Google Fonts: `fonts.google.com` (catalog), `fonts.googleapis.com` and `fonts.gstatic.com` (font files). Fonts are cached on the device after the first download.

## Build the APK
1. Install the latest **Android Studio**.
2. **File → Open** and choose this `DesignKit` folder. Let Gradle sync (it downloads the Android plugin and libraries the first time).
   - If Studio offers to upgrade the Android Gradle Plugin or libraries, accepting is fine.
3. Plug in your phone with USB debugging on and press **Run ▶**, or build an installable file with
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The APK lands in `app/build/outputs/apk/`.
4. For a smaller, faster build use the `release` variant (**Build → Select Build Variant → release**). It's signed with
   your local debug key so you can install it on your own phone; use your own keystore before publishing to Play Store.

Command line alternative (with the Android SDK installed): `./gradlew assembleRelease`

## Requirements
- Android 8.0 (API 26) or newer. On Android 12+ the app follows your wallpaper colors (Material You dynamic color).

## Project layout
```
app/src/main/java/com/designkit/app/
├── MainActivity.kt          entry point, handles images shared from other apps
├── ui/DesignKitApp.kt       Material 3 scaffold, top app bar, navigation bar, snackbar
├── ui/theme/Theme.kt        dynamic color + fallback light/dark schemes
├── palette/                 color math, k-means extractor, view model, screen
├── grid/                    grid model (draw/PNG/SVG/CSS), view model, screen
├── fonts/                   Google Fonts catalog + font file loader, view model, screen
└── util/Platform.kt         clipboard, image decoding, file writing
```
