package com.perevived.tweaks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perevived.tweaks.data.GridOption
import com.perevived.tweaks.data.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GridsScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onApplied: (GridOption) -> Unit,
) {
    var options by remember { mutableStateOf<List<GridOption>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        failed = false
        options = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { RootShell.loadGrids() }
            options = result
            failed = result.isEmpty()
        }
    }

    LaunchedEffect(Unit) { load() }

    fun apply(option: GridOption) {
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { RootShell.setGrid(option.name) }
            busy = false
            if (result.ok) {
                options = options?.map { it.copy(isDefault = it.name == option.name) }
                onApplied(option)
                snackbar.showSnackbar("Grid set to ${option.title}")
            } else {
                snackbar.showSnackbar("Could not change grid. Allow root access.")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("App grid", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Home screen and drawer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::load) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                options == null -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                failed -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Grid list unavailable", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Grant root access, then tap reload.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Pick a grid size. The launcher reloads and keeps your icons in place.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                )
                            }
                        }

                        items(options.orEmpty(), key = { it.name }) { option ->
                            GridRow(
                                option = option,
                                enabled = !busy,
                                onClick = { apply(option) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridRow(option: GridOption, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = if (option.isDefault) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (option.isDefault) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(46.dp),
            ) {
                GridGlyph(
                    cols = option.cols.coerceAtLeast(1),
                    rows = option.rows.coerceAtLeast(1),
                    color = if (option.isDefault) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (option.isDefault) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    "${option.cols} × ${option.rows} · ${option.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (option.isDefault) {
                Icon(Icons.Rounded.Check, contentDescription = "Current grid")
            }
        }
    }
}

/** Miniature grid preview: columns and rows drawn to scale. */
@Composable
private fun GridGlyph(cols: Int, rows: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.055f
        val inset = stroke
        val w = size.width - inset * 2
        val h = size.height - inset * 2
        val left = inset
        val top = inset

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2.2f),
            style = Stroke(width = stroke),
        )
        for (c in 1 until cols) {
            val x = left + w * c / cols
            drawLine(
                color = color.copy(alpha = 0.45f),
                start = androidx.compose.ui.geometry.Offset(x, top + stroke),
                end = androidx.compose.ui.geometry.Offset(x, top + h - stroke),
                strokeWidth = stroke * 0.7f,
            )
        }
        for (r in 1 until rows) {
            val y = top + h * r / rows
            drawLine(
                color = color.copy(alpha = 0.45f),
                start = androidx.compose.ui.geometry.Offset(left + stroke, y),
                end = androidx.compose.ui.geometry.Offset(left + w - stroke, y),
                strokeWidth = stroke * 0.7f,
            )
        }
    }
}
