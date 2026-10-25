package app.errand.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.errand.BuildConfig
import app.errand.R
import app.errand.container
import app.errand.data.Client
import app.errand.data.Currency
import app.errand.data.Shop
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.Language
import app.errand.ui.components.Chip
import app.errand.ui.components.ClientMark
import app.errand.ui.components.CommunityLinks
import app.errand.ui.components.Field
import app.errand.ui.components.PaperCard
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Segmented
import app.errand.ui.components.Sheet
import app.errand.ui.components.TextLink
import app.errand.ui.components.Toggle
import app.errand.ui.components.TopBar
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType

private sealed interface Editing {
    data class OfClient(val c: Client?) : Editing
    data class OfShop(val s: Shop?) : Editing
}

/** The household, the shops, how much the courier carries, the currency, the language. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val s = home.settings
    var editing by remember { mutableStateOf<Editing?>(null) }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(stringResource(R.string.settings), go::back)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SectionTitle(stringResource(R.string.household))
                Text(stringResource(R.string.household_sub), style = SType.Muted)
                Spacer(Modifier.height(8.dp))
                home.clients.forEach { cl ->
                    Row(Modifier.fillMaxWidth().clickable { editing = Editing.OfClient(cl) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ClientMark(cl.name, cl.mark, size = 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(cl.name, style = SType.Strong, modifier = Modifier.weight(1f))
                        if (cl.me) Text(stringResource(R.string.courier), style = SType.Small)
                    }
                }
                TextLink("+ " + stringResource(R.string.new_client), { editing = Editing.OfClient(null) }, color = S.c.enamel)

                SectionTitle(stringResource(R.string.shops))
                Text(stringResource(R.string.shops_settings_sub), style = SType.Muted)
                Spacer(Modifier.height(8.dp))
                home.shops.forEach { sh ->
                    val learned = home.aisles.count { it.shopId == sh.id }
                    Row(Modifier.fillMaxWidth().clickable { editing = Editing.OfShop(sh) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh.name, style = SType.Strong, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.learned_n, learned), style = SType.Small)
                    }
                }
                TextLink("+ " + stringResource(R.string.new_shop), { editing = Editing.OfShop(null) }, color = S.c.enamel)

                SectionTitle(stringResource(R.string.on_the_run))
                Text(stringResource(R.string.carry_title), style = SType.Strong)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 7, 10, 15, 25).forEach { kg -> Chip(stringResource(R.string.kg_n, kg), s.carryKg == kg) { container.write { settings.update { it.copy(carryKg = kg) } } } }
                }
                Spacer(Modifier.height(8.dp))
                Toggle(stringResource(R.string.cold_last), stringResource(R.string.cold_last_sub), s.coldLast) { v -> container.write { settings.update { it.copy(coldLast = v) } } }
                Toggle(stringResource(R.string.note_prices), stringResource(R.string.note_prices_sub), s.notePrices) { v -> container.write { settings.update { it.copy(notePrices = v) } } }

                SectionTitle(stringResource(R.string.currency))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { cur -> Chip("${cur.symbol} ${cur.name}", s.currency == cur) { container.write { settings.update { it.copy(currency = cur) } } } }
                }

                SectionTitle(stringResource(R.string.language))
                Segmented(
                    listOf(Language.SYSTEM, Language.EN, Language.RU), Language.current(),
                    { stringResource(when (it) { Language.EN -> R.string.lang_en; Language.RU -> R.string.lang_ru; else -> R.string.lang_system }) },
                    Language::set,
                )

                SectionTitle(stringResource(R.string.community))
                CommunityLinks()

                SectionTitle(stringResource(R.string.about))
                PaperCard {
                    Text(stringResource(R.string.about_text), style = SType.Muted)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = SType.TypedSmall)
                }
                TextLink(stringResource(R.string.how_it_works), go::guide, Modifier.padding(top = 8.dp), color = S.c.enamel)
                Spacer(Modifier.height(24.dp))
            }
        }

        Sheet(editing != null, { editing = null }) {
            when (val e = editing) {
                is Editing.OfClient -> ClientSheet(e.c) { editing = null }
                is Editing.OfShop -> ShopSheet(e.s) { editing = null }
                null -> Unit
            }
        }
    }
}

@Composable
private fun ColumnScope.ClientSheet(client: Client?, onClose: () -> Unit) {
    val container = ctx().container
    var name by rememberSaveable(client?.id) { mutableStateOf(client?.name.orEmpty()) }
    Text(stringResource(if (client == null) R.string.new_client else R.string.client), style = SType.Title)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.client_sub), style = SType.Muted)
    Spacer(Modifier.height(12.dp))
    Field(name, { name = it.take(20) }, stringResource(R.string.client_name))
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.save), {
        val n = name.trim()
        if (n.isNotEmpty()) container.write { if (client == null) office.addClient(n) else office.rename(client, n) }
        onClose()
    }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    if (client != null && !client.me) {
        Spacer(Modifier.height(6.dp))
        TextLink(stringResource(R.string.remove_client), {
            container.write { office.removeClient(client) }
            onClose()
        }, Modifier.align(Alignment.CenterHorizontally), color = S.c.stamp)
    }
}

@Composable
private fun ColumnScope.ShopSheet(shop: Shop?, onClose: () -> Unit) {
    val container = ctx().container
    var name by rememberSaveable(shop?.id) { mutableStateOf(shop?.name.orEmpty()) }
    Text(stringResource(if (shop == null) R.string.new_shop else R.string.shop), style = SType.Title)
    Spacer(Modifier.height(12.dp))
    Field(name, { name = it.take(30) }, stringResource(R.string.shop_name))
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.save), {
        val n = name.trim()
        if (n.isNotEmpty()) container.write { if (shop == null) office.addShop(n) else office.rename(shop, n) }
        onClose()
    }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    if (shop != null) {
        Spacer(Modifier.height(6.dp))
        TextLink(stringResource(R.string.relearn), {
            container.write { office.relearn(shop) }
            onClose()
        }, Modifier.align(Alignment.CenterHorizontally))
        TextLink(stringResource(R.string.remove_shop), {
            container.write { office.removeShop(shop) }
            onClose()
        }, Modifier.align(Alignment.CenterHorizontally), color = S.c.stamp)
    }
}
