package app.errand.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.errand.domain.Upgrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val currency: Currency = Currency.RUB,
    /** What the courier carries comfortably, in kilos: a heavier waybill gets a warning. */
    val carryKg: Int = 7,
    /** The cold box goes in last, whatever the shop's order. */
    val coldLast: Boolean = true,
    /** Ask for the price when a line is ticked off. */
    val notePrices: Boolean = true,
    /** Bought in the depot. */
    val owned: Set<Upgrade> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        currency = Currency.of(this[CURRENCY]),
        carryKg = this[CARRY] ?: 7,
        coldLast = this[COLD_LAST] ?: true,
        notePrices = this[NOTE_PRICES] ?: true,
        owned = this[OWNED].orEmpty().mapNotNull(Upgrade::of).toSet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[CURRENCY] = s.currency.name
            it[CARRY] = s.carryKg
            it[COLD_LAST] = s.coldLast
            it[NOTE_PRICES] = s.notePrices
            it[OWNED] = s.owned.map { u -> u.name }.toSet()
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CURRENCY = stringPreferencesKey("currency")
        val CARRY = intPreferencesKey("carry_kg")
        val COLD_LAST = booleanPreferencesKey("cold_last")
        val NOTE_PRICES = booleanPreferencesKey("note_prices")
        val OWNED = stringSetPreferencesKey("owned")
    }
}
