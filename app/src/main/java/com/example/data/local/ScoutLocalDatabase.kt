package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_accounts")
data class LocalAccountEntity(
    @PrimaryKey val accountId: String,
    val fullName: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val jenjang: String,
    val hasSelectedJenjang: Boolean = false,
    val institutionName: String = "",
    val gudepNumber: String = "",
    val unitName: String = "",
    val xp: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_gudep")
data class LocalGudepEntity(
    @PrimaryKey val gudepId: String,
    val ownerId: String,
    val ownerName: String,
    val institutionName: String,
    val gudepNumber: String,
    val address: String,
    val logoBase64: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_materials")
data class LocalMaterialEntity(
    @PrimaryKey val id: String,
    val gudepId: String,
    val ownerId: String,
    val title: String,
    val category: String,
    val jenjangTarget: String,
    val summary: String,
    val content: String,
    val published: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_tasks")
data class LocalTaskEntity(
    @PrimaryKey val id: String,
    val gudepId: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val jenjangTarget: String,
    val dueDateLabel: String,
    val xpReward: Int = 25,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class LocalUserProgressEntity(
    @PrimaryKey val sessionId: String,
    val payloadJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_opened_cache")
data class CachedOpenedItemEntity(
    @PrimaryKey val cacheKey: String,
    val itemType: String, // "MATERIAL" or "TASK"
    val itemId: String,
    val title: String,
    val category: String = "",
    val jenjangTarget: String = "Semua",
    val summary: String = "",
    val body: String = "",
    val dueDateLabel: String = "",
    val xpReward: Int = 0,
    val gudepId: String = "",
    val ownerId: String = "",
    val fromPembina: Boolean = false,
    val openedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val authProvider: String,
    val actionType: String, // "DRIVE_BACKUP", "GUDEP_UPSERT", "MATERIAL_UPSERT", "TASK_UPSERT"
    val payloadJson: String,
    val status: String = "PENDING", // "PENDING", "SYNCED"
    val retryCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncedAtMillis: Long = 0L,
    val lastError: String? = null
)

@Dao
interface LocalAccountDao {
    @Query("SELECT * FROM local_accounts WHERE LOWER(username) = LOWER(:identifier) OR LOWER(email) = LOWER(:identifier) LIMIT 1")
    suspend fun findByUsernameOrEmail(identifier: String): LocalAccountEntity?

    @Query("SELECT * FROM local_accounts WHERE accountId = :accountId LIMIT 1")
    suspend fun findById(accountId: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: LocalAccountEntity)
}

@Dao
interface LocalGudepDao {
    @Query("SELECT * FROM local_gudep")
    fun observeAllGudep(): Flow<List<LocalGudepEntity>>

    @Query("SELECT * FROM local_gudep WHERE gudepId = :gudepId LIMIT 1")
    suspend fun getById(gudepId: String): LocalGudepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(gudep: LocalGudepEntity)
}

@Dao
interface LocalMaterialDao {
    @Query("SELECT * FROM local_materials ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<LocalMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LocalMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalMaterialEntity>)

    @Query("DELETE FROM local_materials WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LocalTaskDao {
    @Query("SELECT * FROM local_tasks ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<LocalTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LocalTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalTaskEntity>)

    @Query("DELETE FROM local_tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LocalUserProgressDao {
    @Query("SELECT * FROM user_progress WHERE sessionId = :sessionId LIMIT 1")
    fun observeBySession(sessionId: String): Flow<LocalUserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySession(sessionId: String): LocalUserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LocalUserProgressEntity)
}

@Dao
interface CachedOpenedItemDao {
    @Query("SELECT * FROM offline_opened_cache ORDER BY openedAtMillis DESC")
    fun observeAll(): Flow<List<CachedOpenedItemEntity>>

    @Query("SELECT * FROM offline_opened_cache WHERE itemType = :itemType ORDER BY openedAtMillis DESC")
    fun observeByType(itemType: String): Flow<List<CachedOpenedItemEntity>>

    @Query("SELECT * FROM offline_opened_cache WHERE itemType = :itemType ORDER BY openedAtMillis DESC")
    suspend fun getByType(itemType: String): List<CachedOpenedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedOpenedItemEntity)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT COUNT(*) FROM sync_outbox WHERE sessionId = :sessionId AND status = 'PENDING'")
    fun observePendingCount(sessionId: String): Flow<Int>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun getPendingItems(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = 'SYNCED', syncedAtMillis = :syncedAt, lastError = NULL WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: String, error: String)

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED' AND syncedAtMillis < :cutoffMillis")
    suspend fun deleteOldSynced(cutoffMillis: Long)
}

@Database(
    entities = [
        LocalAccountEntity::class,
        LocalGudepEntity::class,
        LocalMaterialEntity::class,
        LocalTaskEntity::class,
        LocalUserProgressEntity::class,
        CachedOpenedItemEntity::class,
        SyncOutboxEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ScoutLocalDatabase : RoomDatabase() {
    abstract fun accountDao(): LocalAccountDao
    abstract fun gudepDao(): LocalGudepDao
    abstract fun materialDao(): LocalMaterialDao
    abstract fun taskDao(): LocalTaskDao
    abstract fun progressDao(): LocalUserProgressDao
    abstract fun cachedOpenedItemDao(): CachedOpenedItemDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: ScoutLocalDatabase? = null

        fun getInstance(context: Context): ScoutLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScoutLocalDatabase::class.java,
                    "my_scout_local.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
