# PE-Revived

**Pixel Launcher Extended, revived for Android 17.** A Magisk/KernelSU module by [**samakshkambxj**](https://github.com/samakshkambxj), continuing the work of [TeamFiles](https://github.com/TeamFiles) (upstream `saitamasahil/Pixel-Launcher-Extended` ended at v1.7 on Android 13). Original work by TeamFiles, GPL-3.0 (see `LICENSE`).

![Header Image](banner.png)

## Features

### Home screen
- Extra grid sizes (5x7, 5x8, 6x5–6x9) in the Layout picker
- Separate home columns and rows
- Lock layout
- Free widget resizing
- Wallpaper zooming toggle
- Icon labels on desktop
- Hide status bar, top shadow, page indicator (with dark variants)
- Hide At a Glance
- Disable dock, dock spacing
- Hide desktop search bar
- Material You At a Glance greetings
- Launcher Mods companion (icon packs, extra tools)

### App drawer
- App drawer columns and row height
- Drawer tabs (bottom, editable), quick launch
- Icon labels in drawer, drawer background opacity
- Hide scrollbar, search bar toggle
- Hide apps from drawer, search hidden apps, popup toggle

### Icons
- 1250+ themed icons, themed icon packs, force custom colors
- Icon packs with fallback order, per-app icons and themed icons
- 16 extra icon shapes + custom shape, size, and text size
- Rename apps and shortcuts, home-screen shortcut icons
- Remove shortcut badge, separate home-screen icons
- Edit, kill, and uninstall actions in the app popup

### Gestures and sleep
- Double tap to sleep (native in Android 17)
- Double-tap and pinch actions, hide gesture indicator and nav space

### Recents
- Live tile toggle, kill / uninstall / lock apps in recents
- Freeform gesture, clear-all button, fixed width, no screenshot button
- Selection toggle, memory info, background opacity

### Backup and restore
- Home layout, app drawer layout, and settings backup/restore

### Companion app
- PE Tweaks app with Material 3 Expressive UI
- Icon shape picker with live previews, applied to home screen and drawer
- Root and module status, launcher restart, icon cache rebuild

### Launcher internals
- Hidden flags, developer options
- New Wallpaper picker, Themes stub, Recents provider
- Prevent wallpaper-dimming restarts
- Offline / Online / Customize / Manual installers, local builder, WebUI controls

### Status on Android 17

| Feature | Working |
|---|---|
| Extra grid sizes | ✅ |
| Home columns and rows | ✅ |
| Lock layout | ❌ |
| Free widget resizing | ❌ |
| Wallpaper zooming toggle | ❌ |
| Icon labels on desktop | ❌ |
| Hide status bar / shadow / indicator | ❌ |
| Hide At a Glance | ❌ |
| Disable dock / spacing | ❌ |
| Hide desktop search bar | ❌ |
| At a Glance greetings | ✅ (static; dynamic needs runtime source) |
| Launcher Mods companion | ❌ |
| App drawer columns and row height | ✅ (shared with home grid) |
| Drawer tabs, quick launch | ❌ |
| Icon labels in drawer, opacity, scrollbar, search | ❌ |
| Hide apps, hidden search, popup toggle | ❌ |
| Themed icons and packs | ✅ |
| Icon packs with fallback, per-app icons | ❌ |
| Extra icon shapes (app + WebUI picker) | ✅ |
| PE Tweaks companion app | ✅ |
| Custom shape / size / text size | ❌ |
| Rename apps, shortcut icons, badge | ❌ |
| Popup edit / kill / uninstall | ❌ |
| Double tap to sleep | ✅ (native) |
| Double-tap / pinch actions | ❌ |
| Recents tweaks | ❌ |
| Backup and restore | ❌ |
| Hidden flags | ✅ |
| Developer options | ❌ |
| Wallpaper picker, Themes stub, Recents provider | ✅ |
| Installers, builder, WebUI | ✅ |

### Preview
<div align="center">
    <img src="screenshots/preview1.png">
</div>
<div align="center">
    <img src="screenshots/preview2.png">
</div>
<div align="center">
    <img src="screenshots/preview3.png">
</div>
<div align="center">
    <img src="screenshots/preview4.png">
</div>
<div align="center">
    <img src="screenshots/preview5.png">
</div>
<div align="center">
    <img src="screenshots/preview6.png">
</div>

## Compatibility
- **Android 17 (SDK 37) only.** Older Android is refused at install time.
- AOSP-based custom ROMs and Pixel stock ROMs with working GApps.
- Root via [Magisk](https://github.com/topjohnwu/Magisk/releases/latest) (stable) or [KernelSU](https://github.com/tiann/KernelSU) / KSU Next.
- No LSPosed needed: double-tap-to-sleep is native in Android 17.
- Proper GApps (Google app, Play Services, System Intelligence) for At a Glance/weather; without them use the [At a Glance Enhancer](#at-a-glance-enhancer) add-on.

**Disclaimer:** not responsible for anything that happens to your device. Keep a backup.

## Installation
1. Keep only the default launcher; remove other third-party launchers.
2. Download a zip from [Releases](https://github.com/samakshkambxj/PE-Revived/releases) (built automatically on every version tag).
3. Flash in Magisk / KernelSU and answer the volume-key prompts (Up = switch, Down = confirm). If your keys don't register, the installer aborts — use the Manual or Customize variant instead.
4. Reboot. Set **Pixel Launcher Extended** as Home in Settings > Apps > Default apps.
5. Open **PE Tweaks** and grant root when asked (Icons → Shape to pick an icon shape).
6. KernelSU users: in the manager's **App Profiles**, make sure **Unmount modules is OFF** for Pixel Launcher (and the wallpaper app), or the module's files stay invisible to the launcher. Grant root when companion apps ask.

## Variants
- **Offline Installer** (50+ MB): everything inside, no internet needed while flashing, volume-key choices.
- **Online Installer** (<10 MB): downloads only what you pick, needs internet while flashing (includes the PE Tweaks app).
- **Customize Installer**: you pre-pick the options when building the zip (no volume keys at flash time). Good for broken keys.
- **Manual Installer** (50+ MB): like offline, but options come from editing `config.txt` in the zip (see `manual_installer_instructions.md`).

During flashing you choose: At a Glance greetings styles, Launcher Mods app, extra icon shapes. The **PE Tweaks** app is installed automatically — open it and go to **Icons → Shape** to pick a shape.

## Double Tap to Sleep
Built into Android 17 — enable the system's double-tap option and it works on the home screen with no module or LSPosed needed.

## Activating Developer Options in Launcher
- Tap the phone's build number 7 times; launcher dev options unlock alongside the system ones.

## Uninstallation / Updating
- Uninstall from the root manager. To change options or update, flash the newer zip over the old one.

## Add-ons

### At A Glance Enhancer
Enhances At a Glance (doorbell, timer, fitness, torch suggestions…) and fixes weather/select issues on thin GApps. By [Kingsmanz](https://github.com/Kingsman44). Unverified on Android 17 — report results.
- [Download](https://www.pling.com/p/1938895/), flash, reboot.

## Download Links
- [GitHub Releases](https://github.com/samakshkambxj/PE-Revived/releases) (recommended, CI-built)
- Mirrors (upstream v1.7, Android 13 only): [Mirror 1](https://www.pling.com/p/1952604) · [Mirror 2](https://store.kde.org/p/1952604) · [Mirror 3](https://www.opendesktop.org/p/1952604)

## Building from source
```sh
git clone https://github.com/samakshkambxj/PE-Revived
cd PE-Revived
chmod +x builder_dependencies.sh && ./builder_dependencies.sh
```
Then type **PLE** and pick a variant (offline / online / customize / manual). Local porting notes live under `Modifications/`.

## Troubleshooting
- **Installer picks Yes everywhere / keys ignored:** volume detection failed; it now aborts instead — use Manual or Customize.
- **Flashed but nothing changed:** root manager may hide module files from apps (KernelSU App Profiles → disable "Unmount modules" for the launcher), then reboot.
- **Launcher crashes at boot after flashing:** grab `adb logcat -b crash -d` and report it with your ROM + variant.
- **Weather/Select in recents broken:** update Google app, Play Services, System Intelligence, WebView from Play Store; try the At a Glance Enhancer.
- **Material You stuck:** `su -c settings delete secure theme_customization_overlay_packages` (per-boot on some ROMs).
- **Mods app stuck at loading:** force-stop and relaunch it.

## Credits/Thanks
Google · LiArch55 · saitamasahil · iamlooper · KieronQuinn · uragiristereo · Monet Icons Team · siavash79 · Kingsman44 · anarchist22 · WaifuPX-DG · LawnchairLauncher · RadekBledowski · PalmDevs · agam778 · atharvap8 · jaaat4u · selfmusing · Sameera · naokoshoto · Ryuk · micky387 — and TeamFiles for the original. PE-Revived continuation by [samakshkambxj](https://github.com/samakshkambxj).

## Contact me
[![Telegram](https://img.shields.io/badge/Telegram-TeamFiles-%2326A5E4?logo=Telegram)](https://telegram.me/veyn64)

