package com.perevived.tweaks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.perevived.tweaks.data.Glyph
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Draws shape previews as real geometry instead of bitmaps: they scale cleanly,
 * follow the wallpaper palette and stay crisp at any tile size.
 */
@Composable
fun ShapeGlyph(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
    strokeColor: Color? = null,
) {
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawGlyph(glyph, color, strokeColor)
        }
    }
}

fun DrawScope.drawGlyph(glyph: Glyph, color: Color, strokeColor: Color? = null) {
    val side = minOf(size.width, size.height)
    val rect = Rect(
        left = (size.width - side) / 2f,
        top = (size.height - side) / 2f,
        right = (size.width + side) / 2f,
        bottom = (size.height + side) / 2f,
    )

    when (glyph) {
        Glyph.Default -> {
            val inset = rect.inset(side * 0.16f)
            drawPath(
                superellipse(inset, 3.2f),
                color = strokeColor ?: color,
                style = Stroke(
                    width = side * 0.06f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(side * 0.12f, side * 0.09f),
                    ),
                ),
            )
            drawPath(superellipse(rect.inset(side * 0.34f), 4f), color)
        }

        Glyph.Squircle -> drawPath(superellipse(rect.inset(side * 0.04f), 4f), color)
        Glyph.Mallow -> drawPath(superellipse(rect.inset(side * 0.04f), 5f), color)
        Glyph.Samsung -> drawPath(superellipse(rect.inset(side * 0.04f), 3.1f), color)
        Glyph.Pebble -> drawPath(superellipse(rect.inset(side * 0.05f), 2.6f), color)

        Glyph.Circle -> drawCircle(
            color = color,
            radius = side * 0.46f,
            center = rect.center,
        )

        Glyph.RoundedRect -> drawPath(roundedRect(rect.inset(side * 0.05f), side * 0.26f), color)
        Glyph.Square -> drawPath(roundedRect(rect.inset(side * 0.06f), side * 0.10f), color)
        Glyph.Stretched -> drawPath(
            roundedRect(
                Rect(
                    left = rect.left + side * 0.02f,
                    top = rect.top + side * 0.17f,
                    right = rect.right - side * 0.02f,
                    bottom = rect.bottom - side * 0.17f,
                ),
                side * 0.22f,
            ),
            color,
        )

        Glyph.Hexagon -> drawPath(polygon(rect.inset(side * 0.05f), 6, PI.toFloat() / 6f), color)
        Glyph.RoundedHexagon -> drawPath(
            roundedPolygon(rect.inset(side * 0.05f), 6, PI.toFloat() / 6f, side * 0.08f),
            color,
        )

        Glyph.Flower -> drawPath(flower(rect.inset(side * 0.03f)), color)
        Glyph.Cloudy -> drawPath(cloudy(rect.inset(side * 0.03f)), color)
        Glyph.Leaf -> drawPath(leaf(rect.inset(side * 0.05f)), color)
        Glyph.Teardrop -> drawPath(teardrop(rect.inset(side * 0.04f)), color)
        Glyph.Vessel -> drawPath(vessel(rect.inset(side * 0.05f)), color)
        Glyph.Tapered -> drawPath(tapered(rect.inset(side * 0.04f)), color)
    }
}

private fun Rect.inset(fraction: Float): Rect {
    val dx = width * fraction
    val dy = height * fraction
    return Rect(left + dx, top + dy, right - dx, bottom - dy)
}

/** Superellipse (Lamé curve); n = 2 is a circle, larger n approaches a squircle. */
private fun superellipse(rect: Rect, n: Float): Path {
    val path = Path()
    val a = rect.width / 2f
    val b = rect.height / 2f
    val cx = rect.center.x
    val cy = rect.center.y
    val steps = 160

    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val ct = cos(t)
        val st = sin(t)
        val x = cx + a * sign(ct) * abs(ct).pow(2.0 / n)
        val y = cy + b * sign(st) * abs(st).pow(2.0 / n)
        if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
        else path.lineTo(x.toFloat(), y.toFloat())
    }
    path.close()
    return path
}

private fun roundedRect(rect: Rect, radius: Float): Path {
    val r = minOf(radius, minOf(rect.width, rect.height) / 2f)
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(r)),
        )
    }
}

