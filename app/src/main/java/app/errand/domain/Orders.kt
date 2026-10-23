package app.errand.domain

import app.errand.data.Currency
import app.errand.data.Item

/** One order line read from a message: what, how much, and whether it's a rush. */
data class Order(val name: String, val qty: String, val rush: Boolean)

/**
 * Lists that arrive from a messenger ("Share → Errand"): one thing per line, or comma-separated;
 * bullets, numbers and ticks are dropped. A line (or the whole message) starting with "!" or
 * "срочно"/"urgent" is a rush.
 */
object Orders {
    private val rushWord = Regex("(?iu)^\\s*(?:!+|срочно[:!,.]?|urgent[:!,.]?|asap[:!,.]?)\\s*")
    private val bullet = Regex("^\\s*(?:[-–—•*·▪●○◦✓✔☐☑□■]|\\d{1,2}[.)]|\\[[ xх]?])\\s*")
    private val listSplit = Regex("(?<!\\d),|,(?!\\d)|;")
    private val greeting = Regex("(?iu)^(купи(те)?|возьми(те)?|захвати|пожалуйста|please (buy|get)|please|buy|get|can you get|pick up)(?!\\p{L})[:, ]*")

    fun parse(text: String, ru: Boolean): List<Order> {
        var t = text.trim()
        var all = false
        rushWord.find(t)?.let { all = true; t = t.removeRange(it.range) }
        val raw = t.lines().flatMap { line ->
            val l = line.trim()
            // "хлеб, молоко, яйца" is three lines; "молоко 2,5%" is one.
            l.split(listSplit)
        }
        return raw.mapNotNull { r ->
            var s = r.trim()
            // "Купи, пожалуйста:" heads the list; it isn't a thing to buy.
            if (s.endsWith(':')) return@mapNotNull null
            var rush = all
            rushWord.find(s)?.let { rush = true; s = s.removeRange(it.range) }
            s = s.replace(bullet, "").replace(greeting, "").trim().trimEnd('.', ';', ',', '!')
            if (s.length < 2 || s.length > 80 || s.none { it.isLetter() }) return@mapNotNull null
            val (name, qty) = Qty.split(s, ru)
            Order(name, qty, rush)
        }.distinctBy { Goods.key(it.name) }.take(60)
    }

    /** The waybill as text, to send to the family chat. */
    fun text(number: Int, items: List<Item>, names: Map<Long, String>, ru: Boolean, title: String): String = buildString {
        append(title.format(number)).append('\n')
        items.forEachIndexed { i, it ->
            append(i + 1).append(". ").append(it.name)
            if (it.qty.isNotEmpty()) append(" — ").append(it.qty)
            if (it.rush) append(if (ru) " (срочно)" else " (rush)")
            names[it.clientId]?.let { n -> append("  · ").append(n) }
            append('\n')
        }
    }.trimEnd()
}

/** A thing's prices, over time and across shops: the merchandiser's notebook. */
data class PriceRow(
    val key: String,
    val name: String,
    val last: Long,
    val lastShop: Long,
    val lastAt: Long,
    /** Change against the first price noted at least [Notebook.SINCE_DAYS] days earlier, in percent. */
    val change: Int?,
    val since: Long,
    /** The shop where it was last seen cheapest, when there are two to compare. */
    val cheapest: Long?,
    val saving: Long,
    val history: List<Pair<Long, Long>>,
)

object Notebook {
    const val SINCE_DAYS = 28

    fun rows(prices: List<app.errand.data.Price>): List<PriceRow> =
        prices.groupBy { it.key }.map { (key, ps) ->
            val sorted = ps.sortedBy { it.at }
            val last = sorted.last()
            val old = sorted.lastOrNull { it.at <= last.at - SINCE_DAYS * Rules.DAY } ?: sorted.first().takeIf { it.at <= last.at - SINCE_DAYS * Rules.DAY }
            val change = old?.let { if (it.price > 0) ((last.price - it.price) * 100.0 / it.price).let { c -> kotlin.math.round(c).toInt() } else null }
            // The latest price in each shop; the cheapest of them, if it's another shop.
            val perShop = sorted.groupBy { it.shopId }.mapValues { (_, v) -> v.last() }
            val cheap = perShop.values.minByOrNull { it.price }
            val cheapest = if (perShop.size >= 2 && cheap != null && cheap.price < perShop.values.maxOf { it.price }) cheap.shopId else null
            val saving = if (cheapest != null) perShop.values.maxOf { it.price } - cheap!!.price else 0
            PriceRow(key, last.name, last.price, last.shopId, last.at, change, old?.at ?: 0, cheapest, saving, sorted.map { it.at to it.price })
        }.sortedByDescending { it.lastAt }

    /** Things that went up the most, for the notebook's first page. */
    fun risers(rows: List<PriceRow>): List<PriceRow> = rows.filter { (it.change ?: 0) >= 5 }.sortedByDescending { it.change }

    fun price(text: String) = Currency.parse(text)
}

/** A month in the archive: what it took and what it paid. */
data class Month(
    val year: Int,
    val month: Int,
    val runs: Int,
    val lines: Int,
    val grams: Long,
    val spent: Long,
    val saved: Long,
    val coins: Int,
    val missed: Int,
)

object Archive {
    fun months(runs: List<app.errand.data.Run>, items: List<Item>): List<Month> {
        val byRun = items.filter { it.runId > 0 }.groupBy { it.runId }
        return Rules.done(runs).groupBy { Rules.date(it.endedAt).let { d -> d.year * 100 + d.monthValue } }
            .toSortedMap(reverseOrder())
            .map { (ym, rs) ->
                val taken = rs.flatMap { byRun[it.id].orEmpty() }
                val thrift = rs.filter { it.budget > 0 && it.spent in 1..it.budget }
                Month(
                    year = ym / 100, month = ym % 100, runs = rs.size,
                    lines = taken.count { !it.offList },
                    grams = taken.sumOf { it.grams.toLong() },
                    spent = rs.sumOf { it.spent },
                    saved = thrift.sumOf { it.budget - it.spent },
                    coins = rs.sumOf { it.pay },
                    missed = rs.sumOf { it.missed },
                )
            }
    }
}
