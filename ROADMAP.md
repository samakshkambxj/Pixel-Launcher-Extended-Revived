# PE-Revived roadmap

Android 17 only. This document tracks what is shipped, what is being built
right now, and what is deliberately parked.

Last updated: 2026-10-21 · v2.0.0 line

## Shipped

- Launcher port on the Android 17 base (no dex edits — stock `classes*.dex`
  with merged resources/assets).
- 11 grid options: 5×7, 5×8, 6×5–6×9 plus the stock three.
- 16 icon-shape overlays, rebuilt for SDK 37 and installed at flash time.
- At a Glance greetings (static, 22 locales).
- Themed icons overlay (1250+ icons).
- Four installers: Offline, Online, Manual (`config.txt`), Customize.
- KernelSU / KSU Next fixes (volume-key selector, mount helpers, ARCH32).
- WebUI shape picker with SVG previews.
- CI builds all installer zips and uploads them to the tag's release.
- PE Tweaks 2.0 app: Material 3 Expressive shell, shape picker with drawn
  previews, root/module status, launcher restart and icon-cache rebuild.

## Now — PE Tweaks 2.0

- [x] Replace the hand-written smali UI with a real Compose app
      (`tweaks-app/`, built with the Gradle/AGP toolchain).
- [x] Icons → Shape section: 17 tiles, current selection highlighted.
- [x] Apply = `cmd overlay enable-exclusive` + clear `app_icons.db*` +
      force-stop launcher, so home screen and drawer both update.
- [x] Status section: root, applied shape, themed-icon overlay, module version.
- [x] Install the app from all four installers.
- [x] Home screen → Desktop icon labels switch, through the launcher's
      `grid_control` provider (`/set_workspace_items_label_hidden`).
- [x] Themed icons switch through the same provider (`/set_icon_themed`).
- [x] App grid picker: reads `/list_options`, applies `/default_grid`, so the
      in-app list always matches the grids built into the module.
- [x] App drawer → Two-line app labels switch. Writes
      `pref_enable_two_line_toggle` into the launcher's own
      `com.android.launcher3.prefs.xml` (the file its settings screen uses),
      stopping the launcher first so it cannot flush a stale value back.
- [ ] Device pass: verify root prompt, selection read-back, and that a shape
      survives a real reboot.
- [ ] WebUI: link to the app when installed, keep the shape picker as fallback.

## Next — settings that need no launcher code

Goal: restore the toggles that used to live in the removed Extended Settings by
writing the launcher's own preferences instead of patching smali.

Approach:
1. Decode the modded launcher and map each feature to its `LauncherPrefs` key
   and value format (for example `idp_grid_name`).
2. Expose it in PE Tweaks, write the pref as root, force-stop the launcher.
3. Ship only after the toggle both applies and reverts cleanly.

Verified paths (Android 17 build):

- Provider `content://com.google.android.apps.nexuslauncher.grid_control`:
  `/workspace_items_label_hidden` (read), `/set_workspace_items_label_hidden`,
  `/icon_themed`, `/set_icon_themed`, `/list_options`, `/default_grid`.
  Root (`UID 0`) passes the provider's permission check, so no signature is
  needed.
- `com.android.launcher3.prefs.xml` in the launcher's CE data directory is the
  same file its settings screen writes (`SettingsActivity` points the
  preference manager at `com.android.launcher3.prefs`). Plain XML booleans.
  Force-stop → edit → restore owner/SELinux label → let the user press Home.

Candidates, in order of confidence:

- Desktop and drawer icon labels
- Dock on/off and dock spacing
- Search bar visibility
- Status bar, top shadow, page indicator (with dark variants)
- Drawer background opacity and row height
- Lock layout, free widget resizing, wallpaper zooming
- Popup actions (edit / kill / uninstall)

Rules: every toggle needs a read-back in the app, a revert path, and a note in
the README status table.

## Then — features that need runtime components

- Backup and restore of launcher layout plus preferences (app can archive
  `shared_prefs` and databases, then re-inject).
- Icon packs with fallback order and per-app icons (needs a provider-style
  component; the old mods app is discontinued).
- Drawer tabs and quick launch.
- Dynamic At a Glance greetings: the greeting strings are static assets, so a
  time-of-day rotation needs a runtime source. Parked until there is a clean
  way to trigger it.
- Custom shape, icon size, and text size: requires per-icon rendering, not RRO.

## Blocked / not planned right now

- **Launcher smali edits.** Rebuilding the launcher with apktool zeroes parts of
  the dex on this machine. Current releases ship the stock dex with merged
  resources, which is stable. Revisit only with a full source build.
- **Separate home and drawer grids.** The drawer follows the workspace specs in
  this launcher; a real split needs runtime code.
- **Revamped search bar / OneSearch.** The `persist.sys.velvet.force_onesearch`
  property had no effect on this build.
- **Wallpaper carousel.**
- **Per-app icon shapes.** Overlays are per-target, not per-app.

## Release checklist

1. Build the app: `cd tweaks-app && gradle :app:assembleRelease` (JDK 21,
   SDK 37), sign, verify, copy to `apps/PE-Tweaks.apk`.
2. Bump `module.prop`, `update.json`, and the app `versionCode`.
3. Run the builder or tag a release; CI assembles the four zips.
4. Smoke-test on a clean flash: grid picker, shape apply, greetings, themed
   icons, app install, uninstall.
5. Update the README status table.
