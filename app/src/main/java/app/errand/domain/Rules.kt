package app.errand.domain

import app.errand.data.Aisle
import app.errand.data.Item
import app.errand.data.Leg
import app.errand.data.Line
import app.errand.data.Run
import app.errand.data.Weather
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One line of a payslip: what it's for and how many coins. [n] is the count it was paid on, if any. */
data class PayLine(val kind: PayKind, val coins: Int, val n: Int = 0)

enum class PayKind {
    /** Every ordered line brought home. */
    LINES,

    /** Every kilo carried. */
    WEIGHT,

    /** Rush orders home before their time. */
    RUSH,

    /** Nothing forgotten: everything due was taken or the shelf was empty. */
    CLEAN,

    /** Home within the money handed out. */
    BUDGET,

    /** Things picked up without an order, past the one treat. */
    UNORDERED,

    /** Bad weather out there. */
    WEATHER,

    /** A big run: many lines or several shops. */
    BIG,
}

data class Payslip(val lines: List<PayLine>) {
    val total: Int = lines.sumOf { it.coins }.coerceAtLeast(0)
}

/** Everything the office decides, in one place, so the screens and the tests agree. */
object Rules {
    const val DAY = 86_400_000L
    const val MINUTE = 60_000L

    const val PER_LINE = 5
    const val PER_KG = 2
    const val PER_RUSH = 10
    const val CLEAN = 15
    const val BUDGET = 10
    const val BUDGET_SAVED = 20
    const val UNORDERED = 3

    /** A run with this many lines, or more than one shop, is a big one. */
    const val BIG_LINES = 15

    /** Default rush times offered, in minutes. */
    val RUSH_MINUTES = listOf(30, 60, 120)

    private fun zone(): ZoneId = ZoneId.systemDefault()
    fun date(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()

    fun grams(items: List<Item>): Int = items.sumOf { it.grams }
    fun kg(grams: Int): Double = grams / 1000.0

    /** Lines that ride on [run]: everything pending for one of its shops (or any shop). */
    fun due(run: Run, items: List<Item>): List<Item> {
        val shops = run.shopIds
        return items.filter { it.lineOf == Line.PENDING && !it.offList && (it.shopId == 0L || shops.isEmpty() || it.shopId in shops) }
    }

    fun taken(run: Run, items: List<Item>): List<Item> = items.filter { it.runId == run.id && it.lineOf == Line.TAKEN }

    fun big(run: Run, taken: List<Item>): Boolean = taken.count { !it.offList } >= BIG_LINES || run.shopIds.size > 1

    /**
     * The pay for a run at the moment it's delivered ([at]). [taken] is what's in the bag; [forgotten] how
     * many lines were due and stayed on a shelf that wasn't empty.
     */
    fun pay(run: Run, taken: List<Item>, forgotten: Int, at: Long, weather: Weather, spent: Long): Payslip {
        val ordered = taken.filter { !it.offList }
        val lines = mutableListOf<PayLine>()
        if (ordered.isNotEmpty()) lines += PayLine(PayKind.LINES, ordered.size * PER_LINE, ordered.size)
        val kg = (grams(taken) / 1000.0).roundToInt()
        if (kg > 0) lines += PayLine(PayKind.WEIGHT, kg * PER_KG, kg)
        val rush = ordered.count { it.rush && at <= it.rushBy }
        if (rush > 0) lines += PayLine(PayKind.RUSH, rush * PER_RUSH, rush)
        if (ordered.isNotEmpty() && forgotten == 0) lines += PayLine(PayKind.CLEAN, CLEAN)
        if (run.budget > 0 && spent in 1..run.budget) {
            val saved = (run.budget - spent).toDouble() / run.budget
            lines += PayLine(PayKind.BUDGET, BUDGET + (saved * BUDGET_SAVED).roundToInt().coerceAtMost(BUDGET_SAVED))
        }
        val extra = (taken.count { it.offList } - 1).coerceAtLeast(0)
        if (extra > 0) lines += PayLine(PayKind.UNORDERED, -extra * UNORDERED, extra)
        val base = lines.sumOf { it.coins }.coerceAtLeast(0)
        if (weather.bonus > 0 && base > 0) lines += PayLine(PayKind.WEATHER, (base * weather.bonus / 100.0).roundToInt(), weather.bonus)
        if (big(run, taken) && base > 0) lines += PayLine(PayKind.BIG, (base * 0.5).roundToInt(), 50)
        return Payslip(lines)
    }

    /** Over the money handed out. */
    fun overspent(run: Run) = run.budget > 0 && run.spent > run.budget

    /** The next waybill's number. */
    fun nextNumber(runs: List<Run>): Int = (runs.maxOfOrNull { it.number } ?: 0) + 1

    fun done(runs: List<Run>) = runs.filter { it.legOf == Leg.DONE }

    /** Coins earned, less what the depot and the fleet cost. */
    fun balance(runs: List<Run>, owned: Set<Upgrade>): Int = done(runs).sumOf { it.pay } - owned.sumOf { it.cost }

    /** Minutes left on a rush order, negative once late. */
    fun rushLeft(item: Item, now: Long): Long = (item.rushBy - now) / MINUTE
}

/**
 * The walk through a shop. Each shop learns where things lie from the order they're ticked off in;
 * anything not seen there yet stands where its shelf usually is. The cold box is packed last.
 */
object Route {
    /** How many runs the place remembers; newer runs move it more. */
    private const val MEMORY = 5

    fun pos(item: Item, aisles: Map<String, Aisle>): Float = aisles[item.key]?.pos ?: Shelf.of(item.shelf).pos

