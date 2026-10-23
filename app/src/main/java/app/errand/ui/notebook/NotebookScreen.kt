package app.errand.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.errand.R
import app.errand.domain.Notebook
import app.errand.domain.PriceRow
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.TopBar
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType

/** The merchandiser's notebook: what things cost, what went up, where it's cheaper. */
@Composable
fun NotebookScreen(home: Home, go: Go) {
    val rows = home.notebook
    val risers = Notebook.risers(rows).take(5)
    val cheaper = rows.filter { it.cheapest != null && it.saving > 0 }.sortedByDescending { it.saving }.take(5)
    var open by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(S.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.notebook), go::back)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            if (rows.isEmpty()) item { Text(stringResource(R.string.notebook_none), style = SType.Sub, modifier = Modifier.padding(16.dp)) }
            if (risers.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.went_up)) }
                items(risers, key = { "r" + it.key }) { r ->
                    Text(
                        stringResource(R.string.went_up_line, r.name, r.change ?: 0, home.money(r.history.first { it.first >= r.since }.second), home.money(r.last)),
                        style = SType.Body.copy(color = S.c.ink), modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    )
                }
            }
            if (cheaper.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.cheaper_at)) }
                items(cheaper, key = { "c" + it.key }) { r ->
                    Text(
                        stringResource(R.string.cheaper_line, r.name, home.shop(r.cheapest ?: 0)?.name.orEmpty(), home.money(r.saving)),
                        style = SType.Body, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    )
                }
            }
            if (rows.isNotEmpty()) item { SectionTitle(stringResource(R.string.all_prices, rows.size)) }
            items(rows, key = { it.key }) { r -> PriceCard(r, home, open == r.key) { open = if (open == r.key) "" else r.key } }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PriceCard(r: PriceRow, home: Home, open: Boolean, onClick: () -> Unit) {
    val c = ctx()
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(S.RMd).background(S.c.paper).border(1.dp, S.c.line, S.RMd).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = SType.Strong)
                Text((home.shop(r.lastShop)?.name?.let { "$it · " } ?: "") + T.ago(c, r.lastAt), style = SType.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(home.money(r.last), style = SType.Typed)
                r.change?.takeIf { it != 0 }?.let { ch ->
                    Text((if (ch > 0) "+" else "") + "$ch%", style = SType.TypedSmall.copy(color = if (ch > 0) S.c.stamp else S.c.enamel))
                }
            }
        }
        if (open && r.history.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Spark(r.history, S.c.ink, S.c.line, Modifier.fillMaxWidth().height(56.dp))
            Spacer(Modifier.height(4.dp))
            Row {
                Text(T.date(c, r.history.first().first), style = SType.Small, modifier = Modifier.weight(1f))
                Text(T.date(c, r.history.last().first), style = SType.Small)
            }
        }
        if (open) {
            // The latest price in every shop it was seen in.
            home.prices.filter { it.key == r.key }.groupBy { it.shopId }.mapValues { it.value.last() }.values.sortedBy { it.price }.forEach { p ->
                Row(Modifier.padding(top = 4.dp)) {
                    Text(home.shop(p.shopId)?.name ?: stringResource(R.string.other_shop), style = SType.Muted, modifier = Modifier.weight(1f))
                    Text(home.money(p.price), style = SType.TypedSmall.copy(color = S.c.ink))
                }
            }
        }
    }
}

/** A price over time: one line in ink on a faint baseline, the last price marked. */
@Composable
private fun Spark(points: List<Pair<Long, Long>>, ink: Color, base: Color, modifier: Modifier) {
    Canvas(modifier) {
        val x0 = points.first().first.toFloat()
        val x1 = points.last().first.toFloat().coerceAtLeast(x0 + 1)
        val lo = points.minOf { it.second }.toFloat()
        val hi = points.maxOf { it.second }.toFloat().coerceAtLeast(lo + 1)
        val pad = 6.dp.toPx()
        fun at(p: Pair<Long, Long>) = Offset(pad + (p.first - x0) / (x1 - x0) * (size.width - 2 * pad), size.height - pad - (p.second - lo) / (hi - lo) * (size.height - 2 * pad))
        drawLine(base, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
        val path = Path()
        points.forEachIndexed { i, p -> val o = at(p); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
        drawPath(path, ink, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(ink, 4.dp.toPx(), at(points.last()))
    }
}
