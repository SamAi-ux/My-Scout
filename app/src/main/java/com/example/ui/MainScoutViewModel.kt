package com.example.ui

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScoutCatalog
import com.example.data.ScoutRepository
import com.example.data.SyncAttemptOutcome
import com.example.data.remote.UserProgressPayload
import com.example.model.AuthProviderType
import com.example.model.GudepMatchStatus
import com.example.model.GudepProfile
import com.example.model.JenjangPramuka
import com.example.model.PackingItem
import com.example.model.ReguMember
import com.example.model.ScoutActivityItem
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.example.model.SyncStateStatus
import com.example.model.UserSession
import com.example.model.WeatherSnapshot
import com.example.model.sanitizeGudepId
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MainScoutViewModel(
    private val repository: ScoutRepository
) : ViewModel() {

    private val _isAuthLoading = MutableStateFlow(true)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStateStatus.IDLE)
    val syncStatus: StateFlow<SyncStateStatus> = _syncStatus.asStateFlow()

    private val _weather = MutableStateFlow(WeatherSnapshot())
    val weather: StateFlow<WeatherSnapshot> = _weather.asStateFlow()

    val isOnline: StateFlow<Boolean> = repository.isOnlineFlow

    val session: StateFlow<UserSession?> = repository.activeSessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val pendingSyncCount: StateFlow<Int> = session
        .flatMapLatest { s ->
            if (s == null) flowOf(0) else repository.observePendingSyncCount(s.sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cachedOpenedMaterialIds: StateFlow<Set<String>> = repository.cachedOpenedMaterialIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val cachedOpenedTaskIds: StateFlow<Set<String>> = repository.cachedOpenedTaskIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val totalCachedOpenedCount: StateFlow<Int> = repository.totalCachedOpenedCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val cachedOpenedMaterials = repository.cachedOpenedMaterialsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cachedOpenedTasks = repository.cachedOpenedTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allLocalGudep = repository.observeAllLocalGudep()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allLocalMaterials = repository.observeAllLocalMaterials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allLocalTasks = repository.observeAllLocalTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progress: StateFlow<UserProgressPayload> = session
        .flatMapLatest { s ->
            if (s == null) flowOf(UserProgressPayload())
            else repository.observeUserProgress(s.sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProgressPayload())

    private val _remoteMatchedGudep = MutableStateFlow<GudepProfile?>(null)

    val gudepMatchStatus: StateFlow<GudepMatchStatus> = combine(
        session,
        allLocalGudep,
        _remoteMatchedGudep
    ) { s, localList, remoteGudep ->
        if (s == null || s.gudepNumber.isBlank() || s.institutionName.isBlank()) {
            GudepMatchStatus.Unfilled
        } else {
            val targetId = sanitizeGudepId(s.gudepNumber)
            val candidate = remoteGudep?.takeIf { it.gudepId == targetId }
                ?: localList.firstOrNull { it.gudepId == targetId }
            if (candidate != null &&
                candidate.institutionName.trim().equals(s.institutionName.trim(), ignoreCase = true)
            ) {
                GudepMatchStatus.Matched(candidate)
            } else {
                GudepMatchStatus.Unmatched(
                    enteredGudepNumber = s.gudepNumber,
                    enteredInstitution = s.institutionName
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GudepMatchStatus.Unfilled)

    val ownedGudep: StateFlow<GudepProfile?> = combine(
        session,
        allLocalGudep,
        session.flatMapLatest { s ->
            if (s != null && s.authProvider == AuthProviderType.GOOGLE && s.canAccessDashboard) {
                repository.observeFirestoreOwnedGudep()
            } else {
                flowOf(emptyList())
            }
        }
    ) { s, localList, remoteList ->
        if (s == null || !s.canAccessDashboard) null
        else remoteList.firstOrNull() ?: localList.firstOrNull { it.ownerId == s.sessionId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val remoteGudepMaterials = combine(session, gudepMatchStatus) { s, match -> s to match }
        .flatMapLatest { (s, match) ->
            if (s == null || s.authProvider != AuthProviderType.GOOGLE) {
                flowOf(emptyList())
            } else {
                val gudepId = when (match) {
                    is GudepMatchStatus.Matched -> match.gudep.gudepId
                    else -> if (s.canAccessDashboard && s.gudepNumber.isNotBlank()) sanitizeGudepId(s.gudepNumber) else ""
                }
                if (gudepId.isBlank()) flowOf(emptyList())
                else repository.observeFirestoreMaterials(gudepId, s.canAccessDashboard)
                    .onEach { remoteList -> repository.cacheRemoteMaterials(remoteList) }
            }
        }

    private val remoteGudepTasks = combine(session, gudepMatchStatus) { s, match -> s to match }
        .flatMapLatest { (s, match) ->
            if (s == null || s.authProvider != AuthProviderType.GOOGLE) {
                flowOf(emptyList())
            } else {
                val gudepId = when (match) {
                    is GudepMatchStatus.Matched -> match.gudep.gudepId
                    else -> if (s.canAccessDashboard && s.gudepNumber.isNotBlank()) sanitizeGudepId(s.gudepNumber) else ""
                }
                if (gudepId.isBlank()) flowOf(emptyList())
                else repository.observeFirestoreTasks(gudepId)
                    .onEach { remoteList -> repository.cacheRemoteTasks(remoteList) }
            }
        }

    val gudepMaterials: StateFlow<List<ScoutMaterial>> = combine(
        session,
        gudepMatchStatus,
        allLocalMaterials,
        remoteGudepMaterials,
        cachedOpenedMaterials
    ) { s, match, localMats, remoteMats, cachedMats ->
        if (s == null) return@combine emptyList()
        val activeGudepId = (match as? GudepMatchStatus.Matched)?.gudep?.gudepId
            ?: if (s.canAccessDashboard && s.gudepNumber.isNotBlank()) sanitizeGudepId(s.gudepNumber) else ""
        if (activeGudepId.isBlank()) return@combine emptyList()

        val merged = LinkedHashMap<String, ScoutMaterial>()
        cachedMats.filter { it.fromPembina && it.gudepId == activeGudepId }.forEach { merged[it.id] = it }
        localMats.filter { it.gudepId == activeGudepId }.forEach { merged[it.id] = it }
        remoteMats.forEach { merged[it.id] = it }
        merged.values.filter { mat ->
            if (s.canAccessDashboard) true
            else mat.published && (mat.jenjangTarget.equals("Semua", true) || mat.jenjangTarget.equals(s.jenjang.label, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gudepTasks: StateFlow<List<ScoutTask>> = combine(
        session,
        gudepMatchStatus,
        allLocalTasks,
        remoteGudepTasks,
        cachedOpenedTasks
    ) { s, match, localTasks, remoteTasks, cachedTasks ->
        if (s == null) return@combine emptyList()
        val activeGudepId = (match as? GudepMatchStatus.Matched)?.gudep?.gudepId
            ?: if (s.canAccessDashboard && s.gudepNumber.isNotBlank()) sanitizeGudepId(s.gudepNumber) else ""
        if (activeGudepId.isBlank()) return@combine emptyList()

        val merged = LinkedHashMap<String, ScoutTask>()
        cachedTasks.filter { it.gudepId == activeGudepId }.forEach { merged[it.id] = it }
        localTasks.filter { it.gudepId == activeGudepId }.forEach { merged[it.id] = it }
        remoteTasks.forEach { merged[it.id] = it }
        merged.values.filter { task ->
            if (s.canAccessDashboard) true
            else task.jenjangTarget.equals("Semua", true) || task.jenjangTarget.equals(s.jenjang.label, true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.hydrateOfflineCache()
        }
        viewModelScope.launch {
            repository.activeSessionFlow.collect { active ->
                _isAuthLoading.value = false
                if (active != null && active.gudepNumber.isNotBlank()) {
                    _remoteMatchedGudep.value = repository.lookupGudep(active.gudepNumber, active.authProvider)
                } else {
                    _remoteMatchedGudep.value = null
                }
            }
        }
        viewModelScope.launch {
            var previouslyOnline = repository.isCurrentlyOnline()
            repository.isOnlineFlow.collect { onlineNow ->
                if (!onlineNow) {
                    if (pendingSyncCount.value > 0) {
                        _syncStatus.value = SyncStateStatus.QUEUED_OFFLINE
                    }
                    previouslyOnline = false
                } else {
                    val active = session.value
                    val shouldFlush = !previouslyOnline || pendingSyncCount.value > 0
                    previouslyOnline = true
                    if (shouldFlush && active != null) {
                        _syncStatus.value = SyncStateStatus.SYNCING
                        val summary = repository.flushPendingSyncQueue()
                        _syncStatus.value = when {
                            summary.anyFailed || summary.remainingPendingCount > 0 -> SyncStateStatus.QUEUED_OFFLINE
                            active.authProvider == AuthProviderType.GOOGLE -> SyncStateStatus.SYNCED_CLOUD_AND_DRIVE
                            else -> SyncStateStatus.SYNCED_LOCAL_FALLBACK
                        }
                        if (summary.processedCount > 0 && summary.remainingPendingCount == 0) {
                            _feedbackMessage.value = "Koneksi aktif kembali. ${summary.processedCount} antrean Google Drive otomatis tersinkron."
                        }
                        if (active.gudepNumber.isNotBlank()) {
                            _remoteMatchedGudep.value = repository.lookupGudep(active.gudepNumber, active.authProvider)
                        }
                    }
                    refreshWeather()
                }
            }
        }
        refreshWeather()
    }

    fun onMaterialOpened(material: ScoutMaterial) {
        viewModelScope.launch {
            repository.recordOpenedMaterial(material)
        }
    }

    fun onTaskOpened(task: ScoutTask) {
        viewModelScope.launch {
            repository.recordOpenedTask(task)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _weather.value = repository.fetchWeather()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun registerManual(
        fullName: String,
        username: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.registerManual(fullName, username, email, password)
            res.onFailure { _authError.value = it.message ?: "Pendaftaran gagal." }
        }
    }

    fun loginManual(
        identifier: String,
        password: String
    ) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.loginManual(identifier, password)
            res.onFailure { _authError.value = it.message ?: "Login gagal." }
        }
    }

    fun onGoogleAuthenticated(
        uid: String,
        email: String,
        displayName: String
    ) {
        viewModelScope.launch {
            _authError.value = null
            _syncStatus.value = SyncStateStatus.SYNCING
            val res = repository.completeGoogleSignIn(uid, email, displayName)
            res.onSuccess { session ->
                _syncStatus.value = if (session.hasCompletedJenjangSelection) {
                    SyncStateStatus.SYNCED_CLOUD_AND_DRIVE
                } else {
                    SyncStateStatus.IDLE
                }
            }.onFailure {
                _authError.value = it.message ?: "Gagal menyinkronkan akun Google."
                _syncStatus.value = SyncStateStatus.ERROR
            }
        }
    }

    fun confirmInitialJenjang(selectedJenjang: JenjangPramuka) {
        val current = session.value ?: return
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            repository.confirmInitialJenjang(current, progress.value, selectedJenjang)
                .onSuccess { (updated, outcome) ->
                    _syncStatus.value = resolveSyncStatus(updated.authProvider, outcome)
                    _feedbackMessage.value = if (updated.authProvider == AuthProviderType.GOOGLE) {
                        "Jenjang ${selectedJenjang.label} tersimpan ke akun Google terdaftar."
                    } else {
                        "Jenjang ${selectedJenjang.label} berhasil disimpan."
                    }
                }
                .onFailure { err ->
                    _syncStatus.value = SyncStateStatus.ERROR
                    _feedbackMessage.value = err.message ?: "Gagal menyimpan jenjang."
                }
        }
    }

    fun setAuthErrorMessage(msg: String) {
        _authError.value = msg
    }

    fun saveProfileForm(
        fullName: String,
        username: String,
        jenjang: JenjangPramuka,
        institutionName: String,
        gudepNumber: String,
        unitName: String
    ) {
        val current = session.value ?: return
        if (fullName.isBlank() || username.isBlank() || institutionName.isBlank() || gudepNumber.isBlank()) {
            _feedbackMessage.value = "Lengkapi Nama, Username, Nama Lembaga, dan Nomor Gudep."
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            val updated = current.copy(
                fullName = fullName.trim(),
                username = username.trim(),
                jenjang = jenjang,
                institutionName = institutionName.trim(),
                gudepNumber = gudepNumber.trim(),
                unitName = unitName.trim()
            )
            val saveRes = repository.saveProfile(updated, progress.value)
            val outcome = saveRes.getOrNull()?.second
            val matched = repository.lookupGudep(gudepNumber, updated.authProvider)
            _remoteMatchedGudep.value = matched
            _syncStatus.value = resolveSyncStatus(updated.authProvider, outcome)

            val queueNote = if (outcome?.queuedOffline == true) {
                " Antrean sinkronisasi Drive siap jalan otomatis saat online."
            } else ""
            _feedbackMessage.value = if (matched != null &&
                matched.institutionName.trim().equals(institutionName.trim(), ignoreCase = true)
            ) {
                "Data diri tersimpan & terhubung ke ${matched.institutionName}.$queueNote"
            } else {
                "Data tersimpan. Nomor Gudep belum terdaftar oleh Pembina.$queueNote"
            }
        }
    }

    fun saveGudepIdentity(
        institutionName: String,
        gudepNumber: String,
        address: String,
        logoBase64: String?
    ) {
        val current = session.value ?: return
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            val res = repository.saveGudepIdentity(
                session = current,
                progress = progress.value,
                institutionName = institutionName,
                gudepNumber = gudepNumber,
                address = address,
                logoBase64 = logoBase64
            )
            res.onSuccess { (saved, outcome) ->
                _remoteMatchedGudep.value = saved
                _syncStatus.value = resolveSyncStatus(current.authProvider, outcome)
                val offlineNote = if (outcome.queuedOffline) " (masuk antrean sinkronisasi otomatis)" else ""
                _feedbackMessage.value = "Identitas ${saved.institutionName} (Gudep ${saved.gudepNumber}) berhasil didaftarkan.$offlineNote"
            }.onFailure { err ->
                _syncStatus.value = SyncStateStatus.ERROR
                _feedbackMessage.value = err.message ?: "Gagal menyimpan identitas lembaga."
            }
        }
    }

    fun saveGudepMaterial(
        existingId: String,
        title: String,
        category: String,
        jenjangTarget: String,
        summary: String,
        content: String,
        published: Boolean
    ) {
        val current = session.value ?: return
        val activeGudep = ownedGudep.value
        val gudepId = activeGudep?.gudepId
            ?: current.gudepNumber.takeIf { it.isNotBlank() }?.let(::sanitizeGudepId)
        if (gudepId.isNullOrBlank()) {
            _feedbackMessage.value = "Daftarkan Identitas Lembaga & Nomor Gudep terlebih dahulu."
            return
        }
        if (title.trim().length < 2 || content.trim().isBlank()) {
            _feedbackMessage.value = "Judul dan isi materi tidak boleh kosong."
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            val mat = ScoutMaterial(
                id = existingId,
                title = title.trim(),
                category = category.trim().ifBlank { "Materi Gudep" },
                jenjangTarget = jenjangTarget,
                summary = summary.trim().ifBlank { content.trim().take(120) },
                content = content.trim(),
                fromPembina = true,
                published = published,
                gudepId = gudepId,
                ownerId = current.sessionId
            )
            repository.saveMaterial(current, progress.value, mat).onSuccess { (_, outcome) ->
                _syncStatus.value = resolveSyncStatus(current.authProvider, outcome)
                val suffix = if (outcome.queuedOffline) " (disimpan lokal & antrean sinkronisasi)" else ""
                _feedbackMessage.value = (if (published) "Materi berhasil dipublikasikan." else "Draf materi disimpan.") + suffix
            }.onFailure {
                _syncStatus.value = SyncStateStatus.ERROR
                _feedbackMessage.value = it.message ?: "Gagal menyimpan materi."
            }
        }
    }

    fun sendAdditionalTask(
        title: String,
        description: String,
        jenjangTarget: String,
        dueDateLabel: String,
        xpReward: Int
    ) {
        val current = session.value ?: return
        val activeGudep = ownedGudep.value
        val gudepId = activeGudep?.gudepId
            ?: current.gudepNumber.takeIf { it.isNotBlank() }?.let(::sanitizeGudepId)
        if (gudepId.isNullOrBlank()) {
            _feedbackMessage.value = "Daftarkan Nomor Gudep terlebih dahulu sebelum mengirim tugas."
            return
        }
        if (title.trim().length < 2 || description.trim().isBlank()) {
            _feedbackMessage.value = "Judul dan deskripsi tugas wajib diisi."
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            val task = ScoutTask(
                id = "",
                title = title.trim(),
                description = description.trim(),
                jenjangTarget = jenjangTarget,
                dueDateLabel = dueDateLabel.trim().ifBlank { "Minggu Ini" },
                xpReward = xpReward.coerceIn(5, 500),
                gudepId = gudepId,
                ownerId = current.sessionId
            )
            repository.saveTask(current, progress.value, task).onSuccess { (_, outcome) ->
                _syncStatus.value = resolveSyncStatus(current.authProvider, outcome)
                val suffix = if (outcome.queuedOffline) " (masuk antrean sinkronisasi)" else ""
                _feedbackMessage.value = "Tugas untuk jenjang $jenjangTarget berhasil dikirim.$suffix"
            }.onFailure {
                _syncStatus.value = SyncStateStatus.ERROR
                _feedbackMessage.value = it.message ?: "Gagal mengirim tugas."
            }
        }
    }

    fun toggleMaterialRead(materialId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val alreadyRead = materialId in prog.readMaterialIds
        val updatedSet = if (alreadyRead) prog.readMaterialIds - materialId else prog.readMaterialIds + materialId
        val delta = if (alreadyRead) 0 else 15
        persistProgress(current, prog.copy(readMaterialIds = updatedSet), delta)
    }

    fun toggleTaskComplete(task: ScoutTask) {
        val current = session.value ?: return
        viewModelScope.launch { repository.recordOpenedTask(task) }
        val prog = progress.value
        val done = task.id in prog.completedTaskIds
        val updatedSet = if (done) prog.completedTaskIds - task.id else prog.completedTaskIds + task.id
        val delta = if (done) -task.xpReward else task.xpReward
        persistProgress(current, prog.copy(completedTaskIds = updatedSet), delta)
    }

    fun toggleSkuCompleted(skuId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val done = skuId in prog.completedSkuIds
        val updatedSet = if (done) prog.completedSkuIds - skuId else prog.completedSkuIds + skuId
        val delta = if (done) -25 else 25
        persistProgress(current, prog.copy(completedSkuIds = updatedSet), delta)
    }

    fun toggleTkkEarned(tkkId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val earned = tkkId in prog.earnedTkkIds
        val updatedSet = if (earned) prog.earnedTkkIds - tkkId else prog.earnedTkkIds + tkkId
        val delta = if (earned) -40 else 40
        persistProgress(current, prog.copy(earnedTkkIds = updatedSet), delta)
    }

    fun recordQuizResult(scorePercent: Int) {
        val current = session.value ?: return
        val prog = progress.value
        val updated = prog.copy(
            quizBestScore = maxOf(prog.quizBestScore, scorePercent),
            quizAttempts = prog.quizAttempts + 1
        )
        val xpEarned = (scorePercent / 2).coerceAtLeast(10)
        persistProgress(current, updated, xpEarned)
        _feedbackMessage.value = "Skor kuis $scorePercent% tersimpan (+$xpEarned XP)!"
    }

    fun togglePackingItem(itemId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val updatedList = prog.packingList.map {
            if (it.id == itemId) it.copy(checked = !it.checked) else it
        }
        persistProgress(current, prog.copy(packingList = updatedList), 0)
    }

    fun addPackingItem(name: String, category: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val current = session.value ?: return
        val prog = progress.value
        val newItem = PackingItem(
            id = "pack_${UUID.randomUUID().toString().take(8)}",
            name = clean,
            category = category.ifBlank { "Perlengkapan" },
            checked = false
        )
        persistProgress(current, prog.copy(packingList = prog.packingList + newItem), 0)
    }

    fun toggleMemberAttendance(memberId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val updated = prog.reguMembers.map {
            if (it.id == memberId) it.copy(present = !it.present) else it
        }
        persistProgress(current, prog.copy(reguMembers = updated), 0)
    }

    fun addReguMember(name: String, role: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val current = session.value ?: return
        val prog = progress.value
        val newMember = ReguMember(
            id = "mem_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            role = role.trim().ifBlank { "Anggota" },
            jenjang = current.jenjang.label,
            present = true
        )
        persistProgress(current, prog.copy(reguMembers = prog.reguMembers + newMember), 5)
    }

    fun toggleActivityAttendance(activityId: String) {
        val current = session.value ?: return
        val prog = progress.value
        val target = prog.activities.firstOrNull { it.id == activityId } ?: return
        val updated = prog.activities.map {
            if (it.id == activityId) it.copy(attended = !it.attended) else it
        }
        val delta = if (target.attended) 0 else 20
        persistProgress(current, prog.copy(activities = updated), delta)
    }

    fun addActivity(title: String, dateLabel: String, location: String, description: String) {
        if (title.isBlank()) return
        val current = session.value ?: return
        val prog = progress.value
        val item = ScoutActivityItem(
            id = "act_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            dateLabel = dateLabel.trim().ifBlank { "Jadwal Baru" },
            location = location.trim().ifBlank { "Lapangan Gudep" },
            description = description.trim().ifBlank { "Kegiatan kepramukaan satuan." },
            attended = false
        )
        persistProgress(current, prog.copy(activities = listOf(item) + prog.activities), 10)
    }

    fun exportBackupJson(): String {
        val s = session.value ?: UserSession()
        return repository.buildExportJson(s, progress.value)
    }

    fun importBackupJson(rawJson: String) {
        val current = session.value ?: return
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            repository.importBackupJson(current, rawJson).onSuccess { (_, outcome) ->
                _syncStatus.value = resolveSyncStatus(current.authProvider, outcome)
                _feedbackMessage.value = if (outcome.queuedOffline) {
                    "Data cadangan JSON dipulihkan lokal & masuk antrean sinkronisasi."
                } else {
                    "Data cadangan JSON berhasil dipulihkan."
                }
            }.onFailure { err ->
                _syncStatus.value = SyncStateStatus.ERROR
                _feedbackMessage.value = err.message ?: "Gagal mengimpor file JSON."
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Gagal membersihkan Credential Manager: ${e.message}")
                }
                try {
                    Firebase.auth.signOut()
                } catch (e: Exception) {
                    Log.w("Auth", "Firebase signOut warning: ${e.message}")
                }
                repository.logout()
            }
        }
    }

    private fun persistProgress(session: UserSession, updatedProgress: UserProgressPayload, xpDelta: Int) {
        viewModelScope.launch {
            _syncStatus.value = SyncStateStatus.SYNCING
            val (updated, outcome) = repository.saveProgressAndSync(session, updatedProgress, xpDelta)
            _syncStatus.value = resolveSyncStatus(updated.authProvider, outcome)
        }
    }

    private fun resolveSyncStatus(authProvider: AuthProviderType, outcome: SyncAttemptOutcome?): SyncStateStatus {
        return when {
            outcome?.queuedOffline == true -> SyncStateStatus.QUEUED_OFFLINE
            authProvider == AuthProviderType.GOOGLE -> SyncStateStatus.SYNCED_CLOUD_AND_DRIVE
            else -> SyncStateStatus.SYNCED_LOCAL_FALLBACK
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainScoutViewModel(ScoutRepository(context.applicationContext)) as T
            }
        }
    }
}
