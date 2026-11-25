package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.LocalAccountEntity
import com.example.data.local.LocalGudepEntity
import com.example.data.local.LocalMaterialEntity
import com.example.data.local.LocalTaskEntity
import com.example.data.local.LocalUserProgressEntity
import com.example.data.local.OfflineContentCacheStore
import com.example.data.local.ScoutLocalDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.SyncOutboxEntity
import com.example.data.network.NetworkMonitor
import com.example.data.remote.DriveSyncResult
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.GoogleDriveSyncService
import com.example.data.remote.UserProgressPayload
import com.example.data.remote.WeatherService
import com.example.data.sync.DriveAndCloudSyncWorker
import com.example.data.sync.OutboxDrainSummary
import com.example.data.sync.OutboxSyncProcessor
import com.example.data.sync.SyncOutboxAction
import com.example.model.AuthProviderType
import com.example.model.GudepProfile
import com.example.model.JenjangPramuka
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.example.model.UserSession
import com.example.model.WeatherSnapshot
import com.example.model.sanitizeGudepId
import com.example.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class SyncAttemptOutcome(
    val timestampMillis: Long,
    val queuedOffline: Boolean,
    val pendingCount: Int
)

class ScoutRepository(
    context: Context,
    private val firestoreSync: FirestoreSyncRepository = FirestoreSyncRepository(context),
    private val driveSync: GoogleDriveSyncService = GoogleDriveSyncService(context),
    private val weatherService: WeatherService = WeatherService(),
    private val networkMonitor: NetworkMonitor = NetworkMonitor(context.applicationContext)
) {
    private val appContext = context.applicationContext
    private val db = ScoutLocalDatabase.getInstance(appContext)
    private val accountDao = db.accountDao()
    private val gudepDao = db.gudepDao()
    private val materialDao = db.materialDao()
    private val taskDao = db.taskDao()
    private val progressDao = db.progressDao()
    private val outboxDao = db.syncOutboxDao()
    private val offlineCache = OfflineContentCacheStore(appContext, db.cachedOpenedItemDao())
    val sessionPrefs = SessionPreferences(appContext)

    val activeSessionFlow: Flow<UserSession?> = sessionPrefs.activeSessionFlow
    val isOnlineFlow: StateFlow<Boolean> = networkMonitor.isOnline

    val cachedOpenedMaterialsFlow: Flow<List<ScoutMaterial>> = offlineCache.openedMaterialsFlow
    val cachedOpenedTasksFlow: Flow<List<ScoutTask>> = offlineCache.openedTasksFlow
    val cachedOpenedMaterialIdsFlow: Flow<Set<String>> = offlineCache.openedMaterialIdsFlow
    val cachedOpenedTaskIdsFlow: Flow<Set<String>> = offlineCache.openedTaskIdsFlow
    val totalCachedOpenedCountFlow: Flow<Int> = offlineCache.totalCachedCountFlow

    fun isCurrentlyOnline(): Boolean = networkMonitor.checkCurrentlyOnline()

    fun observePendingSyncCount(sessionId: String): Flow<Int> =
        outboxDao.observePendingCount(sessionId)

    suspend fun hydrateOfflineCache() = offlineCache.hydrateFromDiskCacheIfNeeded()

    suspend fun recordOpenedMaterial(material: ScoutMaterial) {
        offlineCache.cacheOpenedMaterial(material)
    }

    suspend fun recordOpenedTask(task: ScoutTask) {
        offlineCache.cacheOpenedTask(task)
    }

    fun observeAllLocalGudep(): Flow<List<GudepProfile>> =
        gudepDao.observeAllGudep().map { list -> list.map { it.toDomain() } }

    fun observeAllLocalMaterials(): Flow<List<ScoutMaterial>> =
        materialDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAllLocalTasks(): Flow<List<ScoutTask>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeUserProgress(sessionId: String): Flow<UserProgressPayload> =
        progressDao.observeBySession(sessionId).map { entity ->
            if (entity == null) {
                defaultProgressPayload()
            } else {
                driveSync.deserializeBackupJson(entity.payloadJson).second ?: defaultProgressPayload()
            }
        }

    fun observeFirestoreOwnedGudep(): Flow<List<GudepProfile>> = firestoreSync.observeOwnedGudep()

    fun observeFirestoreMaterials(gudepId: String, includeDraftsIfOwner: Boolean): Flow<List<ScoutMaterial>> =
        firestoreSync.observeGudepMaterials(gudepId, includeDraftsIfOwner)

    fun observeFirestoreTasks(gudepId: String): Flow<List<ScoutTask>> =
        firestoreSync.observeGudepTasks(gudepId)

    suspend fun fetchWeather(): WeatherSnapshot = weatherService.fetchIndonesiaWeather()

    suspend fun registerManual(
        fullName: String,
        username: String,
        email: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanName = fullName.trim()
        val cleanUser = username.trim()
        val cleanEmail = email.trim()
        if (cleanName.length < 2) return@withContext Result.failure(IllegalArgumentException("Nama lengkap minimal 2 karakter."))
        if (cleanUser.length < 3) return@withContext Result.failure(IllegalArgumentException("Username minimal 3 karakter."))
        if (!cleanEmail.contains("@")) return@withContext Result.failure(IllegalArgumentException("Format email tidak valid."))
        if (password.length < 6) return@withContext Result.failure(IllegalArgumentException("Password minimal 6 karakter."))

        val existingByUser = accountDao.findByUsernameOrEmail(cleanUser)
        if (existingByUser != null) {
            return@withContext Result.failure(IllegalStateException("Username sudah terdaftar."))
        }
        val existingByEmail = accountDao.findByUsernameOrEmail(cleanEmail)
        if (existingByEmail != null) {
            return@withContext Result.failure(IllegalStateException("Email sudah terdaftar."))
        }

        val accountId = "manual_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val hashed = PasswordHasher.hashPassword(password)
        val account = LocalAccountEntity(
            accountId = accountId,
            fullName = cleanName,
            username = cleanUser,
            email = cleanEmail,
            passwordHash = hashed,
            jenjang = JenjangPramuka.PENGGALANG.label,
            hasSelectedJenjang = false,
            xp = 50
        )
        accountDao.upsert(account)

        val session = UserSession(
            sessionId = accountId,
            authProvider = AuthProviderType.MANUAL,
            fullName = cleanName,
            username = cleanUser,
            email = cleanEmail,
            jenjang = JenjangPramuka.PENGGALANG,
            hasCompletedJenjangSelection = false,
            xp = 50
        )
        val defaultProgress = defaultProgressPayload()
        saveProgressInternal(session, defaultProgress)
        sessionPrefs.saveSession(session)
        Result.success(session)
    }

    suspend fun loginManual(
        identifier: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        if (cleanId.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Username/email dan password wajib diisi."))
        }
        val account = accountDao.findByUsernameOrEmail(cleanId)
            ?: return@withContext Result.failure(IllegalStateException("Akun tidak ditemukan. Silakan daftar terlebih dahulu."))

        if (!PasswordHasher.verifyPassword(password, account.passwordHash)) {
            return@withContext Result.failure(IllegalArgumentException("Password tidak sesuai."))
        }

        val resolvedJenjang = JenjangPramuka.fromLabel(account.jenjang)
        val session = UserSession(
            sessionId = account.accountId,
            authProvider = AuthProviderType.MANUAL,
            fullName = account.fullName,
            username = account.username,
            email = account.email,
            jenjang = resolvedJenjang,
            hasCompletedJenjangSelection = account.hasSelectedJenjang,
            institutionName = account.institutionName,
            gudepNumber = account.gudepNumber,
            unitName = account.unitName,
            xp = account.xp
        )
        ensureProgressInitialized(session)
        sessionPrefs.saveSession(session)
        Result.success(session)
    }

    suspend fun completeGoogleSignIn(
        uid: String,
        email: String,
        displayName: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cloudProfile = firestoreSync.loadUserProfile().getOrNull()
        val localMirror = driveSync.restoreFromLocalMirror(uid)
        val cloudBackupJson = firestoreSync.loadLatestBackupJson().getOrNull()
        val cloudBackupParsed = cloudBackupJson?.let { driveSync.deserializeBackupJson(it) }
        val cloudProgress = cloudBackupParsed?.second
        val localProgressParsed = progressDao.getBySession(uid)?.let {
            driveSync.deserializeBackupJson(it.payloadJson)
        }

        val isAlreadyRegisteredInGoogle = cloudProfile != null ||
            (cloudBackupParsed?.first?.hasCompletedJenjangSelection == true) ||
            (localMirror.first?.hasCompletedJenjangSelection == true) ||
            (localProgressParsed?.first?.hasCompletedJenjangSelection == true)

        val resolvedJenjang = when {
            cloudProfile != null -> JenjangPramuka.fromLabel(cloudProfile.jenjang)
            cloudBackupParsed?.first != null -> cloudBackupParsed.first!!.jenjang
            localMirror.first != null -> localMirror.first!!.jenjang
            localProgressParsed?.first != null -> localProgressParsed.first!!.jenjang
            else -> JenjangPramuka.PENGGALANG
        }

        val resolvedName = cloudProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: localMirror.first?.fullName?.takeIf { it.isNotBlank() }
            ?: displayName.ifBlank { "Anggota Pramuka" }
        val resolvedUsername = cloudProfile?.username?.takeIf { it.isNotBlank() }
            ?: localMirror.first?.username?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "pramuka_${uid.take(6)}" }

        val session = UserSession(
            sessionId = uid,
            authProvider = AuthProviderType.GOOGLE,
            fullName = resolvedName,
            username = resolvedUsername,
            email = cloudProfile?.email?.ifBlank { email } ?: email,
            jenjang = resolvedJenjang,
            hasCompletedJenjangSelection = isAlreadyRegisteredInGoogle,
            institutionName = cloudProfile?.institutionName ?: localMirror.first?.institutionName.orEmpty(),
            gudepNumber = cloudProfile?.gudepNumber ?: localMirror.first?.gudepNumber.orEmpty(),
            unitName = cloudProfile?.unitName ?: localMirror.first?.unitName.orEmpty(),
            xp = cloudProfile?.xp?.toInt()?.coerceAtLeast(50) ?: localMirror.first?.xp?.coerceAtLeast(50) ?: 50
        )

        val existingProgress = localProgressParsed?.second
            ?: cloudProgress
            ?: localMirror.second
            ?: defaultProgressPayload()

        saveProgressInternal(session, existingProgress)
        if (isAlreadyRegisteredInGoogle && networkMonitor.checkCurrentlyOnline()) {
            firestoreSync.upsertUserProfile(session)
        }
        sessionPrefs.saveSession(session)
        Result.success(session)
    }

    suspend fun confirmInitialJenjang(
        session: UserSession,
        progress: UserProgressPayload,
        selectedJenjang: JenjangPramuka
    ): Result<Pair<UserSession, SyncAttemptOutcome>> = withContext(Dispatchers.IO) {
        val updated = session.copy(
            jenjang = selectedJenjang,
            hasCompletedJenjangSelection = true
        )
        if (updated.authProvider == AuthProviderType.MANUAL) {
            val existing = accountDao.findById(updated.sessionId)
            if (existing != null) {
                accountDao.upsert(
                    existing.copy(
                        jenjang = selectedJenjang.label,
                        hasSelectedJenjang = true,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        saveProgressInternal(updated, progress)
        val outcome = syncAllSilently(updated, progress)
        val finalSession = updated.copy(lastSyncedMillis = outcome.timestampMillis)
        sessionPrefs.saveSession(finalSession)
        Result.success(finalSession to outcome)
    }

    suspend fun saveProfile(session: UserSession, progress: UserProgressPayload): Result<Pair<UserSession, SyncAttemptOutcome>> =
        withContext(Dispatchers.IO) {
            if (session.authProvider == AuthProviderType.MANUAL) {
                val existing = accountDao.findById(session.sessionId)
                if (existing != null) {
                    accountDao.upsert(
                        existing.copy(
                            fullName = session.fullName,
                            username = session.username,
                            jenjang = session.jenjang.label,
                            hasSelectedJenjang = true,
                            institutionName = session.institutionName,
                            gudepNumber = session.gudepNumber,
                            unitName = session.unitName,
                            xp = session.xp,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            } else if (networkMonitor.checkCurrentlyOnline()) {
                firestoreSync.upsertUserProfile(session)
            }

            // If user entered a Gudep number and Google signed in while online, prefetch that Gudep into local cache
            if (session.gudepNumber.isNotBlank() && session.authProvider == AuthProviderType.GOOGLE && networkMonitor.checkCurrentlyOnline()) {
                firestoreSync.fetchGudepByNumber(session.gudepNumber).getOrNull()?.let { remoteGudep ->
                    gudepDao.upsert(remoteGudep.toEntity())
                }
            }

            val outcome = syncAllSilently(session, progress)
            val updated = session.copy(lastSyncedMillis = outcome.timestampMillis)
            sessionPrefs.saveSession(updated)
            Result.success(updated to outcome)
        }

    suspend fun lookupGudep(gudepNumber: String, authProvider: AuthProviderType): GudepProfile? =
        withContext(Dispatchers.IO) {
            if (gudepNumber.isBlank()) return@withContext null
            val gudepId = sanitizeGudepId(gudepNumber)
            val local = gudepDao.getById(gudepId)?.toDomain()
            if (authProvider == AuthProviderType.GOOGLE && networkMonitor.checkCurrentlyOnline()) {
                val remote = firestoreSync.fetchGudepByNumber(gudepNumber).getOrNull()
                if (remote != null) {
                    gudepDao.upsert(remote.toEntity())
                    return@withContext remote
                }
            }
            local
        }

    suspend fun saveGudepIdentity(
        session: UserSession,
        progress: UserProgressPayload,
        institutionName: String,
        gudepNumber: String,
        address: String,
        logoBase64: String?
    ): Result<Pair<GudepProfile, SyncAttemptOutcome>> = withContext(Dispatchers.IO) {
        if (!session.canAccessDashboard) {
            return@withContext Result.failure(SecurityException("Hanya Pembina atau Pembina Damping yang dapat mengelola Lembaga & Gudep."))
        }
        val cleanInst = institutionName.trim()
        val cleanNum = gudepNumber.trim()
        val cleanAddr = address.trim()
        if (cleanInst.length < 2) return@withContext Result.failure(IllegalArgumentException("Nama lembaga minimal 2 karakter."))
        if (cleanNum.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nomor Gudep wajib diisi."))

        val gudepId = sanitizeGudepId(cleanNum)
        val profile = GudepProfile(
            gudepId = gudepId,
            ownerId = session.sessionId,
            ownerName = session.fullName.ifBlank { session.username },
            institutionName = cleanInst,
            gudepNumber = cleanNum,
            address = cleanAddr,
            logoBase64 = logoBase64,
            updatedAtMillis = System.currentTimeMillis()
        )

        // Save locally immediately and enqueue outbox action
        gudepDao.upsert(profile.toEntity())
        outboxDao.upsert(
            SyncOutboxEntity(
                id = "gudep_${gudepId}",
                sessionId = session.sessionId,
                authProvider = session.authProvider.name,
                actionType = SyncOutboxAction.GUDEP_UPSERT,
                payloadJson = OutboxSyncProcessor.encodeGudepJson(profile),
                status = "PENDING",
                createdAtMillis = System.currentTimeMillis()
            )
        )

        val updatedSession = session.copy(
            institutionName = cleanInst,
            gudepNumber = cleanNum
        )
        val outcome = saveProfile(updatedSession, progress).getOrNull()?.second
            ?: SyncAttemptOutcome(System.currentTimeMillis(), queuedOffline = true, pendingCount = 1)
        val latest = gudepDao.getById(gudepId)?.toDomain() ?: profile
        Result.success(latest to outcome)
    }

    suspend fun saveMaterial(
        session: UserSession,
        progress: UserProgressPayload,
        material: ScoutMaterial
    ): Result<Pair<ScoutMaterial, SyncAttemptOutcome>> = withContext(Dispatchers.IO) {
        if (!session.canAccessDashboard) {
            return@withContext Result.failure(SecurityException("Akses Dashboard khusus Pembina."))
        }
        val gudepId = material.gudepId.ifBlank { sanitizeGudepId(session.gudepNumber) }
        val id = material.id.ifBlank { "mat_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val saved = material.copy(id = id, gudepId = gudepId, ownerId = session.sessionId, fromPembina = true)

        materialDao.upsert(saved.toEntity())
        outboxDao.upsert(
            SyncOutboxEntity(
                id = "material_${saved.id}",
                sessionId = session.sessionId,
                authProvider = session.authProvider.name,
                actionType = SyncOutboxAction.MATERIAL_UPSERT,
                payloadJson = OutboxSyncProcessor.encodeMaterialJson(saved),
                status = "PENDING",
                createdAtMillis = System.currentTimeMillis()
            )
        )
        val outcome = syncAllSilently(session, progress)
        Result.success(saved to outcome)
    }

    suspend fun saveTask(
        session: UserSession,
        progress: UserProgressPayload,
        task: ScoutTask
    ): Result<Pair<ScoutTask, SyncAttemptOutcome>> = withContext(Dispatchers.IO) {
        if (!session.canAccessDashboard) {
            return@withContext Result.failure(SecurityException("Akses Dashboard khusus Pembina."))
        }
        val gudepId = task.gudepId.ifBlank { sanitizeGudepId(session.gudepNumber) }
        val id = task.id.ifBlank { "task_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val saved = task.copy(id = id, gudepId = gudepId, ownerId = session.sessionId)

        taskDao.upsert(saved.toEntity())
        outboxDao.upsert(
            SyncOutboxEntity(
                id = "task_${saved.id}",
                sessionId = session.sessionId,
                authProvider = session.authProvider.name,
                actionType = SyncOutboxAction.TASK_UPSERT,
                payloadJson = OutboxSyncProcessor.encodeTaskJson(saved),
                status = "PENDING",
                createdAtMillis = System.currentTimeMillis()
            )
        )
        val outcome = syncAllSilently(session, progress)
        Result.success(saved to outcome)
    }

    suspend fun cacheRemoteMaterials(items: List<ScoutMaterial>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            materialDao.upsertAll(items.map { it.toEntity() })
        }
    }

    suspend fun cacheRemoteTasks(items: List<ScoutTask>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            taskDao.upsertAll(items.map { it.toEntity() })
        }
    }

    suspend fun saveProgressAndSync(
        session: UserSession,
        progress: UserProgressPayload,
        xpDelta: Int = 0
    ): Pair<UserSession, SyncAttemptOutcome> = withContext(Dispatchers.IO) {
        val updatedSession = session.copy(xp = (session.xp + xpDelta).coerceAtLeast(0))
        saveProgressInternal(updatedSession, progress)
        if (updatedSession.authProvider == AuthProviderType.MANUAL) {
            accountDao.findById(updatedSession.sessionId)?.let { acc ->
                accountDao.upsert(acc.copy(xp = updatedSession.xp, updatedAtMillis = System.currentTimeMillis()))
            }
        }
        val outcome = syncAllSilently(updatedSession, progress)
        val finalSession = updatedSession.copy(lastSyncedMillis = outcome.timestampMillis)
        sessionPrefs.saveSession(finalSession)
        finalSession to outcome
    }

    suspend fun flushPendingSyncQueue(): OutboxDrainSummary = withContext(Dispatchers.IO) {
        if (!networkMonitor.checkCurrentlyOnline()) {
            DriveAndCloudSyncWorker.enqueueSync(appContext)
            val pendingCount = outboxDao.getPendingItems().size
            return@withContext OutboxDrainSummary(0, pendingCount, false)
        }
        val processor = OutboxSyncProcessor(db, sessionPrefs, driveSync, firestoreSync)
        val summary = processor.drainPendingQueue()
        if (summary.anyFailed || summary.remainingPendingCount > 0) {
            DriveAndCloudSyncWorker.enqueueSync(appContext)
        }
        summary
    }

    fun buildExportJson(session: UserSession, progress: UserProgressPayload): String =
        driveSync.serializeBackupJson(session, progress)

    suspend fun importBackupJson(
        currentSession: UserSession,
        rawJson: String
    ): Result<Pair<UserSession, SyncAttemptOutcome>> = withContext(Dispatchers.IO) {
        val (parsedProfile, parsedProgress) = driveSync.deserializeBackupJson(rawJson)
        if (parsedProgress == null) {
            return@withContext Result.failure(IllegalArgumentException("File JSON cadangan tidak valid."))
        }
        val mergedSession = currentSession.copy(
            institutionName = parsedProfile?.institutionName?.takeIf { it.isNotBlank() } ?: currentSession.institutionName,
            gudepNumber = parsedProfile?.gudepNumber?.takeIf { it.isNotBlank() } ?: currentSession.gudepNumber,
            unitName = parsedProfile?.unitName?.takeIf { it.isNotBlank() } ?: currentSession.unitName,
            xp = maxOf(currentSession.xp, parsedProfile?.xp ?: 0)
        )
        val result = saveProgressAndSync(mergedSession, parsedProgress, xpDelta = 0)
        Result.success(result)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionPrefs.clearSession()
    }

    private suspend fun ensureProgressInitialized(session: UserSession) {
        val existing = progressDao.getBySession(session.sessionId)
        if (existing == null) {
            saveProgressInternal(session, defaultProgressPayload())
        }
    }

    private suspend fun saveProgressInternal(session: UserSession, progress: UserProgressPayload) {
        val json = driveSync.serializeBackupJson(session, progress)
        progressDao.upsert(
            LocalUserProgressEntity(
                sessionId = session.sessionId,
                payloadJson = json,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun syncAllSilently(session: UserSession, progress: UserProgressPayload): SyncAttemptOutcome {
        // 1. Always write the local Drive mirror JSON immediately (zero data loss offline)
        val localResult = driveSync.syncBackupSilently(session, progress, oauthAccessToken = null)
        val ts = when (localResult) {
            is DriveSyncResult.CloudSynced -> localResult.timestampMillis
            is DriveSyncResult.LocalMirrorSynced -> localResult.timestampMillis
        }

        // 2. Enqueue into persistent Room outbox so WorkManager guarantees eventual Google Drive/cloud delivery
        val serializedBackup = driveSync.serializeBackupJson(session, progress, ts)
        outboxDao.upsert(
            SyncOutboxEntity(
                id = "backup_${session.sessionId}",
                sessionId = session.sessionId,
                authProvider = session.authProvider.name,
                actionType = SyncOutboxAction.DRIVE_BACKUP,
                payloadJson = serializedBackup,
                status = "PENDING",
                createdAtMillis = ts
            )
        )

        // 3. If offline, schedule WorkManager with NetworkType.CONNECTED constraint and return queued state
        if (!networkMonitor.checkCurrentlyOnline()) {
            DriveAndCloudSyncWorker.enqueueSync(appContext)
            sessionPrefs.updateLastSynced(ts)
            val pendingForUser = outboxDao.getPendingItems().count { it.sessionId == session.sessionId }
            return SyncAttemptOutcome(
                timestampMillis = ts,
                queuedOffline = true,
                pendingCount = pendingForUser
            )
        }

        // 4. Online: drain queue immediately, falling back to WorkManager retry on transient failures
        val processor = OutboxSyncProcessor(db, sessionPrefs, driveSync, firestoreSync)
        val summary = processor.drainPendingQueue()
        val pendingForUser = outboxDao.getPendingItems().count { it.sessionId == session.sessionId }
        if (summary.anyFailed || pendingForUser > 0) {
            DriveAndCloudSyncWorker.enqueueSync(appContext)
            return SyncAttemptOutcome(
                timestampMillis = ts,
                queuedOffline = true,
                pendingCount = pendingForUser
            )
        }
        sessionPrefs.updateLastSynced(ts)
        return SyncAttemptOutcome(
            timestampMillis = ts,
            queuedOffline = false,
            pendingCount = 0
        )
    }

    private fun defaultProgressPayload() = UserProgressPayload(
        packingList = ScoutCatalog.defaultPackingList,
        reguMembers = ScoutCatalog.defaultReguMembers,
        activities = ScoutCatalog.defaultActivities
    )
}

private fun LocalGudepEntity.toDomain() = GudepProfile(
    gudepId = gudepId,
    ownerId = ownerId,
    ownerName = ownerName,
    institutionName = institutionName,
    gudepNumber = gudepNumber,
    address = address,
    logoBase64 = logoBase64,
    updatedAtMillis = updatedAtMillis
)

private fun GudepProfile.toEntity() = LocalGudepEntity(
    gudepId = gudepId,
    ownerId = ownerId,
    ownerName = ownerName,
    institutionName = institutionName,
    gudepNumber = gudepNumber,
    address = address,
    logoBase64 = logoBase64,
    updatedAtMillis = updatedAtMillis
)

private fun LocalMaterialEntity.toDomain() = ScoutMaterial(
    id = id,
    title = title,
    category = category,
    jenjangTarget = jenjangTarget,
    summary = summary,
    content = content,
    fromPembina = true,
    published = published,
    gudepId = gudepId,
    ownerId = ownerId
)

private fun ScoutMaterial.toEntity() = LocalMaterialEntity(
    id = id,
    gudepId = gudepId,
    ownerId = ownerId,
    title = title,
    category = category,
    jenjangTarget = jenjangTarget,
    summary = summary,
    content = content,
    published = published,
    updatedAtMillis = System.currentTimeMillis()
)

private fun LocalTaskEntity.toDomain() = ScoutTask(
    id = id,
    title = title,
    description = description,
    jenjangTarget = jenjangTarget,
    dueDateLabel = dueDateLabel,
    xpReward = xpReward,
    gudepId = gudepId,
    ownerId = ownerId
)

private fun ScoutTask.toEntity() = LocalTaskEntity(
    id = id,
    gudepId = gudepId,
    ownerId = ownerId,
    title = title,
    description = description,
    jenjangTarget = jenjangTarget,
    dueDateLabel = dueDateLabel,
    xpReward = xpReward,
    updatedAtMillis = System.currentTimeMillis()
)
