package app.errand.ui.depot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.container
import app.errand.domain.Upgrade
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.art.DepotLook
import app.errand.ui.art.DepotScene
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SectionTitle
import app.errand.ui.components.Stamp
import app.errand.ui.components.TopBar
import app.errand.ui.ctx
import app.errand.ui.office.Coins
import app.errand.ui.office.Token
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import java.time.Instant
import java.time.ZoneId

/** The depot: what the coins buy. Tap a thing to see it in its place; buy it when the till allows. */
@Composable
fun DepotScreen(home: Home, go: Go, startPreview: Upgrade? = null) {
    val c = ctx()
    val container = c.container
    var preview by rememberSaveable { mutableStateOf(startPreview ?: Upgrade.entries.firstOrNull { it !in home.owned && it.needs <= home.rank }) }
    val t = Instant.ofEpochMilli(home.now).atZone(ZoneId.systemDefault())

    Column(Modifier.fillMaxSize().background(S.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.depot), go::back) {
            Coins(home.balance)
            Spacer(Modifier.width(10.dp))
        }
        DepotScene(
            DepotLook(home.owned, t.hour, t.minute, stringResource(R.string.sign), preview = preview),
            Modifier.fillMaxWidth().aspectRatio(360f / 220f),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            val p = preview
            if (p != null && p !in home.owned) {
                Spacer(Modifier.height(12.dp))
                val (name, sub) = T.upgrade(c, p)
                Column(Modifier.fillMaxWidth().clip(S.RMd).background(S.c.paper).border(1.dp, S.c.line, S.RMd).padding(14.dp)) {
                    Text(name, style = SType.Title)
                    Text(sub, style = SType.Muted)
                    Spacer(Modifier.height(10.dp))
                    when {
                        p.needs > home.rank -> Text(stringResource(R.string.needs_rank, T.rank(c, p.needs)), style = SType.Muted.copy(color = S.c.stamp))
                        home.balance < p.cost -> Text(stringResource(R.string.need_more, T.coins(c, p.cost - home.balance)), style = SType.Muted)
                        else -> PrimaryButton(stringResource(R.string.buy_for, T.coins(c, p.cost)), { container.write { office.buy(p) } }, Modifier.fillMaxWidth())
                    }
                }
            }
            SectionTitle(stringResource(R.string.fleet))
            Upgrade.entries.filter { it.fleet }.forEach { UpgradeRow(it, home, it == preview) { preview = it } }
            SectionTitle(stringResource(R.string.premises))
            Upgrade.entries.filter { !it.fleet }.forEach { UpgradeRow(it, home, it == preview) { preview = it } }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UpgradeRow(u: Upgrade, home: Home, selected: Boolean, onClick: () -> Unit) {
    val c = ctx()
    val owned = u in home.owned
    val locked = u.needs > home.rank
    val (name, _) = T.upgrade(c, u)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(S.RMd)
            .background(if (selected) S.c.paper else S.c.paper2)
            .border(1.dp, if (selected) S.c.enamel.copy(alpha = 0.6f) else S.c.line, S.RMd)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = SType.Strong.copy(color = if (locked) S.c.faint else S.c.ink))
            if (locked) Text(stringResource(R.string.needs_rank, T.rank(c, u.needs)), style = SType.Small)
        }
        if (owned) Stamp(stringResource(R.string.owned), S.c.enamel, angle = -5f, size = 11.sp)
        else Row(verticalAlignment = Alignment.CenterVertically) {
            Token(14.dp)
            Spacer(Modifier.width(6.dp))
            Text(u.cost.toString(), style = SType.Strong.copy(color = if (home.balance >= u.cost && !locked) S.c.ink else S.c.faint))
        }
    }
}