    fun order(items: List<Item>, aisles: List<Aisle>, coldLast: Boolean): List<Item> {
        val map = aisles.associateBy { it.key }
        return items.sortedWith(compareBy<Item>({ if (coldLast && it.cold) 1 else 0 }, { pos(it, map) }, { it.addedAt }))
    }

    /** What a run teaches a shop: the lines taken there, in the order they were taken. */
    fun learn(shopId: Long, taken: List<Item>, known: List<Aisle>): List<Aisle> {
        val here = taken.filter { it.takenShop == shopId && it.takenAt > 0 }.sortedBy { it.takenAt }.distinctBy { it.key }
        if (here.size < 2) return emptyList()
        val map = known.associateBy { it.key }
        return here.mapIndexed { i, item ->
            val r = i.toFloat() / (here.size - 1)
            val old = map[item.key]
            if (old == null) Aisle(shopId, item.key, r, 1)
            else {
                val n = (old.n + 1).coerceAtMost(MEMORY)
                Aisle(shopId, item.key, old.pos + (r - old.pos) / n, n)
            }
        }
    }
}

/** The courier's rank, earned by delivered runs. */
enum class Rank(val runs: Int) {
    ERRAND(0), COURIER(5), SENIOR(15), FORWARDER(40), CHIEF_FORWARDER(80), BRANCH(150), DIRECTOR(300);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(runs: Int): Rank = entries.last { runs >= it.runs }
    }
}

/** What the coins buy: the fleet at the door and the depot itself. [fleet] things stand in the street in turn. */
enum class Upgrade(val cost: Int, val fleet: Boolean = false, val needs: Rank = Rank.ERRAND) {
    FLAG(50),
    FLOWERS(80),
    MAILBOX(100),
    LAMP(150),
    SIGN(200),
    CLOCK(250),
    BICYCLE(400, fleet = true),
    PAINT(450),
    PHONE(500, needs = Rank.COURIER),
    DOVECOTE(900, needs = Rank.SENIOR),
    TRICYCLE(1200, fleet = true, needs = Rank.COURIER),
    SCOOTER(2500, fleet = true, needs = Rank.SENIOR),
    SECOND_FLOOR(3000, needs = Rank.FORWARDER),
    VAN(5000, fleet = true, needs = Rank.FORWARDER);

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s }

        /** The best vehicle owned: the one parked at the door. */
        fun ride(owned: Set<Upgrade>): Upgrade? = entries.lastOrNull { it.fleet && it in owned }
    }
}

/** The badges in the personnel file. */
enum class Badge {
    FIRST_RUN, TEN_RUNS, FIFTY_RUNS, HUNDRED_RUNS, CLEAN_FIVE, STORM, RUSH, HEAVY, THRIFTY, BIG, HOLIDAY, HOUSEHOLD, MERCHANDISER, FLEET;
}

data class Stats(
    val runs: Int,
    val lines: Int,
    val grams: Long,
    val clean: Int,
    val rushOnTime: Int,
    val withinBudget: Int,
    /** Money left over from budgets, minor units. */
    val saved: Long,
    val coins: Int,
    val prices: Int,
)

object Badges {
    fun stats(runs: List<Run>, items: List<Item>, prices: Int): Stats {
        val done = Rules.done(runs)
        val ids = done.map { it.id }.toSet()
        val taken = items.filter { it.lineOf == Line.TAKEN && it.runId in ids }
        val byRun = done.associate { it.id to it.endedAt }
        return Stats(
            runs = done.size,
            lines = taken.count { !it.offList },
            grams = taken.sumOf { it.grams.toLong() },
            clean = done.count { it.missed == 0 },
            rushOnTime = taken.count { it.rush && (byRun[it.runId] ?: Long.MAX_VALUE) <= it.rushBy },
            withinBudget = done.count { it.budget > 0 && it.spent in 1..it.budget },
            saved = done.filter { it.budget > 0 && it.spent in 1..it.budget }.sumOf { it.budget - it.spent },
            coins = done.sumOf { it.pay },
            prices = prices,
        )
    }

    fun earned(runs: List<Run>, items: List<Item>, prices: Int, owned: Set<Upgrade>): Set<Badge> {
        val done = Rules.done(runs).sortedBy { it.endedAt }
        val s = stats(runs, items, prices)
        val out = mutableSetOf<Badge>()
        if (s.runs >= 1) out += Badge.FIRST_RUN
        if (s.runs >= 10) out += Badge.TEN_RUNS
        if (s.runs >= 50) out += Badge.FIFTY_RUNS
        if (s.runs >= 100) out += Badge.HUNDRED_RUNS
        if (done.windowed(5).any { w -> w.all { it.missed == 0 } }) out += Badge.CLEAN_FIVE
        if (done.any { it.weatherOf in setOf(Weather.RAIN, Weather.SNOW) }) out += Badge.STORM
        if (s.rushOnTime > 0) out += Badge.RUSH
        val byRun = items.filter { it.lineOf == Line.TAKEN }.groupBy { it.runId }
        if (done.any { r -> Rules.grams(byRun[r.id].orEmpty()) >= 10_000 }) out += Badge.HEAVY
        if (s.withinBudget >= 5) out += Badge.THRIFTY
        if (done.any { r -> Rules.big(r, byRun[r.id].orEmpty()) }) out += Badge.BIG
        if (done.any { r -> Rules.date(r.endedAt).let { it.monthValue == 12 && it.dayOfMonth >= 20 } && Rules.big(r, byRun[r.id].orEmpty()) }) out += Badge.HOLIDAY
        if (done.any { r -> byRun[r.id].orEmpty().filter { !it.offList }.map { it.clientId }.distinct().size >= 3 }) out += Badge.HOUSEHOLD
        if (prices >= 50) out += Badge.MERCHANDISER
        if (Upgrade.VAN in owned) out += Badge.FLEET
        return out
    }
}
