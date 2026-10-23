package app.errand.ui.office

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.container
import app.errand.domain.Qty
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.art.DepotLook
import app.errand.ui.art.DepotScene
import app.errand.ui.art.RankBadge
import app.errand.ui.components.Field
import app.errand.ui.components.PaperCard
import app.errand.ui.components.PrimaryButton
import app.errand.ui.components.SoftButton
import app.errand.ui.components.Stamp
import app.errand.ui.components.weight
import app.errand.ui.ctx
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import app.errand.ui.waybill.AcceptSheet
import app.errand.ui.waybill.LineRow
import app.errand.ui.components.Sheet
import java.time.Instant
import java.time.ZoneId

/** The office: the depot in its street, the courier's rank and coins, today's waybill, the way out. */
@Composable
fun OfficeScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    var text by rememberSaveable { mutableStateOf("") }
    var accept by rememberSaveable { mutableStateOf(false) }
    val t = Instant.ofEpochMilli(home.now).atZone(ZoneId.systemDefault())

    fun add() {
        val s = text.trim()
        if (s.isEmpty()) return
        val (name, qty) = Qty.split(s, home.ru)
        text = ""
        container.write { office.order(name, qty, home.me?.id ?: office.me(c.getString(R.string.me))) }
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Box {
                DepotScene(
                    DepotLook(home.owned, t.hour, t.minute, stringResource(R.string.sign), out = home.open != null),
                    Modifier.fillMaxWidth().aspectRatio(360f / 220f),
                )
                // Ink by day, cream under the night sky.
                val tint = if (t.hour in 6..20) S.c.ink.copy(alpha = 0.75f) else S.c.paper.copy(alpha = 0.85f)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = go::guide) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.how_it_works), tint = tint) }
                    IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = tint) }
                }
            }

            // The courier: rank and coins.
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(S.RMd).clickable(onClick = go::file).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(home.rank.ordinal, Modifier.size(38.dp, 46.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(T.rank(c, home.rank), style = SType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(home.me?.name.orEmpty().ifEmpty { stringResource(R.string.me) } + " · " + T.runs(c, home.stats.runs), style = SType.Muted, maxLines = 1)
                    }
                }
                Coins(home.balance, go::depot)
            }

            Column(Modifier.padding(horizontal = 12.dp)) {
                val open = home.open
                if (open != null) {
                    PaperCard(onClick = go::run) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.waybill_n, open.number), style = SType.Title)
                                Text(stringResource(R.string.out_for, T.duration(c, home.now - open.startedAt)), style = SType.Muted)
                                Text(stringResource(R.string.in_bag, home.bag.count { !it.offList }, home.bag.count { !it.offList } + home.due.size), style = SType.Typed.copy(color = S.c.muted))
                            }
                            Stamp(stringResource(R.string.on_the_way), S.c.violet, angle = -8f, size = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(stringResource(R.string.continue_run), go::run, Modifier.fillMaxWidth())
                    }
                } else {
                    PaperCard(ruled = 0.dp) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(stringResource(R.string.waybill_n, home.number), style = SType.Title, modifier = Modifier.weight(1f))
                            Text(T.typed(home.now), style = SType.TypedSmall)
                        }
                        Text(
                            if (home.pending.isEmpty()) stringResource(R.string.waybill_empty_short)
                            else stringResource(R.string.waybill_sum, T.lines(c, home.pending.size), weight(home.grams, home.ru)),
                            style = SType.Muted.copy(color = if (home.heavy) S.c.stamp else S.c.muted),
                        )
                        Spacer(Modifier.height(6.dp))
                        home.pending.take(6).forEachIndexed { i, item -> LineRow(i + 1, item, home, onClick = go::waybill) }
                        if (home.pending.size > 6) Text(
                            stringResource(R.string.more_lines, home.pending.size - 6), style = SType.Typed.copy(color = S.c.faint),
                            modifier = Modifier.clickable(onClick = go::waybill).padding(start = 30.dp, top = 4.dp, bottom = 4.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(
                                text, { text = it.take(80) }, stringResource(R.string.add_line), Modifier.weight(1f),
                                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                actions = KeyboardActions(onDone = { add() }),
                            )
                            IconButton(onClick = ::add, enabled = text.isNotBlank()) {
                                Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.add), tint = if (text.isNotBlank()) S.c.enamel else S.c.faint)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SoftButton(stringResource(R.string.open_waybill), go::waybill, Modifier.weight(1f))
                            PrimaryButton(stringResource(R.string.accept), { accept = true }, Modifier.weight(1f), enabled = home.pending.isNotEmpty())
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(Icons.Rounded.Inventory2, stringResource(R.string.archive), last(home, c), Modifier.weight(1f), go::archive)
                    Tile(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.notebook), stringResource(R.string.notebook_sub, home.notebook.size), Modifier.weight(1f), go::notebook)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(Icons.Rounded.Storefront, stringResource(R.string.depot), stringResource(R.string.depot_sub, home.owned.size), Modifier.weight(1f), go::depot)
                    Tile(Icons.Rounded.Badge, stringResource(R.string.file), stringResource(R.string.file_sub, home.badges.size), Modifier.weight(1f), go::file)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Sheet(accept, { accept = false }) { AcceptSheet(home, go) { accept = false } }
    }
}

private fun last(home: Home, c: android.content.Context): String {
    val r = home.done.firstOrNull() ?: return c.getString(R.string.archive_empty)
    return c.getString(R.string.archive_last, r.number, T.ago(c, r.endedAt))
}

/** The coins in the till, a brass token beside them. */
@Composable
fun Coins(n: Int, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(S.RMd).background(S.c.brassSoft).border(1.dp, S.c.brass.copy(alpha = 0.6f), S.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Token(18.dp)
        Spacer(Modifier.width(7.dp))
        Text(n.toString(), style = SType.Strong.copy(fontSize = 17.sp))
    }
}

/** A brass token with a hole, the office's coin. */
@Composable
fun Token(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(S.c.brass), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 0.72f).border(1.dp, S.c.brassSoft.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * 0.22f).clip(CircleShape).background(S.c.brassSoft))
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(S.RMd).background(S.c.paper).border(1.dp, S.c.line, S.RMd).clickable(onClick = onClick).padding(14.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = S.c.enamel)
        Spacer(Modifier.height(8.dp))
        Text(title, style = SType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = SType.Muted.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
