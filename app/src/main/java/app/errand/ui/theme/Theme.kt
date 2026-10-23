package app.errand.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.errand.R

/**
 * A courier office of the thirties: cream waybill paper, typewriter ink, red and violet rubber stamps,
 * enamel signs in bottle green, brass on the counter. Only the paper look.
 */
@Immutable
data class Palette(
    val bg: Color,
    val paper: Color,
    val paper2: Color,
    val line: Color,
    val rule: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** Enamel green: the thing to do. */
    val enamel: Color,
    val enamelSoft: Color,
    val onEnamel: Color,
    /** Red stamp ink: delivered, rush, the forgotten. */
    val stamp: Color,
    val stampSoft: Color,
    /** Violet copying ink: the cold box, the client marks. */
    val violet: Color,
    val violetSoft: Color,
    val brass: Color,
    val brassSoft: Color,
    val brick: Color,
    val sky: Color,
)

val Office = Palette(
    bg = Color(0xFFE9DFCB),
    paper = Color(0xFFF6EFDF),
    paper2 = Color(0xFFEFE5D0),
    line = Color(0xFFD5C6A8),
    rule = Color(0xFFC9D3DA),
    ink = Color(0xFF2A2520),
    muted = Color(0xFF675C4D),
    faint = Color(0xFF9A8D78),
    enamel = Color(0xFF2E5B4C),
    enamelSoft = Color(0xFFD3E0D6),
    onEnamel = Color(0xFFF6EFDF),
    stamp = Color(0xFFB4372B),
    stampSoft = Color(0xFFF2D8CF),
    violet = Color(0xFF4A4C93),
    violetSoft = Color(0xFFDDDDEE),
    brass = Color(0xFFB0832F),
    brassSoft = Color(0xFFF0E1BC),
    brick = Color(0xFFA85A3C),
    sky = Color(0xFFDCE6E3),
)

val LocalPalette = staticCompositionLocalOf { Office }

/** Russo One: the enamel sign and the stamps' capitals. */
val Sign = FontFamily(Font(R.font.russo_one, FontWeight.Normal))

/** PT Mono: everything typed on the waybill. */
val Type = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

/** Manrope: plain and clear, for everything around the paper. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat: notes written by hand in the margin. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object S {
    val c: Palette @Composable get() = LocalPalette.current

    val RLg = RoundedCornerShape(14.dp)
    val RMd = RoundedCornerShape(10.dp)
    val RSm = RoundedCornerShape(7.dp)
    val RXs = RoundedCornerShape(3.dp)
}

object SType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Sign, fontSize = 34.sp, lineHeight = 40.sp, color = S.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Sign, fontSize = 25.sp, lineHeight = 31.sp, color = S.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Sign, fontSize = 19.sp, lineHeight = 24.sp, color = S.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sign, fontSize = 12.sp, letterSpacing = 0.16.em, color = S.c.stamp)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = S.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = S.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = S.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Typed: TextStyle @Composable get() = TextStyle(fontFamily = Type, fontSize = 15.sp, lineHeight = 20.sp, color = S.c.ink)
    val TypedSmall: TextStyle @Composable get() = TextStyle(fontFamily = Type, fontSize = 12.5.sp, lineHeight = 16.sp, color = S.c.muted)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.errand.ui.theme.Hand, fontSize = 21.sp, lineHeight = 23.sp, color = S.c.violet)
}

@Composable
fun ErrandTheme(content: @Composable () -> Unit) {
    val p = Office
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.enamel,
                onPrimary = p.onEnamel,
                primaryContainer = p.enamelSoft,
                onPrimaryContainer = p.ink,
                secondary = p.stamp,
                background = p.bg,
                surface = p.paper,
                surfaceContainer = p.paper,
                surfaceContainerHigh = p.paper2,
                surfaceContainerHighest = p.paper2,
                surfaceContainerLow = p.paper,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = SType.Body,
                bodyMedium = SType.Body.copy(fontSize = 15.sp),
                labelLarge = SType.Button,
            ),
            content = content,
        )
    }
}
