package app.errand.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.errand.R
import app.errand.data.Weather
import app.errand.domain.Badge
import app.errand.domain.PayKind
import app.errand.domain.PayLine
import app.errand.domain.Rank
import app.errand.domain.Shelf
import app.errand.domain.Upgrade
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Words and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]
    private fun zone() = ZoneId.systemDefault()

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "24 October", or with the year when it isn't this one. */
    fun date(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        return d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(c)))
    }

    /** "23.10.26", typed on a waybill. */
    fun typed(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yy"))

    fun time(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

    /** "today", "yesterday", "5 days ago", or the date. */
    fun ago(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        val days = ChronoUnit.DAYS.between(d, today).toInt()
        return when {
            days <= 0 -> c.getString(R.string.today)
            days == 1 -> c.getString(R.string.yesterday)
            days < 14 -> plural(c, R.plurals.days_ago, days)
            else -> date(c, at, today)
        }
    }

    /** "October 2026", standing alone. */
    fun month(c: Context, year: Int, month: Int): String {
        val name = YearMonth.of(year, month).month.getDisplayName(TextStyle.FULL_STANDALONE, locale(c)).replaceFirstChar { it.uppercase(locale(c)) }
        return if (year == LocalDate.now().year) name else "$name $year"
    }

    /** "1 h 20 min", "25 min". */
    fun duration(c: Context, ms: Long): String {
        val m = (ms / 60_000).coerceAtLeast(0)
        return if (m < 60) c.getString(R.string.dur_min, m) else c.getString(R.string.dur_h_min, m / 60, m % 60)
    }

    fun coins(c: Context, n: Int) = plural(c, R.plurals.coins, n)

    fun lines(c: Context, n: Int) = plural(c, R.plurals.lines, n)

    fun runs(c: Context, n: Int) = plural(c, R.plurals.runs, n)

    fun weather(c: Context, w: Weather): String = c.getString(
        when (w) {
            Weather.FAIR -> R.string.w_fair
            Weather.WIND -> R.string.w_wind
            Weather.RAIN -> R.string.w_rain
            Weather.SNOW -> R.string.w_snow
            Weather.FROST -> R.string.w_frost
            Weather.HEAT -> R.string.w_heat
        },
    )

    fun pay(c: Context, l: PayLine): String = when (l.kind) {
        PayKind.LINES -> c.getString(R.string.pay_lines, l.n)
        PayKind.WEIGHT -> c.getString(R.string.pay_weight, l.n)
        PayKind.RUSH -> c.getString(R.string.pay_rush, l.n)
        PayKind.CLEAN -> c.getString(R.string.pay_clean)
        PayKind.BUDGET -> c.getString(R.string.pay_budget)
        PayKind.UNORDERED -> c.getString(R.string.pay_unordered, l.n)
        PayKind.WEATHER -> c.getString(R.string.pay_weather, l.n)
        PayKind.BIG -> c.getString(R.string.pay_big)
    }

    fun rank(c: Context, r: Rank): String = c.getString(
        when (r) {
            Rank.ERRAND -> R.string.rank_errand
            Rank.COURIER -> R.string.rank_courier
            Rank.SENIOR -> R.string.rank_senior
            Rank.FORWARDER -> R.string.rank_forwarder
            Rank.CHIEF_FORWARDER -> R.string.rank_chief
            Rank.BRANCH -> R.string.rank_branch
            Rank.DIRECTOR -> R.string.rank_director
        },
    )

    fun upgrade(c: Context, u: Upgrade): Pair<String, String> = when (u) {
        Upgrade.FLAG -> R.string.u_flag to R.string.u_flag_sub
        Upgrade.MAILBOX -> R.string.u_mailbox to R.string.u_mailbox_sub
        Upgrade.FLOWERS -> R.string.u_flowers to R.string.u_flowers_sub
        Upgrade.LAMP -> R.string.u_lamp to R.string.u_lamp_sub
        Upgrade.SIGN -> R.string.u_sign to R.string.u_sign_sub
        Upgrade.CLOCK -> R.string.u_clock to R.string.u_clock_sub
        Upgrade.BICYCLE -> R.string.u_bicycle to R.string.u_bicycle_sub
        Upgrade.PAINT -> R.string.u_paint to R.string.u_paint_sub
        Upgrade.PHONE -> R.string.u_phone to R.string.u_phone_sub
        Upgrade.TRICYCLE -> R.string.u_tricycle to R.string.u_tricycle_sub
        Upgrade.DOVECOTE -> R.string.u_dovecote to R.string.u_dovecote_sub
        Upgrade.SCOOTER -> R.string.u_scooter to R.string.u_scooter_sub
        Upgrade.SECOND_FLOOR -> R.string.u_floor to R.string.u_floor_sub
        Upgrade.VAN -> R.string.u_van to R.string.u_van_sub
    }.let { (a, b) -> c.getString(a) to c.getString(b) }

    fun badge(c: Context, b: Badge): Pair<String, String> = when (b) {
        Badge.FIRST_RUN -> R.string.b_first to R.string.b_first_sub
        Badge.TEN_RUNS -> R.string.b_ten to R.string.b_ten_sub
        Badge.FIFTY_RUNS -> R.string.b_fifty to R.string.b_fifty_sub
        Badge.HUNDRED_RUNS -> R.string.b_hundred to R.string.b_hundred_sub
        Badge.CLEAN_FIVE -> R.string.b_clean to R.string.b_clean_sub
        Badge.STORM -> R.string.b_storm to R.string.b_storm_sub
        Badge.RUSH -> R.string.b_rush to R.string.b_rush_sub
        Badge.HEAVY -> R.string.b_heavy to R.string.b_heavy_sub
        Badge.THRIFTY -> R.string.b_thrifty to R.string.b_thrifty_sub
        Badge.BIG -> R.string.b_big to R.string.b_big_sub
        Badge.HOLIDAY -> R.string.b_holiday to R.string.b_holiday_sub
        Badge.HOUSEHOLD -> R.string.b_household to R.string.b_household_sub
        Badge.MERCHANDISER -> R.string.b_merch to R.string.b_merch_sub
        Badge.FLEET -> R.string.b_fleet to R.string.b_fleet_sub
    }.let { (a, b) -> c.getString(a) to c.getString(b) }

    fun shelf(c: Context, s: Shelf): String = c.getString(
        when (s) {
            Shelf.PRODUCE -> R.string.sh_produce
            Shelf.BREAD -> R.string.sh_bread
            Shelf.GROCERY -> R.string.sh_grocery
            Shelf.CANS -> R.string.sh_cans
            Shelf.SWEETS -> R.string.sh_sweets
            Shelf.DRINKS -> R.string.sh_drinks
            Shelf.HOUSEHOLD -> R.string.sh_household
            Shelf.HYGIENE -> R.string.sh_hygiene
            Shelf.PHARMACY -> R.string.sh_pharmacy
            Shelf.PETS -> R.string.sh_pets
            Shelf.OTHER -> R.string.sh_other
            Shelf.MEAT -> R.string.sh_meat
            Shelf.DAIRY -> R.string.sh_dairy
            Shelf.FROZEN -> R.string.sh_frozen
        },
    )
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
