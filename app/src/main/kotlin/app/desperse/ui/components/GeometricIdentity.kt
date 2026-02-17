package app.desperse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// Core Engine
// ============================================================

private fun hashString(str: String): Long {
    var hash = 5381L
    for (c in str) {
        hash = ((hash shl 5) + hash) + c.code
    }
    return hash and 0x7FFFFFFFL
}

private class SeededRandom(seed: Long) {
    private var state = (seed % 2147483646L + 1).coerceAtLeast(1)

    fun nextFloat(): Float {
        state = (state * 16807L) % 2147483647L
        return state.toFloat() / 2147483647f
    }

    fun nextInt(max: Int): Int = (nextFloat() * max).toInt().coerceIn(0, max - 1)
}

// ============================================================
// Shape Types & Glyph Map
// ============================================================

private enum class ShapeType {
    FILLED_CIRCLE, STROKED_CIRCLE, TRIANGLE_UP, TRIANGLE_DOWN,
    SQUARE, DIAMOND, PENTAGON, HEXAGON, OCTAGON,
    STAR_4, STAR_6, CROSS, X_MARK,
    SEMICIRCLE_TOP, SEMICIRCLE_LEFT, QUARTER_ARC,
    RING, CRESCENT, TEARDROP, LENS,
    WAVE, ZIGZAG, PARALLEL_LINES,
    DOT_PAIR, DOT_TRIANGLE, CONCENTRIC
}

private val GLYPH_MAP: Map<Char, ShapeType> = mapOf(
    'a' to ShapeType.FILLED_CIRCLE, 'b' to ShapeType.STROKED_CIRCLE,
    'c' to ShapeType.TRIANGLE_UP, 'd' to ShapeType.TRIANGLE_DOWN,
    'e' to ShapeType.SQUARE, 'f' to ShapeType.DIAMOND,
    'g' to ShapeType.PENTAGON, 'h' to ShapeType.HEXAGON,
    'i' to ShapeType.OCTAGON, 'j' to ShapeType.STAR_4,
    'k' to ShapeType.STAR_6, 'l' to ShapeType.CROSS,
    'm' to ShapeType.X_MARK, 'n' to ShapeType.SEMICIRCLE_TOP,
    'o' to ShapeType.SEMICIRCLE_LEFT, 'p' to ShapeType.QUARTER_ARC,
    'q' to ShapeType.RING, 'r' to ShapeType.CRESCENT,
    's' to ShapeType.TEARDROP, 't' to ShapeType.LENS,
    'u' to ShapeType.WAVE, 'v' to ShapeType.ZIGZAG,
    'w' to ShapeType.PARALLEL_LINES, 'x' to ShapeType.DOT_PAIR,
    'y' to ShapeType.DOT_TRIANGLE, 'z' to ShapeType.CONCENTRIC
)

// ============================================================
// Composition Data
// ============================================================

private data class ShapeInstance(
    val type: ShapeType,
    val cx: Float,
    val cy: Float,
    val size: Float,
    val rotation: Float,
    val alpha: Float
)

private data class LineElement(
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val strokeWidth: Float
)

private data class AvatarComposition(
    val shapes: List<ShapeInstance>,
    val line: LineElement?
)

private data class BannerComposition(
    val shapes: List<ShapeInstance>
)

// ============================================================
// Composition Generation
// ============================================================

private fun generateAvatarComposition(input: String): AvatarComposition {
    val hash = hashString(input)
    val rng = SeededRandom(hash)
    val chars = input.lowercase().filter { it in 'a'..'z' }
    val shapeCount = 3 + (hash % 3).toInt()
    val layout = rng.nextInt(5)

    val shapes = (0 until shapeCount).map { i ->
        val char = if (chars.isNotEmpty()) chars[i % chars.length] else ('a' + ((hash.toInt() + i) % 26))
        val type = GLYPH_MAP[char] ?: ShapeType.FILLED_CIRCLE
        val baseSize = 0.15f + rng.nextFloat() * 0.25f
        val alpha = 0.3f + rng.nextFloat() * 0.7f
        val rotation = rng.nextFloat() * 360f

        val (cx, cy) = when (layout) {
            0 -> { // Centered stack
                val off = 0.1f * rng.nextFloat() - 0.05f
                (0.5f + off) to (0.5f + off)
            }
            1 -> { // Orbital
                if (i == 0) 0.5f to 0.5f
                else {
                    val angle = (2f * PI.toFloat() * i) / (shapeCount - 1) + rng.nextFloat() * 0.5f
                    val r = 0.2f + rng.nextFloat() * 0.15f
                    (0.5f + cos(angle) * r) to (0.5f + sin(angle) * r)
                }
            }
            2 -> { // Diagonal
                val t = i.toFloat() / (shapeCount - 1).coerceAtLeast(1)
                val j = rng.nextFloat() * 0.1f - 0.05f
                (0.2f + t * 0.6f + j) to (0.2f + t * 0.6f + j)
            }
            3 -> { // Split
                val side = if (i % 2 == 0) 0.3f else 0.7f
                (side + rng.nextFloat() * 0.1f - 0.05f) to (0.25f + rng.nextFloat() * 0.5f)
            }
            else -> { // Scattered
                (0.15f + rng.nextFloat() * 0.7f) to (0.15f + rng.nextFloat() * 0.7f)
            }
        }
        ShapeInstance(type, cx, cy, baseSize, rotation, alpha)
    }

    val line = if (rng.nextFloat() > 0.4f) {
        LineElement(
            rng.nextFloat() * 0.3f + 0.1f, rng.nextFloat() * 0.3f + 0.1f,
            rng.nextFloat() * 0.3f + 0.6f, rng.nextFloat() * 0.3f + 0.6f,
            0.01f + rng.nextFloat() * 0.015f
        )
    } else null

    return AvatarComposition(shapes, line)
}

