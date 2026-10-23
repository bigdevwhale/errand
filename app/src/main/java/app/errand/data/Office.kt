package app.errand.data

import androidx.room.withTransaction
import app.errand.domain.Goods
import app.errand.domain.Order
import app.errand.domain.Route
import app.errand.domain.Rules
import app.errand.domain.Upgrade

/** Every change to the office's books: orders taken, runs made, coins spent. */
class Office(private val db: ErrandDatabase, private val settings: SettingsStore) {
    private val dao = db.dao()

    /** "Me" is always a client: the first time anything is ordered, the courier is entered in the books. */
    suspend fun me(name: String): Long {
        dao.clients().firstOrNull { it.me }?.let { return it.id }
        return dao.insert(Client(name = name, mark = 0, me = true))
    }

    suspend fun order(name: String, qty: String, clientId: Long, rushMinutes: Int = 0, shopId: Long = 0, now: Long = System.currentTimeMillis()): Long {
        val g = Goods.guess(if (qty.isEmpty()) name else "$name $qty")
        return dao.insert(
            Item(
                name = name.trim(), key = Goods.key(name), qty = qty.trim(), clientId = clientId,
                grams = g.grams, cold = g.cold, shelf = g.shelf.name, shopId = shopId,
                rushBy = if (rushMinutes > 0) now + rushMinutes * Rules.MINUTE else 0, addedAt = now,
            ),
        )
    }

    /** Orders from a message, all for one client. Lines already on the waybill aren't added twice. */
    suspend fun orders(list: List<Order>, clientId: Long): Int {
        val pending = dao.items().filter { it.lineOf == Line.PENDING }.map { it.key }.toSet()
        var n = 0
        val now = System.currentTimeMillis()
        list.filter { Goods.key(it.name) !in pending }.forEachIndexed { i, o ->
            order(o.name, o.qty, clientId, if (o.rush) 60 else 0, now = now + i)
            n++
        }
        return n
    }

    suspend fun save(item: Item) = dao.update(item.copy(key = Goods.key(item.name)))

    suspend fun delete(id: Long) = dao.deleteItem(id)

    /** Out of the door: a new waybill number, the money handed out, the shops on the way. */
    suspend fun start(budget: Long, shops: List<Long>, at: Long = System.currentTimeMillis()): Long {
        dao.runs().firstOrNull { it.legOf == Leg.OUT }?.let { return it.id }
        return dao.insert(Run(number = Rules.nextNumber(dao.runs()), startedAt = at, budget = budget, shops = shops.joinToString(",")))
    }

    /** Into the bag. The price, when noted, goes into the merchandiser's notebook too. */
    suspend fun take(item: Item, run: Run, shopId: Long, price: Long = 0, at: Long = System.currentTimeMillis()) {
        dao.update(item.copy(state = Line.TAKEN.name, runId = run.id, takenShop = shopId, takenAt = at, price = price, outOn = 0))
        if (price > 0) dao.insert(Price(key = item.key, name = item.name, shopId = shopId, price = price, at = at))
    }

    suspend fun notePrice(item: Item, price: Long) {
        dao.update(item.copy(price = price))
        if (price > 0) dao.insert(Price(key = item.key, name = item.name, shopId = item.takenShop, price = price, at = item.takenAt.takeIf { it > 0 } ?: System.currentTimeMillis()))
    }

    /** Back on the shelf (a tick by mistake). Off-list things just go. */
    suspend fun putBack(item: Item) {
        if (item.offList) {
            dao.deleteItem(item.id)
            return
        }
        dao.update(item.copy(state = Line.PENDING.name, runId = 0, takenShop = 0, takenAt = 0, price = 0))
        dao.prices().lastOrNull { it.key == item.key && it.at == item.takenAt }?.let { dao.deletePrice(it.id) }
    }

    /** The shelf was empty: it rides over to the next run without a mark. */
    suspend fun out(item: Item, run: Run, on: Boolean) = dao.update(item.copy(outOn = if (on) run.id else 0))

