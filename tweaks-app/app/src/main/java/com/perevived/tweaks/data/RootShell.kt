package com.perevived.tweaks.data

import java.io.File
import java.util.concurrent.TimeUnit

data class ShellResult(val code: Int, val output: String) {
    val ok: Boolean get() = code == 0
}

/** Everything the home screen needs, read in a single root call. */
data class LauncherState(
    val hasRoot: Boolean,
    val shapePackage: String?,
    val themedIcons: Boolean?,
    val labelsHidden: Boolean?,
    val moduleVersion: String?,
    val moduleInstalled: Boolean,
    val gridTitle: String?,
    /** Raw values of every launcher preference the app can switch. */
    val prefValues: Map<String, Boolean>,
    /** Android's global notification-dot setting, null when it could not be read. */
    val notificationDots: Boolean?,
    /** Which section the long-press menu expands first: SYSTEM or DEEP. */
    val popupSection: String?,
    /**
     * KernelSU's Kernel Umount feature. When true, module mounts are stripped from
     * app processes and Pixel Launcher cannot read its own APK. Null when the
     * device is not running KernelSU.
     */
    val kernelUmountEnabled: Boolean?,
)

/** One entry from the launcher's grid options list. */
data class GridOption(
    val name: String,
    val title: String,
    val cols: Int,
    val rows: Int,
    val isDefault: Boolean,
)

/**
 * Thin root shell for the handful of privileged operations the launcher tweaks
 * need. Everything runs through `su -c`, so Magisk / KernelSU decide whether to
 * grant it. State is read in one batch to avoid a stack of root prompts.
 */
object RootShell {

    private const val TIMEOUT_SECONDS = 25L
    private const val SEPARATOR = "@@pe@@"

