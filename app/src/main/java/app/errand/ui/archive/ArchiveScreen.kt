package app.errand.ui.archive

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.domain.Month
import app.errand.domain.Rules
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.components.FormLine
import app.errand.ui.components.PaperCard
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Stamp
import app.errand.ui.components.TopBar
import app.errand.ui.components.weight
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType

/** The archive: the waybills, a month at a time, each month with its report on top. */
@Composable
fun ArchiveScreen(home: Home, go: Go) {
    val c = ctx()
    val byMonth = remember(home.done) { home.done.groupBy { Rules.date(it.endedAt).let { d -> d.year * 100 + d.monthValue } } }
    Column(Modifier.fillMaxSize().background(S.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.archive), go::back)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            if (home.done.isEmpty()) item { Text(stringResource(R.string.archive_none), style = SType.Sub, modifier = Modifier.padding(16.dp)) }
            home.months.forEach { m ->
                item(key = "m${m.year}${m.month}") { MonthCard(m, home) }
                items(byMonth[m.year * 100 + m.month].orEmpty(), key = { it.id }) { r ->
                    val taken = home.taken(r)
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(S.RMd).background(S.c.paper).border(1.dp, S.c.line, S.RMd)
                            .clickable { go.slip(r.id) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(64.dp)) {
                            Text("№${r.number}", style = SType.Typed)
                            Text(T.typed(r.endedAt), style = SType.TypedSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(T.lines(c, taken.count { !it.offList }) + " · " + weight(Rules.grams(taken), home.ru), style = SType.Body.copy(fontSize = 15.sp))
                            val notes = buildList {
                                if (r.spent > 0) add(home.money(r.spent))
                                if (r.weatherOf.bonus > 0) add(T.weather(c, r.weatherOf))
                            }
                            if (notes.isNotEmpty()) Text(notes.joinToString(" · "), style = SType.Muted)
                        }
                        when {
                            r.missed > 0 -> Stamp(stringResource(R.string.not_delivered_short, r.missed), S.c.stamp, angle = -4f, size = 10.sp, modifier = Modifier.padding(end = 8.dp))
                            Rules.overspent(r) -> Stamp(stringResource(R.string.overspent), S.c.violet, angle = 4f, size = 10.sp, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("+${r.pay}", style = SType.Strong.copy(color = S.c.enamel))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MonthCard(m: Month, home: Home) {
    val c = ctx()
    SectionTitle(T.month(c, m.year, m.month))
    PaperCard(Modifier.padding(bottom = 8.dp)) {
        FormLine(stringResource(R.string.s_runs), m.runs.toString())
        FormLine(stringResource(R.string.s_lines), m.lines.toString())
        FormLine(stringResource(R.string.s_carried), weight(m.grams.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), home.ru))
        if (m.spent > 0) FormLine(stringResource(R.string.f_spent), home.money(m.spent))
        if (m.saved > 0) FormLine(stringResource(R.string.s_saved), home.money(m.saved), valueColor = S.c.enamel)
        if (m.missed > 0) FormLine(stringResource(R.string.f_forgotten), m.missed.toString(), valueColor = S.c.stamp)
        FormLine(stringResource(R.string.s_coins), m.coins.toString())
    }
}