private fun generateBannerComposition(input: String): BannerComposition {
    val hash = hashString(input + "_banner")
    val rng = SeededRandom(hash)
    val chars = input.lowercase().filter { it in 'a'..'z' }
    val shapeCount = 2 + (hash % 2).toInt()

    val shapes = (0 until shapeCount).map { i ->
        val char = if (chars.isNotEmpty()) chars[(i + 1) % chars.length] else ('a' + ((hash.toInt() + i) % 26))
        val type = GLYPH_MAP[char] ?: ShapeType.HEXAGON
        ShapeInstance(
            type = type,
            cx = 0.15f + (i.toFloat() / shapeCount.coerceAtLeast(1)) * 0.7f + rng.nextFloat() * 0.15f,
            cy = 0.3f + rng.nextFloat() * 0.4f,
            size = 0.3f + rng.nextFloat() * 0.35f,
            rotation = rng.nextFloat() * 360f,
            alpha = 0.08f + rng.nextFloat() * 0.12f
        )
    }
    return BannerComposition(shapes)
}

// ============================================================
// Shape Renderer Helpers
// ============================================================

private fun DrawScope.drawRegularPolygon(
    sides: Int, center: Offset, radius: Float,
    color: Color, alpha: Float, startAngleDeg: Float = 0f
) {
    val path = Path().apply {
        val step = (2f * PI.toFloat()) / sides
        val start = startAngleDeg * PI.toFloat() / 180f
        for (i in 0 until sides) {
            val angle = start + i * step
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color, alpha = alpha)
}

private fun DrawScope.drawStar(
    points: Int, center: Offset,
    outerR: Float, innerR: Float,
    color: Color, alpha: Float
) {
    val path = Path().apply {
        val step = PI.toFloat() / points
        val startAngle = -PI.toFloat() / 2f
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = startAngle + i * step
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color, alpha = alpha)
}

// ============================================================
// Shape Renderer
// ============================================================

private fun DrawScope.drawShape(
    type: ShapeType, center: Offset, radius: Float,
    color: Color, alpha: Float, rotation: Float
) {
    withTransform({ rotate(rotation, center) }) {
        when (type) {
            ShapeType.FILLED_CIRCLE ->
                drawCircle(color, radius, center, alpha = alpha)

            ShapeType.STROKED_CIRCLE ->
                drawCircle(color, radius, center, alpha = alpha, style = Stroke(radius * 0.15f))

            ShapeType.TRIANGLE_UP ->
                drawRegularPolygon(3, center, radius, color, alpha, -90f)

            ShapeType.TRIANGLE_DOWN ->
                drawRegularPolygon(3, center, radius, color, alpha, 90f)

            ShapeType.SQUARE -> {
                val h = radius * 0.85f
                drawRect(color, Offset(center.x - h, center.y - h), Size(h * 2, h * 2), alpha = alpha)
            }

            ShapeType.DIAMOND ->
                drawRegularPolygon(4, center, radius, color, alpha, 0f)

            ShapeType.PENTAGON ->
                drawRegularPolygon(5, center, radius, color, alpha, -90f)

            ShapeType.HEXAGON ->
                drawRegularPolygon(6, center, radius, color, alpha, 0f)

            ShapeType.OCTAGON ->
                drawRegularPolygon(8, center, radius, color, alpha, 22.5f)

            ShapeType.STAR_4 ->
                drawStar(4, center, radius, radius * 0.4f, color, alpha)

            ShapeType.STAR_6 ->
                drawStar(6, center, radius, radius * 0.5f, color, alpha)

            ShapeType.CROSS -> {
                val arm = radius * 0.3f
                drawRect(color, Offset(center.x - arm, center.y - radius), Size(arm * 2, radius * 2), alpha = alpha)
                drawRect(color, Offset(center.x - radius, center.y - arm), Size(radius * 2, arm * 2), alpha = alpha)
            }

            ShapeType.X_MARK -> {
                val sw = radius * 0.2f
                drawLine(color, Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius), sw, alpha = alpha)
                drawLine(color, Offset(center.x + radius, center.y - radius), Offset(center.x - radius, center.y + radius), sw, alpha = alpha)
            }

            ShapeType.SEMICIRCLE_TOP -> {
                val path = Path().apply {
                    arcTo(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), 180f, 180f, false)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.SEMICIRCLE_LEFT -> {
                val path = Path().apply {
                    arcTo(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), 90f, 180f, false)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.QUARTER_ARC -> {
                val path = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), -90f, 90f, false)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.RING -> {
                drawCircle(color, radius, center, alpha = alpha, style = Stroke(radius * 0.25f))
                drawCircle(color, radius * 0.4f, center, alpha = alpha * 0.5f)
            }

            ShapeType.CRESCENT -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - radius)
                    quadraticTo(center.x - radius * 1.4f, center.y, center.x, center.y + radius)
                    quadraticTo(center.x + radius * 0.3f, center.y, center.x, center.y - radius)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.TEARDROP -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - radius)
                    quadraticTo(center.x + radius, center.y + radius * 0.2f, center.x, center.y + radius * 0.8f)
                    quadraticTo(center.x - radius, center.y + radius * 0.2f, center.x, center.y - radius)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.LENS -> {
                val path = Path().apply {
                    moveTo(center.x - radius, center.y)
                    quadraticTo(center.x, center.y - radius * 0.7f, center.x + radius, center.y)
                    quadraticTo(center.x, center.y + radius * 0.7f, center.x - radius, center.y)
                    close()
                }
                drawPath(path, color, alpha = alpha)
            }

            ShapeType.WAVE -> {
                val path = Path().apply {
                    moveTo(center.x - radius, center.y)
                    quadraticTo(center.x - radius * 0.5f, center.y - radius * 0.6f, center.x, center.y)
                    quadraticTo(center.x + radius * 0.5f, center.y + radius * 0.6f, center.x + radius, center.y)
                }
                drawPath(path, color, alpha = alpha, style = Stroke(radius * 0.15f))
            }

            ShapeType.ZIGZAG -> {
                val path = Path().apply {
                    moveTo(center.x - radius, center.y + radius * 0.5f)
                    lineTo(center.x - radius * 0.33f, center.y - radius * 0.5f)
                    lineTo(center.x + radius * 0.33f, center.y + radius * 0.5f)
                    lineTo(center.x + radius, center.y - radius * 0.5f)
                }
                drawPath(path, color, alpha = alpha, style = Stroke(radius * 0.15f))
            }

            ShapeType.PARALLEL_LINES -> {
                val sw = radius * 0.12f
                val gap = radius * 0.5f
                for (j in -1..1) {
                    drawLine(color, Offset(center.x - radius, center.y + j * gap), Offset(center.x + radius, center.y + j * gap), sw, alpha = alpha)
                }
            }

            ShapeType.DOT_PAIR -> {
                val dotR = radius * 0.35f
                drawCircle(color, dotR, Offset(center.x - radius * 0.5f, center.y), alpha = alpha)
                drawCircle(color, dotR, Offset(center.x + radius * 0.5f, center.y), alpha = alpha)
            }

            ShapeType.DOT_TRIANGLE -> {
                val dotR = radius * 0.3f
                drawCircle(color, dotR, Offset(center.x, center.y - radius * 0.5f), alpha = alpha)
                drawCircle(color, dotR, Offset(center.x - radius * 0.45f, center.y + radius * 0.35f), alpha = alpha)
                drawCircle(color, dotR, Offset(center.x + radius * 0.45f, center.y + radius * 0.35f), alpha = alpha)
            }

            ShapeType.CONCENTRIC -> {
                drawCircle(color, radius, center, alpha = alpha * 0.3f)
                drawCircle(color, radius * 0.65f, center, alpha = alpha * 0.5f)
                drawCircle(color, radius * 0.3f, center, alpha = alpha)
            }
        }
    }
}

