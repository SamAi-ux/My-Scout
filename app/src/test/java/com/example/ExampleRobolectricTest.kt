package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.local.OfflineContentCacheStore
import com.example.data.local.ScoutLocalDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.SyncOutboxEntity
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.GoogleDriveSyncService
import com.example.data.remote.UserProgressPayload
import com.example.data.sync.DriveAndCloudSyncWorker
import com.example.data.sync.OutboxSyncProcessor
import com.example.data.sync.SyncOutboxAction
import com.example.model.AuthProviderType
import com.example.model.JenjangPramuka
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.example.model.UserSession
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My Scout", appName)
    }

    @Test
    fun `offlineCacheStore persists opened materials and tasks to Room and disk`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ScoutLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val store = OfflineContentCacheStore(context, db.cachedOpenedItemDao())
            val material = ScoutMaterial(
                id = "mat_pionering_01",
                title = "Simpul Pangkal & Ikatan Palang",
                category = "Pionering",
                jenjangTarget = "Penggalang",
                summary = "Teknik dasar membangun menara kaki tiga.",
                content = "Gunakan simpul pangkal untuk memulai dan mengakhiri ikatan.",
                fromPembina = true,
                gudepId = "01_001"
            )
            val task = ScoutTask(
                id = "task_tali_01",
                title = "Buat Simpul Jangkar",
                description = "Praktikkan simpul jangkar bersama regu.",
                jenjangTarget = "Penggalang",
                dueDateLabel = "Sabtu Depan",
                xpReward = 40,
                gudepId = "01_001"
            )

            store.cacheOpenedMaterial(material)
            store.cacheOpenedTask(task)

            val cachedMats = store.openedMaterialsFlow.first()
            val cachedTasks = store.openedTasksFlow.first()
            assertEquals(1, cachedMats.size)
            assertEquals("mat_pionering_01", cachedMats.first().id)
            assertEquals(1, cachedTasks.size)
            assertEquals("task_tali_01", cachedTasks.first().id)
            assertEquals(2, store.totalCachedCountFlow.first())

            // Verify disk JSON fallback can hydrate a fresh in-memory DB
            val freshDb = Room.inMemoryDatabaseBuilder(context, ScoutLocalDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val freshStore = OfflineContentCacheStore(context, freshDb.cachedOpenedItemDao())
                freshStore.hydrateFromDiskCacheIfNeeded()
                assertEquals(2, freshStore.totalCachedCountFlow.first())
            } finally {
                freshDb.close()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `outboxSyncProcessor drains queued Drive backup and WorkManager requires network`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val db = Room.inMemoryDatabaseBuilder(context, ScoutLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val driveSync = GoogleDriveSyncService(context)
            val session = UserSession(
                sessionId = "manual_test_1",
                authProvider = AuthProviderType.MANUAL,
                fullName = "Budi Pramuka",
                username = "budi_scout",
                jenjang = JenjangPramuka.PENGGALANG,
                xp = 120
            )
            val backupJson = driveSync.serializeBackupJson(session, UserProgressPayload(readMaterialIds = setOf("mat_1")))

            db.syncOutboxDao().upsert(
                SyncOutboxEntity(
                    id = "backup_${session.sessionId}",
                    sessionId = session.sessionId,
                    authProvider = AuthProviderType.MANUAL.name,
                    actionType = SyncOutboxAction.DRIVE_BACKUP,
                    payloadJson = backupJson,
                    status = "PENDING"
                )
            )
            assertEquals(1, db.syncOutboxDao().observePendingCount(session.sessionId).first())

            // Enqueue WorkManager job and verify NetworkType.CONNECTED constraint is attached
            DriveAndCloudSyncWorker.enqueueSync(context)
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("my_scout_drive_auto_sync")
                .get()
            assertEquals(1, workInfos.size)
            val constraints = workInfos.first().constraints
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            assertTrue(
                workInfos.first().state == WorkInfo.State.ENQUEUED ||
                    workInfos.first().state == WorkInfo.State.RUNNING
            )

            // Drain queue via OutboxSyncProcessor
            val processor = OutboxSyncProcessor(
                db = db,
                sessionPrefs = SessionPreferences(context),
                driveSync = driveSync,
                firestoreSync = FirestoreSyncRepository(FirebaseFirestore.getInstance()),
                isFirebaseSignedInProvider = { false }
            )
            val summary = processor.drainPendingQueue()
            assertEquals(1, summary.processedCount)
            assertEquals(0, summary.remainingPendingCount)
            assertEquals(0, db.syncOutboxDao().observePendingCount(session.sessionId).first())
        } finally {
            db.close()
        }
    }

    @Test
    fun `manual registration requires one-time jenjang selection then remembers it on login`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val repository = com.example.data.ScoutRepository(context)
        val regResult = repository.registerManual(
            fullName = "Siti Aminah",
            username = "siti_pramuka_${System.currentTimeMillis()}",
            email = "siti_${System.currentTimeMillis()}@example.com",
            password = "password123"
        )
        assertTrue(regResult.isSuccess)
        val initialSession = regResult.getOrThrow()
        assertEquals(false, initialSession.hasCompletedJenjangSelection)

        val confirmResult = repository.confirmInitialJenjang(
            session = initialSession,
            progress = UserProgressPayload(),
            selectedJenjang = JenjangPramuka.PENEGAK
        )
        assertTrue(confirmResult.isSuccess)
        val confirmedSession = confirmResult.getOrThrow().first
        assertEquals(true, confirmedSession.hasCompletedJenjangSelection)
        assertEquals(JenjangPramuka.PENEGAK, confirmedSession.jenjang)

        repository.logout()
        val loginResult = repository.loginManual(
            identifier = initialSession.username,
            password = "password123"
        )
        assertTrue(loginResult.isSuccess)
        val restoredSession = loginResult.getOrThrow()
        assertEquals(true, restoredSession.hasCompletedJenjangSelection)
        assertEquals(JenjangPramuka.PENEGAK, restoredSession.jenjang)
    }

    @Test
    fun `drive backup json preserves hasSelectedJenjang flag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val driveSync = GoogleDriveSyncService(context)
        val sessionPending = UserSession(
            sessionId = "google_uid_1",
            authProvider = AuthProviderType.GOOGLE,
            fullName = "Andi Google",
            username = "andi_g",
            email = "andi@example.com",
            jenjang = JenjangPramuka.SIAGA,
            hasCompletedJenjangSelection = false
        )
        val jsonPending = driveSync.serializeBackupJson(sessionPending, UserProgressPayload())
        val (parsedPending, _) = driveSync.deserializeBackupJson(jsonPending)
        assertEquals(false, parsedPending?.hasCompletedJenjangSelection)

        val sessionDone = sessionPending.copy(
            jenjang = JenjangPramuka.PEMBINA,
            hasCompletedJenjangSelection = true
        )
        val jsonDone = driveSync.serializeBackupJson(sessionDone, UserProgressPayload())
        val (parsedDone, _) = driveSync.deserializeBackupJson(jsonDone)
        assertEquals(true, parsedDone?.hasCompletedJenjangSelection)
        assertEquals(JenjangPramuka.PEMBINA, parsedDone?.jenjang)
    }
}
