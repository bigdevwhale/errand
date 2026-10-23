package app.errand.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.errand.domain.Upgrade
import app.errand.ui.theme.Sign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** What the depot looks like: what was bought, the hour (for the sky, the lamp and the clock), the sign's words. */
@Immutable
data class DepotLook(
    val owned: Set<Upgrade>,
    val hour: Int,
    val minute: Int,
    val sign: String,
    /** An upgrade shown faintly, as it would stand, before it's bought. */
    val preview: Upgrade? = null,
    /** Out on a run: the vehicle (or the bag) is gone from the door. */
    val out: Boolean = false,
)

private object P {
    val skyDay = Color(0xFFD9E4E0)
    val skyDayLow = Color(0xFFEFE8D6)
    val skyEve = Color(0xFFE8B98F)
    val skyEveLow = Color(0xFFF2DDB8)
    val skyNight = Color(0xFF26304A)
    val skyNightLow = Color(0xFF3E4766)
    val roofs = Color(0xFFBFB39C)
    val roofsDark = Color(0xFF5A5468)
    val plaster = Color(0xFFDDBF8F)
    val plasterShade = Color(0xFFC9A675)
    val painted = Color(0xFFB9CDB4)
    val paintedShade = Color(0xFF9DB598)
    val trim = Color(0xFFF3EBDA)
    val brick = Color(0xFFA85A3C)
    val brickDark = Color(0xFF7E412B)
    val wood = Color(0xFF6B4A2E)
    val woodDark = Color(0xFF4A3220)
    val glass = Color(0xFF8FA8A6)
    val glassDark = Color(0xFF5E7676)
    val glow = Color(0xFFF4D58A)
    val enamel = Color(0xFF2E5B4C)
    val cream = Color(0xFFF6EFDF)
    val iron = Color(0xFF2C2C2E)
    val pavement = Color(0xFFB7AE9E)
    val pavementLine = Color(0xFF9E9584)
    val road = Color(0xFF8C8578)
    val curb = Color(0xFFD3CBBB)
    val red = Color(0xFFB4372B)
    val blue = Color(0xFF2F5E8C)
    val brass = Color(0xFFC79A45)
    val leaf = Color(0xFF4F7A3E)
}

/** The scene is laid out on a 360 × 220 plan and scaled to the width it's given. */
private const val W = 360f
private const val H = 220f

@Composable
fun DepotScene(look: DepotLook, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val style = remember { TextStyle(fontFamily = Sign, fontSize = 11.sp) }
    Canvas(modifier) {
        val k = size.width / W
        withTransform({ scale(k, k, Offset.Zero) }) { depot(look, tm, style, k) }
    }
}

/** 0 day, 1 evening, 2 night. */
private fun phase(hour: Int) = when (hour) {
    in 7..16 -> 0
    in 17..20, 6 -> 1
    else -> 2
}

