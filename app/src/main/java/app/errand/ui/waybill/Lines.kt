package app.errand.ui.waybill

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.errand.R
import app.errand.data.Item
import app.errand.domain.Rules
import app.errand.ui.Home
import app.errand.ui.T
import app.errand.ui.components.ClientMark
import app.errand.ui.components.weight
import app.errand.ui.theme.S
import app.errand.ui.theme.SType
import app.errand.ui.theme.Sign

/** A small inked tag in a line: rush, cold, a shop, a red "not delivered". */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontFamily = Sign, fontSize = 10.sp, letterSpacing = 0.6.sp, color = color, maxLines = 1,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.8f), S.RXs)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** The tags a line wears on the waybill. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineTags(item: Item, home: Home, showShop: Boolean = true) {
    val tags = buildList<@Composable () -> Unit> {
        if (item.rush) {
            val left = Rules.rushLeft(item, home.now)
            add { Tag(if (left >= 0) stringResource(R.string.rush_by, T.time(item.rushBy)) else stringResource(R.string.rush_late), S.c.stamp) }
        }
        if (item.missed > 0) add { Tag(stringResource(R.string.not_delivered), S.c.stamp) }
        else if (item.carried > 0) add { Tag(stringResource(R.string.carried_over), S.c.faint) }
        if (showShop && item.shopId != 0L) home.shop(item.shopId)?.let { s -> add { Tag(s.name, S.c.enamel) } }
    }
    if (tags.isNotEmpty() || item.cold || item.note.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            tags.forEach { it() }
            if (item.cold) Icon(Icons.Rounded.AcUnit, stringResource(R.string.cold), Modifier.size(15.dp), tint = S.c.violet)
            if (item.note.isNotEmpty()) Text(item.note, style = SType.Hand.copy(fontSize = 18.sp, lineHeight = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** One typed line of the waybill: number, name and amount, its tags, the weight, whose order. */
@Composable
fun LineRow(n: Int, item: Item, home: Home, modifier: Modifier = Modifier, struck: Boolean = false, onClick: (() -> Unit)? = null, showShop: Boolean = true, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$n.", style = SType.Typed.copy(color = S.c.faint), modifier = Modifier.width(30.dp))
        Column(Modifier.weight(1f)) {
            val name = if (item.qty.isEmpty()) item.name else "${item.name} — ${item.qty}"
            Text(
                name,
                style = SType.Typed.copy(color = if (struck) S.c.faint else S.c.ink, textDecoration = if (struck) TextDecoration.LineThrough else null),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            LineTags(item, home, showShop)
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) trailing()
        else {
            Text(weight(item.grams, home.ru), style = SType.TypedSmall, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            home.client(item.clientId)?.let { ClientMark(it.name, it.mark, size = 24.dp) }
        }
    }
}
