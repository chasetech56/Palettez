# AGENTS.md — Palettez handoff for coding agents

Read this file first, then `docs/PRD.md`. Together they are the full brief.

## What we are building

**Palettez** is a free, open-source, local-first Android app for designers with four tools:

1. **Palette** — extract 2–12 colors from a photo (HEX, RGB, CMYK, HSL), plus 14 preset palettes. Any palette opens as a **color system**: roles, 10-step scales, harmonies, WCAG contrast checks, CSS/Tailwind/JSON export.
2. **Grid** — column/row grids for any canvas size, plus **panel layouts** (15 templates in 5 families and a Shuffle generator).
3. **Fonts** — the full Google Fonts catalog, refreshed automatically, with previews, embed code and .ttf download.
4. **Onboarding and Settings.**

Images never leave the phone. The only network traffic is HTTPS to Google Fonts. No accounts, ads, analytics or crash SDKs.

## Source of truth, in order

1. `docs/PRD.md` — requirements with IDs (PA-, CS-, GR-, LA-, FO-, ON-, SE-) and "Done when" acceptance tests. Also in `docs/PRD.pdf`.
2. `design/screens/*.html` and `design/screenshots/*.png` — the approved "Bold edition" look. Match these, not stock Material 3.
3. `design/engines/*.js` — working reference implementations of the color system and the panel-layout engine. Port them to Kotlin and keep the outputs identical; write golden tests against them.
4. `android/` — the existing Kotlin/Compose project. It works structurally but still uses the old Material 3 look and lacks Presets, Layouts, Onboarding and Settings.

The online originals (editable, commentable) are the [design canvas](https://claude.ai/artifact/H3aU5sdMJQaUaUZatzTqNQ) and the [PRD doc](https://claude.ai/artifact/NGJ9WTHKRmgVbc2ozY6QWq). If they disagree with this zip, the online versions are newer.

## Decisions already made (do not reopen)

| Topic | Decision |
| --- | --- |
| Name | Palettez (verify Play Store and trademark availability before launch) |
| Platform | Android 8.0+ (minSdk 26), phones only for v1.0 |
| Stack | Kotlin, Jetpack Compose, Material 3 components restyled with Palettez tokens, Hilt, Room, DataStore, OkHttp, kotlinx.serialization |
| Look | "Bold edition": cream ground #EFE9DF, Anton display type, Figtree body, JetBrains Mono values, color-block cards, floating black pill tab bar |
| Dark mode | Use the Dark column of the token table in the PRD exactly |
| License | Open source, Apache 2.0, public GitHub repository |
| Privacy policy | GitHub Pages page from the repository; link in the Play listing and Settings → About |
| Team | One Android developer plus a part-time designer; launch target 16 March 2027 |
| Permissions | `INTERNET` only. Photo Picker, system camera app, Storage Access Framework for every export |

## Folder map

```
AGENTS.md                 this file
README.md                 human overview and how to open things
docs/PRD.md               product requirements (Markdown, diagrams in docs/prd-diagrams/)
docs/PRD.pdf              same, as PDF
design/screens/           every screen as a runnable HTML page; open index.html for the gallery
design/screens/dc-lite.js tiny renderer that makes the screen files run in a browser
design/screenshots/       PNG of every screen at 2x
design/canvas-source/     the original canvas files (.dc.html) and canvas.json layout
design/engines/           color-engine.js and layout-engine.js: reference logic to port
design/inspiration/       third-party reference shots; internal use only, do not commit
android/                  Android Studio project (Kotlin + Compose)
prototypes/               the first single-file web prototype (palette, grid, fonts)
```

## Screen index

| File | Screen | PRD section |
| --- | --- | --- |
| Main | Palette → From image | 2. Palette from image |
| PalettePresets | Palette → Presets | 3. Presets and color system |
| ColorSystem | Color system for one palette | 3 |
| ColorSystemSheet | Reference sheet: presets, scales, roles, contrast | 3 |
| Grid | Grid → Columns | 4. Grid: columns |
| GridLayouts | Grid → Layouts gallery | 5. Grid: panel layouts |
| LayoutEditor | Layout editor with Shuffle | 5 |
| LayoutSystem | Reference sheet: the five layout families | 5 |
| Fonts | Fonts catalog | 6. Fonts |
| FontDetail | Font detail | 6 |
| Onboarding1–3 | Welcome, Colors, Grids and fonts | 1. Onboarding |
| Settings | Settings | 7. Settings |

The screen files are mock-ups with sample data. Placeholders in square brackets (for example `[N] families`, `[size]`) are values the app computes at runtime.

## Suggested first tasks (milestone M0 in the PRD)

1. Rename the package from `com.designkit.app` to your final application id (for example `app.palettez`) and update `namespace`/`applicationId`.
2. Update Gradle, AGP, Kotlin and the Compose BOM to current versions; make the project compile.
3. Bundle Anton, Figtree and JetBrains Mono (SIL OFL) in `res/font`; build a `PalettezTheme` from the PRD token tables (light and dark).
4. Build shared components: floating pill tab bar, screen header, mode switch, choice chips, stepper tile, swatch card, primary CTA, toast.
5. Restructure into the modules listed in the PRD (`core/design`, `core/color`, `core/layout`, `core/data`, `feature/*`).
6. Port `design/engines/color-engine.js` to `core/color` and `layout-engine.js` to `core/layout` as pure Kotlin, with JVM unit tests that compare against the JS outputs (for example, the scale of #E4572E is `#FFF2EF #FFE2DA #FCC6B6 #F7A58E #EF8061 #E4572E #B84423 #873017 #581C0B #310C04`).

## Conventions

- Engines (`core/color`, `core/layout`) are pure Kotlin with no Android imports.
- Every requirement ID gets at least one test named after it (for example `CS6_scaleMatchesReference`).
- Touch targets are 48 dp minimum (the mock-ups draw 44 px).
- Text on any user color is ink (#151515) or white, whichever has the higher WCAG contrast.
- Never add analytics, ads or network calls to hosts other than the three Google Fonts hosts.

## Known caveats

- `android/` has never been compiled: it was written without access to Google's Maven repository. Expect to fix version or API mismatches on first sync.
- The font catalog uses an undocumented endpoint (`fonts.google.com/metadata/fonts`); keep the documented Developer API as a fallback (see PRD risks).
- `design/inspiration/` holds other people's work used only as mood references. Keep it out of the public repository (it is in `.gitignore`).
