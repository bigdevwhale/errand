package app.errand.ui.waybill

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.errand.R
import app.errand.container
import app.errand.data.Item
import app.errand.domain.Goods
import app.errand.domain.Orders
import app.errand.domain.Qty
import app.errand.domain.Rules
import app.errand.domain.Shelf
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.components.Chip
import app.errand.ui.components.ClientMark
import app.errand.ui.components.Field
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Sheet
import app.errand.ui.components.SoftButton
import app.errand.ui.components.TextLink
import app.errand.ui.components.Toggle
import app.errand.ui.components.TopBar
import app.errand.ui.components.waybill
import app.errand.ui.components.weight
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import kotlinx.coroutines.launch

/** The waybill: what home has ordered, line by line, ready to be taken out on a run. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaybillScreen(home: Home, go: Go, startAccept: Boolean = false) {
    val c = ctx()
    val container = c.container
    var editing by rememberSaveable { mutableLongStateOf(0L) }
    var accept by rememberSaveable { mutableStateOf(startAccept) }
    var text by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableLongStateOf(0L) }
    val incoming by container.incoming.collectAsStateWithLifecycle()
    val who = home.clients.firstOrNull { it.id == client } ?: home.me

    fun add() {
        val t = text.trim()
        if (t.isEmpty()) return
        val (name, qty) = Qty.split(t, home.ru)
        text = ""
        container.write {
            val id = who?.id ?: office.me(c.getString(R.string.me))
            office.order(name, qty, id)
        }
    }

    // What was ordered before and matches what's being typed: one tap to order it again.
    val suggestions = remember(text, home.items) {
        val k = Goods.key(text)
        if (k.length < 2) emptyList()
        else home.items.asReversed().distinctBy { it.key }.filter { it.key.startsWith(k) && it.key != k && home.pending.none { p -> p.key == it.key } }.take(4)
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            TopBar(stringResource(R.string.waybill_n, home.number), go::back) {
                IconButton(onClick = {
                    val names = home.clients.filter { !it.me }.associate { it.id to it.name }
                    val body = Orders.text(home.number, home.pending, names, home.ru, c.getString(R.string.waybill_text_title))
                    c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body), null))
                }, enabled = home.pending.isNotEmpty()) { Icon(Icons.Rounded.Share, stringResource(R.string.share_waybill), tint = if (home.pending.isNotEmpty()) S.c.ink else S.c.faint) }
            }
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 12.dp).clip(S.RXs).waybill(S.c.paper, S.c.rule, S.c.stamp, 0.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(R.string.waybill_head), style = SType.Eyebrow, modifier = Modifier.weight(1f))
                        Text(T.typed(home.now), style = SType.TypedSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.waybill_sum, T.lines(c, home.pending.size), weight(home.grams, home.ru)),
                        style = SType.Typed.copy(color = if (home.heavy) S.c.stamp else S.c.muted),
                    )
                    if (home.heavy) Text(stringResource(R.string.too_heavy, home.settings.carryKg), style = SType.Muted.copy(color = S.c.stamp), modifier = Modifier.padding(top = 4.dp))
                    if (home.open != null) Text(stringResource(R.string.run_open_note), style = SType.Muted, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(10.dp))
                }
                if (home.pending.isEmpty()) item {
                    Text(stringResource(R.string.waybill_empty), style = SType.Sub, modifier = Modifier.padding(vertical = 24.dp))
                }
                itemsIndexed(home.pending, key = { _, it -> it.id }) { i, item ->
                    LineRow(i + 1, item, home, onClick = { editing = item.id })
                    Box(Modifier.fillMaxWidth().height(1.dp).background(S.c.rule))
                }
            }
            // Ordering: who it's for, then the line.
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (suggestions.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { s ->
                        Chip(s.name, false) {
                            text = ""
                            container.write { office.order(s.name, s.qty, who?.id ?: office.me(c.getString(R.string.me))) }
                        }
                    }
                }
                if (home.clients.size > 1) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.order_for), style = SType.Small)
                    home.clients.forEach { cl ->
                        Box(
                            Modifier.clip(CircleShape).background(if (cl.id == who?.id) S.c.paper else S.c.bg).clickable { client = cl.id }.padding(3.dp),
                        ) { ClientMark(cl.name, cl.mark, size = 30.dp) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field(
                        text, { text = it.take(80) }, stringResource(R.string.add_line), Modifier.weight(1f),
                        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        actions = KeyboardActions(onDone = { add() }),
                    )
                    IconButton(onClick = ::add, enabled = text.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.add), tint = if (text.isNotBlank()) S.c.enamel else S.c.faint) }
                }
                Spacer(Modifier.height(8.dp))
                if (home.open != null) PrimaryButton(stringResource(R.string.continue_run), go::run, Modifier.fillMaxWidth())
                else PrimaryButton(stringResource(R.string.accept_order), { accept = true }, Modifier.fillMaxWidth(), enabled = home.pending.isNotEmpty())
            }
        }

        val item = home.item(editing)
        Sheet(item != null, { editing = 0 }) { if (item != null) ItemSheet(item, home) { editing = 0 } }
        Sheet(accept, { accept = false }) { AcceptSheet(home, go) { accept = false } }
        Sheet(incoming != null, { container.incoming.value = null }) {
            incoming?.let { t -> IncomingSheet(t, home) { container.incoming.value = null } }
        }
    }
}

/** One line opened: its amount, whose it is, which shop, a rush, its weight and the cold box. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ItemSheet(item: Item, home: Home, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    var name by remember(item.id) { mutableStateOf(item.name) }
    var qty by remember(item.id) { mutableStateOf(item.qty) }
    var note by remember(item.id) { mutableStateOf(item.note) }
    var client by remember(item.id) { mutableLongStateOf(item.clientId) }
    var shop by remember(item.id) { mutableLongStateOf(item.shopId) }
    var rush by remember(item.id) { mutableStateOf(if (item.rush) -1 else 0) }
    var grams by remember(item.id) { mutableStateOf(item.grams.toString()) }
    var cold by remember(item.id) { mutableStateOf(item.cold) }

    Text(stringResource(R.string.line_title), style = SType.Title)
    Spacer(Modifier.height(14.dp))
    Field(name, { name = it.take(60) }, stringResource(R.string.line_name))
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(qty, { qty = it.take(20) }, stringResource(R.string.line_qty), Modifier.weight(1f))
        Field(
            grams, { grams = it.filter(Char::isDigit).take(5) }, stringResource(R.string.line_weight), Modifier.weight(1f),
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = stringResource(R.string.g),
        )
    }
    Text(T.shelf(c, Shelf.of(item.shelf)), style = SType.Small, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    if (home.clients.size > 1) {
        SectionTitle(stringResource(R.string.line_client))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            home.clients.forEach { cl -> Chip(cl.name, client == cl.id) { client = cl.id } }
        }
    }
    if (home.shops.isNotEmpty()) {
        SectionTitle(stringResource(R.string.line_shop))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.any_shop), shop == 0L) { shop = 0 }
            home.shops.forEach { s -> Chip(s.name, shop == s.id) { shop = s.id } }
        }
    }
    SectionTitle(stringResource(R.string.line_rush))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.no_rush), rush == 0) { rush = 0 }
        if (item.rush) Chip(stringResource(R.string.rush_by, T.time(item.rushBy)), rush == -1) { rush = -1 }
        Rules.RUSH_MINUTES.forEach { m -> Chip(T.duration(c, m * Rules.MINUTE), rush == m) { rush = m } }
    }
    Spacer(Modifier.height(10.dp))
    Toggle(stringResource(R.string.cold_box), stringResource(R.string.cold_box_sub), cold) { cold = it }
    Spacer(Modifier.height(8.dp))
    Field(note, { note = it.take(60) }, stringResource(R.string.line_note))
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.save), {
        val rushBy = when (rush) {
            -1 -> item.rushBy
            0 -> 0L
            else -> System.currentTimeMillis() + rush * Rules.MINUTE
        }
        val changedName = name.trim().ifEmpty { item.name }
        container.write {
            office.save(
                item.copy(
                    name = changedName, qty = qty.trim(), note = note.trim(), clientId = client, shopId = shop, rushBy = rushBy,
                    grams = grams.toIntOrNull()?.coerceIn(1, 50_000) ?: item.grams, cold = cold,
                    shelf = if (changedName != item.name) Goods.guess(changedName).shelf.name else item.shelf,
                ),
            )
        }
        onClose()
    }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    TextLink(stringResource(R.string.remove_line), {
        container.write { office.delete(item.id) }
        onClose()
    }, Modifier.align(Alignment.CenterHorizontally), color = S.c.stamp)
}

/** Out of the door: the money handed out and the shops on the way. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcceptSheet(home: Home, go: Go, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    var budget by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var newShop by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Text(stringResource(R.string.accept_title, home.number), style = SType.Title)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.waybill_sum, T.lines(c, home.pending.size), weight(home.grams, home.ru)), style = SType.Muted)
    if (home.heavy) Text(stringResource(R.string.too_heavy, home.settings.carryKg), style = SType.Muted.copy(color = S.c.stamp), modifier = Modifier.padding(top = 4.dp))
    SectionTitle(stringResource(R.string.budget))
    Field(
        budget, { budget = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(10) }, stringResource(R.string.budget_hint),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), suffix = home.currency.symbol,
    )
    Text(stringResource(R.string.budget_sub), style = SType.Small, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    SectionTitle(stringResource(R.string.shops_today))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        home.shops.forEach { s -> Chip(s.name, s.id in picked) { picked = if (s.id in picked) picked - s.id else picked + s.id } }
        Chip("+ " + stringResource(R.string.new_shop), adding) { adding = !adding }
    }
    if (adding) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Field(newShop, { newShop = it.take(30) }, stringResource(R.string.shop_name), Modifier.weight(1f))
        IconButton(onClick = {
            val n = newShop.trim()
            if (n.isNotEmpty()) scope.launch {
                val id = container.office.addShop(n)
                picked = picked + id
                newShop = ""
                adding = false
            }
        }) { Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = S.c.enamel) }
    }
    Text(stringResource(if (home.shops.isEmpty()) R.string.shops_none else R.string.shops_sub), style = SType.Small, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    Spacer(Modifier.height(20.dp))
    PrimaryButton(stringResource(R.string.set_off), {
        val sum = app.errand.data.Currency.parse(budget) ?: 0
        scope.launch {
            container.office.start(sum, picked)
            onClose()
            go.run()
        }
    }, Modifier.fillMaxWidth())
}

/** A list shared in from a messenger: whose it is, then onto the waybill. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.IncomingSheet(text: String, home: Home, onClose: () -> Unit) {
    val c = ctx()
    val container = c.container
    val orders = remember(text) { Orders.parse(text, home.ru) }
    var client by rememberSaveable { mutableLongStateOf(home.clients.firstOrNull { !it.me }?.id ?: home.me?.id ?: 0) }
    var newName by rememberSaveable { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Text(stringResource(R.string.incoming_title), style = SType.Title)
    Spacer(Modifier.height(6.dp))
    if (orders.isEmpty()) {
        Text(stringResource(R.string.incoming_empty), style = SType.Sub)
        Spacer(Modifier.height(16.dp))
        SoftButton(stringResource(R.string.close), onClose, Modifier.fillMaxWidth())
        return
    }
    Text(stringResource(R.string.incoming_sub, T.lines(c, orders.size)), style = SType.Muted)
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth().clip(S.RXs).background(S.c.paper2).padding(12.dp)) {
        orders.take(12).forEachIndexed { i, o ->
            Text(
                "${i + 1}. ${o.name}" + (if (o.qty.isNotEmpty()) " — ${o.qty}" else "") + (if (o.rush) "  ⚡" else ""),
                style = SType.Typed,
            )
        }
        if (orders.size > 12) Text("…", style = SType.Typed)
    }
    SectionTitle(stringResource(R.string.incoming_whose))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        home.clients.forEach { cl -> Chip(cl.name, client == cl.id) { client = cl.id } }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Field(newName, { newName = it.take(20) }, stringResource(R.string.new_client), Modifier.weight(1f))
        IconButton(onClick = {
            val n = newName.trim()
            if (n.isNotEmpty()) scope.launch {
                client = container.office.addClient(n)
                newName = ""
            }
        }) { Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = S.c.enamel) }
    }
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.incoming_enter), {
        scope.launch {
            val who = if (client != 0L) client else container.office.me(c.getString(R.string.me))
            container.office.orders(orders, who)
            onClose()
        }
    }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    TextLink(stringResource(R.string.cancel), onClose, Modifier.align(Alignment.CenterHorizontally))
    LaunchedEffect(Unit) { if (home.me == null) container.office.me(c.getString(R.string.me)) }
}