    /** Picked up without an order. */
    suspend fun unordered(name: String, qty: String, run: Run, shopId: Long, clientId: Long, price: Long) {
        val g = Goods.guess(if (qty.isEmpty()) name else "$name $qty")
        val now = System.currentTimeMillis()
        dao.insert(
            Item(
                name = name.trim(), key = Goods.key(name), qty = qty.trim(), clientId = clientId, grams = g.grams, cold = g.cold, shelf = g.shelf.name,
                state = Line.TAKEN.name, runId = run.id, takenShop = shopId, takenAt = now, price = price, offList = true, addedAt = now,
            ),
        )
        if (price > 0) dao.insert(Price(key = Goods.key(name), name = name.trim(), shopId = shopId, price = price, at = now))
    }

    /**
     * Home. The pay is worked out and fixed, the shops learn their shelves, and what stayed behind rides
     * over: forgotten lines with a red mark, empty-shelf ones without.
     */
    suspend fun deliver(runId: Long, weather: Weather, spent: Long, at: Long = System.currentTimeMillis()): Run? {
        var done: Run? = null
        db.withTransaction {
            val run = dao.run(runId) ?: return@withTransaction
            val items = dao.items()
            val taken = Rules.taken(run, items)
            val left = Rules.due(run, items)
            val missed = left.count { it.outOn != run.id }
            val slip = Rules.pay(run, taken, missed, at, weather, spent)
            val out = left.size - missed
            // A rush that missed its run is no longer a rush.
            dao.updateItems(left.map { if (it.outOn == run.id) it.copy(carried = it.carried + 1, outOn = 0, rushBy = 0) else it.copy(carried = it.carried + 1, missed = it.missed + 1, rushBy = 0) })
            taken.map { it.takenShop }.distinct().forEach { shop ->
                val learned = Route.learn(shop, taken, dao.aisles(shop))
                if (learned.isNotEmpty()) dao.put(learned)
            }
            val r = run.copy(leg = Leg.DONE.name, endedAt = at, weather = weather.name, spent = spent, pay = slip.total, missed = missed, out = out)
            dao.update(r)
            done = r
        }
        return done
    }

    /** Called off: everything goes back on the waybill as it was. */
    suspend fun cancel(runId: Long) {
        db.withTransaction {
            val run = dao.run(runId) ?: return@withTransaction
            val items = dao.items()
            Rules.taken(run, items).forEach { putBack(it) }
            dao.updateItems(dao.items().filter { it.outOn == run.id }.map { it.copy(outOn = 0) })
            dao.deleteRun(run.id)
        }
    }

    suspend fun buy(u: Upgrade): Boolean {
        val s = settings.now()
        if (u in s.owned || Rules.balance(dao.runs(), s.owned) < u.cost) return false
        settings.update { it.copy(owned = it.owned + u) }
        return true
    }

    suspend fun addClient(name: String): Long {
        val cs = dao.clients()
        val used = cs.map { it.mark }.toSet()
        val mark = (1 until 8).firstOrNull { it !in used } ?: (cs.size % 8)
        return dao.insert(Client(name = name.trim(), mark = mark))
    }

    suspend fun rename(c: Client, name: String) = dao.update(c.copy(name = name.trim()))

    /** A client leaves; their lines become the courier's own. */
    suspend fun removeClient(c: Client) {
        if (c.me) return
        val me = dao.clients().firstOrNull { it.me }?.id ?: 0
        dao.moveClient(c.id, me)
        dao.deleteClient(c.id)
    }

    suspend fun addShop(name: String): Long = dao.insert(Shop(name = name.trim()))

    suspend fun rename(s: Shop, name: String) = dao.update(s.copy(name = name.trim()))

    suspend fun removeShop(s: Shop) {
        dao.forgetShop(s.id)
        dao.deleteAisles(s.id)
        dao.deleteShop(s.id)
    }

    /** Forget what a shop learned, after a refit. */
    suspend fun relearn(s: Shop) = dao.deleteAisles(s.id)
}
