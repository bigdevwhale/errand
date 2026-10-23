package app.errand.data

import android.content.Context
import app.errand.AppContainer
import app.errand.domain.Goods
import app.errand.domain.Rules
import app.errand.domain.Upgrade
import app.errand.localized
import app.errand.ru
import kotlin.random.Random

/**
 * An office four months in, for the demo build and the screenshots. Every run is made by the real
 * engine: orders placed by the household, a walk through the shop in roughly its shelf order, prices
 * creeping up, the odd thing forgotten, then the pay. The coins went on a bicycle and the depot.
 */
object DemoSeed {
    private const val DAY = 86_400_000L
    private const val HOUR = 3_600_000L

    /** A thing the household buys: names, amount, price in roubles and in cents, whose it usually is, which shop. */
    private class Ware(val ru: String, val en: String, val qtyRu: String, val qtyEn: String, val rub: Long, val cents: Long, val who: Int, val shop: Int = 0)

    private val wares = listOf(
        Ware("Молоко", "Milk", "2 л", "2 l", 92, 189, 1),
        Ware("Хлеб бородинский", "Rye bread", "", "", 64, 279, 0),
        Ware("Яйца", "Eggs", "10 шт", "10 pcs", 119, 399, 1),
        Ware("Бананы", "Bananas", "1 кг", "1 kg", 139, 159, 2),
        Ware("Гречка", "Buckwheat", "", "", 89, 349, 0),
        Ware("Сыр", "Cheese", "300 г", "300 g", 389, 549, 1),
        Ware("Кофе в зёрнах", "Coffee beans", "", "", 649, 1199, 0),
        Ware("Йогурт", "Yogurt", "4 шт", "4 pcs", 59, 129, 2),
        Ware("Курица", "Chicken", "1 кг", "1 kg", 329, 799, 0),
        Ware("Помидоры", "Tomatoes", "", "", 249, 349, 3, shop = 1),
        Ware("Огурцы", "Cucumbers", "", "", 159, 199, 3, shop = 1),
        Ware("Яблоки", "Apples", "1,5 кг", "1.5 kg", 169, 349, 2, shop = 1),
        Ware("Творог", "Cottage cheese", "", "", 129, 299, 3, shop = 1),
        Ware("Картошка", "Potatoes", "3 кг", "3 kg", 159, 399, 0, shop = 1),
        Ware("Масло сливочное", "Butter", "", "", 239, 449, 1),
        Ware("Макароны", "Pasta", "", "", 79, 189, 0),
        Ware("Чай", "Tea", "", "", 199, 399, 3),
        Ware("Печенье", "Biscuits", "", "", 119, 249, 2),
        Ware("Туалетная бумага", "Toilet paper", "", "", 289, 699, 1),
        Ware("Средство для посуды", "Washing-up liquid", "", "", 179, 299, 1),
        Ware("Сок апельсиновый", "Orange juice", "1 л", "1 l", 159, 349, 2),
        Ware("Пельмени", "Dumplings", "", "", 349, 599, 2),
        Ware("Витамин D", "Vitamin D", "", "", 459, 899, 3, shop = 2),
        Ware("Пластырь", "Plasters", "", "", 129, 349, 2, shop = 2),
        Ware("Сметана", "Sour cream", "", "", 89, 199, 3),
        Ware("Лук", "Onions", "1 кг", "1 kg", 49, 129, 0, shop = 1),
        Ware("Кефир", "Kefir", "", "", 79, 219, 3),
        Ware("Шоколад", "Chocolate", "", "", 99, 229, 2),
    )

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.runs().isNotEmpty() || dao.items().isNotEmpty()) return
        val ru = context.localized().ru
        val now = System.currentTimeMillis()
        val r = Random(42)
        fun n(en: String, rus: String) = if (ru) rus else en
        c.settings.update { it.copy(onboarded = true, currency = if (ru) Currency.RUB else Currency.USD, carryKg = 7) }

        val me = c.office.me(n("Sam", "Саша"))
        val clients = listOf(me, c.office.addClient(n("Anna", "Аня")), c.office.addClient(n("Misha", "Миша")), c.office.addClient(n("Grandma", "Бабушка")))
        val shops = listOf(c.office.addShop(n("Corner shop", "Магазин у дома")), c.office.addShop(n("Market", "Рынок")), c.office.addShop(n("Pharmacy", "Аптека")))
        // Where things really lie in the corner shop, door to tills: the run follows this with a little wandering.
        val truth = wares.map { Goods.key(if (ru) it.ru else it.en) }.shuffled(Random(5)).withIndex().associate { (i, k) -> k to i / wares.size.toFloat() }

        val start = java.time.LocalDate.now().minusDays(122).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var day = start
        var made = 0
        while (day < now - 2 * DAY) {
            val at = day + (10 + r.nextInt(9)) * HOUR
            // The household orders through the days before.
            val picks = wares.shuffled(r).take(5 + r.nextInt(9))
            val pending = dao.items().filter { it.lineOf == Line.PENDING }.map { it.key }.toSet()
            picks.filter { Goods.key(if (ru) it.ru else it.en) !in pending }.forEachIndexed { i, w ->
                val name = if (ru) w.ru else w.en
                val rush = r.nextInt(14) == 0
                val placed = if (rush) at - 20 * 60_000L + i else at - (2 + r.nextInt(30)) * HOUR + i
                c.office.order(name, if (ru) w.qtyRu else w.qtyEn, clients[if (r.nextInt(4) == 0) r.nextInt(4) else w.who], if (rush) 60 else 0, shops[w.shop].takeIf { w.shop != 0 } ?: 0, now = placed)
            }
            val wants = dao.items().filter { it.lineOf == Line.PENDING }.map { it.shopId }.toSet()
            val visit = listOf(shops[0]) + listOfNotNull(shops[1].takeIf { it in wants && r.nextInt(3) > 0 }, shops[2].takeIf { it in wants && r.nextBoolean() })
            val budget = if (r.nextInt(3) == 0) 0L else (if (ru) (1500L + r.nextInt(6) * 500) * 100 else (40L + r.nextInt(5) * 10) * 100)
            val runId = c.office.start(budget, visit, at)
            val run = dao.run(runId)!!
            val due = Rules.due(run, dao.items())
            // A thing or two forgotten now and then, an empty shelf sometimes.
            val forget = if (r.nextInt(5) == 0) due.filter { it.shopId == 0L }.shuffled(r).take(1).map { it.id }.toSet() else emptySet()
            var t = at
            val inflation = 1.0 + (day - start) / (122.0 * DAY) * 0.12
            for (shop in visit) {
                val here = due.filter { (it.shopId == shop || (it.shopId == 0L && shop == shops[0])) && it.id !in forget }
                    .sortedBy { (truth[it.key] ?: 0.5f) + (r.nextFloat() - 0.5f) * 0.25f }
                for (item in here) {
                    if (r.nextInt(25) == 0) {
                        c.office.out(item, run, true)
                        continue
                    }
                    t += (1 + r.nextInt(3)) * 60_000L
                    val w = wares.first { Goods.key(if (ru) it.ru else it.en) == item.key }
                    val base = if (ru) w.rub * 100 else w.cents
                    val price = if (r.nextInt(4) == 0) 0L else (base * inflation * (if (shop == shops[1]) 0.88 else 1.0) / (if (ru) 100 else 1)).toLong() * (if (ru) 100 else 1) - (if (ru) 10 else 1)
                    c.office.take(dao.item(item.id)!!, run, shop, price.coerceAtLeast(0), t)
                }
            }
            if (r.nextInt(4) == 0) c.office.unordered(n("Croissant", "Круассан"), "", run, shops[0], me, if (ru) 8900 else 249)
            val weather = listOf(Weather.FAIR, Weather.FAIR, Weather.FAIR, Weather.WIND, Weather.RAIN, Weather.SNOW, Weather.FROST)[r.nextInt(7)]
            val bag = Rules.taken(run, dao.items())
            val noted = bag.sumOf { it.price }
            val spent = if (noted > 0) noted + bag.count { it.price == 0L } * (if (ru) 15000 else 300) else 0
            c.office.deliver(runId, weather, spent, t + 25 * 60_000L)
            made++
            day += (2 + r.nextInt(3)) * DAY
        }

        // The coins went on the depot, a bicycle and lately a cargo trike.
        val wanted = listOf(Upgrade.FLAG, Upgrade.FLOWERS, Upgrade.SIGN, Upgrade.LAMP, Upgrade.BICYCLE, Upgrade.CLOCK, Upgrade.MAILBOX, Upgrade.PHONE, Upgrade.TRICYCLE)
        for (u in wanted) c.office.buy(u)

        // Today's waybill: some of it ordered this morning, a rush from Anna, one line left over from the last run.
        val today = listOf(0, 2, 5, 7, 9, 13, 18, 22)
        today.forEachIndexed { i, idx ->
            val w = wares[idx]
            val name = if (ru) w.ru else w.en
            if (dao.items().none { it.lineOf == Line.PENDING && it.key == Goods.key(name) }) {
                c.office.order(name, if (ru) w.qtyRu else w.qtyEn, clients[w.who], if (i == 1) 60 else 0, shops[w.shop].takeIf { w.shop != 0 } ?: 0, now = if (i == 1) now - 20 * 60_000L else now - (5 - i % 5) * HOUR + i)
            }
        }
        dao.items().firstOrNull { it.lineOf == Line.PENDING && it.key == Goods.key(if (ru) wares[18].ru else wares[18].en) }?.let {
            dao.update(it.copy(note = n("the soft one", "мягкую"), clientId = clients[1]))
        }
        check(made > 20)
    }
}