private fun DrawScope.depot(look: DepotLook, tm: TextMeasurer, style: TextStyle, k: Float) {
    val ph = phase(look.hour)
    val has = { u: Upgrade -> u in look.owned }
    val ghost = { u: Upgrade -> look.preview == u && u !in look.owned }
    val shows = { u: Upgrade -> has(u) || ghost(u) }
    val lit = ph != 0

    // Sky.
    val (top, low) = when (ph) {
        0 -> P.skyDay to P.skyDayLow
        1 -> P.skyEve to P.skyEveLow
        else -> P.skyNight to P.skyNightLow
    }
    drawRect(Brush.verticalGradient(listOf(top, low), 0f, 190f), size = Size(W, 192f))
    if (ph == 2) {
        val r = kotlin.random.Random(7)
        repeat(26) { drawCircle(P.cream, 0.5f + r.nextFloat() * 0.7f, Offset(r.nextFloat() * W, r.nextFloat() * 90f), alpha = 0.5f + r.nextFloat() * 0.5f) }
        drawCircle(P.cream, 9f, Offset(312f, 34f))
        drawCircle(top, 8f, Offset(316f, 31f))
    } else if (ph == 1) {
        drawCircle(Color(0xFFF7D9A0), 16f, Offset(40f, 150f), alpha = 0.8f)
    }

    // Rooftops beyond.
    val far = if (ph == 2) P.roofsDark else P.roofs
    val roofs = Path().apply {
        moveTo(0f, 120f); lineTo(0f, 92f); lineTo(30f, 92f); lineTo(30f, 76f); lineTo(58f, 76f); lineTo(58f, 96f)
        lineTo(300f, 96f); lineTo(300f, 70f); lineTo(318f, 58f); lineTo(336f, 70f); lineTo(336f, 88f); lineTo(W, 88f); lineTo(W, 120f); close()
    }
    drawPath(roofs, far)
    drawRect(far.copy(alpha = 0.9f), Offset(44f, 66f), Size(6f, 12f))
    drawRect(far.copy(alpha = 0.9f), Offset(326f, 54f), Size(5f, 12f))

    val second = shows(Upgrade.SECOND_FLOOR)
    val fx = 70f
    val fw = 220f
    val ground = 190f
    val floorTop = 108f
    val roofY = if (second) 46f else floorTop
    val wall = if (has(Upgrade.PAINT)) P.painted else P.plaster
    val wallShade = if (has(Upgrade.PAINT)) P.paintedShade else P.plasterShade

    // Upper floor (bought or previewed).
    if (second) {
        val a = if (ghost(Upgrade.SECOND_FLOOR)) 0.4f else 1f
        drawRect(wall, Offset(fx, roofY), Size(fw, floorTop - roofY), alpha = a)
        drawRect(P.trim, Offset(fx - 4f, roofY - 6f), Size(fw + 8f, 7f), alpha = a)
        drawRect(wallShade, Offset(fx, floorTop - 4f), Size(fw, 4f), alpha = a)
        for (wx in listOf(96f, 160f, 224f)) {
            drawRect(P.trim, Offset(wx - 3f, 60f), Size(46f, 40f), alpha = a)
            drawRect(if (lit) P.glow else P.glass, Offset(wx, 63f), Size(40f, 34f), alpha = a)
            drawRect(P.trim, Offset(wx + 19f, 63f), Size(2f, 34f), alpha = a)
            drawRect(P.trim, Offset(wx, 78f), Size(40f, 2f), alpha = a)
        }
    } else {
        // A flat roof with a parapet.
        drawRect(P.trim, Offset(fx - 4f, floorTop - 6f), Size(fw + 8f, 7f))
    }

    // Roof things: the flag and the dovecote.
    val roofLine = if (second) roofY - 6f else floorTop - 6f
    if (shows(Upgrade.FLAG)) {
        val a = if (ghost(Upgrade.FLAG)) 0.4f else 1f
        drawRect(P.iron, Offset(96f, roofLine - 40f), Size(2f, 40f), alpha = a)
        val flag = Path().apply { moveTo(98f, roofLine - 40f); lineTo(124f, roofLine - 35f); lineTo(98f, roofLine - 29f); close() }
        drawPath(flag, P.red, alpha = a)
    }
    if (shows(Upgrade.DOVECOTE)) {
        val a = if (ghost(Upgrade.DOVECOTE)) 0.4f else 1f
        val x = 236f
        val y = roofLine
        drawRect(P.woodDark, Offset(x + 12f, y - 14f), Size(3f, 14f), alpha = a)
        drawRect(P.wood, Offset(x, y - 34f), Size(28f, 20f), alpha = a)
        val roof = Path().apply { moveTo(x - 4f, y - 34f); lineTo(x + 14f, y - 46f); lineTo(x + 32f, y - 34f); close() }
        drawPath(roof, P.brickDark, alpha = a)
        drawRect(P.iron, Offset(x + 5f, y - 28f), Size(6f, 7f), alpha = a)
        drawRect(P.iron, Offset(x + 17f, y - 28f), Size(6f, 7f), alpha = a)
        // Two doves.
        for ((dx, dy) in listOf(42f to -50f, 54f to -58f)) {
            drawOval(P.cream, Offset(x + dx, y + dy), Size(8f, 4f), alpha = a)
            drawLine(P.cream, Offset(x + dx + 4f, y + dy + 1f), Offset(x + dx + 1f, y + dy - 3f), 1.5f, alpha = a)
            drawLine(P.cream, Offset(x + dx + 4f, y + dy + 1f), Offset(x + dx + 8f, y + dy - 3f), 1.5f, alpha = a)
        }
    }

    // Ground floor.
    drawRect(wall, Offset(fx, floorTop), Size(fw, ground - floorTop))
    drawRect(wallShade, Offset(fx, floorTop), Size(6f, ground - floorTop))
    drawRect(P.brick, Offset(fx, ground - 10f), Size(fw, 10f))
    for (bx in 0 until 22) drawLine(P.brickDark, Offset(fx + bx * 10f + 5f, ground - 10f), Offset(fx + bx * 10f + 5f, ground - 5f), 0.8f)
    drawLine(P.brickDark, Offset(fx, ground - 5f), Offset(fx + fw, ground - 5f), 0.8f)

    // The sign band.
    val signRect = Rect(90f, 112f, 270f, 130f)
    if (shows(Upgrade.SIGN)) {
        val a = if (ghost(Upgrade.SIGN)) 0.45f else 1f
        drawRoundRect(P.enamel, signRect.topLeft, signRect.size, CornerRadius(3f), alpha = a)
        drawRoundRect(P.cream, signRect.topLeft + Offset(2f, 2f), Size(signRect.width - 4f, signRect.height - 4f), CornerRadius(2f), style = Stroke(1f), alpha = a)
        for (rx in listOf(signRect.left + 5f, signRect.right - 5f)) for (ry in listOf(signRect.top + 5f, signRect.bottom - 5f)) drawCircle(P.cream, 1f, Offset(rx, ry), alpha = a)
        text(tm, look.sign, style.copy(color = P.cream), signRect, k, a)
    }
    if (!has(Upgrade.SIGN)) {
        // A plank with the name painted by hand.
        if (!ghost(Upgrade.SIGN)) {
            drawRect(P.wood, signRect.topLeft, signRect.size)
            drawLine(P.woodDark, Offset(signRect.left, signRect.center.y), Offset(signRect.right, signRect.center.y), 0.6f)
            text(tm, look.sign, style.copy(color = P.trim.copy(alpha = 0.85f)), signRect, k, 1f)
        }
    }

    // The shop window, left.
    val win = Rect(90f, 138f, 172f, 178f)
    drawRect(P.woodDark, win.topLeft - Offset(3f, 3f), Size(win.width + 6f, win.height + 6f))
    drawRect(if (lit) P.glow else P.glass, win.topLeft, win.size)
    if (!lit) {
        val sheen = Path().apply { moveTo(win.left + 8f, win.bottom); lineTo(win.left + 30f, win.top); lineTo(win.left + 40f, win.top); lineTo(win.left + 18f, win.bottom); close() }
        drawPath(sheen, Color.White, alpha = 0.16f)
    }
    drawRect(P.woodDark, Offset(win.left + 39f, win.top), Size(3f, win.height))
    // Parcels stacked behind the glass.
    drawRect(Color(0xFFB98D58), Offset(win.left + 6f, win.bottom - 14f), Size(18f, 14f))
    drawLine(Color(0xFF7E5A33), Offset(win.left + 15f, win.bottom - 14f), Offset(win.left + 15f, win.bottom), 1f)
    drawRect(Color(0xFFCCA670), Offset(win.left + 9f, win.bottom - 24f), Size(13f, 10f))
    if (shows(Upgrade.PHONE)) {
        val a = if (ghost(Upgrade.PHONE)) 0.45f else 1f
        val x = win.left + 58f
        val y = win.bottom
        drawRect(P.iron, Offset(x - 6f, y - 4f), Size(12f, 4f), alpha = a)
        drawRect(P.iron, Offset(x - 1.5f, y - 22f), Size(3f, 18f), alpha = a)
        drawOval(P.iron, Offset(x - 4f, y - 25f), Size(8f, 5f), alpha = a)
        drawRect(P.iron, Offset(x + 4f, y - 20f), Size(2f, 10f), alpha = a)
        drawOval(P.iron, Offset(x + 2f, y - 22f), Size(6f, 4f), alpha = a)
    }
    // Sill.
    drawRect(P.trim, Offset(win.left - 5f, win.bottom + 3f), Size(win.width + 10f, 4f))
    if (shows(Upgrade.FLOWERS)) {
        val a = if (ghost(Upgrade.FLOWERS)) 0.45f else 1f
        for (i in 0 until 3) {
            val px = win.left + 10f + i * 28f
            drawRect(P.brick, Offset(px, win.bottom - 4f), Size(12f, 7f), alpha = a)
            for (j in 0 until 4) {
                val ang = -PI / 2 + (j - 1.5) * 0.5
                val tip = Offset(px + 6f + (cos(ang) * 7).toFloat(), win.bottom - 4f + (sin(ang) * 7).toFloat())
                drawLine(P.leaf, Offset(px + 6f, win.bottom - 4f), tip, 1.4f, alpha = a)
                drawCircle(if (j % 2 == 0) P.red else Color(0xFFE9A447), 2.2f, tip, alpha = a)
            }
        }
    }

    // The door.
    val door = Rect(190f, 138f, 224f, 190f)
    drawRect(P.woodDark, door.topLeft - Offset(3f, 3f), Size(door.width + 6f, door.height + 3f))
    drawRect(if (has(Upgrade.PAINT)) P.enamel else P.wood, door.topLeft, door.size)
    drawRect(if (lit) P.glow else P.glassDark, Offset(door.left + 6f, door.top + 6f), Size(door.width - 12f, 20f))
    drawRect(P.woodDark, Offset(door.left + 6f, door.top + 32f), Size(door.width - 12f, 14f), style = Stroke(1f))
    drawCircle(P.brass, 1.8f, Offset(door.right - 6f, door.top + 30f))
    // The string bag on its hook, when there's nothing to ride.
    val ride = Upgrade.ride(look.owned)
    if (ride == null && !look.out) bag(Offset(232f, 150f))

    // The small window, right.
    val small = Rect(238f, 140f, 268f, 170f)
    drawRect(P.woodDark, small.topLeft - Offset(2f, 2f), Size(small.width + 4f, small.height + 4f))
    drawRect(if (lit) P.glow else P.glass, small.topLeft, small.size)
    drawRect(P.woodDark, Offset(small.center.x - 1f, small.top), Size(2f, small.height))
    drawRect(P.woodDark, Offset(small.left, small.center.y - 1f), Size(small.width, 2f))

    if (shows(Upgrade.MAILBOX)) {
        val a = if (ghost(Upgrade.MAILBOX)) 0.45f else 1f
        val x = 274f
        drawRoundRect(P.blue, Offset(x, 146f), Size(13f, 20f), CornerRadius(3f), alpha = a)
        drawRect(P.iron, Offset(x + 3f, 152f), Size(7f, 1.6f), alpha = a)
        drawRect(P.cream, Offset(x + 3f, 157f), Size(7f, 4f), alpha = a)
    }

    if (shows(Upgrade.CLOCK)) {
        val a = if (ghost(Upgrade.CLOCK)) 0.45f else 1f
        val c = Offset(180f, 100f).let { if (second) it else Offset(180f, floorTop - 16f) }
        drawRect(P.iron, Offset(c.x - 1f, c.y + 10f), Size(2f, if (second) 12f else 10f), alpha = a)
        drawCircle(P.iron, 12f, c, alpha = a)
        drawCircle(P.cream, 10f, c, alpha = a)
        for (i in 0 until 12) {
            val ang = i * PI / 6
            drawLine(P.iron, c + Offset((sin(ang) * 8.5).toFloat(), (-cos(ang) * 8.5).toFloat()), c + Offset((sin(ang) * 10).toFloat(), (-cos(ang) * 10).toFloat()), 0.8f, alpha = a)
        }
        val hAng = ((look.hour % 12) + look.minute / 60.0) * PI / 6
        val mAng = look.minute * PI / 30
        drawLine(P.iron, c, c + Offset((sin(hAng) * 5.5).toFloat(), (-cos(hAng) * 5.5).toFloat()), 1.6f, alpha = a)
        drawLine(P.iron, c, c + Offset((sin(mAng) * 8).toFloat(), (-cos(mAng) * 8).toFloat()), 1f, alpha = a)
        drawCircle(P.red, 1.2f, c, alpha = a)
    }

    // Pavement and road.
    drawRect(P.pavement, Offset(0f, ground), Size(W, 18f))
    for (i in 0 until 19) drawLine(P.pavementLine, Offset(i * 20f + 6f, ground), Offset(i * 20f, ground + 18f), 0.7f)
    drawRect(P.curb, Offset(0f, ground + 18f), Size(W, 3f))
    drawRect(P.road, Offset(0f, ground + 21f), Size(W, H - ground - 21f))

    if (shows(Upgrade.LAMP)) {
        val a = if (ghost(Upgrade.LAMP)) 0.45f else 1f
        val x = 38f
        if (lit && has(Upgrade.LAMP)) drawCircle(Brush.radialGradient(listOf(P.glow.copy(alpha = 0.55f), Color.Transparent), Offset(x, 112f), 46f), 46f, Offset(x, 112f))
        drawRect(P.iron, Offset(x - 1.5f, 116f), Size(3f, ground - 116f + 6f), alpha = a)
        drawRect(P.iron, Offset(x - 5f, ground + 2f), Size(10f, 4f), alpha = a)
        val head = Path().apply { moveTo(x - 7f, 106f); lineTo(x + 7f, 106f); lineTo(x + 4f, 118f); lineTo(x - 4f, 118f); close() }
        drawPath(head, if (lit) P.glow else P.glass, alpha = a)
        drawPath(head, P.iron, style = Stroke(1.2f), alpha = a)
        drawRect(P.iron, Offset(x - 8f, 104f), Size(16f, 2.5f), alpha = a)
    }

    // The ride at the door, or the one previewed.
    val shown = look.preview?.takeIf { it.fleet && it !in look.owned } ?: ride?.takeIf { !look.out }
    when (shown) {
        Upgrade.BICYCLE -> bicycle(Offset(118f, 205f), if (shown == look.preview) 0.45f else 1f)
        Upgrade.TRICYCLE -> tricycle(Offset(112f, 205f), if (shown == look.preview) 0.45f else 1f)
        Upgrade.SCOOTER -> scooter(Offset(110f, 205f), if (shown == look.preview) 0.45f else 1f)
        Upgrade.VAN -> van(Offset(84f, 208f), if (shown == look.preview) 0.45f else 1f)
        else -> Unit
    }
}

