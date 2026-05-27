package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM match_sessions ORDER BY dateTime DESC")
    fun getAllSessionsFlow(): Flow<List<MatchSession>>

    @Query("SELECT * FROM match_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): MatchSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MatchSession): Long

    @Update
    suspend fun updateSession(session: MatchSession)

    @Delete
    suspend fun deleteSession(session: MatchSession)
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM delivery_clips WHERE sessionId = :sessionId ORDER BY overNumber ASC, ballNumber ASC")
    fun getClipsForSessionFlow(sessionId: Int): Flow<List<DeliveryClip>>

    @Query("SELECT * FROM delivery_clips WHERE sessionId = :sessionId ORDER BY overNumber ASC, ballNumber ASC")
    suspend fun getClipsForSessionDirect(sessionId: Int): List<DeliveryClip>

    @Query("SELECT * FROM delivery_clips WHERE id = :clipId")
    suspend fun getClipById(clipId: Int): DeliveryClip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: DeliveryClip): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<DeliveryClip>)

    @Update
    suspend fun updateClip(clip: DeliveryClip)

    @Delete
    suspend fun deleteClip(clip: DeliveryClip)

    @Query("DELETE FROM delivery_clips WHERE sessionId = :sessionId")
    suspend fun deleteClipsForSession(sessionId: Int)
}

@Database(entities = [MatchSession::class, DeliveryClip::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cricket_segmenter_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
