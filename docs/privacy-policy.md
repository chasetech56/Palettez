# Privacy Policy for Palettez

**Last updated:** November 21, 2026

**Palettez** is a free, open-source, local-first Android application designed for creators and designers. We believe your creative work and device data belong strictly to you.

---

### 1. Zero Tracking, Zero Analytics, Zero Ads
- **No Accounts Required**: You never need an account, username, email, or password to use Palettez.
- **No Third-Party Trackers**: Palettez contains no tracking libraries, no advertising SDKs, and no analytics frameworks.
- **No Crash Diagnostic Transmissions**: Palettez does not report crash logs or diagnostic telemetries to external servers.

---

### 2. Photos and Colors Stay On Your Device
- **On-Device Color Extraction**: When you pick an image via the Android Photo Picker or take a photo with your device camera, the image is decoded and clustered strictly inside your phone's memory.
- **Zero Photo Uploads**: Your images, photos, camera captures, and moodboards are never uploaded, sent, or mirrored to any server or cloud service.
- **Camera Access**: Camera captures use the system camera application (`TakePicturePreview` activity contract). Palettez declares zero `CAMERA` permissions in its manifest. Temporary camera captures go to your app cache and are removed after processing.

---

### 3. Network Usage
- **Google Fonts Metadata & Previews**: Palettez connects to the internet solely via encrypted HTTPS to official Google Fonts endpoints (`fonts.google.com` and `fonts.gstatic.com`) and our static GitHub Pages release manifest (`chasetech56.github.io/Palettez/version.json`).
  - Fetches the open font catalog and typeface metadata.
  - Downloads `.ttf` font files that you explicitly preview, save, or export.
  - Checks if a newer Palettez APK build is available (carrying zero user or device identifiers).
- Palettez initiates **no network requests** to any other host or domain.

---

### 4. Local Storage & Backups
- **Device Preferences**: Theme choices, color count defaults, and grid settings are saved on-device using Jetpack DataStore.
- **Recent Palettes**: Colors extracted from images and their thumbnails are stored privately in the application's internal files directory (`context.filesDir`). You can delete individual recent entries or clear all recents at any time from within the app.
- **Font Cache**: Downloaded fonts are cached locally in your app cache directory. You can inspect the storage size and clear all cached fonts with a single tap in **Settings → Storage**.
- **File Exports**: When you export CSS variables, SVG grids, JSON color systems, or PNG palette files, they are written strictly to locations you choose via the Android Storage Access Framework.

---

### 5. Open Source Transparency
Palettez is licensed under the **Apache License 2.0**. The source code is publicly accessible, allowing anyone to inspect, verify, and audit our privacy commitments and network activity.

---

### 6. Contact
If you have questions, feedback, or need assistance regarding this privacy policy, please open an issue on our [GitHub repository](https://github.com/chasetech56/Palettez).
