package com.perevived.tweaks.data

import java.io.File
import java.util.concurrent.TimeUnit

data class ShellResult(val code: Int, val output: String) {
    val ok: Boolean get() = code == 0
}

/**
 * Thin root shell used for the handful of privileged operations the launcher
 * tweaks need. Everything runs through `su -c`, so Magisk / KernelSU decide
 * whether to grant it.
 */
object RootShell {

    private const val TIMEOUT_SECONDS = 25L

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

    /** Returns true when root is usable. */
    fun hasRoot(): Boolean = su("id -u").output.trim().contains("0")

    /**
     * Enabled overlay packages, one per line, as reported by `cmd overlay list`.
     * Lines look like `[x] com.android.theme.icon.mallow`.
     */
    private fun enabledOverlays(): List<String> =
        su("cmd overlay list --user 0")
            .output
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("[x]") }
            .map { it.removePrefix("[x]").trim() }
            .toList()

    /** Currently applied shape overlay package, or null for the stock shape. */
    fun currentShapePackage(): String? {
        val enabled = enabledOverlays()
        return Shapes.overlayPackages.firstOrNull { pkg -> enabled.any { it == pkg } }
    }

    /** Static themed-icon overlay shipped by the module. */
    fun themedIconsEnabled(): Boolean =
        enabledOverlays().any { it == THEMED_ICONS_OVERLAY }

    fun moduleVersion(): String? {
        val prop = File("/data/adb/modules/$MODULE_ID/module.prop")
        val result = su("cat ${prop.absolutePath}")
        if (!result.ok) return null
        return result.output.lineSequence()
            .firstOrNull { it.startsWith("version=") }
            ?.removePrefix("version=")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun moduleInstalled(): Boolean = su("test -d /data/adb/modules/$MODULE_ID && echo yes")
        .output
        .contains("yes")

    /**
     * Applies a shape everywhere and clears the launcher icon cache so both the
     * workspace and the drawer pick it up immediately.
     */
    fun applyShape(overlayPackage: String?): ShellResult {
        val command = buildString {
            if (overlayPackage == null) {
                Shapes.overlayPackages.forEach {
                    append("cmd overlay disable --user 0 $it; ")
                }
            } else {
                append("cmd overlay enable-exclusive --user 0 $overlayPackage; ")
            }
            append("rm -f ${Shapes.LAUNCHER_DATA}/databases/app_icons.db*; ")
            append("am force-stop ${Shapes.LAUNCHER_PACKAGE}")
        }
        return su(command)
    }

    fun restartLauncher(): ShellResult = su("am force-stop ${Shapes.LAUNCHER_PACKAGE}")

    fun clearIconCache(): ShellResult =
        su("rm -f ${Shapes.LAUNCHER_DATA}/databases/app_icons.db*; am force-stop ${Shapes.LAUNCHER_PACKAGE}")

    const val MODULE_ID = "PE_Revived"
    const val THEMED_ICONS_OVERLAY = "com.romcontrolicons.nexuslauncher"
}
