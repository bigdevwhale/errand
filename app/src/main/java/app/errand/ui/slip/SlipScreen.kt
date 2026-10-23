package app.errand.ui.slip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.data.Leg
import app.errand.domain.Badges
import app.errand.domain.Rank
import app.errand.domain.Rules
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.art.Art
import app.errand.ui.art.RankBadge
import app.errand.ui.components.FormLine
import app.errand.ui.components.PaperCard
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Stamp
import app.errand.ui.components.TopBar
import app.errand.ui.components.weight
import app.errand.ui.ctx
import app.errand.ui.file.BadgeMark
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import app.errand.ui.waybill.LineRow
import app.errand.ui.waybill.Tag

/**
 * A run handed in: the waybill with what was carried, stamped, and the payslip under it. Just
 * delivered ([fresh]), the stamp comes down and any new rank or badge is announced.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlipScreen(id: Long, fresh: Boolean, home: Home, go: Go) {
    val c = ctx()
    val run = home.run(id)
    if (run == null || run.legOf != Leg.DONE) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", go::back) }
        return
    }
    val taken = home.taken(run)
    val slip = remember(run, taken) { Rules.pay(run, taken, run.missed, run.endedAt, run.weatherOf, run.spent) }
    val earlier = remember(run, home.runs) { home.runs.filter { it.id != run.id && (it.legOf != Leg.DONE || it.endedAt < run.endedAt) } }
    val newBadges = remember(run, home.runs, home.items) {
        if (!fresh) emptySet() else home.badges - Badges.earned(earlier, home.items, home.prices.size, home.owned)
    }
    val rankBefore = Rank.of(Rules.done(earlier).size)
    val rankAfter = Rank.of(Rules.done(earlier).size + 1)
    val promoted = fresh && rankAfter != rankBefore

    val drop = remember { Animatable(if (fresh && !Art.still) 0f else 1f) }
    LaunchedEffect(fresh) { if (fresh && !Art.still) drop.animateTo(1f, tween(420)) }

    Column(Modifier.fillMaxSize().background(S.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.waybill_n, run.number), if (fresh) go::office else go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Box {
                PaperCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(R.string.waybill_head), style = SType.Eyebrow, modifier = Modifier.weight(1f))
                        Text(T.typed(run.endedAt), style = SType.TypedSmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.slip_times, T.time(run.startedAt), T.time(run.endedAt), T.duration(c, run.endedAt - run.startedAt)),
                        style = SType.TypedSmall,
                    )
                    val shops = taken.map { it.takenShop }.distinct().mapNotNull(home::shop).map { it.name }
                    if (shops.isNotEmpty()) Text(shops.joinToString(" · "), style = SType.TypedSmall)
                    Spacer(Modifier.height(8.dp))
                    taken.forEachIndexed { i, item ->
                        LineRow(i + 1, item, home, showShop = false) {
                            if (item.price > 0) Text(home.money(item.price), style = SType.TypedSmall.copy(color = S.c.ink))
                            if (item.offList) Tag(stringResource(if (taken.firstOrNull { it.offList }?.id == item.id) R.string.treat else R.string.unordered_short), S.c.violet, Modifier.padding(start = 6.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FormLine(stringResource(R.string.f_weight), weight(Rules.grams(taken), home.ru))
                    if (run.out > 0) FormLine(stringResource(R.string.f_out), run.out.toString())
                    if (run.missed > 0) FormLine(stringResource(R.string.f_forgotten), run.missed.toString(), valueColor = S.c.stamp)
                    if (run.budget > 0) FormLine(stringResource(R.string.f_budget), home.money(run.budget))
                    if (run.spent > 0) FormLine(stringResource(R.string.f_spent), home.money(run.spent), valueColor = if (Rules.overspent(run)) S.c.stamp else S.c.ink)
                    FormLine(stringResource(R.string.weather), T.weather(c, run.weatherOf))
                    Spacer(Modifier.height(70.dp))
                }
                // The stamps along the bottom of the sheet.
                Row(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (Rules.overspent(run)) Stamp(stringResource(R.string.overspent), S.c.violet, angle = 5f, size = 12.sp, modifier = Modifier.padding(end = 10.dp))
                    else if (Rules.big(run, taken)) Stamp(stringResource(R.string.big_run), S.c.violet, angle = 4f, size = 12.sp, modifier = Modifier.padding(end = 10.dp))
                    Stamp(
                        stringResource(R.string.delivered), S.c.stamp, angle = -9f, size = 22.sp, sub = T.typed(run.endedAt), seed = run.id.toInt(),
                        modifier = Modifier.scale(1f + (1f - drop.value) * 1.2f).alpha(drop.value),
                    )
                }
            }

            SectionTitle(stringResource(R.string.payslip))
            PaperCard(ruled = 0.dp) {
                slip.lines.forEach { l -> FormLine(T.pay(c, l), (if (l.coins > 0) "+" else "") + l.coins, valueColor = if (l.coins < 0) S.c.stamp else S.c.ink) }
                if (slip.lines.isEmpty()) Text(stringResource(R.string.payslip_empty), style = SType.Muted)
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(S.c.ink))
                FormLine(stringResource(R.string.pay_total), T.coins(c, run.pay), valueColor = S.c.enamel)
            }

            if (promoted) {
                SectionTitle(stringResource(R.string.promoted))
                PaperCard(ruled = 0.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(rankAfter.ordinal, Modifier.size(52.dp, 62.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(T.rank(c, rankAfter), style = SType.Title)
                            Text(stringResource(R.string.promoted_sub), style = SType.Muted)
                        }
                    }
                }
            }
            if (newBadges.isNotEmpty()) {
                SectionTitle(stringResource(R.string.new_badges))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    newBadges.forEach { b ->
                        Column(Modifier.width(150.dp)) {
                            BadgeMark(b, true, Modifier.size(64.dp))
                            Spacer(Modifier.height(6.dp))
                            val (t, s) = T.badge(c, b)
                            Text(t, style = SType.Strong)
                            Text(s, style = SType.Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (fresh) PrimaryButton(stringResource(R.string.to_office), go::office, Modifier.fillMaxWidth().padding(12.dp))
    }
}
