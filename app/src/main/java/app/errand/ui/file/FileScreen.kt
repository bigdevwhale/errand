package app.errand.ui.file

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.domain.Badge
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.art.RankBadge
import app.errand.ui.components.FormLine
import app.errand.ui.components.PaperCard
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.TopBar
import app.errand.ui.components.weight
import app.errand.ui.components.worn
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType

/** The personnel file: rank, the record, the badges pressed into it. */
@Composable
fun FileScreen(home: Home, go: Go) {
    val c = ctx()
    val s = home.stats
    Column(Modifier.fillMaxSize().background(S.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.file), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            PaperCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(home.rank.ordinal, Modifier.size(70.dp, 84.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(home.me?.name.orEmpty().ifEmpty { stringResource(R.string.me) }, style = SType.Hand.copy(color = S.c.ink))
                        Text(T.rank(c, home.rank), style = SType.Title)
                        val next = home.rank.next
                        if (next != null) {
                            Text(stringResource(R.string.next_rank, T.rank(c, next), T.runs(c, next.runs - s.runs)), style = SType.Muted)
                            Spacer(Modifier.height(6.dp))
                            val from = home.rank.runs
                            val p = ((s.runs - from).toFloat() / (next.runs - from)).coerceIn(0f, 1f)
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(S.RXs).background(S.c.paper2)) {
                                Box(Modifier.fillMaxWidth(p).height(6.dp).background(S.c.enamel))
                            }
                        } else Text(stringResource(R.string.top_rank), style = SType.Muted)
                    }
                }
                Spacer(Modifier.height(14.dp))
                FormLine(stringResource(R.string.s_runs), s.runs.toString())
                FormLine(stringResource(R.string.s_lines), s.lines.toString())
                FormLine(stringResource(R.string.s_carried), weight(s.grams.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), home.ru))
                FormLine(stringResource(R.string.s_clean), s.clean.toString())
                FormLine(stringResource(R.string.s_rush), s.rushOnTime.toString())
                FormLine(stringResource(R.string.s_budget), s.withinBudget.toString())
                if (s.saved > 0) FormLine(stringResource(R.string.s_saved), home.money(s.saved), valueColor = S.c.enamel)
                FormLine(stringResource(R.string.s_coins), s.coins.toString())
                FormLine(stringResource(R.string.s_prices), s.prices.toString())
            }
            SectionTitle(stringResource(R.string.badges_n, home.badges.size, Badge.entries.size))
            Badge.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { b ->
                        val got = b in home.badges
                        Row(Modifier.weight(1f).clip(S.RMd).background(S.c.paper).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            BadgeMark(b, got, Modifier.size(46.dp))
                            Spacer(Modifier.width(10.dp))
                            val (t, sub) = T.badge(c, b)
                            Column {
                                Text(t, style = SType.Strong.copy(color = if (got) S.c.ink else S.c.faint))
                                Text(sub, style = SType.Muted.copy(fontSize = 12.5.sp, color = if (got) S.c.muted else S.c.faint))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun icon(b: Badge): ImageVector = when (b) {
    Badge.FIRST_RUN -> Icons.AutoMirrored.Rounded.DirectionsWalk
    Badge.TEN_RUNS -> Icons.Rounded.Repeat
    Badge.FIFTY_RUNS -> Icons.Rounded.Route
    Badge.HUNDRED_RUNS -> Icons.Rounded.EmojiEvents
    Badge.CLEAN_FIVE -> Icons.Rounded.TaskAlt
    Badge.STORM -> Icons.Rounded.Umbrella
    Badge.RUSH -> Icons.Rounded.Bolt
    Badge.HEAVY -> Icons.Rounded.FitnessCenter
    Badge.THRIFTY -> Icons.Rounded.Savings
    Badge.BIG -> Icons.Rounded.ShoppingCart
    Badge.HOLIDAY -> Icons.Rounded.Celebration
    Badge.HOUSEHOLD -> Icons.Rounded.Groups
    Badge.MERCHANDISER -> Icons.AutoMirrored.Rounded.MenuBook
    Badge.FLEET -> Icons.Rounded.LocalShipping
}

/** A round violet stamp with the badge's sign; a dashed empty ring until it's earned. */
@Composable
fun BadgeMark(b: Badge, got: Boolean, modifier: Modifier = Modifier) {
    val ink = if (got) S.c.violet else S.c.line
    Box(modifier.rotate(if (got) (b.ordinal % 5 - 2) * 4f else 0f).then(if (got) Modifier.worn(b.ordinal * 13 + 1, 0.7f) else Modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2
            val dash = if (got) null else PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            drawCircle(ink, r - 2f, Offset(size.width / 2, size.height / 2), style = Stroke(3f, pathEffect = dash))
            if (got) drawCircle(ink, r * 0.8f, Offset(size.width / 2, size.height / 2), style = Stroke(1.2f))
        }
        Icon(icon(b), null, Modifier.fillMaxSize(0.46f), tint = ink)
    }
}
