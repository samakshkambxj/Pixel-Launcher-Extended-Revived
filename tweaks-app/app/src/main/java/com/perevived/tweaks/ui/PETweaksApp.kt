package com.perevived.tweaks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perevived.tweaks.data.Glyph
import com.perevived.tweaks.data.IconShape
import com.perevived.tweaks.data.RootShell
import com.perevived.tweaks.data.ShellResult
import com.perevived.tweaks.data.Shapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri

private enum class Screen { Home, Shapes, Grids }

@Composable
fun PETweaksApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var hasRoot by remember { mutableStateOf<Boolean?>(null) }
    var currentShape by remember { mutableStateOf<IconShape?>(null) }
    var themedIcons by remember { mutableStateOf<Boolean?>(null) }
    var labelsHidden by remember { mutableStateOf<Boolean?>(null) }
    var gridTitle by remember { mutableStateOf<String?>(null) }
    var twoLineLabels by remember { mutableStateOf(false) }
    var moduleVersion by remember { mutableStateOf<String?>(null) }
    var moduleInstalled by remember { mutableStateOf<Boolean?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            val info = withContext(Dispatchers.IO) { RootShell.loadState() }
            hasRoot = info.hasRoot
            currentShape = Shapes.byPackage(info.shapePackage ?: "")
            themedIcons = info.themedIcons
            labelsHidden = info.labelsHidden
            gridTitle = info.gridTitle
            twoLineLabels = info.twoLineLabels
            moduleVersion = info.moduleVersion
            moduleInstalled = info.moduleInstalled
        }
    }

    fun setLabels(hidden: Boolean) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { RootShell.setLabelsHidden(hidden) }
            if (result.ok) {
                labelsHidden = hidden
                snackbar.showSnackbar(if (hidden) "Icon labels hidden" else "Icon labels shown")
            } else {
                snackbar.showSnackbar("Failed. Allow root access in your manager.")
            }
        }
    }

    fun setThemed(enabled: Boolean) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { RootShell.setThemedIcons(enabled) }
            if (result.ok) {
                themedIcons = enabled
                snackbar.showSnackbar(if (enabled) "Themed icons on" else "Themed icons off")
            } else {
                snackbar.showSnackbar("Failed. Allow root access in your manager.")
            }
        }
    }

    fun setTwoLineLabels(enabled: Boolean) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                RootShell.setBooleanPref(RootShell.PREF_TWO_LINE_LABELS, enabled)
            }
            if (result.ok) {
                twoLineLabels = enabled
                snackbar.showSnackbar(
                    (if (enabled) "Two-line labels on" else "Two-line labels off") +
                        " · press Home to reload",
                )
            } else {
                snackbar.showSnackbar("Failed. Allow root access in your manager.")
            }
        }
    }

    fun runRootAction(message: String, action: () -> ShellResult) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { action() }
            snackbar.showSnackbar(
                if (result.ok) message else "Failed. Allow root access in your manager.",
            )
        }
    }

    LaunchedEffect(Unit) { refresh() }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.Home -> HomeScreen(
                hasRoot = hasRoot,
                currentShape = currentShape,
                themedIcons = themedIcons,
                labelsHidden = labelsHidden,
                gridTitle = gridTitle,
                twoLineLabels = twoLineLabels,
                moduleVersion = moduleVersion,
                moduleInstalled = moduleInstalled,
                snackbar = snackbar,
                onOpenShapes = { screen = Screen.Shapes },
                onLabelsChanged = ::setLabels,
                onThemedChanged = ::setThemed,
                onOpenGrids = { screen = Screen.Grids },
                onTwoLineChanged = ::setTwoLineLabels,
                onRestart = { runRootAction("Launcher restarted", RootShell::restartLauncher) },
                onClearCache = { runRootAction("Icon cache cleared", RootShell::clearIconCache) },
                onRefresh = {
                    hasRoot = null
                    refresh()
                },
            )

            Screen.Grids -> GridsScreen(
                snackbar = snackbar,
                onBack = { screen = Screen.Home },
                onApplied = { grid ->
                    gridTitle = grid.title
                    hasRoot = true
                },
            )

            Screen.Shapes -> ShapesScreen(
                hasRoot = hasRoot,
                currentShape = currentShape,
                snackbar = snackbar,
                onBack = { screen = Screen.Home },
                onApplied = { shape ->
                    currentShape = shape
                },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    hasRoot: Boolean?,
    currentShape: IconShape?,
    themedIcons: Boolean?,
    labelsHidden: Boolean?,
    gridTitle: String?,
    twoLineLabels: Boolean,
    moduleVersion: String?,
    moduleInstalled: Boolean?,
    snackbar: SnackbarHostState,
    onOpenShapes: () -> Unit,
    onLabelsChanged: (Boolean) -> Unit,
    onThemedChanged: (Boolean) -> Unit,
    onOpenGrids: () -> Unit,
    onTwoLineChanged: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onClearCache: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        ) {
            item {
                HeroCard(currentShape)
                Spacer(Modifier.height(20.dp))
                StatusCard(hasRoot)
            }

            item { SectionHeader("Icons") }
            item {
                SettingCard {
                    SettingRow(
                        title = "Shape",
                        subtitle = currentShape?.label ?: "Default",
                        leading = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp),
                            ) {
                                ShapeGlyph(
                                    glyph = currentShape?.glyph ?: Glyph.Default,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(9.dp),
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = onOpenShapes,
                    )
                    SettingRow(
                        title = "Themed icons",
                        subtitle = when (themedIcons) {
                            true -> "Enabled by the module"
                            false -> "Not enabled"
                            null -> "Checking…"
                        },
                        icon = Icons.Rounded.Star,
                        trailing = {
                            Switch(
                                checked = themedIcons == true,
                                onCheckedChange = onThemedChanged,
                                enabled = hasRoot == true,
                            )
                        },
                    )
                }
            }

            item { SectionHeader("Home screen") }
            item {
                SettingCard {
                    SettingRow(
                        title = "App grid",
                        subtitle = gridTitle ?: "Choose columns and rows",
                        icon = Icons.Rounded.List,
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = onOpenGrids,
                    )
                    SettingRow(
                        title = "Desktop icon labels",
                        subtitle = "Show app names under home screen icons",
                        icon = Icons.Rounded.Info,
                        trailing = {
                            Switch(
                                checked = labelsHidden == false,
                                onCheckedChange = { shown -> onLabelsChanged(!shown) },
                                enabled = hasRoot == true && labelsHidden != null,
                            )
                        },
                    )
                }
            }

            item { SectionHeader("App drawer") }
            item {
                SettingCard {
                    SettingRow(
                        title = "Two-line app labels",
                        subtitle = "Let long app names wrap onto a second line",
                        icon = Icons.Rounded.List,
                        trailing = {
                            Switch(
                                checked = twoLineLabels,
                                onCheckedChange = onTwoLineChanged,
                                enabled = hasRoot == true,
                            )
                        },
                    )
                }
            }

            item { SectionHeader("Launcher") }
            item {
                SettingCard {
                    SettingRow(
                        title = "Restart launcher",
                        subtitle = "Reload Pixel Launcher after changes",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRestart,
                    )
                    SettingRow(
                        title = "Clear icon cache",
                        subtitle = "Rebuild icon and shape rendering",
                        icon = Icons.Rounded.Delete,
                        onClick = onClearCache,
                    )
                    SettingRow(
                        title = "Re-check status",
                        subtitle = "Refresh root and module state",
                        icon = Icons.Rounded.Settings,
                        onClick = onRefresh,
                    )
                }
            }

            item { SectionHeader("About") }
            item {
                SettingCard {
                    SettingRow(
                        title = "PE Tweaks",
                        subtitle = "Version ${com.perevived.tweaks.BuildConfig.VERSION_NAME}",
                        icon = Icons.Rounded.Info,
                    )
                    SettingRow(
                        title = "Module",
                        subtitle = when {
                            moduleInstalled == null -> "Checking…"
                            moduleInstalled == false -> "PE-Revived is not installed"
                            moduleVersion != null -> "PE-Revived $moduleVersion"
                            else -> "PE-Revived installed"
                        },
                        icon = Icons.Rounded.Settings,
                    )
                    SettingRow(
                        title = "Source and releases",
                        subtitle = "github.com/samakshkambxj/PE-Revived",
                        icon = Icons.Rounded.Share,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/samakshkambxj/PE-Revived"),
                                    ),
                                )
                            }
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Companion for PE-Revived on Android 17.\nNot affiliated with Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(currentShape: IconShape?) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp, end = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "PE Tweaks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Pixel Launcher Extended\non Android 17",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            ShapeGlyph(
                glyph = currentShape?.glyph ?: Glyph.Squircle,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(140.dp)
                    .offset(x = 26.dp, y = 34.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(hasRoot: Boolean?) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Launcher tweaks", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = when (hasRoot) {
                        null -> "Checking root access…"
                        true -> "Root is ready. Changes apply instantly."
                        false -> "Root access is needed to apply changes."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            StatusPill(
                text = when (hasRoot) {
                    null -> "…"
                    true -> "Ready"
                    false -> "No root"
                },
                container = if (hasRoot == false) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                content = if (hasRoot == false) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    }
}
