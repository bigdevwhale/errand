package app.errand.ui.run

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RemoveShoppingCart
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.container
import app.errand.data.Currency
import app.errand.data.Item
import app.errand.data.Run
import app.errand.data.Weather
import app.errand.domain.Qty
import app.errand.domain.Rules
import app.errand.domain.Shelf
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.art.Art
import app.errand.ui.art.StringBag
import app.errand.ui.components.Chip
import app.errand.ui.components.Field
import app.errand.ui.components.FormLine
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Sheet
import app.errand.ui.components.SoftButton
import app.errand.ui.components.TextLink
import app.errand.ui.components.TopBar
import app.errand.ui.components.waybill
import app.errand.ui.components.weight
import app.errand.ui.components.worn
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import app.errand.ui.theme.Sign
import app.errand.ui.waybill.LineRow
import app.errand.ui.waybill.Tag
import kotlinx.coroutines.launch

/**
 * The run: a shop at a time, the waybill in the order things lie on its shelves, ticked into the bag.
 * Then home to hand it in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunScreen(home: Home, go: Go, startShop: Long? = null) {
    val run = home.open
    if (run == null) {
        // Delivered or called off elsewhere.
        LaunchedEffect(Unit) { go.office() }
        return
    }
    val c = ctx()
    val container = c.container
    val shops = remember(run, home.shops) { if (run.shopIds.isNotEmpty()) run.shopIds.mapNotNull(home::shop) else home.shops }
    var shop by rememberSaveable { mutableLongStateOf(startShop ?: shops.firstOrNull()?.id ?: 0L) }
    var pricing by rememberSaveable { mutableLongStateOf(0L) }
    var unordered by rememberSaveable { mutableStateOf(false) }
    var deliver by rememberSaveable { mutableStateOf(false) }
    var cancel by rememberSaveable { mutableStateOf(false) }
    var lastTaken by rememberSaveable { mutableLongStateOf(0L) }
    val walk = home.walk(shop)
    val bag = home.bag
    val load = Rules.grams(bag) / (home.settings.carryKg * 1000f)
    val noted = bag.sumOf { it.price }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(stringResource(R.string.run_n, run.number), go::office) {
                Text(T.duration(c, home.now - run.startedAt), style = SType.Typed.copy(color = S.c.muted), modifier = Modifier.padding(end = 4.dp))
                IconButton(onClick = { cancel = true }) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel_run), tint = S.c.muted) }
            }
            if (shops.isNotEmpty()) FlowRow(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shops.forEach { s -> Chip(s.name + countIn(home, s.id), shop == s.id) { shop = s.id } }
                if (run.shopIds.isEmpty()) Chip(stringResource(R.string.other_shop), shop == 0L) { shop = 0 }
            }

            // The bag and the money.
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                StringBag(bag.map { Shelf.of(it.shelf) }, load, Modifier.size(78.dp, 96.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bag_weight, weight(Rules.grams(bag), home.ru), home.settings.carryKg), style = SType.Strong.copy(color = if (load > 1f) S.c.stamp else S.c.ink))
                    Text(stringResource(R.string.bag_count, bag.count { !it.offList }, bag.count { !it.offList } + home.due.size), style = SType.Muted)
                    if (run.budget > 0) Text(
                        stringResource(R.string.budget_noted, home.money(run.budget), home.money(noted)),
                        style = SType.Muted.copy(color = if (noted > run.budget) S.c.stamp else S.c.muted),
                    ) else if (noted > 0) Text(stringResource(R.string.noted, home.money(noted)), style = SType.Muted)
                    if (load > 1f) Text(stringResource(R.string.bag_heavy), style = SType.Small.copy(color = S.c.stamp))
                }
            }

            val list = rememberLazyListState()
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 12.dp).clip(S.RXs).waybill(S.c.paper, S.c.rule, S.c.stamp, 0.dp),
                state = list,
                contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            ) {
                item {
                    Text(stringResource(if (home.aisles.any { it.shopId == shop && shop != 0L }) R.string.walk_learned else R.string.walk_plain), style = SType.Small, modifier = Modifier.padding(bottom = 6.dp, end = 8.dp))
                }
                if (walk.isEmpty()) item {
                    Text(stringResource(if (home.due.isEmpty()) R.string.walk_done else R.string.walk_done_here), style = SType.Sub, modifier = Modifier.padding(vertical = 18.dp))
                }
                itemsIndexed(walk, key = { _, it -> "d" + it.id }) { i, item ->
                    val out = item.outOn == run.id
                    LineRow(
                        i + 1, item, home, struck = out, showShop = false,
                        onClick = {
                            if (!out) {
                                lastTaken = item.id
                                container.write { office.take(item, run, shop) }
                                if (home.settings.notePrices) pricing = item.id
                            }
                        },
                    ) {
                        if (out) Tag(stringResource(R.string.shelf_empty), S.c.faint)
                        IconButton(onClick = { container.write { office.out(item, run, !out) } }) {
                            Icon(if (out) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.RemoveShoppingCart, stringResource(if (out) R.string.shelf_back else R.string.shelf_empty), tint = S.c.faint, modifier = Modifier.size(20.dp))
                        }
                        Box(Modifier.size(26.dp).border(1.5.dp, S.c.muted, S.RXs))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(S.c.rule))
                }
                if (bag.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.in_the_bag)) }
                    itemsIndexed(bag, key = { _, it -> "b" + it.id }) { i, item ->
                        LineRow(i + 1, item, home, struck = true, showShop = false, onClick = { pricing = item.id }) {
                            if (item.price > 0) Text(home.money(item.price), style = SType.TypedSmall.copy(color = S.c.ink))
                            if (item.offList) Tag(stringResource(if (bag.firstOrNull { it.offList }?.id == item.id) R.string.treat else R.string.unordered_short), S.c.violet, Modifier.padding(start = 6.dp))
                            Spacer(Modifier.width(6.dp))
                            Tick(item.id == lastTaken)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.unordered), { unordered = true }, Modifier.weight(0.9f), icon = Icons.Rounded.Add)
                PrimaryButton(stringResource(R.string.home_deliver), { deliver = true }, Modifier.weight(1.1f), enabled = bag.isNotEmpty())
            }
        }

        val priced = home.item(pricing)
        Sheet(priced != null, { pricing = 0 }) { if (priced != null) PriceSheet(priced, home) { pricing = 0 } }
        Sheet(unordered, { unordered = false }) { UnorderedSheet(home, run, shop) { unordered = false } }
        Sheet(deliver, { deliver = false }) { DeliverSheet(home, run, go) { deliver = false } }
        Sheet(cancel, { cancel = false }) {
            Text(stringResource(R.string.cancel_run_title), style = SType.Title)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cancel_run_text), style = SType.Sub)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.keep_going), { cancel = false }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            TextLink(stringResource(R.string.cancel_run), {
                cancel = false
                container.write { office.cancel(run.id) }
                go.office()
            }, Modifier.align(Alignment.CenterHorizontally), color = S.c.stamp)
        }
    }
}

private fun countIn(home: Home, shopId: Long): String {
    val n = home.walk(shopId).count { it.outOn != home.open?.id }
    return if (n > 0) " · $n" else ""
}

/** The tick in the box: pressed like a stamp, with a little bounce on the one just taken. */
@Composable
private fun Tick(fresh: Boolean) {
    val k = remember { Animatable(if (fresh && !Art.still) 1.8f else 1f) }
    LaunchedEffect(fresh) { if (fresh && !Art.still) k.animateTo(1f, tween(220)) }
    Box(Modifier.size(26.dp).border(1.5.dp, S.c.muted, S.RXs), contentAlignment = Alignment.Center) {
        Text("✓", fontFamily = Sign, fontSize = 22.sp, color = S.c.stamp, modifier = Modifier.scale(k.value).rotate(-8f).worn(fresh.hashCode() + 3, 0.5f))
    }
}