private fun polygon(rect: Rect, sides: Int, rotation: Float): Path {
    val path = Path()
    val radius = minOf(rect.width, rect.height) / 2f
    for (i in 0 until sides) {
        val angle = rotation + i * (2.0 * PI / sides).toFloat()
        val x = rect.center.x + radius * cos(angle)
        val y = rect.center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Polygon with the corners clipped and curved, matching the rounded hex shape. */
private fun roundedPolygon(rect: Rect, sides: Int, rotation: Float, radius: Float): Path {
    val path = Path()
    val r = minOf(rect.width, rect.height) / 2f
    val points = (0 until sides).map {
        val angle = rotation + it * (2.0 * PI / sides).toFloat()
        Offset(rect.center.x + r * cos(angle), rect.center.y + r * sin(angle))
    }
    for (i in points.indices) {
        val current = points[i]
        val next = points[(i + 1) % points.size]
        val previous = points[(i - 1 + points.size) % points.size]

        val start = current + (previous - current).normalized() * radius
        val end = current + (next - current).normalized() * radius
        if (i == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        path.quadraticTo(current.x, current.y, end.x, end.y)
    }
    path.close()
    return path
}

private fun Offset.normalized(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    return if (length == 0f) Offset.Zero else Offset(x / length, y / length)
}

/** Nine overlapping lobes around a centre, like the Cloudy shape. */
private fun cloudy(rect: Rect): Path {
    val path = Path()
    val cx = rect.center.x
    val cy = rect.center.y
    val base = minOf(rect.width, rect.height) / 2f
    val lobeCount = 9
    val lobeRadius = base * 0.30f
    val orbit = base * 0.56f

    for (i in 0 until lobeCount) {
        val angle = i * (2.0 * PI / lobeCount).toFloat()
        val x = cx + orbit * cos(angle)
        val y = cy + orbit * sin(angle)
        path.addOval(
            Rect(
                left = x - lobeRadius,
                top = y - lobeRadius,
                right = x + lobeRadius,
                bottom = y + lobeRadius,
            ),
        )
    }
    path.addOval(Rect(cx - base * 0.52f, cy - base * 0.52f, cx + base * 0.52f, cy + base * 0.52f))
    return path
}

/** Eight rounded petals around a centre. */
private fun flower(rect: Rect): Path {
    val path = Path()
    val cx = rect.center.x
    val cy = rect.center.y
    val base = minOf(rect.width, rect.height) / 2f
    val petalRadius = base * 0.30f
    val orbit = base * 0.50f

    for (i in 0 until 8) {
        val angle = i * (2.0 * PI / 8.0).toFloat()
        val x = cx + orbit * cos(angle)
        val y = cy + orbit * sin(angle)
        path.addOval(
            Rect(
                left = x - petalRadius,
                top = y - petalRadius,
                right = x + petalRadius,
                bottom = y + petalRadius,
            ),
        )
    }
    path.addOval(
        Rect(cx - base * 0.42f, cy - base * 0.42f, cx + base * 0.42f, cy + base * 0.42f),
    )
    return path
}

/** Pointed oval: two mirrored cubics meeting at the top-right and bottom-left. */
private fun leaf(rect: Rect): Path {
    val w = rect.width
    val h = rect.height
    return Path().apply {
        moveTo(rect.left + w * 0.10f, rect.bottom - h * 0.10f)
        cubicTo(
            rect.left + w * 0.12f, rect.top + h * 0.34f,
            rect.left + w * 0.34f, rect.top + h * 0.12f,
            rect.left + w * 0.90f, rect.top + h * 0.10f,
        )
        cubicTo(
            rect.left + w * 0.88f, rect.top + h * 0.66f,
            rect.left + w * 0.66f, rect.top + h * 0.88f,
            rect.left + w * 0.10f, rect.bottom - h * 0.10f,
        )
        close()
    }
}

/** Circle with a soft point on top. */
private fun teardrop(rect: Rect): Path {
    val cx = rect.center.x
    val w = rect.width
    val h = rect.height
    val radius = w * 0.35f
    return Path().apply {
        addOval(
            Rect(
                left = cx - radius,
                top = rect.top + h * 0.34f,
                right = cx + radius,
                bottom = rect.top + h * 0.34f + radius * 2f,
            ),
        )
        moveTo(cx - radius * 0.72f, rect.top + h * 0.52f)
        cubicTo(
            cx - radius * 0.35f, rect.top + h * 0.16f,
            cx + radius * 0.35f, rect.top + h * 0.16f,
            cx + radius * 0.72f, rect.top + h * 0.52f,
        )
        close()
    }
}

/** Barrel: flat top and bottom, slightly bowed sides. */
private fun vessel(rect: Rect): Path {
    val w = rect.width
    val h = rect.height
    return Path().apply {
        moveTo(rect.left + w * 0.08f, rect.top + h * 0.22f)
        cubicTo(
            rect.left + w * 0.02f, rect.top + h * 0.42f,
            rect.left + w * 0.02f, rect.top + h * 0.58f,
            rect.left + w * 0.08f, rect.top + h * 0.78f,
        )
        lineTo(rect.left + w * 0.30f, rect.bottom - h * 0.06f)
        lineTo(rect.left + w * 0.70f, rect.bottom - h * 0.06f)
        lineTo(rect.left + w * 0.92f, rect.top + h * 0.78f)
        cubicTo(
            rect.left + w * 0.98f, rect.top + h * 0.58f,
            rect.left + w * 0.98f, rect.top + h * 0.42f,
            rect.left + w * 0.92f, rect.top + h * 0.22f,
        )
        lineTo(rect.left + w * 0.70f, rect.top + h * 0.06f)
        lineTo(rect.left + w * 0.30f, rect.top + h * 0.06f)
        close()
    }
}

/** Trapezoid with rounded corners, wider at the bottom. */
private fun tapered(rect: Rect): Path {
    val w = rect.width
    val h = rect.height
    return Path().apply {
        moveTo(rect.left + w * 0.30f, rect.top + h * 0.08f)
        lineTo(rect.left + w * 0.70f, rect.top + h * 0.08f)
        cubicTo(
            rect.left + w * 0.82f, rect.top + h * 0.30f,
            rect.left + w * 0.88f, rect.top + h * 0.56f,
            rect.left + w * 0.94f, rect.top + h * 0.88f,
        )
        lineTo(rect.left + w * 0.06f, rect.top + h * 0.88f)
        cubicTo(
            rect.left + w * 0.12f, rect.top + h * 0.56f,
            rect.left + w * 0.18f, rect.top + h * 0.30f,
            rect.left + w * 0.30f, rect.top + h * 0.08f,
        )
        close()
    }
}