// ============================================================
// Public Composables
// ============================================================

@Composable
fun GeometricAvatar(
    input: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = DesperseColors.Zinc400
) {
    val composition = remember(input) { generateAvatarComposition(input) }
    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        val dim = this.size.minDimension
        composition.line?.let { l ->
            drawLine(
                color = color,
                start = Offset(l.x1 * this.size.width, l.y1 * this.size.height),
                end = Offset(l.x2 * this.size.width, l.y2 * this.size.height),
                strokeWidth = l.strokeWidth * dim,
                alpha = 0.3f
            )
        }
        for (s in composition.shapes) {
            drawShape(
                type = s.type,
                center = Offset(s.cx * this.size.width, s.cy * this.size.height),
                radius = s.size * dim,
                color = color,
                alpha = s.alpha,
                rotation = s.rotation
            )
        }
    }
}

@Composable
fun GeometricBanner(
    input: String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    color: Color = DesperseColors.Zinc400
) {
    val composition = remember(input) { generateBannerComposition(input) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        for (s in composition.shapes) {
            drawShape(
                type = s.type,
                center = Offset(s.cx * size.width, s.cy * size.height),
                radius = s.size * size.minDimension,
                color = color,
                alpha = s.alpha,
                rotation = s.rotation
            )
        }
    }
}
