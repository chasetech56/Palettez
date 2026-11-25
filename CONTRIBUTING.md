# Contributing to Palettez

Thank you for considering contributing to **Palettez**! Palettez is a free, open-source, local-first Android toolkit for designers.

## Ground Rules (Non-Negotiable)

1. **Zero tracking, zero ads, zero accounts.** Never add analytics, telemetry, crash reporting SDKs, or ad networks.
2. **Strict network boundary.** The only allowed outbound hosts are `fonts.google.com`, `fonts.googleapis.com`, and `fonts.gstatic.com`. Images and user data must never leave the device.
3. **Minimum permissions.** Only `INTERNET` is declared in `AndroidManifest.xml`. Use Photo Picker, `TakePicture`, and Storage Access Framework (`CreateDocument`) instead of storage/camera permissions.
4. **Bold Edition design system.** Match `docs/PRD.md` tokens and `design/screens/` rather than stock Material 3 defaults.

## Getting Started

1. Clone the repository and open `android/` in Android Studio (Ladybug or newer) with JDK 17.
2. Run the JVM unit test suite:
   ```bash
   cd android
   ./gradlew test
   ```
3. Verify Roborazzi screenshots:
   ```bash
   ./gradlew verifyRoborazziDebug
   ```
   If your change intentionally alters UI, update the golden snapshots with:
   ```bash
   ./gradlew recordRoborazziDebug
   ```

## Conventions

- Pure math and layout engines (`ColorEngine`, `LayoutEngine`, `PaletteExtractor`, `FontPairingEngine`, `GridAnalysisEngine`) belong in pure Kotlin without Android UI imports where possible, paired with JVM unit tests named after the PRD requirement ID (e.g., `CS6_scaleMatchesReference`).
- Minimum touch target size is `48 dp`.
- Text on any user color must be ink (`#151515`) or white (`#FFFFFF`), whichever yields higher WCAG contrast.

## Pull Request Checklist

- [ ] `./gradlew test` passes cleanly
- [ ] `./gradlew verifyRoborazziDebug` passes cleanly (or updated snapshots are included)
- [ ] No new manifest permissions or third-party tracking dependencies
- [ ] Code adheres to our [Code of Conduct](CODE_OF_CONDUCT.md)
