package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.LocalGudepEntity
import com.example.data.local.LocalMaterialEntity
import com.example.data.local.LocalTaskEntity
import com.example.data.local.ScoutLocalDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.SyncOutboxEntity
import com.example.data.network.NetworkMonitor
import com.example.data.remote.DriveSyncResult
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.GoogleDriveSyncService
import com.example.model.AuthProviderType
import com.example.model.GudepProfile
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SyncOutboxAction {
    const val DRIVE_BACKUP = "DRIVE_BACKUP"
    const val GUDEP_UPSERT = "GUDEP_UPSERT"
    const val MATERIAL_UPSERT = "MATERIAL_UPSERT"
    const val TASK_UPSERT = "TASK_UPSERT"
}

data class OutboxDrainSummary(
    val processedCount: Int = 0,
    val remainingPendingCount: Int = 0,
    val anyFailed: Boolean = false
)

/**
 * Worker latar belakang Android (padanan Service Worker Background Sync) yang mengosongkan
 * antrean sinkronisasi ke Google Drive & Cloud Firestore otomatis begitu koneksi internet aktif.
 */
class DriveAndCloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val networkMonitor = NetworkMonitor(applicationContext)
        val isOnline = networkMonitor.checkCurrentlyOnline()
        networkMonitor.unregister()

        if (!isOnline) {
            return Result.retry()
        }

        val db = ScoutLocalDatabase.getInstance(applicationContext)
        val processor = OutboxSyncProcessor(
            db = db,
            sessionPrefs = SessionPreferences(applicationContext),
            driveSync = GoogleDriveSyncService(applicationContext),
            firestoreSync = FirestoreSyncRepository(applicationContext)
        )

        val summary = processor.drainPendingQueue()
        return if (summary.anyFailed || summary.remainingPendingCount > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE_SYNC_WORK_NAME = "my_scout_drive_auto_sync"

        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DriveAndCloudSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(UNIQUE_SYNC_WORK_NAME)
                .build()

            try {
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_SYNC_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                Log.w("DriveSyncWorker", "Tidak dapat menjadwalkan WorkManager: ${e.message}")
            }
        }
    }
}