private fun DrawScope.text(tm: TextMeasurer, s: String, style: TextStyle, r: Rect, k: Float, alpha: Float) {
    // Measured in screen pixels (the plan is scaled by k), fitted to the board, drawn back in plan units.
    var px = r.height * 0.62f * k
    var m = tm.measure(s.uppercase(), style.copy(fontSize = px.toSp(), letterSpacing = (px * 0.12f).toSp()))
    val room = r.width * 0.9f * k
    if (m.size.width > room) {
        px *= room / m.size.width
        m = tm.measure(s.uppercase(), style.copy(fontSize = px.toSp(), letterSpacing = (px * 0.12f).toSp()))
    }
    val w = m.size.width / k
    val h = m.size.height / k
    withTransform({
        translate(r.center.x - w / 2, r.center.y - h / 2)
        scale(1 / k, 1 / k, Offset.Zero)
    }) { drawText(m, alpha = alpha) }
}

/** A string bag hanging on a hook: the courier on foot. */
private fun DrawScope.bag(hook: Offset) {
    drawCircle(P.iron, 1.5f, hook)
    val net = Color(0xFF7A6A4C)
    val body = Path().apply {
        moveTo(hook.x - 2f, hook.y + 2f)
        cubicTo(hook.x - 12f, hook.y + 10f, hook.x - 10f, hook.y + 30f, hook.x, hook.y + 32f)
        cubicTo(hook.x + 10f, hook.y + 30f, hook.x + 12f, hook.y + 10f, hook.x + 2f, hook.y + 2f)
    }
    drawPath(body, net, style = Stroke(1.2f))
    for (i in 1..4) {
        val y = hook.y + 4f + i * 6f
        val half = 9f - kotlin.math.abs(i - 2.5f) * 1.6f
        drawLine(net, Offset(hook.x - half, y), Offset(hook.x + half, y), 0.8f)
    }
    for (i in -2..2) drawLine(net, Offset(hook.x + i * 1f, hook.y + 3f), Offset(hook.x + i * 4f, hook.y + 31f), 0.8f)
    drawCircle(Color(0xFFE2A13B), 3.5f, Offset(hook.x - 3f, hook.y + 26f))
    drawCircle(Color(0xFF8BAF4A), 3f, Offset(hook.x + 3.5f, hook.y + 27f))
}

