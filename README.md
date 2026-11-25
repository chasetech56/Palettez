# Palettez

**Colors, grids and type, free on your phone.**  
An open-source, local-first Android toolkit for designers: extract palettes from photos with recents history, explore 15 curated preset moods, build 10-step OKLab color systems, design column/row grids and 15 panel layouts with procedural PRNG shuffle, and browse the full Google Fonts library with offline caching and specimen downloads.

No accounts, no ads, no tracking.

---

## Features

### 1. Palette & Color Systems
- **On-Device Photo Extraction**: K-means++ clustering extracts 2–12 dominant swatches entirely in memory.
- **Recents**: Automatically saves previously extracted image palettes and thumbnails so you can instantly return to them.
- **15 Presets across 6 Moods**: Earthy, Editorial, Pop, Minimal, Retro, and Neon.
- **Color System Studio**:
  - Auto-assigned roles: Background, Surface, Accent, Primary, Ink with WCAG 2.1 AAA luminance grading.
  - 10-step OKLab lightness scales for every palette color.
  - Harmonies generator (Complementary, Analogous, Triadic, Split-Complementary).
  - One-tap export to CSS Variables, Tailwind config, JSON tokens, and high-res PNG.

### 2. Grid & Panel Layouts
- **Column & Row Grids**: Pixel-precise layout grids for Square, Portrait, Story, Landscape, Poster, and custom artboard sizes.
- **15 Quadrilateral Templates**: 5 distinct design families (Equal, Golden, Diagonal, Modular, Asymmetric).
- **Procedural Shuffle Generator**: Mulberry32 PRNG generates infinite reproducible, deterministic panel layouts across 4–9 panels.
- **SVG Export**: Direct download or sharing of ready-to-import layout vectors.

### 3. Google Fonts Browser & Specimen Studio
- **Full Catalog (1,960+ Families)**: Categorized by Sans, Serif, Display, Handwriting, and Monospace.
- **Local Cache & Offline Mode**: Seamless browsing whether online or offline; filter by popularity, recent additions (60-day window), or A–Z.
- **Specimen Studio**: Lime specimen hero card, custom live preview strings, weight/style cards, and direct `.ttf` & `.zip` downloads.

### 4. Settings & Onboarding
- **Bold Edition Design**: Cream ground (`#EFE9DF`), Dark ground (`#1D2320`), floating pill tab bar, Anton display headlines, Figtree body typography, and JetBrains Mono code values.
- **3-Step Interactive Onboarding**: Welcome carousel with live theme preview.
- **Customizable Defaults**: Extraction color count, default copy format (HEX, RGB, CMYK, HSL), grid memory, and font cache management.

---

## Architecture & Tech Stack

- **Platform**: Android 8.0+ (minSdk 26, targetSdk 35, compileSdk 35).
- **Language**: Kotlin 2.1.0 with 100% Jetpack Compose UI (BOM 2024.12.01).
- **Theme**: "Bold Edition" design token system supporting dynamic Light & Dark themes with edge-to-edge system bar controls.
- **Storage**:
  - Jetpack DataStore Preferences for user settings and flags.
  - Local-first JSON repository for recent image palettes and cached thumbnails.
  - Storage Access Framework for private SAF document exports (PNG, SVG, CSS, TTF).
- **Networking**: OkHttp 4.12.0 strictly scoped to official Google Fonts endpoints (`fonts.google.com`, `fonts.gstatic.com`).
- **Release Optimization**: R8 minification and resource shrinking resulting in a **1.6 MB APK** (well below the 15 MB budget) and **3.4 MB AAB**.

---

## Building & Testing

### Prerequisites
- JDK 17 (e.g. OpenJDK 17)
- Android SDK with platform tools (API 35)

### Build Commands
```bash
# Set Java 17
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

# Run all JVM unit tests (27 passing tests)
./gradlew test

# Build debug APK and install to connected device / emulator
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk (1.6 MB)

# Build Google Play App Bundle
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab (3.4 MB)
```

---

## Privacy Policy

Palettez collects zero personal data, tracks no telemetry, uses no ads, and never sends images off the device. See our full [Privacy Policy](docs/privacy-policy.md).

---

## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).  
Bundled UI typefaces (Anton, Figtree, JetBrains Mono) are licensed under the SIL Open Font License (OFL).