class OutboxSyncProcessor(
    private val db: ScoutLocalDatabase,
    private val sessionPrefs: SessionPreferences,
    private val driveSync: GoogleDriveSyncService,
    private val firestoreSync: FirestoreSyncRepository,
    private val isFirebaseSignedInProvider: () -> Boolean = {
        try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
    }
) {
    private val outboxDao = db.syncOutboxDao()

    suspend fun drainPendingQueue(): OutboxDrainSummary {
        val pending = outboxDao.getPendingItems()
        if (pending.isEmpty()) {
            return OutboxDrainSummary(processedCount = 0, remainingPendingCount = 0, anyFailed = false)
        }

        var processed = 0
        var anyFailed = false

        for (item in pending) {
            val ok = runCatching { processSingleItem(item) }.getOrElse { err ->
                outboxDao.incrementRetry(item.id, err.message ?: "Gagal sinkronisasi")
                false
            }
            if (ok) {
                val now = System.currentTimeMillis()
                outboxDao.markSynced(item.id, now)
                processed++
            } else {
                anyFailed = true
            }
        }

        // Clean up synced outbox entries older than 7 days
        outboxDao.deleteOldSynced(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
        val remaining = outboxDao.getPendingItems().size
        return OutboxDrainSummary(
            processedCount = processed,
            remainingPendingCount = remaining,
            anyFailed = anyFailed
        )
    }

    private suspend fun processSingleItem(item: SyncOutboxEntity): Boolean {
        val isGoogleProvider = item.authProvider == AuthProviderType.GOOGLE.name
        val firebaseReady = isFirebaseSignedInProvider()

        return when (item.actionType) {
            SyncOutboxAction.DRIVE_BACKUP -> {
                val (parsedSession, parsedProgress) = driveSync.deserializeBackupJson(item.payloadJson)
                if (parsedSession == null || parsedProgress == null) {
                    return true
                }
                val sessionWithProvider = parsedSession.copy(
                    authProvider = if (isGoogleProvider) AuthProviderType.GOOGLE else AuthProviderType.MANUAL
                )
                val driveResult = driveSync.syncBackupSilently(
                    session = sessionWithProvider,
                    progress = parsedProgress,
                    oauthAccessToken = null
                )
                val ts = when (driveResult) {
                    is DriveSyncResult.CloudSynced -> driveResult.timestampMillis
                    is DriveSyncResult.LocalMirrorSynced -> driveResult.timestampMillis
                }
                if (isGoogleProvider) {
                    if (!firebaseReady) {
                        outboxDao.incrementRetry(item.id, "Menunggu sesi Google aktif untuk sinkronisasi cloud.")
                        return false
                    }
                    val profileRes = firestoreSync.upsertUserProfile(sessionWithProvider)
                    val backupRes = firestoreSync.syncBackupJson(
                        driveSync.serializeBackupJson(sessionWithProvider, parsedProgress, ts)
                    )
                    if (profileRes.isFailure || backupRes.isFailure) {
                        val errMsg = profileRes.exceptionOrNull()?.message
                            ?: backupRes.exceptionOrNull()?.message
                            ?: "Gagal mengunggah cadangan ke cloud."
                        outboxDao.incrementRetry(item.id, errMsg)
                        return false
                    }
                }
                sessionPrefs.updateLastSynced(ts)
                true
            }

            SyncOutboxAction.GUDEP_UPSERT -> {
                val gudep = decodeGudepJson(item.payloadJson) ?: return true
                if (isGoogleProvider) {
                    if (!firebaseReady) {
                        outboxDao.incrementRetry(item.id, "Menunggu sesi Google aktif.")
                        return false
                    }
                    val res = firestoreSync.upsertGudep(gudep)
                    if (res.isFailure) {
                        outboxDao.incrementRetry(item.id, res.exceptionOrNull()?.message ?: "Gagal sinkron Gudep.")
                        return false
                    }
                    val synced = res.getOrThrow()
                    db.gudepDao().upsert(
                        LocalGudepEntity(
                            gudepId = synced.gudepId,
                            ownerId = synced.ownerId,
                            ownerName = synced.ownerName,
                            institutionName = synced.institutionName,
                            gudepNumber = synced.gudepNumber,
                            address = synced.address,
                            logoBase64 = synced.logoBase64,
                            updatedAtMillis = synced.updatedAtMillis
                        )
                    )
                }
                true
            }

            SyncOutboxAction.MATERIAL_UPSERT -> {
                val material = decodeMaterialJson(item.payloadJson) ?: return true
                if (isGoogleProvider) {
                    if (!firebaseReady) {
                        outboxDao.incrementRetry(item.id, "Menunggu sesi Google aktif.")
                        return false
                    }
                    val res = firestoreSync.saveGudepMaterial(material)
                    if (res.isFailure) {
                        outboxDao.incrementRetry(item.id, res.exceptionOrNull()?.message ?: "Gagal sinkron materi.")
                        return false
                    }
                    val synced = res.getOrThrow()
                    db.materialDao().upsert(
                        LocalMaterialEntity(
                            id = synced.id,
                            gudepId = synced.gudepId,
                            ownerId = synced.ownerId,
                            title = synced.title,
                            category = synced.category,
                            jenjangTarget = synced.jenjangTarget,
                            summary = synced.summary,
                            content = synced.content,
                            published = synced.published,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                true
            }

            SyncOutboxAction.TASK_UPSERT -> {
                val task = decodeTaskJson(item.payloadJson) ?: return true
                if (isGoogleProvider) {
                    if (!firebaseReady) {
                        outboxDao.incrementRetry(item.id, "Menunggu sesi Google aktif.")
                        return false
                    }
                    val res = firestoreSync.saveGudepTask(task)
                    if (res.isFailure) {
                        outboxDao.incrementRetry(item.id, res.exceptionOrNull()?.message ?: "Gagal sinkron tugas.")
                        return false
                    }
                    val synced = res.getOrThrow()
                    db.taskDao().upsert(
                        LocalTaskEntity(
                            id = synced.id,
                            gudepId = synced.gudepId,
                            ownerId = synced.ownerId,
                            title = synced.title,
                            description = synced.description,
                            jenjangTarget = synced.jenjangTarget,
                            dueDateLabel = synced.dueDateLabel,
                            xpReward = synced.xpReward,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                true
            }

            else -> true
        }
    }

    companion object {
        fun encodeGudepJson(g: GudepProfile): String = JSONObject().apply {
            put("gudepId", g.gudepId)
            put("ownerId", g.ownerId)
            put("ownerName", g.ownerName)
            put("institutionName", g.institutionName)
            put("gudepNumber", g.gudepNumber)
            put("address", g.address)
            if (g.logoBase64 != null) put("logoBase64", g.logoBase64)
            put("updatedAtMillis", g.updatedAtMillis)
        }.toString()

        fun decodeGudepJson(raw: String): GudepProfile? = runCatching {
            val o = JSONObject(raw)
            GudepProfile(
                gudepId = o.optString("gudepId"),
                ownerId = o.optString("ownerId"),
                ownerName = o.optString("ownerName"),
                institutionName = o.optString("institutionName"),
                gudepNumber = o.optString("gudepNumber"),
                address = o.optString("address"),
                logoBase64 = if (o.has("logoBase64") && !o.isNull("logoBase64")) o.optString("logoBase64") else null,
                updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        }.getOrNull()

        fun encodeMaterialJson(m: ScoutMaterial): String = JSONObject().apply {
            put("id", m.id)
            put("title", m.title)
            put("category", m.category)
            put("jenjangTarget", m.jenjangTarget)
            put("summary", m.summary)
            put("content", m.content)
            put("fromPembina", m.fromPembina)
            put("published", m.published)
            put("gudepId", m.gudepId)
            put("ownerId", m.ownerId)
            put("readMinutes", m.readMinutes)
        }.toString()

        fun decodeMaterialJson(raw: String): ScoutMaterial? = runCatching {
            val o = JSONObject(raw)
            ScoutMaterial(
                id = o.optString("id"),
                title = o.optString("title"),
                category = o.optString("category"),
                jenjangTarget = o.optString("jenjangTarget"),
                summary = o.optString("summary"),
                content = o.optString("content"),
                fromPembina = o.optBoolean("fromPembina", true),
                published = o.optBoolean("published", true),
                gudepId = o.optString("gudepId"),
                ownerId = o.optString("ownerId"),
                readMinutes = o.optInt("readMinutes", 4)
            )
        }.getOrNull()

        fun encodeTaskJson(t: ScoutTask): String = JSONObject().apply {
            put("id", t.id)
            put("title", t.title)
            put("description", t.description)
            put("jenjangTarget", t.jenjangTarget)
            put("dueDateLabel", t.dueDateLabel)
            put("xpReward", t.xpReward)
            put("gudepId", t.gudepId)
            put("ownerId", t.ownerId)
        }.toString()

        fun decodeTaskJson(raw: String): ScoutTask? = runCatching {
            val o = JSONObject(raw)
            ScoutTask(
                id = o.optString("id"),
                title = o.optString("title"),
                description = o.optString("description"),
                jenjangTarget = o.optString("jenjangTarget"),
                dueDateLabel = o.optString("dueDateLabel"),
                xpReward = o.optInt("xpReward", 25),
                gudepId = o.optString("gudepId"),
                ownerId = o.optString("ownerId")
            )
        }.getOrNull()
    }
}
