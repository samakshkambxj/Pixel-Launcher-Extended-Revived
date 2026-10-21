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
    val themedIcons: Boolean,
    val moduleVersion: String?,
    val moduleInstalled: Boolean,
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
                "test -d /data/adb/modules/$MODULE_ID && echo installed",
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

        return LauncherState(
            hasRoot = hasRoot,
            shapePackage = shape,
            themedIcons = overlays.contains(THEMED_ICONS_OVERLAY),
            moduleVersion = version,
            moduleInstalled = installed,
        )
    }

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
}