    fun su(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = Thread {
                runCatching {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        synchronized(output) { output.appendLine(line) }
                    }
                }
            }
            reader.isDaemon = true
            reader.start()

            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ShellResult(1, output.toString())
            }
            reader.join(2_000)
            ShellResult(process.exitValue(), output.toString())
        } catch (t: Throwable) {
            ShellResult(1, t.message ?: "su unavailable")
        }
    }

    fun loadState(): LauncherState {
        val result = su(
            "id -u; echo $SEPARATOR; cmd overlay list --user 0; echo $SEPARATOR; " +
                "cat ${moduleProp().absolutePath} 2>/dev/null; echo $SEPARATOR; " +
                "test -d /data/adb/modules/$MODULE_ID && echo installed; echo $SEPARATOR; " +
                "content query --uri $PROVIDER/workspace_items_label_hidden; echo $SEPARATOR; " +
                "content query --uri $PROVIDER/icon_themed; echo $SEPARATOR; " +
                "content query --uri $PROVIDER/list_options; echo $SEPARATOR; " +
                "cat $LAUNCHER_PREFS 2>/dev/null; echo $SEPARATOR; " +
                "settings get secure notification_badging; echo $SEPARATOR; " +
                "if command -v ksud >/dev/null 2>&1; then " +
                "ksud feature get kernel_umount 2>/dev/null; else echo no-ksu; fi",
        )

        val parts = result.output.split(SEPARATOR)
        val uid = parts.getOrNull(0)?.trim().orEmpty()
        val hasRoot = uid == "0"

        val overlays = parts.getOrNull(1).orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("[x]") }
            .map { it.removePrefix("[x]").trim() }
            .toList()

        val shape = Shapes.overlayPackages.firstOrNull { pkg -> overlays.contains(pkg) }

        val version = parts.getOrNull(2).orEmpty()
            .lineSequence()
            .firstOrNull { it.startsWith("version=") }
            ?.removePrefix("version=")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val installed = parts.getOrNull(3).orEmpty().contains("installed")
        val themed = parseBoolean(parts.getOrNull(5))
        val labelsHidden = parseBoolean(parts.getOrNull(4))
        val grids = parseGrids(parts.getOrNull(6))
        val prefsXml = parts.getOrNull(7).orEmpty()

        return LauncherState(
            hasRoot = hasRoot,
            shapePackage = shape,
            themedIcons = themed ?: overlays.contains(THEMED_ICONS_OVERLAY),
            labelsHidden = labelsHidden,
            moduleVersion = version,
            moduleInstalled = installed,
            gridTitle = grids.firstOrNull { it.isDefault }?.title,
            prefValues = KNOWN_PREFS.mapNotNull { key ->
                readPrefFromXml(prefsXml, key)?.let { key to it }
            }.toMap(),
            notificationDots = parseSecureBoolean(parts.getOrNull(8)),
            popupSection = readStringPrefFromXml(prefsXml, PREF_POPUP_SECTION),
            kernelUmountEnabled = parseKernelUmount(parts.getOrNull(9)),
        )
    }

    fun loadGrids(): List<GridOption> =
        parseGrids(su("content query --uri $PROVIDER/list_options").output)

    private fun parseGrids(block: String?): List<GridOption> {
        return block.orEmpty().lineSequence().mapNotNull { line ->
            if (!line.startsWith("Row:")) return@mapNotNull null
            fun field(key: String) =
                Regex("$key=([^,]+)").find(line)?.groupValues?.get(1)?.trim()

            val name = field("name") ?: return@mapNotNull null
            GridOption(
                name = name,
                title = field("grid_title") ?: name,
                cols = field("cols")?.toIntOrNull() ?: 0,
                rows = field("rows")?.toIntOrNull() ?: 0,
                isDefault = field("is_default") == "1",
            )
        }.toList()
    }

    private fun readPrefFromXml(xml: String, key: String): Boolean? {
        val match = Regex("name=\"$key\" value=\"(true|false)\"").find(xml) ?: return null
        return match.groupValues[1] == "true"
    }

    private fun readStringPrefFromXml(xml: String, key: String): String? =
        Regex("<string name=\"$key\">([^<]*)</string>").find(xml)?.groupValues?.get(1)

    /**
     * Write one boolean into the launcher's own preference file. The launcher is
     * stopped first so it cannot flush stale values over the change, and the
     * file keeps its original owner and SELinux label.
     */
    fun setBooleanPref(key: String, value: Boolean): ShellResult {
        val flag = if (value) "true" else "false"
        val script = """
            F=$LAUNCHER_PREFS
            am force-stop $LAUNCHER_PKG
            sleep 1
            if [ -f "${'$'}F" ]; then
              OWN=${'$'}(stat -c '%u:%g' "${'$'}F")
              CTX=${'$'}(ls -Z "${'$'}F" 2>/dev/null | awk '{print ${'$'}1}')
            else
              mkdir -p "${'$'}(dirname "${'$'}F")"
              printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>" '<map>' '</map>' > "${'$'}F"
              OWN=${'$'}(stat -c '%u:%g' /data/user/0/$LAUNCHER_PKG)
              CTX=${'$'}(ls -Z /data/user/0/$LAUNCHER_PKG 2>/dev/null | awk '{print ${'$'}1}')
            fi
            if grep -q "name=\"$key\"" "${'$'}F"; then
              sed -i "s|name=\"$key\" value=\"[a-z]*\"|name=\"$key\" value=\"$flag\"|" "${'$'}F"
            elif grep -q "</map>" "${'$'}F"; then
              sed -i "s|</map>|<boolean name=\"$key\" value=\"$flag\" /></map>|" "${'$'}F"
            else
              sed -i "s|<map */>|<map><boolean name=\"$key\" value=\"$flag\" /></map>|" "${'$'}F"
            fi
            if [ -n "${'$'}OWN" ]; then chown "${'$'}OWN" "${'$'}F"; fi
            chmod 600 "${'$'}F" 2>/dev/null
            if [ -n "${'$'}CTX" ]; then chcon "${'$'}CTX" "${'$'}F" 2>/dev/null || true; fi
            grep -o "name=\"$key\" value=\"[a-z]*\"" "${'$'}F"
        """.trimIndent()
        return su(script)
    }

    /** Switch the workspace grid. The provider reloads the launcher itself. */
    fun setGrid(name: String): ShellResult =
        su("content update --uri $PROVIDER/default_grid --bind grid_name:s:$name")

    /**
     * Choose which section the long-press popup expands first. This is a String
     * preference, so it cannot go through [setBooleanPref].
     */
    fun setPopupSection(section: String): ShellResult {
        require(section == POPUP_SECTION_SYSTEM || section == POPUP_SECTION_DEEP)
        val script = """
            F=$LAUNCHER_PREFS
            am force-stop $LAUNCHER_PKG
            sleep 1
            if [ -f "${'$'}F" ]; then
              OWN=${'$'}(stat -c '%u:%g' "${'$'}F")
              CTX=${'$'}(ls -Z "${'$'}F" 2>/dev/null | awk '{print ${'$'}1}')
            else
              mkdir -p "${'$'}(dirname "${'$'}F")"
              printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>" '<map>' '</map>' > "${'$'}F"
              OWN=${'$'}(stat -c '%u:%g' /data/user/0/$LAUNCHER_PKG)
              CTX=${'$'}(ls -Z /data/user/0/$LAUNCHER_PKG 2>/dev/null | awk '{print ${'$'}1}')
            fi
            if grep -q "name=\"$PREF_POPUP_SECTION\"" "${'$'}F"; then
              sed -i "s|<string name=\"$PREF_POPUP_SECTION\">[^<]*</string>|<string name=\"$PREF_POPUP_SECTION\">$section</string>|" "${'$'}F"
            elif grep -q "</map>" "${'$'}F"; then
              sed -i "s|</map>|<string name=\"$PREF_POPUP_SECTION\">$section</string></map>|" "${'$'}F"
            else
              sed -i "s|<map */>|<map><string name=\"$PREF_POPUP_SECTION\">$section</string></map>|" "${'$'}F"
            fi
            if [ -n "${'$'}OWN" ]; then chown "${'$'}OWN" "${'$'}F"; fi
            chmod 600 "${'$'}F" 2>/dev/null
            if [ -n "${'$'}CTX" ]; then chcon "${'$'}CTX" "${'$'}F" 2>/dev/null || true; fi
            grep -o "<string name=\"$PREF_POPUP_SECTION\">[^<]*</string>" "${'$'}F"
        """.trimIndent()
        return su(script)
    }

    private fun parseBoolean(block: String?): Boolean? {
        val match = Regex("boolean_value=(\\d)").find(block.orEmpty()) ?: return null
        return match.groupValues[1] == "1"
    }

    /** `settings get secure` prints 1/0, or "null" when the setting is unset. */
    private fun parseSecureBoolean(block: String?): Boolean? =
        when (block.orEmpty().trim()) {
            "1" -> true
            "0" -> false
            else -> null
        }

    /** KernelSU prints a "Value: 0/1" line; "no-ksu" means another root manager. */
    private fun parseKernelUmount(block: String?): Boolean? {
        val text = block.orEmpty()
        if (text.contains("no-ksu")) return null
        val match = Regex("Value:\\s*(\\d)").find(text) ?: return null
        return match.groupValues[1] == "1"
    }

    /** Notification dots are a platform secure setting, shared by every launcher. */
    fun setNotificationDots(enabled: Boolean): ShellResult =
        su(
            "settings put secure notification_badging ${if (enabled) 1 else 0}; " +
                "settings get secure notification_badging",
        )

    /**
     * KernelSU's global Kernel Umount hides module mounts from app processes. With
     * it on, Pixel Launcher cannot open the APK this module mounts, so it crashes.
     */
    fun disableKernelUmount(): ShellResult =
        su("ksud feature set kernel_umount 0; am force-stop $LAUNCHER_PKG")

    /** Make Pixel Launcher the system default home app. */
    fun setDefaultHome(): ShellResult =
        su("cmd package set-home-activity $LAUNCHER_PKG/.NexusLauncherActivity")

    /** Hide or show icon labels on the workspace. Live update, no restart. */
    fun setLabelsHidden(hidden: Boolean): ShellResult =
        su("content update --uri $PROVIDER/set_workspace_items_label_hidden --bind boolean_value:b:$hidden")

    /** Turn the launcher's themed (monochrome) icons on or off. */
    fun setThemedIcons(enabled: Boolean): ShellResult =
        su("content update --uri $PROVIDER/set_icon_themed --bind boolean_value:b:$enabled")

    /**
     * Applies a shape everywhere: disable the other shapes, enable the chosen
     * one, clear the launcher icon cache and restart the launcher. The disable
     * pass avoids depending on overlay priority when several are enabled.
     */
    fun applyShape(overlayPackage: String?): ShellResult {
        val command = buildString {
            Shapes.overlayPackages
                .filter { it != overlayPackage }
                .forEach { append("cmd overlay disable --user 0 $it; ") }
            if (overlayPackage != null) {
                append("cmd overlay enable --user 0 $overlayPackage; ")
            }
            append("rm -f ${Shapes.LAUNCHER_DATA}/databases/app_icons.db*; ")
            append("am force-stop ${Shapes.LAUNCHER_PACKAGE}")
        }
        return su(command)
    }

    fun restartLauncher(): ShellResult = su("am force-stop ${Shapes.LAUNCHER_PACKAGE}")

    fun clearIconCache(): ShellResult =
        su("rm -f ${Shapes.LAUNCHER_DATA}/databases/app_icons.db*; am force-stop ${Shapes.LAUNCHER_PACKAGE}")

    private fun moduleProp() = File("/data/adb/modules/$MODULE_ID/module.prop")

    const val MODULE_ID = "PE_Revived"
    const val THEMED_ICONS_OVERLAY = "com.romcontrolicons.nexuslauncher"

    private const val PROVIDER = "content://com.google.android.apps.nexuslauncher.grid_control"
    private const val LAUNCHER_PKG = "com.google.android.apps.nexuslauncher"
    private const val LAUNCHER_PREFS =
        "/data/user/0/$LAUNCHER_PKG/shared_prefs/com.android.launcher3.prefs.xml"

    const val PREF_TWO_LINE_LABELS = "pref_enable_two_line_toggle"
    const val PREF_SEARCH_KEYBOARD = "pref_search_show_keyboard"
    const val PREF_SEARCH_HIDDEN = "pref_search_show_hidden_targets"
    const val PREF_MINUS_ONE = "pref_enable_minus_one"
    const val PREF_ADD_ICON_TO_HOME = "pref_add_icon_to_home"
    const val PREF_WEB_SUGGESTIONS = "pref_allowWebResult"
    const val PREF_OVERVIEW_SUGGESTIONS = "pref_overview_action_suggestions"
    const val PREF_FIXED_LANDSCAPE = "pref_fixed_landscape_mode"
    const val PREF_POPUP_SECTION = "pref_expanded_popup_menu"

    const val POPUP_SECTION_SYSTEM = "SYSTEM"
    const val POPUP_SECTION_DEEP = "DEEP"

    val KNOWN_PREFS = listOf(
        PREF_TWO_LINE_LABELS,
        PREF_SEARCH_KEYBOARD,
        PREF_SEARCH_HIDDEN,
        PREF_MINUS_ONE,
        PREF_ADD_ICON_TO_HOME,
        PREF_WEB_SUGGESTIONS,
        PREF_OVERVIEW_SUGGESTIONS,
        PREF_FIXED_LANDSCAPE,
    )
}
