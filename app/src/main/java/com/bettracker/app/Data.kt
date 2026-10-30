package com.bettracker.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class Market(val label: String) {
    OVER_UNDER("Over/Under"), BTTS("BTTS"), CORNERS("ركنيات"), CARDS("بطاقات"), OTHER("أخرى")
}

enum class Status(val label: String) {
    PENDING("معلّق"), WON("ناجح"), LOST("فاشل"), VOID("ملغي")
}

enum class Mode(val label: String) { PRE("Pre-match"), LIVE("Live") }

@Entity(tableName = "bets")
data class Bet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchName: String,
    val selection: String,
    val market: String,
    val odds: Double,
    val stake: Double,
    val mode: String,
    val status: String = Status.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
)

val Bet.profit: Double
    get() = when (status) {
        "WON" -> stake * (odds - 1)
        "LOST" -> -stake
        else -> 0.0
    }

val Bet.settled: Boolean get() = status == "WON" || status == "LOST"

data class Summary(val won: Int, val lost: Int, val profit: Double, val staked: Double) {
    val total get() = won + lost
    val winRate get() = if (total == 0) 0.0 else won * 100.0 / total
    val roi get() = if (staked == 0.0) 0.0 else profit * 100.0 / staked
}

fun summarize(list: List<Bet>): Summary {
    val s = list.filter { it.settled }
    return Summary(
        s.count { it.status == "WON" },
        s.count { it.status == "LOST" },
        s.sumOf { it.profit },
        s.sumOf { it.stake }
    )
}

@Dao
interface BetDao {
    @Query("SELECT * FROM bets ORDER BY createdAt DESC")
    fun all(): Flow<List<Bet>>

    @Insert suspend fun insert(bet: Bet)
    @Update suspend fun update(bet: Bet)
    @Delete suspend fun delete(bet: Bet)
}

@Database(entities = [Bet::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): BetDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(c: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "bets.db")
                .build().also { instance = it }
        }
    }
}
