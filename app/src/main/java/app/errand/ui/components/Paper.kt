package app.errand.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import app.errand.ui.theme.Sign
import kotlin.random.Random

/** The courier's client marks: each household member's own stamp ink. */
val MARKS = listOf(
    Color(0xFF4A4C93), Color(0xFFB4372B), Color(0xFF2E7A5C), Color(0xFFB0832F),
    Color(0xFF8A3E78), Color(0xFF2F6E9A), Color(0xFF7A5A33), Color(0xFF5E6B2E),
)

fun markColor(mark: Int) = MARKS[((mark % MARKS.size) + MARKS.size) % MARKS.size]

/**
 * Rubber-stamp wear: specks of ink missing, the same for the same [seed], so a stamp
 * looks pressed once and stays that way.
 */
fun Modifier.worn(seed: Int, amount: Float = 1f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val r = Random(seed)
        val n = (size.width * size.height / 90f * amount).toInt().coerceIn(12, 900)
        repeat(n) {
            val x = r.nextFloat() * size.width
            val y = r.nextFloat() * size.height
            drawCircle(Color.Black, radius = 0.6f + r.nextFloat() * 1.8f * density / 2.6f, center = Offset(x, y), alpha = 0.35f + r.nextFloat() * 0.6f, blendMode = BlendMode.DstOut)
        }
        // A stamp never prints evenly: one side a little lighter.
        drawRect(Color.Black, alpha = 0.12f + r.nextFloat() * 0.1f, topLeft = Offset(if (r.nextBoolean()) 0f else size.width / 2, 0f), size = size.copy(width = size.width / 2), blendMode = BlendMode.DstOut)
    }

/** A rubber stamp: capitals in a frame, at a slant. */
@Composable
fun Stamp(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    angle: Float = -6f,
    size: TextUnit = 15.sp,
    sub: String? = null,
    seed: Int = text.hashCode(),
) {
    Box(
        modifier
            .rotate(angle)
            .worn(seed)
            .border(2.dp, color, S.RXs)
            .padding(2.5.dp)
            .border(1.dp, color, RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text.uppercase(), fontFamily = Sign, fontSize = size, color = color, letterSpacing = 0.06.sp * size.value, maxLines = 1)
            if (sub != null) Text(sub.uppercase(), fontFamily = Sign, fontSize = size * 0.55f, color = color, letterSpacing = 1.sp, maxLines = 1)
        }
    }
}

/** A round stamp with a letter: whose order a line is. */
@Composable
fun ClientMark(name: String, mark: Int, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val c = markColor(mark)
    Box(
        modifier.size(size).worn(name.hashCode() + mark, 0.6f).border(1.6.dp, c, CircleShape).padding(2.dp).border(0.8.dp, c.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().take(1).uppercase(), fontFamily = Sign, fontSize = (size.value * 0.46f).sp, color = c, textAlign = TextAlign.Center)
    }
}

/**
 * Waybill paper: cream, ruled in pale blue, with a perforated tear-off top edge and a red margin rule.
 * [ruled] spacing in dp; 0 for plain paper.
 */
fun Modifier.waybill(paper: Color, rule: Color, margin: Color, ruled: Dp = 0.dp, perforated: Boolean = true): Modifier = this
    .drawBehind {
        drawRect(paper)
        if (ruled > 0.dp) {
            val step = ruled.toPx()
            var y = step * 2.4f
            while (y < size.height - 6f) {
                drawLine(rule, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            drawLine(margin.copy(alpha = 0.45f), Offset(36.dp.toPx(), 0f), Offset(36.dp.toPx(), size.height), 1.4f)
        }
        if (perforated) {
            val r = 3.dp.toPx()
            var x = r * 2
            while (x < size.width) {
                drawCircle(Color(0xFFE9DFCB), r, Offset(x, 0f))
                x += r * 3.4f
            }
        }
    }

@Composable
fun PaperCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, ruled: Dp = 0.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(S.RXs)
            .waybill(S.c.paper, S.c.rule, S.c.stamp, ruled, perforated = true)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        content = content,
    )
}

/**
 * A sheet that slides up over the screen, drawn inside the window so it shows in the screen renders.
 * The back gesture and the scrim close it.
 */
@Composable
fun BoxScope.Sheet(visible: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x66201A14)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose))
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(S.c.paper)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(S.c.line))
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** A typed label and value on one line, dotted between, like a form. */
@Composable
fun FormLine(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = S.c.ink) {
    androidx.compose.foundation.layout.Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Bottom) {
        Text(label, style = SType.Typed.copy(color = S.c.muted))
        val dots = S.c.line
        Box(Modifier.weight(1f).height(14.dp).padding(horizontal = 6.dp).drawBehind {
            var x = 0f
            while (x < size.width) {
                drawCircle(dots, 1.2f, Offset(x, size.height - 3f))
                x += 7f
            }
        })
        Text(value, style = SType.Typed.copy(color = valueColor))
    }
}

/** "4,2 кг", "850 г". */
fun weight(grams: Int, ru: Boolean): String = when {
    grams < 1000 -> "$grams ${if (ru) "г" else "g"}"
    else -> {
        val kg = grams / 1000.0
        val s = if (kg >= 10) "%.0f".format(kg) else "%.1f".format(kg)
        "${if (ru) s.replace('.', ',') else s.replace(',', '.')} ${if (ru) "кг" else "kg"}"
    }
}
