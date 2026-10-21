# PE Tweaks

Companion app for PE-Revived: icon-shape picker and launcher tools.

- Kotlin + Jetpack Compose, Material 3 Expressive (`MaterialExpressiveTheme`,
  expressive motion, dynamic color on Android 12+).
- Shapes are applied through root: enable the RRO with
  `cmd overlay enable-exclusive`, clear the launcher icon cache, force-stop the
  launcher. Both the home screen and the drawer update.
- Shape previews are drawn with Compose `Canvas`, so they follow the wallpaper
  palette and stay sharp at any size.

## Build

Requirements: JDK 21 and Android SDK 37 (AGP 9.4, Kotlin 2.4, Compose BOM
2026.09).

```sh
cd tweaks-app
gradle :app:assembleRelease
```

Sign the unsigned APK with the module test key and copy it to
`apps/PE-Tweaks.apk`:

```sh
apksigner sign --ks ~/.pe-revived-testkey/testkey.jks --ks-key-alias test \
  --ks-pass pass:android --key-pass pass:android \
  --out PE-Tweaks.apk app/build/outputs/apk/release/app-release-unsigned.apk
cp PE-Tweaks.apk ../apps/PE-Tweaks.apk
```

All four installers install the APK at flash time.

## Layout

```
app/src/main/java/com/perevived/tweaks/
  MainActivity.kt          edge-to-edge Compose host
  data/Shapes.kt           shape list, overlay packages, glyph ids
  data/RootShell.kt        su helpers, overlay state, apply/reset
  ui/PETweaksApp.kt        home screen, sections, about
  ui/ShapesScreen.kt       shape grid and apply flow
  ui/ShapeGlyph.kt         Canvas previews
  ui/theme/Theme.kt        dynamic color + expressive geometry
```
