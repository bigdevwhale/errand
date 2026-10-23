package app.errand.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.errand.domain.Shelf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object Art {
    /** Screen renders want everything settled: no stamp drops, no ticking. */
    var still = false
}

/** What lands in the bag, coloured by shelf. */
fun shelfColor(s: Shelf): Color = when (s) {
    Shelf.PRODUCE -> Color(0xFF7FA650)
    Shelf.BREAD -> Color(0xFFC8954E)
    Shelf.GROCERY -> Color(0xFFD9BE86)
    Shelf.CANS -> Color(0xFF9A9FA6)
    Shelf.SWEETS -> Color(0xFF8A4F3A)
    Shelf.DRINKS -> Color(0xFF6F9FB8)
    Shelf.HOUSEHOLD -> Color(0xFF5D8FA6)
    Shelf.HYGIENE -> Color(0xFFD8A1B0)
    Shelf.PHARMACY -> Color(0xFFE7E1D6)
    Shelf.PETS -> Color(0xFFB07A44)
    Shelf.OTHER -> Color(0xFFB9A88A)
    Shelf.MEAT -> Color(0xFFC0574A)
    Shelf.DAIRY -> Color(0xFFF2EEE4)
    Shelf.FROZEN -> Color(0xFFBFD8E6)
}

/**
 * The string bag, filling as lines are ticked: one round shape per thing, the net stretching.
 * [load] 0..1 of what the courier carries comfortably; past 1 the handles turn red.
 */
@Composable
fun StringBag(things: List<Shelf>, load: Float, modifier: Modifier = Modifier, net: Color = Color(0xFF6E5E42), warn: Color = Color(0xFFB4372B)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val top = h * 0.30f
        val bottom = h * 0.96f
        val half = w * 0.40f * (0.88f + load.coerceIn(0f, 1.2f) * 0.12f)
        val body = Path().apply {
            moveTo(cx - half * 0.55f, top)
            cubicTo(cx - half * 1.15f, top + (bottom - top) * 0.25f, cx - half * 1.05f, bottom, cx, bottom)
            cubicTo(cx + half * 1.05f, bottom, cx + half * 1.15f, top + (bottom - top) * 0.25f, cx + half * 0.55f, top)
            close()
        }
        // The things, from the bottom up.
        clipPath(body) {
            val r = Random(things.size * 31 + 7)
            val rad = w * 0.11f
            var i = 0
            val perRow = 4
            for (s in things.take(25)) {
                val row = i / perRow
                val col = i % perRow
                val x = cx - half * 0.85f + (col + 0.5f) * (half * 1.7f / perRow) + (r.nextFloat() - 0.5f) * rad * 0.6f
                val y = bottom - rad * 1.0f - row * rad * 1.55f + (r.nextFloat() - 0.5f) * rad * 0.4f
                val c = shelfColor(s)
                val rr = rad * (0.85f + r.nextFloat() * 0.3f)
                drawCircle(c, rr, Offset(x, y))
                drawCircle(net.copy(alpha = 0.55f), rr, Offset(x, y), style = Stroke(1.2f))
                drawCircle(Color.White, rad * 0.25f, Offset(x - rad * 0.3f, y - rad * 0.3f), alpha = 0.35f)
                i++
            }
        }
        // The net: diamonds.
        val step = w * 0.09f
        clipPath(body) {
            var d = -h
            while (d < w + h) {
                drawLine(net, Offset(d, top), Offset(d + (bottom - top), bottom), 1.3f)
                drawLine(net, Offset(d + (bottom - top), top), Offset(d, bottom), 1.3f)
                d += step
            }
        }
        drawPath(body, net, style = Stroke(2f))
        drawLine(net, Offset(cx - half * 0.6f, top), Offset(cx + half * 0.6f, top), 3f)
        // Handles.
        val hc = if (load > 1f) warn else net
        for (sx in listOf(-1f, 1f)) {
            val loop = Path().apply {
                moveTo(cx + sx * half * 0.55f, top)
                cubicTo(cx + sx * half * 0.7f, h * 0.02f, cx + sx * half * 0.05f, h * 0.02f, cx + sx * half * 0.1f, top)
            }
            drawPath(loop, hc, style = Stroke(3f))
        }
    }
}

/**
 * The rank's enamel badge: a brass-rimmed shield with a star for every rank above the first and
 * a red bar across for the senior ones.
 */
@Composable
fun RankBadge(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 0.1f, h * 0.08f)
            lineTo(w * 0.9f, h * 0.08f)
            lineTo(w * 0.9f, h * 0.55f)
            cubicTo(w * 0.9f, h * 0.78f, w * 0.65f, h * 0.9f, w * 0.5f, h * 0.97f)
            cubicTo(w * 0.35f, h * 0.9f, w * 0.1f, h * 0.78f, w * 0.1f, h * 0.55f)
            close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(Color(0xFFE2BC6A), Color(0xFFA3772C))))
        val inner = Path().apply {
            moveTo(w * 0.17f, h * 0.14f)
            lineTo(w * 0.83f, h * 0.14f)
            lineTo(w * 0.83f, h * 0.54f)
            cubicTo(w * 0.83f, h * 0.73f, w * 0.62f, h * 0.83f, w * 0.5f, h * 0.89f)
            cubicTo(w * 0.38f, h * 0.83f, w * 0.17f, h * 0.73f, w * 0.17f, h * 0.54f)
            close()
        }
        drawPath(inner, Color(0xFF2E5B4C))
        drawCircle(Color.White, w * 0.3f, Offset(w * 0.35f, h * 0.25f), alpha = 0.08f)
        if (level >= 4) drawRect(Color(0xFFB4372B), Offset(w * 0.17f, h * 0.2f), Size(w * 0.66f, h * 0.09f))
        // Stars, up to three in a row, then a second row.
        val stars = level.coerceIn(0, 6)
        val rows = if (stars > 3) listOf(3, stars - 3) else listOf(stars)
        rows.forEachIndexed { ri, n ->
            for (i in 0 until n) {
                val cx = w * 0.5f + (i - (n - 1) / 2f) * w * 0.2f
                val cy = h * (if (rows.size == 1) 0.52f else 0.42f + ri * 0.2f)
                star(Offset(cx, cy), w * 0.085f)
            }
        }
        if (stars == 0) drawCircle(Color(0xFFE2BC6A), w * 0.06f, Offset(w * 0.5f, h * 0.5f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.star(c: Offset, r: Float) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = c.x + (cos(a) * rr).toFloat()
        val y = c.y + (sin(a) * rr).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, Color(0xFFF0D58E))
}
