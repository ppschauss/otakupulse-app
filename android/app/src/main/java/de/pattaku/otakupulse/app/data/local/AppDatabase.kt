package de.pattaku.otakupulse.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist_entry ORDER BY addedAt DESC")
    fun all(): Flow<List<WatchlistEntry>>

    @Query("SELECT * FROM watchlist_entry WHERE status = :status ORDER BY addedAt DESC")
    fun byStatus(status: WatchStatus): Flow<List<WatchlistEntry>>

    @Query("SELECT * FROM watchlist_entry WHERE animeId = :animeId")
    suspend fun find(animeId: Int): WatchlistEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntry)

    @Query("UPDATE watchlist_entry SET progress = :progress, hasUnseenEpisode = 0 WHERE animeId = :animeId")
    suspend fun setProgress(animeId: Int, progress: Int)

    @Query("UPDATE watchlist_entry SET status = :status WHERE animeId = :animeId")
    suspend fun setStatus(animeId: Int, status: WatchStatus)

    @Query("UPDATE watchlist_entry SET hasUnseenEpisode = 1 WHERE animeId IN (:animeIds)")
    suspend fun markUnseen(animeIds: List<Int>)

    @Query("SELECT animeId FROM watchlist_entry")
    suspend fun allIds(): List<Int>

    @Query("DELETE FROM watchlist_entry WHERE animeId = :animeId")
    suspend fun remove(animeId: Int)
}

@Dao
interface PendingSwipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(swipe: PendingSwipe)

    @Query("SELECT * FROM pending_swipe ORDER BY createdAt LIMIT :limit")
    suspend fun oldest(limit: Int = 100): List<PendingSwipe>

    @Query("DELETE FROM pending_swipe WHERE animeId IN (:animeIds)")
    suspend fun remove(animeIds: List<Int>)

    @Query("SELECT COUNT(*) FROM pending_swipe")
    suspend fun count(): Int
}

class Converters {
    @TypeConverter
    fun toStatus(value: String): WatchStatus = WatchStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: WatchStatus): String = status.name
}

@Database(
    entities = [WatchlistEntry::class, PendingSwipe::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchlist(): WatchlistDao
    abstract fun pendingSwipes(): PendingSwipeDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "companion.db").build()
    }
}
