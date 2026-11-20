package app.errand.ui.art

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.domain.Upgrade
import app.errand.ui.theme.Sign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** What the depot looks like: what was bought, the hour (for the light and the clock), the sign's words. */
@Immutable
data class DepotLook(
    val owned: Set<Upgrade>,
    val hour: Int,
    val minute: Int,
    val sign: String,
    /** An upgrade shown faintly, as it would stand, before it's bought. */
    val preview: Upgrade? = null,
    /** Out on a run: the vehicle is gone from the door. */
    val out: Boolean = false,
)

/**
 * The office is painted in four stages, each by day, evening and night (res/drawable-nodpi/depot_<stage>_<phase>).
 * A stage is reached with its key purchase: the enamel sign, the fresh paint, the second floor.
 * Over the painting go the sign's words, the clock's real hands and the vehicle at the door.
 */
fun stage(owned: Set<Upgrade>): Int = when {
    Upgrade.SECOND_FLOOR in owned -> 3
    Upgrade.PAINT in owned -> 2
    Upgrade.SIGN in owned -> 1
    else -> 0
}

/** The scene is laid out on a 360 × 220 plan and scaled to the width it's given. */
private const val W = 360f
private const val H = 220f

private val paintings = listOf(
    listOf(R.drawable.depot_0_day, R.drawable.depot_0_eve, R.drawable.depot_0_night),
    listOf(R.drawable.depot_1_day, R.drawable.depot_1_eve, R.drawable.depot_1_night),
    listOf(R.drawable.depot_2_day, R.drawable.depot_2_eve, R.drawable.depot_2_night),
    listOf(R.drawable.depot_3_day, R.drawable.depot_3_eve, R.drawable.depot_3_night),
)

/** Where things stand in each painting, in plan units. */
private class Plate(val sign: Rect, val clock: Offset?, val clockR: Float)

private val plates = listOf(
    Plate(Rect(49.2f, 79.7f, 308.9f, 100.1f), null, 0f),
    Plate(Rect(49.2f, 79.7f, 308.9f, 100.1f), Offset(210.9f, 39.8f), 11.7f),
    Plate(Rect(49.2f, 79.7f, 308.9f, 100.1f), Offset(210.9f, 39.8f), 11.7f),
    Plate(Rect(49.0f, 87.4f, 308.9f, 106.2f), Offset(207.4f, 14.1f), 9.4f),
)

/** A vehicle parked on the pavement before the shop window: its picture, left edge and height. */
private class Ride(@DrawableRes val res: Int, val x: Float, val h: Float)

private val rides = mapOf(
    Upgrade.BICYCLE to Ride(R.drawable.depot_bicycle, 60f, 46f),
    Upgrade.TRICYCLE to Ride(R.drawable.depot_tricycle, 56f, 46f),
    Upgrade.SCOOTER to Ride(R.drawable.depot_scooter, 62f, 50f),
    Upgrade.VAN to Ride(R.drawable.depot_van, 58f, 72f),
)

/** The wheels stand on this line. */
private const val KERB = 205f

private val cream = Color(0xFFF6EFDF)
private val iron = Color(0xFF2C2C2E)
private val red = Color(0xFFB4372B)

@Composable
fun DepotScene(look: DepotLook, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val style = remember { TextStyle(fontFamily = Sign, fontSize = 11.sp) }
    val ph = phase(look.hour)
    val now = stage(look.owned)
    val next = look.preview?.takeIf { !it.fleet && it !in look.owned }?.let { stage(look.owned + it) }?.takeIf { it > now }
    val back = ImageBitmap.imageResource(paintings[now][ph])
    val ghost = next?.let { ImageBitmap.imageResource(paintings[it][ph]) }
    val ridden = look.preview?.takeIf { it.fleet && it !in look.owned } ?: Upgrade.ride(look.owned)?.takeIf { !look.out }
    val ride = ridden?.let { rides[it] }
    val rideImage = ride?.let { ImageBitmap.imageResource(it.res) }
    val rideAlpha = if (ridden == look.preview) 0.45f else 1f
    Canvas(modifier) {
        val k = size.width / W
        painting(back, k, 1f)
        if (ghost != null) painting(ghost, k, 0.5f)
        val plate = plates[next ?: now]
        withTransform({ scale(k, k, Offset.Zero) }) {
            words(tm, look.sign, style.copy(color = if (now == 0 && next == null) cream.copy(alpha = 0.85f) else cream), plate.sign, k)
            plate.clock?.let { clock(it, plate.clockR, look.hour, look.minute) }
        }
        if (ride != null && rideImage != null) parked(rideImage, ride, ph, k, rideAlpha)
    }
}

/** 0 day, 1 evening, 2 night. */
private fun phase(hour: Int) = when (hour) {
    in 7..16 -> 0
    in 17..20, 6 -> 1
    else -> 2
}

private fun DrawScope.painting(img: ImageBitmap, k: Float, alpha: Float) {
    drawImage(img, dstSize = IntSize((W * k).roundToInt(), (H * k).roundToInt()), alpha = alpha, filterQuality = FilterQuality.High)
}

/** Daylight vehicles dimmed to the hour: warm at dusk, blue under the moon. */
private val dusk = ColorFilter.tint(Color(0xFFFFDCC0), BlendMode.Modulate)
private val moon = ColorFilter.tint(Color(0xFF8C96B4), BlendMode.Modulate)

private fun DrawScope.parked(img: ImageBitmap, ride: Ride, ph: Int, k: Float, alpha: Float) {
    val h = ride.h
    val w = h * img.width / img.height
    // A soft shadow on the cobbles.
    drawOval(Color.Black.copy(alpha = 0.18f * alpha), Offset((ride.x + w * 0.04f) * k, (KERB - 2.5f) * k), Size(w * 0.92f * k, 5f * k))
    drawImage(
        img,
        dstOffset = IntOffset((ride.x * k).roundToInt(), ((KERB - h) * k).roundToInt()),
        dstSize = IntSize((w * k).roundToInt(), (h * k).roundToInt()),
        alpha = alpha,
        colorFilter = when (ph) { 1 -> dusk; 2 -> moon; else -> null },
        filterQuality = FilterQuality.High,
    )
}

/** The clock on its bracket keeps real time. */
private fun DrawScope.clock(c: Offset, r: Float, hour: Int, minute: Int) {
    val hAng = ((hour % 12) + minute / 60.0) * PI / 6
    val mAng = minute * PI / 30
    drawLine(iron, c, c + Offset((sin(hAng) * r * 0.55).toFloat(), (-cos(hAng) * r * 0.55).toFloat()), r * 0.13f, StrokeCap.Round)
    drawLine(iron, c, c + Offset((sin(mAng) * r * 0.82).toFloat(), (-cos(mAng) * r * 0.82).toFloat()), r * 0.08f, StrokeCap.Round)
    drawCircle(red, r * 0.09f, c)
}

private fun DrawScope.words(tm: TextMeasurer, s: String, style: TextStyle, r: Rect, k: Float) {
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
    }) { drawText(m) }
}
