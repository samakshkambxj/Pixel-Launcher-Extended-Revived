package com.perevived.tweaks.data

/** A selectable icon shape backed by an RRO overlay package. */
data class IconShape(
    val label: String,
    val overlayPackage: String?,
    val glyph: Glyph,
)

enum class Glyph {
    Default,
    Squircle,
    Circle,
    RoundedRect,
    Square,
    Hexagon,
    RoundedHexagon,
    Flower,
    Leaf,
    Teardrop,
    Vessel,
    Cloudy,
    Mallow,
    Stretched,
    Tapered,
    Samsung,
    Pebble,
}

object Shapes {
    const val LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"
    const val LAUNCHER_DATA = "/data/data/$LAUNCHER_PACKAGE"

    val all: List<IconShape> = listOf(
        IconShape("Default", null, Glyph.Default),
        IconShape("Cloudy", "com.android.theme.icon.cloudy", Glyph.Cloudy),
        IconShape("Cylinder", "com.android.theme.icon.cylinder", Glyph.Circle),
        IconShape("Flower", "com.android.theme.icon.flower", Glyph.Flower),
        IconShape("Hexagon", "com.android.theme.icon.hexagon", Glyph.Hexagon),
        IconShape("Leaf", "com.android.theme.icon.leaf", Glyph.Leaf),
        IconShape("Mallow", "com.android.theme.icon.mallow", Glyph.Mallow),
        IconShape("Pebble", "com.android.theme.icon.pebble", Glyph.Pebble),
        IconShape("Rounded hex", "com.android.theme.icon.roundedhexagon", Glyph.RoundedHexagon),
        IconShape("Rounded rect", "com.android.theme.icon.roundedrect", Glyph.RoundedRect),
        IconShape("Samsung", "com.android.theme.icon.samsung", Glyph.Samsung),
        IconShape("Square", "com.android.theme.icon.square", Glyph.Square),
        IconShape("Squircle", "com.android.theme.icon.squircle", Glyph.Squircle),
        IconShape("Stretched", "com.android.theme.icon.stretched", Glyph.Stretched),
        IconShape("Tapered rect", "com.android.theme.icon.taperedrect", Glyph.Tapered),
        IconShape("Teardrops", "com.android.theme.icon.teardrop", Glyph.Teardrop),
        IconShape("Vessel", "com.android.theme.icon.vessel", Glyph.Vessel),
    )

    /** Overlay packages the module installs; used for reset and state detection. */
    val overlayPackages: List<String> = all.mapNotNull { it.overlayPackage }

    fun byPackage(pkg: String): IconShape =
        all.firstOrNull { it.overlayPackage == pkg } ?: all.first()
}