private fun DrawScope.wheel(c: Offset, r: Float, a: Float) {
    drawCircle(P.iron, r, c, style = Stroke(2.2f), alpha = a)
    for (i in 0 until 8) {
        val ang = i * PI / 4
        drawLine(P.iron, c, c + Offset((cos(ang) * r).toFloat(), (sin(ang) * r).toFloat()), 0.5f, alpha = a)
    }
    drawCircle(P.iron, 1.6f, c, alpha = a)
}

/** A black roadster with a wicker basket on the front. [o] is the ground under the back wheel. */
private fun DrawScope.bicycle(o: Offset, a: Float) {
    val r = 11f
    val back = Offset(o.x, o.y - r)
    val front = Offset(o.x + 44f, o.y - r)
    wheel(back, r, a)
    wheel(front, r, a)
    val crank = Offset(o.x + 18f, o.y - r)
    val seat = Offset(o.x + 12f, o.y - 32f)
    val head = Offset(o.x + 38f, o.y - 30f)
    val frame = P.enamel
    drawLine(frame, back, crank, 2f, alpha = a)
    drawLine(frame, back, seat, 2f, alpha = a)
    drawLine(frame, seat, crank, 2f, alpha = a)
    drawLine(frame, crank, head, 2f, alpha = a)
    drawLine(frame, seat + Offset(2f, 3f), head + Offset(0f, 2f), 2f, alpha = a)
    drawLine(frame, head, front, 2f, alpha = a)
    drawLine(P.iron, head, head + Offset(-4f, -6f), 1.6f, alpha = a)
    drawLine(P.iron, head + Offset(-4f, -6f), head + Offset(-10f, -6f), 1.8f, alpha = a)
    drawRoundRect(P.iron, seat + Offset(-5f, -3f), Size(10f, 3f), CornerRadius(1.5f), alpha = a)
    drawCircle(P.iron, 3f, crank, style = Stroke(1f), alpha = a)
    // Mudguard and basket.
    drawArc(P.iron, 200f, 120f, false, Offset(front.x - r - 2f, front.y - r - 2f), Size(2 * r + 4f, 2 * r + 4f), style = Stroke(1.4f), alpha = a)
    val basket = Rect(head.x + 1f, head.y - 4f, head.x + 18f, head.y + 8f)
    drawRect(Color(0xFFB38A4E), basket.topLeft, basket.size, alpha = a)
    for (i in 1..3) drawLine(Color(0xFF7E5A33), Offset(basket.left, basket.top + i * 3f), Offset(basket.right, basket.top + i * 3f), 0.7f, alpha = a)
    drawCircle(Color(0xFFE2A13B), 3f, Offset(basket.left + 5f, basket.top - 1f), alpha = a)
    drawRect(Color(0xFFE9DFCB), Offset(basket.left + 9f, basket.top - 7f), Size(5f, 7f), alpha = a)
}