/** What it cost, for the merchandiser's notebook; or put it back on the shelf. */
@Composable
private fun ColumnScope.PriceSheet(item: Item, home: Home, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    var price by remember(item.id) { mutableStateOf(Currency.field(item.price, home.ru)) }
    val focus = remember { FocusRequester() }
    val last = home.notebook.firstOrNull { it.key == item.key }

    Text(item.name + if (item.qty.isNotEmpty()) " — ${item.qty}" else "", style = SType.Title)
    Spacer(Modifier.height(4.dp))
    if (last != null) Text(stringResource(R.string.price_last, home.money(last.last), T.ago(c, last.lastAt)), style = SType.Muted)
    Spacer(Modifier.height(12.dp))
    Field(
        price, { price = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(10) }, stringResource(R.string.price), Modifier.focusRequester(focus),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), suffix = home.currency.symbol,
    )
    LaunchedEffect(item.id) { if (!Art.still) runCatching { focus.requestFocus() } }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SoftButton(stringResource(R.string.skip), onClose, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.save), {
            val p = Currency.parse(price) ?: 0
            if (p != item.price) container.write { office.notePrice(item, p) }
            onClose()
        }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    TextLink(stringResource(if (item.offList) R.string.remove_line else R.string.put_back), {
        container.write { office.putBack(item) }
        onClose()
    }, Modifier.align(Alignment.CenterHorizontally), color = S.c.stamp)
}

