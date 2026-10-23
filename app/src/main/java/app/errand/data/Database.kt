package app.errand.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Where a line of the waybill stands. */
enum class Line {
    /** On the waybill, waiting for a run. */
    PENDING,

    /** In the bag: taken from the shelf on a run. */
    TAKEN;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: PENDING
    }
}

/** The weather the run was made in: the courier's bonus. Picked by hand at the door, there's no internet. */
enum class Weather(val bonus: Int) {
    FAIR(0), WIND(20), RAIN(50), SNOW(50), FROST(30), HEAT(30);

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: FAIR
    }
}

/** How far a run got. */
enum class Leg {
    /** Out with the bag. */
    OUT,

    /** Delivered and paid. */
    DONE;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OUT
    }
}

/** Someone at home who places orders: "me" is always there. [mark] picks the stamp colour. */
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mark: Int = 0,
    val me: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
)

/** A warehouse: a shop the courier goes to. Each one learns its own shelf order. */
@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * One line of the waybill. [key] is the name folded for learning (shelf places, prices).
 * [grams] and [cold] come from the goods catalogue and can be changed. [shopId] 0 means any shop.
 * A rush order ([rushBy] > 0) pays extra if it's home by then. [outOn] is the run on which the shelf
 * was empty; [missed] counts the runs it was forgotten on, [carried] all the runs it rode over.
 * [offList] lines were picked up on the run without an order; the first of a run is the courier's treat.
 */
@Entity(tableName = "items", indices = [Index("state"), Index("runId")])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val key: String,
    val qty: String = "",
    val note: String = "",
    val clientId: Long = 0,
    val grams: Int = 500,
    val cold: Boolean = false,
    val shelf: String = "",
    val shopId: Long = 0,
    val rushBy: Long = 0,
    val state: String = Line.PENDING.name,
    val runId: Long = 0,
    val takenShop: Long = 0,
    val takenAt: Long = 0,
    /** What it cost, in minor units, if it was noted. */
    val price: Long = 0,
    val offList: Boolean = false,
    val outOn: Long = 0,
    val missed: Int = 0,
    val carried: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
) {
    val lineOf: Line get() = Line.of(state)
    val rush: Boolean get() = rushBy > 0
}

/** A run: out of the door with the waybill, round the shops, home again. Pay is fixed when it's delivered. */
@Entity(tableName = "runs")
data class Run(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0,
    val leg: String = Leg.OUT.name,
    /** Money handed out for the run, minor units (0 = none). */
    val budget: Long = 0,
    /** The receipt total, minor units (0 = not written). */
    val spent: Long = 0,
    val weather: String = Weather.FAIR.name,
    /** Shops planned for the run, comma-separated ids (empty = any shop). */
    val shops: String = "",
    val pay: Int = 0,
    /** Lines due on this run that stayed on the shelf, counted at delivery. */
    val missed: Int = 0,
    val out: Int = 0,
) {
    val legOf: Leg get() = Leg.of(leg)
    val weatherOf: Weather get() = Weather.of(weather)
    val shopIds: List<Long> get() = shops.split(',').mapNotNull { it.trim().toLongOrNull() }
}

/** Where a thing has lain in a shop, 0 at the door to 1 at the tills, averaged over the last few runs. */
@Entity(tableName = "aisles", primaryKeys = ["shopId", "key"])
data class Aisle(val shopId: Long, val key: String, val pos: Float, val n: Int)

/** A price seen: the merchandiser's notebook. */
@Entity(tableName = "prices", indices = [Index("key")])
data class Price(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val name: String,
    val shopId: Long,
    val price: Long,
    val at: Long = System.currentTimeMillis(),
)

@Dao
interface ErrandDao {
    @Query("SELECT * FROM items ORDER BY addedAt, id")
    fun observeItems(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY addedAt, id")
    suspend fun items(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun item(id: Long): Item?

    @Insert
    suspend fun insert(i: Item): Long

    @Update
    suspend fun update(i: Item)

    @Update
    suspend fun updateItems(i: List<Item>)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM runs ORDER BY startedAt")
    fun observeRuns(): Flow<List<Run>>

    @Query("SELECT * FROM runs ORDER BY startedAt")
    suspend fun runs(): List<Run>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun run(id: Long): Run?

    @Insert
    suspend fun insert(r: Run): Long

    @Update
    suspend fun update(r: Run)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRun(id: Long)

    @Query("SELECT * FROM clients ORDER BY addedAt, id")
    fun observeClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY addedAt, id")
    suspend fun clients(): List<Client>

    @Insert
    suspend fun insert(c: Client): Long

    @Update
    suspend fun update(c: Client)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClient(id: Long)

    @Query("SELECT * FROM shops ORDER BY addedAt, id")
    fun observeShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shops ORDER BY addedAt, id")
    suspend fun shops(): List<Shop>

    @Insert
    suspend fun insert(s: Shop): Long

    @Update
    suspend fun update(s: Shop)

    @Query("DELETE FROM shops WHERE id = :id")
    suspend fun deleteShop(id: Long)

    @Query("SELECT * FROM aisles")
    fun observeAisles(): Flow<List<Aisle>>

    @Query("SELECT * FROM aisles WHERE shopId = :shopId")
    suspend fun aisles(shopId: Long): List<Aisle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(a: List<Aisle>)

    @Query("DELETE FROM aisles WHERE shopId = :shopId")
    suspend fun deleteAisles(shopId: Long)

    @Query("SELECT * FROM prices ORDER BY at")
    fun observePrices(): Flow<List<Price>>

    @Query("SELECT * FROM prices ORDER BY at")
    suspend fun prices(): List<Price>

    @Insert
    suspend fun insert(p: Price): Long

    @Query("DELETE FROM prices WHERE id = :id")
    suspend fun deletePrice(id: Long)

    @Query("UPDATE items SET clientId = :to WHERE clientId = :from")
    suspend fun moveClient(from: Long, to: Long)

    @Query("UPDATE items SET shopId = 0 WHERE shopId = :shopId")
    suspend fun forgetShop(shopId: Long)
}

@Database(entities = [Client::class, Shop::class, Item::class, Run::class, Aisle::class, Price::class], version = 1, exportSchema = true)
abstract class ErrandDatabase : RoomDatabase() {
    abstract fun dao(): ErrandDao

    companion object {
        fun create(context: Context): ErrandDatabase =
            Room.databaseBuilder(context.applicationContext, ErrandDatabase::class.java, "errand.db").build()
    }
}
