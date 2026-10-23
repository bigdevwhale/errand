package app.errand.data

/** The office's currency. Prices are kept in minor units (kopecks, cents) because a carton of milk costs 89.90. */
enum class Currency(val symbol: String, val before: Boolean, val cents: Boolean) {
    RUB("₽", false, true),
    USD("$", true, true),
    EUR("€", true, true),
    GBP("£", true, true),
    KZT("₸", false, false),
    UAH("₴", false, true);

    /** "1 249,90 ₽", "$12.49"; whole amounts without the zeros. */
    fun format(minor: Long): String {
        val whole = minor / 100
        val rest = (minor % 100).toInt()
        val sep = if (before) "." else ","
        val tail = if (cents && rest != 0) sep + rest.toString().padStart(2, '0') else ""
        val n = digits(whole) + tail
        return if (before) "$symbol$n" else "$n $symbol"
    }

    /** "1 249", grouped by thousands, without the sign. */
    fun digits(amount: Long): String {
        val s = kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(" ").reversed()
        return if (amount < 0) "−$s" else s
    }

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: RUB

        /** "89,90", "89.9", "1 249" → minor units; null if it isn't a sum. */
        fun parse(text: String): Long? {
            val t = text.replace(" ", "").replace(" ", "").replace(',', '.').trim()
            if (t.isEmpty()) return null
            val d = t.toBigDecimalOrNull() ?: return null
            if (d.signum() < 0) return null
            return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        }

        /** Minor units back to what was typed: "89,90" / "89.9" style, "1249" for whole sums. */
        fun field(minor: Long, ru: Boolean): String {
            if (minor <= 0) return ""
            val rest = minor % 100
            return if (rest == 0L) (minor / 100).toString() else "${minor / 100}${if (ru) "," else "."}${rest.toString().padStart(2, '0')}"
        }
    }
}