/** Picked up without an order: the first of a run is the courier's treat; more come off the pay. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnorderedSheet(home: Home, run: Run, shop: Long, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    var text by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableLongStateOf(home.me?.id ?: 0) }
    val first = home.bag.none { it.offList }

    Text(stringResource(R.string.unordered_title), style = SType.Title)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(if (first) R.string.unordered_first else R.string.unordered_more, Rules.UNORDERED), style = SType.Muted)
    Spacer(Modifier.height(12.dp))
    Field(text, { text = it.take(60) }, stringResource(R.string.line_name))
    Spacer(Modifier.height(10.dp))
    Field(
        price, { price = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(10) }, stringResource(R.string.price),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), suffix = home.currency.symbol,
    )
    if (home.clients.size > 1) {
        SectionTitle(stringResource(R.string.line_client))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { home.clients.forEach { cl -> Chip(cl.name, client == cl.id) { client = cl.id } } }
    }
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.into_bag), {
        val (name, qty) = Qty.split(text, home.ru)
        if (name.isNotBlank()) container.write { office.unordered(name, qty, run, shop, client, Currency.parse(price) ?: 0) }
        onClose()
    }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
}

/** At the door: the receipt, the weather out there, what stayed behind. Then the pay. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliverSheet(home: Home, run: Run, go: Go, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    val noted = home.bag.sumOf { it.price }
    var spent by rememberSaveable { mutableStateOf(Currency.field(noted, home.ru)) }
    var weather by rememberSaveable { mutableStateOf(Weather.FAIR) }
    val scope = rememberCoroutineScope()
    val forgotten = home.due.count { it.outOn != run.id }
    val out = home.due.size - forgotten

    Text(stringResource(R.string.deliver_title, run.number), style = SType.Title)
    Spacer(Modifier.height(10.dp))
    FormLine(stringResource(R.string.f_lines), home.bag.count { !it.offList }.toString())
    FormLine(stringResource(R.string.f_weight), weight(Rules.grams(home.bag), home.ru))
    if (out > 0) FormLine(stringResource(R.string.f_out), out.toString())
    if (forgotten > 0) FormLine(stringResource(R.string.f_forgotten), forgotten.toString(), valueColor = S.c.stamp)
    if (forgotten > 0) Text(stringResource(R.string.forgotten_note), style = SType.Small, modifier = Modifier.padding(top = 4.dp))
    SectionTitle(stringResource(R.string.receipt))
    Field(
        spent, { spent = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(10) }, stringResource(R.string.receipt_hint),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), suffix = home.currency.symbol,
    )
    if (run.budget > 0) Text(stringResource(R.string.budget_was, home.money(run.budget)), style = SType.Small, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    SectionTitle(stringResource(R.string.weather))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Weather.entries.forEach { w -> Chip(T.weather(c, w) + if (w.bonus > 0) " +${w.bonus}%" else "", weather == w) { weather = w } }
    }
    Spacer(Modifier.height(20.dp))
    PrimaryButton(stringResource(R.string.hand_in), {
        val sum = Currency.parse(spent) ?: 0
        scope.launch {
            container.office.deliver(run.id, weather, sum)
            onClose()
            go.slip(run.id, fresh = true)
        }
    }, Modifier.fillMaxWidth())
}