/** A cargo trike: a box on two front wheels with the office's name on it. */
private fun DrawScope.tricycle(o: Offset, a: Float) {
    val r = 10f
    val back = Offset(o.x + 62f, o.y - r)
    val front = Offset(o.x + 14f, o.y - r)
    val box = Rect(o.x, o.y - 46f, o.x + 34f, o.y - 18f)
    wheel(front, r, a)
    wheel(back, r, a)
    drawRect(P.enamel, box.topLeft, box.size, alpha = a)
    drawRect(P.cream, box.topLeft + Offset(2f, 2f), Size(box.width - 4f, box.height - 4f), style = Stroke(0.8f), alpha = a)
    drawRect(P.red, Offset(box.left + 6f, box.top + 10f), Size(box.width - 12f, 7f), alpha = a)
    drawRect(Color(0xFFCCA670), Offset(box.left + 4f, box.top - 8f), Size(14f, 8f), alpha = a)
    val seat = Offset(o.x + 56f, o.y - 34f)
    val crank = Offset(o.x + 50f, o.y - r)
    drawLine(P.enamel, box.bottomRight + Offset(0f, -4f), crank, 2.2f, alpha = a)
    drawLine(P.enamel, crank, back, 2f, alpha = a)
    drawLine(P.enamel, crank, seat, 2f, alpha = a)
    drawLine(P.enamel, seat, back, 2f, alpha = a)
    drawLine(P.iron, Offset(box.right, box.top + 2f), Offset(box.right + 8f, box.top - 6f), 1.6f, alpha = a)
    drawLine(P.iron, Offset(box.right + 4f, box.top - 6f), Offset(box.right + 12f, box.top - 6f), 1.8f, alpha = a)
    drawRoundRect(P.iron, seat + Offset(-5f, -3f), Size(10f, 3f), CornerRadius(1.5f), alpha = a)
}

