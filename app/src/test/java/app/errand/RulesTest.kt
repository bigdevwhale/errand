package app.errand

import app.errand.data.Aisle
import app.errand.data.Currency
import app.errand.data.Item
import app.errand.data.Line
import app.errand.data.Leg
import app.errand.data.Price
import app.errand.data.Run
import app.errand.data.Weather
import app.errand.domain.Archive
import app.errand.domain.Badge
import app.errand.domain.Badges
import app.errand.domain.Goods
import app.errand.domain.Notebook
import app.errand.domain.Orders
import app.errand.domain.PayKind
import app.errand.domain.Qty
import app.errand.domain.Rank
import app.errand.domain.Route
import app.errand.domain.Rules
import app.errand.domain.Shelf
import app.errand.domain.Upgrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private fun item(name: String, id: Long = name.hashCode().toLong(), grams: Int = 500, run: Long = 0, taken: Boolean = false, offList: Boolean = false, rushBy: Long = 0, shop: Long = 0, at: Long = 0, outOn: Long = 0, client: Long = 1) =
        Item(
            id = id, name = name, key = Goods.key(name), grams = grams, runId = run, state = if (taken) Line.TAKEN.name else Line.PENDING.name,
            offList = offList, rushBy = rushBy, takenShop = shop, takenAt = at, outOn = outOn, clientId = client, shelf = Goods.guess(name).shelf.name, cold = Goods.guess(name).cold,
        )

    @Test fun catalogueKnowsCommonThings() {
        assertEquals(Shelf.DAIRY, Goods.guess("Молоко").shelf)
        assertTrue(Goods.guess("молоко").cold)
        assertEquals(2000, Goods.guess("молоко 2 л").grams)
        assertEquals(Shelf.PRODUCE, Goods.guess("Картошка").shelf)
        assertEquals(Shelf.GROCERY, Goods.guess("масло подсолнечное").shelf)
        assertEquals(Shelf.DAIRY, Goods.guess("Масло сливочное").shelf)
        assertEquals(Shelf.HYGIENE, Goods.guess("Зубная паста").shelf)
        assertEquals(Shelf.SWEETS, Goods.guess("пастила").shelf)
        assertEquals(Shelf.FROZEN, Goods.guess("Pizza").shelf)
        assertEquals(Shelf.BREAD, Goods.guess("Rye bread").shelf)
        assertEquals(Shelf.OTHER, Goods.guess("Гвозди").shelf)
        assertEquals(600, Goods.guess("яйца 10 шт").grams)
        assertEquals(2000, Goods.guess("сок 2 шт").grams)
        assertEquals("молоко 2 5", Goods.key("Молоко 2,5%"))
        assertEquals("еж", Goods.key("Ёж!"))
    }

    @Test fun amountsAreSplitOff() {
        assertEquals("Молоко" to "2 л", Qty.split("молоко 2л", true))
        assertEquals("Яблоки" to "3 шт", Qty.split("3x яблоки", true))
        assertEquals("Картошка" to "3 кг", Qty.split("Картошка 3 кг", true))
        assertEquals("Хлеб" to "", Qty.split("хлеб", true))
        assertEquals("Eggs" to "12 pcs", Qty.split("eggs x12", false))
    }

    @Test fun messagesBecomeOrders() {
        val list = Orders.parse("Купи, пожалуйста:\nпожалуйста, кефир\n- молоко 2 л\n• хлеб\n3. яйца 10 шт\n! таблетки от головы\n\nхлеб", true)
        assertEquals(listOf("Кефир", "Молоко", "Хлеб", "Яйца", "Таблетки от головы"), list.map { it.name })
        assertTrue(list.last().rush)
        assertFalse(list[1].rush)
        val commas = Orders.parse("срочно: сыр, молоко 2,5%, бананы", true)
        assertEquals(listOf("Сыр", "Молоко 2,5%", "Бананы"), commas.map { it.name })
        assertTrue(commas.all { it.rush })
        assertTrue(Orders.parse("1, 2", false).isEmpty())
    }

    @Test fun money() {
        assertEquals(8990L, Currency.parse("89,90"))
        assertEquals(124900L, Currency.parse("1 249"))
        assertNull(Currency.parse("abc"))
        assertEquals("89,90 ₽", Currency.RUB.format(8990))
        assertEquals("1 249 ₽", Currency.RUB.format(124900))
        assertEquals("$12.05", Currency.USD.format(1205))
        assertEquals("89,90", Currency.field(8990, true))
        assertEquals("1249", Currency.field(124900, false))
    }

    @Test fun payslip() {
        val run = Run(id = 1, number = 1, budget = 100_000)
        val taken = listOf(
            item("Молоко", 1, 2000, 1, true),
            item("Хлеб", 2, 400, 1, true, rushBy = 10_000),
            item("Картошка", 3, 3000, 1, true),
            item("Круассан", 4, 100, 1, true, offList = true),
            item("Шоколад", 5, 100, 1, true, offList = true),
        )
        val slip = Rules.pay(run, taken, forgotten = 0, at = 5_000, weather = Weather.RAIN, spent = 80_000)
        val by = slip.lines.associateBy { it.kind }
        assertEquals(15, by[PayKind.LINES]!!.coins)
        assertEquals(12, by[PayKind.WEIGHT]!!.coins) // 5.6 kg → 6
        assertEquals(10, by[PayKind.RUSH]!!.coins)
        assertEquals(Rules.CLEAN, by[PayKind.CLEAN]!!.coins)
        assertEquals(Rules.BUDGET + 4, by[PayKind.BUDGET]!!.coins)
        assertEquals(-3, by[PayKind.UNORDERED]!!.coins)
        val base = 15 + 12 + 10 + 15 + 14 - 3
        assertEquals(Math.round(base * 0.5).toInt(), by[PayKind.WEATHER]!!.coins)
        assertEquals(base + Math.round(base * 0.5).toInt(), slip.total)

        val late = Rules.pay(run, taken, forgotten = 1, at = 20_000, weather = Weather.FAIR, spent = 120_000)
        val kinds = late.lines.map { it.kind }
        assertFalse(PayKind.CLEAN in kinds)
        assertFalse(PayKind.RUSH in kinds)
        assertFalse(PayKind.BUDGET in kinds)
        assertTrue(Rules.overspent(run.copy(spent = 120_000)))
    }

    @Test fun nothingOrderedPaysNothing() {
        val run = Run(id = 1, number = 1)
        val slip = Rules.pay(run, listOf(item("Шоколад", 1, 100, 1, true, offList = true), item("Сок", 2, 100, 1, true, offList = true)), 0, 0, Weather.SNOW, 0)
        assertEquals(0, slip.total)
    }

    @Test fun dueFollowsTheShops() {
        val run = Run(id = 1, number = 1, shops = "7")
        val items = listOf(item("a", 1, shop = 0), item("b", 2).copy(shopId = 7), item("c", 3).copy(shopId = 8), item("d", 4, taken = true, run = 1))
        assertEquals(listOf(1L, 2L), Rules.due(run, items).map { it.id })
        assertEquals(3, Rules.due(Run(id = 1, number = 1), items).size)
    }

    @Test fun shopsLearnTheirShelves() {
        val taken = listOf(item("Молоко", 1, shop = 5, at = 1), item("Хлеб", 2, shop = 5, at = 2), item("Яблоки", 3, shop = 5, at = 3))
        val learned = Route.learn(5, taken, emptyList())
        assertEquals(listOf(0f, 0.5f, 1f), learned.map { it.pos })
        // Milk was found at the door; the walk now starts there, cold box or not, unless the cold goes last.
        val pending = listOf(item("Яблоки", 3), item("Молоко", 1), item("Хлеб", 2))
        assertEquals(listOf("Молоко", "Хлеб", "Яблоки"), Route.order(pending, learned, coldLast = false).map { it.name })
        assertEquals(listOf("Хлеб", "Яблоки", "Молоко"), Route.order(pending, learned, coldLast = true).map { it.name })
        // A second run moves the place, not replaces it.
        val again = Route.learn(5, listOf(item("Хлеб", 2, shop = 5, at = 1), item("Молоко", 1, shop = 5, at = 2)), learned)
        assertEquals(0.5f, again.first { it.key == "молоко" }.pos, 0.001f)
        assertTrue(Route.learn(5, listOf(item("x", 1, shop = 5, at = 1)), emptyList()).isEmpty())
        // Unknown in this shop: the usual shelf order.
        assertEquals(listOf("Яблоки", "Хлеб", "Молоко"), Route.order(pending, emptyList<Aisle>(), coldLast = false).map { it.name })
    }

    @Test fun ranksAndTill() {
        assertEquals(Rank.ERRAND, Rank.of(0))
        assertEquals(Rank.COURIER, Rank.of(5))
        assertEquals(Rank.SENIOR, Rank.of(39))
        assertEquals(Rank.DIRECTOR, Rank.of(1000))
        val runs = listOf(Run(id = 1, number = 1, leg = Leg.DONE.name, pay = 100), Run(id = 2, number = 2, leg = Leg.DONE.name, pay = 80), Run(id = 3, number = 3))
        assertEquals(180 - Upgrade.FLAG.cost, Rules.balance(runs, setOf(Upgrade.FLAG)))
        assertEquals(4, Rules.nextNumber(runs))
        assertEquals(Upgrade.BICYCLE, Upgrade.ride(setOf(Upgrade.FLAG, Upgrade.BICYCLE)))
    }

    @Test fun badges() {
        val day = Rules.DAY
        val runs = (1..5).map { Run(id = it.toLong(), number = it, leg = Leg.DONE.name, endedAt = it * day, weather = if (it == 3) Weather.RAIN.name else Weather.FAIR.name) }
        val items = listOf(item("Картошка", 1, 11_000, 2, true), item("a", 2, run = 4, taken = true, client = 1), item("b", 3, run = 4, taken = true, client = 2), item("c", 4, run = 4, taken = true, client = 3))
        val b = Badges.earned(runs, items, 0, emptySet())
        assertTrue(Badge.FIRST_RUN in b)
        assertTrue(Badge.CLEAN_FIVE in b)
        assertTrue(Badge.STORM in b)
        assertTrue(Badge.HEAVY in b)
        assertTrue(Badge.HOUSEHOLD in b)
        assertFalse(Badge.TEN_RUNS in b)
    }

    @Test fun notebookAndArchive() {
        val day = Rules.DAY
        val prices = listOf(
            Price(key = "молоко", name = "Молоко", shopId = 1, price = 8000, at = 0),
            Price(key = "молоко", name = "Молоко", shopId = 1, price = 9200, at = 40 * day),
            Price(key = "молоко", name = "Молоко", shopId = 2, price = 8500, at = 41 * day),
        )
        val row = Notebook.rows(prices).single()
        assertEquals(6, row.change) // 8000 → 8500 over 41 days
        assertEquals(2L, row.cheapest)
        assertEquals(700L, row.saving)
        val runs = listOf(Run(id = 1, number = 1, leg = Leg.DONE.name, endedAt = System.currentTimeMillis(), pay = 50, budget = 1000, spent = 800))
        val m = Archive.months(runs, listOf(item("Молоко", 1, 1000, 1, true))).single()
        assertEquals(1, m.runs)
        assertEquals(200L, m.saved)
        assertEquals(1, m.lines)
    }
}
