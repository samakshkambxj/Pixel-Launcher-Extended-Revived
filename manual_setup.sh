#!/sbin/sh

###########################
# MMT Reborn Logic
###########################

############
# Config Vars
############

# Set this to true if you want to skip mount for your module
SKIPMOUNT="false"
# Set this to true if you want to clean old files in module before injecting new module
CLEANSERVICE="false"
# Set this to true if you want to load vskel after module info print. If you want to manually load it, consider using load_vksel function
AUTOVKSEL="true"
# Set this to true if you want store debug logs of installation
DEBUG="false"

############
# Permissions
############

# Set permissions
set_permissions() {
  set_perm_recursive "$MODPATH" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/app/PixelThemesStub2022_and_newer" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/app/ThemesStub" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/etc/default-permissions" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/etc/permissions" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/etc/sysconfig" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/overlay" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/overlay/PixelLauncherModsOverlay" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/overlay/PixelRecentsProvider" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/overlay/ThemedIconsOverlay" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/priv-app/PixelLauncherMods" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/product/priv-app/PixelLauncherMods/lib/arm64" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/system_ext/etc/permissions" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/system_ext/priv-app/NexusLauncherRelease" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/system_ext/priv-app/QuickAccessWallet" 0 0 0777 0755
  set_perm_recursive "$MODPATH/system/system_ext/priv-app/WallpaperPickerGoogleRelease" 0 0 0777 0755
}

############
# Info Print
############

# Set what you want to be displayed on header of installation process
info_print() {
  # Get the Android SDK version
  sdk_version=$(getprop ro.build.version.sdk)

  # PE-Revived ships an Android 17 base (minSdk 37)
  if [ $sdk_version -lt 37 ]; then
    # Fail the script immediately
    echo "Error: PE-Revived needs Android 17 (SDK 37), found SDK $sdk_version"
    exit 1
  fi

  ui_print ""
  ui_print "**********************************************"
  ui_print "• PE-Revived"
  ui_print "• By samakshkambxj, based on PLE by TeamFiles"
  ui_print "**********************************************"
  ui_print ""

  sleep 2
}

############
# Replace List
############

# List all directories you want to directly replace in the system
# Construct your list in the following example format
REPLACE_EXAMPLE="
/system/app/Youtube
/system/priv-app/SystemUI
/system/priv-app/Settings
/system/framework
"
# Construct your own list here
REPLACE="
/system/priv-app/AsusLauncherDev
/system/priv-app/Lawnchair
/system/priv-app/NexusLauncherPrebuilt
/system/product/priv-app/ParanoidQuickStep
/system/product/priv-app/ShadyQuickStep
/system/product/priv-app/TrebuchetQuickStep
/system/product/overlay/PixelLauncherIconsOverlay
/system/product/overlay/CustomPixelLauncherOverlay
/system/system_ext/priv-app/NexusLauncherRelease
/system/system_ext/priv-app/TrebuchetQuickStep
/system/system_ext/priv-app/Launcher3QuickStepGoogle
/system/system_ext/priv-app/Launcher3QuickStepMock
/system/system_ext/priv-app/Lawnchair
/system/system_ext/priv-app/PixelLauncherRelease
/system/system_ext/priv-app/Launcher3QuickStep
/system/system_ext/priv-app/ArrowLauncher
/system/system_ext/priv-app/WallpaperPickerGoogleRelease
/system/product/overlay/ThemedIconsOverlay.apk
/system/product/overlay/PixelLauncherIconsOverlay.apk
/system/product/overlay/CustomPixelLauncherOverlay.apk
/system/product/overlay/MiuiCameraOverlay/MiuiCameraOverlay.apk
/system/product/overlay/MiuiGalleryOverlay/MiuiGalleryOverlay.apk
"

############
# Helpers
############

# Install icon-shape overlays as user apps so they register
# regardless of module mount timing. Reinstall keeps current selection.
install_icon_shapes() {
  ui_print "Installing icon shape overlays..."
  for apk in "$MODPATH/shapes"/TeamFiles_we_*.apk; do
    pm install -r "$apk" >/dev/null 2>&1
  done
}

# Companion app: icon-shape picker and launcher tools. Installed as a user
# app so it survives module updates and can request root on demand.
install_tweaks_app() {
  if [ -f "$MODPATH/apps/PE-Tweaks.apk" ]; then
    ui_print "Installing PE Tweaks app..."
    pm install -r "$MODPATH/apps/PE-Tweaks.apk" >/dev/null 2>&1
  fi
}

############
# Main
############

# Change the logic to whatever you want
init_main() {
  ui_print ""
  ui_print "[*] Installing..."
  ui_print ""

  # Loop over each line in config.txt
  while read -r line; do
    # Skip blank lines and comments
    case "$line" in ""|"#"*) continue ;; esac
    # Extract the variable name and value
    var_name=$(echo "$line" | cut -d '=' -f 1)
    var_value=$(echo "$line" | cut -d '=' -f 2)

    # Check the variable name and execute different commands based on the value
    case $var_name in
    LAUNCHER)
      if [ "$var_value" = "0" ]; then
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Dark.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Dark2.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Empty.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Empty2.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Light.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Light2.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Light_Accent.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_Pill_Light_Accent2.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_UserChip.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_UserCL.apk"
        rm -rf "$MODPATH/system/product/overlay/TeamFiles_UserLockscreen.apk"
      elif [ "$var_value" = "1" ]; then
        :
      else
        echo "Invalid value of LAUNCHER in config.txt (use 0 or 1)"
        exit 1
      fi
      ;;
    Pixel_Launcher_Mods_App)
      if [ "$var_value" = "0" ]; then
        rm -rf "$MODPATH/system/product/priv-app/PixelLauncherMods"
        rm -rf "$MODPATH/system/product/etc/permissions/privapp-permissions-com.kieronquinn.app.pixellaunchermods.xml"
        rm -rf "$MODPATH/system/product/overlay/PixelLauncherModsOverlay"
      elif [ "$var_value" = "1" ]; then
        :
      else
        echo "Invalid value of Pixel_Launcher_Mods_App in config.txt"
        exit 1
      fi
      ;;
    Icon_Shapes)
      if [ "$var_value" = "0" ]; then
        :
      elif [ "$var_value" = "1" ]; then
        install_icon_shapes
      else
        echo "Invalid value of Icon_Shapes in config.txt"
        exit 1
      fi
      ;;
    *)
      echo "Unknown variable name in config.txt"
      exit 1
      ;;
    esac
  done <"$MODPATH/config.txt"

  sleep 0.5

  ui_print ""
  ui_print "Finished Installing PE-Revived"

  install_tweaks_app

  ui_print "[*] Clearing system cache to make it work properly..."
  ui_print ""

  rm -rf "/data/system/package_cache"

  sleep 0.5

  ui_print "[*] Done!"
  ui_print ""

  sleep 0.5

  ui_print " --- Notes --- "
  ui_print ""
  ui_print "[*] Reboot is required"
  ui_print ""
  ui_print "[*] Report issues to @fileschat on Telegram"

  sleep 2

  touch "$MODPATH/first"

}
# KernelSU sources customize.sh directly and never calls init_main (that
# call lives in the Magisk update-binary). Run the install here when this
# script runs outside the Magisk flow.
if [ -n "$KSU" ]; then
  info_print
  init_main
fi