/** A little three-wheeled motor scooter with a cargo box behind. */
private fun DrawScope.scooter(o: Offset, a: Float) {
    val r = 8f
    val front = Offset(o.x + 8f, o.y - r)
    val back = Offset(o.x + 58f, o.y - r)
    wheel(front, r, a)
    wheel(back, r, a)
    val body = Path().apply {
        moveTo(o.x + 4f, o.y - 22f); lineTo(o.x + 14f, o.y - 40f); lineTo(o.x + 20f, o.y - 40f); lineTo(o.x + 18f, o.y - 22f)
        lineTo(o.x + 40f, o.y - 22f); lineTo(o.x + 44f, o.y - 12f); lineTo(o.x + 14f, o.y - 12f); close()
    }
    drawPath(body, Color(0xFF8C3A2E), alpha = a)
    drawLine(P.iron, Offset(o.x + 16f, o.y - 40f), Offset(o.x + 12f, o.y - 48f), 1.6f, alpha = a)
    drawLine(P.iron, Offset(o.x + 7f, o.y - 48f), Offset(o.x + 18f, o.y - 48f), 1.8f, alpha = a)
    drawCircle(P.glow, 2.5f, Offset(o.x + 11f, o.y - 36f), alpha = a)
    drawRoundRect(P.iron, Offset(o.x + 28f, o.y - 27f), Size(14f, 4f), CornerRadius(2f), alpha = a)
    val box = Rect(o.x + 44f, o.y - 42f, o.x + 74f, o.y - 16f)
    drawRect(P.enamel, box.topLeft, box.size, alpha = a)
    drawRect(P.cream, box.topLeft + Offset(2f, 2f), Size(box.width - 4f, box.height - 4f), style = Stroke(0.8f), alpha = a)
    drawRect(P.red, Offset(box.left + 5f, box.top + 9f), Size(box.width - 10f, 6f), alpha = a)
    drawLine(P.iron, Offset(o.x + 40f, o.y - 12f), back, 2f, alpha = a)
}

