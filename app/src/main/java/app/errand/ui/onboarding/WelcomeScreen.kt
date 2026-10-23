package app.errand.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.container
import app.errand.data.Currency
import app.errand.domain.Shelf
import app.errand.domain.Upgrade
import app.errand.ru
import app.errand.ui.Language
import app.errand.ui.art.DepotLook
import app.errand.ui.art.DepotScene
import app.errand.ui.art.RankBadge
import app.errand.ui.art.StringBag
import app.errand.ui.components.Chip
import app.errand.ui.components.ClientMark
import app.errand.ui.components.Field
import app.errand.ui.components.PaperCard
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.Stamp
import app.errand.ui.components.TextLink
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import kotlinx.coroutines.launch

private const val GUIDE = 5

/** First run: the office, the five pages of how it works, then the courier's name and the currency. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val c = ctx()
    val container = c.container
    val pager = rememberPagerState(0) { GUIDE + 2 }
    val scope = rememberCoroutineScope()
    val ru = c.ru || Language.current() == Language.RU
    var currency by rememberSaveable { mutableStateOf(if (ru) Currency.RUB.name else Currency.USD.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var carry by rememberSaveable { mutableIntStateOf(7) }
    val meDefault = stringResource(R.string.me)

    fun finish() {
        container.write {
            office.me(name.trim().ifEmpty { meDefault })
            settings.update { it.copy(onboarded = true, currency = Currency.of(currency), carryKg = carry) }
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Hero()
                in 1..GUIDE -> GuidePage(page - 1)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(stringResource(R.string.setup_title), style = SType.Display)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.setup_name), style = SType.Sub)
                    Spacer(Modifier.height(12.dp))
                    Field(name, { name = it.take(24) }, stringResource(R.string.your_name))
                    Spacer(Modifier.height(22.dp))
                    Text(stringResource(R.string.setup_currency), style = SType.Sub)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Currency.entries.forEach { cur -> Chip("${cur.symbol} ${cur.name}", currency == cur.name) { currency = cur.name } }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(stringResource(R.string.setup_carry), style = SType.Sub)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 7, 10, 15, 25).forEach { kg -> Chip(stringResource(R.string.kg_n, kg), carry == kg) { carry = kg } }
                    }
                    Text(stringResource(R.string.setup_later), style = SType.Small, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                }
            }
        }
        Dots(pager)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage in 1..GUIDE) TextLink(stringResource(R.string.skip), { scope.launch { pager.animateScrollToPage(GUIDE + 1) } })
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == GUIDE + 1
            PrimaryButton(
                stringResource(if (last) R.string.open_office else if (pager.currentPage == 0) R.string.how_it_works else R.string.next),
                { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(if (last || pager.currentPage == 0) 260.dp else 160.dp),
            )
        }
    }
}

/** The guide on its own, from the "?" in the office. */
@Composable
fun GuideScreen(startPage: Int = 0, onDone: () -> Unit) {
    val pager = rememberPagerState(startPage) { GUIDE }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { GuidePage(it) }
        Dots(pager)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextLink(stringResource(R.string.close), onDone)
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == GUIDE - 1
            PrimaryButton(
                stringResource(if (last) R.string.got_it else R.string.next),
                { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(160.dp),
            )
        }
    }
}

@Composable
private fun Hero() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DepotScene(
            DepotLook(setOf(Upgrade.SIGN, Upgrade.FLOWERS, Upgrade.LAMP, Upgrade.CLOCK, Upgrade.BICYCLE, Upgrade.FLAG), 10, 10, stringResource(R.string.sign)),
            Modifier.fillMaxWidth().aspectRatio(360f / 220f),
        )
        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            Text(stringResource(R.string.app_name), style = SType.Hero)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.tagline), style = SType.Title.copy(color = S.c.stamp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.welcome_text), style = SType.Sub)
        }
    }
}

/** One page of how it works, with its picture. */
@Composable
fun GuidePage(i: Int) {
    val titles = listOf(R.string.g1_title, R.string.g2_title, R.string.g3_title, R.string.g4_title, R.string.g5_title)
    val texts = listOf(R.string.g1_text, R.string.g2_text, R.string.g3_text, R.string.g4_text, R.string.g5_text)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(260.dp).clip(S.RLg).background(S.c.paper2), contentAlignment = Alignment.Center) {
            when (i) {
                0 -> PaperCard(Modifier.padding(18.dp), ruled = 26.dp) {
                    Text(stringResource(R.string.waybill_n, 1), style = SType.Title)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        Triple(R.string.sample_1, R.string.sample_client_1, 1),
                        Triple(R.string.sample_2, R.string.me, 0),
                        Triple(R.string.sample_3, R.string.sample_client_2, 2),
                        Triple(R.string.sample_4, R.string.sample_client_1, 1),
                    ).forEachIndexed { n, (line, who, mark) ->
                        Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${n + 1}.", style = SType.Typed.copy(color = S.c.faint), modifier = Modifier.width(26.dp))
                            Text(stringResource(line), style = SType.Typed, modifier = Modifier.weight(1f))
                            ClientMark(stringResource(who), mark, size = 22.dp)
                        }
                    }
                }
                1 -> StringBag(
                    listOf(Shelf.PRODUCE, Shelf.PRODUCE, Shelf.BREAD, Shelf.GROCERY, Shelf.DRINKS, Shelf.SWEETS, Shelf.DAIRY, Shelf.DAIRY, Shelf.MEAT),
                    0.7f, Modifier.size(170.dp, 220.dp),
                )
                2 -> Box(contentAlignment = Alignment.Center) {
                    PaperCard(Modifier.width(240.dp).padding(8.dp), ruled = 24.dp) {
                        repeat(4) { Spacer(Modifier.height(24.dp)) }
                    }
                    Stamp(stringResource(R.string.delivered), S.c.stamp, angle = -9f, size = 26.sp, sub = "+ " + app.errand.ui.T.coins(ctx(), 84))
                }
                3 -> DepotScene(
                    DepotLook(setOf(Upgrade.FLOWERS, Upgrade.SIGN, Upgrade.MAILBOX), 18, 40, stringResource(R.string.sign), preview = Upgrade.TRICYCLE),
                    Modifier.fillMaxWidth().aspectRatio(360f / 220f),
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(0, 1, 2, 4).forEach { RankBadge(it, Modifier.size(58.dp + (it * 4).dp, 70.dp + (it * 5).dp)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(titles[i]), style = SType.Display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(texts[i]), style = SType.Sub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Dots(pager: PagerState) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        repeat(pager.pageCount) { i ->
            Box(Modifier.padding(horizontal = 4.dp).size(if (i == pager.currentPage) 9.dp else 7.dp).clip(CircleShape).background(if (i == pager.currentPage) S.c.stamp else S.c.line))
        }
    }
}
