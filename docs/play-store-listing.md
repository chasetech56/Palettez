# Palettez — Google Play Store Listing & Launch Kit

Ready-to-paste copy and form answers for when the Google Play Console account is active.

---

## 1. Store Listing Copy

- **App Name (max 30 chars):**
  `Palettez: Colors, Grids & Type` *(30 chars)*

- **Short Description (max 80 chars):**
  `Extract palettes, build 10-step color systems, grids & Google Fonts. No ads.` *(77 chars)*

- **Full Description (max 4,000 chars):**
  ```
  Colors, grids and type, free on your phone. No account, no ads, no tracking.

  Palettez is a free, open-source, local-first Android toolkit built for graphic designers, brand creators, UI designers, and comic/zine makers.

  1. PALETTE FROM PHOTO & COLOR SYSTEMS
  • Extract 2–12 dominant colors from any photo on-device in under 2 seconds (HEX, RGB, CMYK, HSL).
  • Open any extracted or preset palette as a full color system: auto-assigned roles (Background, Text, Primary, Secondary, Accent), 10-step OKLab shade scales, color harmonies, and live WCAG 2.2 contrast checking.
  • Export presentation-ready Brand Identity cards (16:9 slide or 1:1 square) as high-res PNGs, or copy CSS variables, Tailwind config, and JSON tokens.
  • Recent image palettes stay saved locally on your phone for instant recall.

  2. COLUMN GRIDS, PANEL LAYOUTS & POSTER ANALYSIS
  • Generate column/row grids for Instagram, Stories, Slides, Desktop, A4, US Letter, and Posters (or custom sizes up to 4096 px) with PNG, SVG, and CSS Grid export.
  • Explore 15 curated Panel Layouts across 5 families (Classic, Stacks, Slants, Insets, Mosaic), or hit Shuffle to generate balanced layouts with mirror and slant controls.
  • On-Device Grid Analysis: Upload any poster or flyer to reverse-engineer its columns, margins, gutters, and layout family locally—then open it straight in the Grid or Layout Editor.

  3. GOOGLE FONTS CATALOG & PAIRINGS
  • Browse the full, always-current Google Fonts catalog with live text previews, size sliders, and category/popularity sorting.
  • Inspect every weight and style, discover curated Font Pairings in an interactive Split Specimen Studio with Role Swap (⇄), copy HTML/CSS embed links, or save .ttf files directly to your device.

  PRIVACY PROMISE
  Images never leave your phone. Palettez uses Android Photo Picker, system camera, and Storage Access Framework with zero storage/camera permissions, zero accounts, zero analytics, and zero ads. The only outbound network traffic is HTTPS directly to Google Fonts.

  Open source under Apache 2.0.
  ```

- **App Category:** `Art & Design`
- **Tags:** `Design`, `Color palette`, `Typography`, `Layout`
- **Contact Email:** *(Add developer support email)*
- **Privacy Policy URL:** `https://chasetech56.github.io/Palettez/privacy-policy.html`

---

## 2. Play Console Data Safety Form

- **Does your app collect or share any of the required user data types?**
  -> **No** ("No data collected" and "No data shared").
- **Why?** Images stay in device memory/cache; preferences, saved palettes, and saved layouts stay on-device in DataStore/Room. The only network calls are anonymous HTTPS requests to `fonts.google.com`, `fonts.googleapis.com`, and `fonts.gstatic.com`.
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes** (`cleartextTrafficPermitted="false"`).

---

## 3. Content Rating (IARC)

- **Category:** `Utility, Productivity, Communication, or Other`
- **Violence / Sexuality / Language / Controlled Substances / User Interaction / Shares Location / Digital Purchases:** **No** to all.
- **Expected Rating:** `Everyone` / `PEGI 3`.

---

## 4. Release Notes (`v1.1.0` — Build 2)

```xml
<en-US>
• New: On-Device Grid Analysis — upload any poster or flyer to estimate columns, margins, gutters, and matching panel layout families locally.
• New: Brand Identity Specimen Export — export 16:9 or 1:1 presentation PNGs with HEX, RGB, CMYK, HSL, OKLab values, and WCAG contrast badges.
• New: Font Pairing Suggestions & Split Specimen Studio with role swap and combined embed export.
• Improved: Image Recents shelf on the Palette tab and refined Dark Mode contrast.
</en-US>
```

---

## 5. Build Artifacts Checklist

- Signed Release App Bundle (`.aab`): `android/app/build/outputs/bundle/release/app-release.aab`
- Signed Release APK (`.apk`): `android/app/build/outputs/apk/release/app-release.apk`
- Upload Keystore (gitignored, keep backed up!): `android/keystore/palettez-upload.jks`