/** A rounded thirties van in enamel green with a red stripe. */
private fun DrawScope.van(o: Offset, a: Float) {
    val body = Path().apply {
        moveTo(o.x, o.y - 12f)
        lineTo(o.x, o.y - 40f)
        cubicTo(o.x, o.y - 52f, o.x + 6f, o.y - 56f, o.x + 18f, o.y - 56f)
        lineTo(o.x + 100f, o.y - 56f)
        lineTo(o.x + 104f, o.y - 52f)
        lineTo(o.x + 104f, o.y - 36f)
        lineTo(o.x + 126f, o.y - 34f)
        cubicTo(o.x + 136f, o.y - 32f, o.x + 140f, o.y - 26f, o.x + 140f, o.y - 18f)
        lineTo(o.x + 140f, o.y - 12f)
        close()
    }
    drawPath(body, P.enamel, alpha = a)
    // Cab window, the stripe, the wheels.
    val cab = Path().apply { moveTo(o.x + 106f, o.y - 50f); lineTo(o.x + 118f, o.y - 50f); lineTo(o.x + 126f, o.y - 36f); lineTo(o.x + 106f, o.y - 36f); close() }
    drawPath(cab, P.glass, alpha = a)
    drawRect(P.red, Offset(o.x, o.y - 30f), Size(104f, 5f), alpha = a)
    drawRect(P.cream, Offset(o.x + 14f, o.y - 50f), Size(76f, 16f), style = Stroke(1f), alpha = a)
    drawLine(P.iron.copy(alpha = 0.5f), Offset(o.x + 104f, o.y - 36f), Offset(o.x + 104f, o.y - 14f), 1f, alpha = a)
    drawCircle(P.glow, 3f, Offset(o.x + 137f, o.y - 22f), alpha = a)
    drawRect(P.iron, Offset(o.x - 2f, o.y - 14f), Size(144f, 3f), alpha = a)
    for (wx in listOf(o.x + 26f, o.x + 114f)) {
        drawCircle(P.iron, 11f, Offset(wx, o.y - 8f), alpha = a)
        drawCircle(Color(0xFF8C8578), 4.5f, Offset(wx, o.y - 8f), alpha = a)
    }
    // Two parcels on the roof rack.
    drawRect(Color(0xFFB98D58), Offset(o.x + 30f, o.y - 64f), Size(20f, 8f), alpha = a)
    drawRect(Color(0xFFCCA670), Offset(o.x + 54f, o.y - 62f), Size(14f, 6f), alpha = a)
}
