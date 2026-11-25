package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.model.FirestoreGudep
import com.example.model.FirestoreMaterial
import com.example.model.FirestoreTask
import com.example.model.FirestoreUserProfile
import com.example.model.GudepProfile
import com.example.model.JenjangPramuka
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.example.model.UserSession
import com.example.model.sanitizeGudepId
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun currentUidOrNull(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun requireUserId(): String {
        return currentUidOrNull()
            ?: throw IllegalStateException("Pengguna belum masuk dengan akun Google.")
    }

    suspend fun loadUserProfile(): Result<FirestoreUserProfile?> {
        val uid = currentUidOrNull() ?: return Result.success(null)
        val docPath = "users/$uid"
        return try {
            val snap = db.collection("users").document(uid).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(FirestoreUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertUserProfile(session: UserSession): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeFullName = session.fullName.trim().take(100).ifBlank { "Anggota Pramuka" }
            val safeUsername = session.username.trim().take(50).ifBlank { "pramuka" }
            val safeEmail = session.email.trim().take(120)
            val safeInst = session.institutionName.trim().take(120)
            val safeGudep = session.gudepNumber.trim().take(40)
            val safeUnit = session.unitName.trim().take(80)
            val safeXp = session.xp.coerceIn(0, 1_000_000).toLong()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to safeFullName,
                    "username" to safeUsername,
                    "email" to safeEmail,
                    "jenjang" to session.jenjang.label,
                    "institutionName" to safeInst,
                    "gudepNumber" to safeGudep,
                    "unitName" to safeUnit,
                    "xp" to safeXp,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "fullName" to safeFullName,
                        "username" to safeUsername,
                        "email" to safeEmail,
                        "jenjang" to session.jenjang.label,
                        "institutionName" to safeInst,
                        "gudepNumber" to safeGudep,
                        "unitName" to safeUnit,
                        "xp" to safeXp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncBackupJson(payloadJson: String): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val docPath = "users/$uid/backups/latest"
        return try {
            val docRef = db.collection("users").document(uid).collection("backups").document("latest")
            val existing = docRef.get().await()
            val boundedJson = payloadJson.take(490_000)
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "payloadJson" to boundedJson,
                        "deviceLabel" to "Android",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "payloadJson" to boundedJson,
                        "deviceLabel" to "Android",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun loadLatestBackupJson(): Result<String?> {
        val uid = currentUidOrNull() ?: return Result.success(null)
        val docPath = "users/$uid/backups/latest"
        return try {
            val snap = db.collection("users").document(uid).collection("backups").document("latest").get().await()
            Result.success(snap.getString("payloadJson"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertGudep(profile: GudepProfile): Result<GudepProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val gudepId = sanitizeGudepId(profile.gudepNumber)
        val docPath = "gudep/$gudepId"
        return try {
            val docRef = db.collection("gudep").document(gudepId)
            val existing = docRef.get().await()
            val cleanInst = profile.institutionName.trim().take(120)
            val cleanNum = profile.gudepNumber.trim().take(40)
            val cleanAddr = profile.address.trim().take(250)
            val cleanOwnerName = profile.ownerName.trim().take(100).ifBlank { "Pembina" }
            val cleanLogo = profile.logoBase64?.take(195_000)

            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "ownerId" to uid,
                    "ownerName" to cleanOwnerName,
                    "institutionName" to cleanInst,
                    "gudepNumber" to cleanNum,
                    "address" to cleanAddr,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanLogo != null) {
                    payload["logoBase64"] = cleanLogo
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "ownerName" to cleanOwnerName,
                    "institutionName" to cleanInst,
                    "gudepNumber" to cleanNum,
                    "address" to cleanAddr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanLogo != null) {
                    updates["logoBase64"] = cleanLogo
                } else if (existing.contains("logoBase64")) {
                    updates["logoBase64"] = FieldValue.delete()
                }
                docRef.update(updates).await()
            }

            Result.success(
                profile.copy(
                    gudepId = gudepId,
                    ownerId = uid,
                    ownerName = cleanOwnerName,
                    institutionName = cleanInst,
                    gudepNumber = cleanNum,
                    address = cleanAddr,
                    logoBase64 = cleanLogo,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchGudepByNumber(gudepNumber: String): Result<GudepProfile?> {
        if (currentUidOrNull() == null) return Result.success(null)
        val gudepId = sanitizeGudepId(gudepNumber)
        val docPath = "gudep/$gudepId"
        return try {
            val snap = db.collection("gudep").document(gudepId).get().await()
            if (!snap.exists()) return Result.success(null)
            val dto = snap.toObject(FirestoreGudep::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.success(null)
            Result.success(
                GudepProfile(
                    gudepId = dto.id.ifBlank { gudepId },
                    ownerId = dto.ownerId,
                    ownerName = dto.ownerName,
                    institutionName = dto.institutionName,
                    gudepNumber = dto.gudepNumber,
                    address = dto.address,
                    logoBase64 = dto.logoBase64,
                    updatedAtMillis = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    fun observeOwnedGudep(): Flow<List<GudepProfile>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "gudep"
        return db.collection("gudep")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreGudep::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { dto ->
                        GudepProfile(
                            gudepId = dto.id,
                            ownerId = dto.ownerId,
                            ownerName = dto.ownerName,
                            institutionName = dto.institutionName,
                            gudepNumber = dto.gudepNumber,
                            address = dto.address,
                            logoBase64 = dto.logoBase64,
                            updatedAtMillis = dto.updatedAt?.toDate()?.time ?: 0L
                        )
                    }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (currentUidOrNull() == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
            }
    }

    fun observeGudepMaterials(gudepId: String, includeDraftsIfOwner: Boolean): Flow<List<ScoutMaterial>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (gudepId.isBlank()) return emptyFlow()
        val path = "gudep/$gudepId/materials"
        val query = if (includeDraftsIfOwner) {
            db.collection("gudep").document(gudepId).collection("materials")
                .whereEqualTo("ownerId", uid)
        } else {
            db.collection("gudep").document(gudepId).collection("materials")
                .whereEqualTo("gudepId", gudepId)
                .whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreMaterial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { dto ->
                        ScoutMaterial(
                            id = dto.id,
                            title = dto.title,
                            category = dto.category,
                            jenjangTarget = dto.jenjangTarget,
                            summary = dto.summary,
                            content = dto.content,
                            fromPembina = true,
                            published = dto.published,
                            gudepId = dto.gudepId,
                            ownerId = dto.ownerId
                        )
                    }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (currentUidOrNull() == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
            }
    }

    suspend fun saveGudepMaterial(material: ScoutMaterial): Result<ScoutMaterial> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val gudepId = material.gudepId.ifBlank { return Result.failure(IllegalArgumentException("Gudep belum terdaftar.")) }
        val materialId = material.id.ifBlank { "mat_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docPath = "gudep/$gudepId/materials/$materialId"
        return try {
            val docRef = db.collection("gudep").document(gudepId).collection("materials").document(materialId)
            val existing = docRef.get().await()
            val title = material.title.trim().take(120)
            val category = material.category.trim().take(50).ifBlank { "Materi Gudep" }
            val target = material.jenjangTarget.ifBlank { "Semua" }
            val summary = material.summary.trim().take(300)
            val content = material.content.trim().take(10_000)

            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "gudepId" to gudepId,
                        "title" to title,
                        "category" to category,
                        "jenjangTarget" to target,
                        "summary" to summary,
                        "content" to content,
                        "published" to material.published,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to title,
                        "category" to category,
                        "jenjangTarget" to target,
                        "summary" to summary,
                        "content" to content,
                        "published" to material.published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(material.copy(id = materialId, ownerId = uid, gudepId = gudepId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeGudepTasks(gudepId: String): Flow<List<ScoutTask>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (gudepId.isBlank()) return emptyFlow()
        val path = "gudep/$gudepId/tasks"
        return db.collection("gudep").document(gudepId).collection("tasks")
            .whereEqualTo("gudepId", gudepId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { dto ->
                        ScoutTask(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description,
                            jenjangTarget = dto.jenjangTarget,
                            dueDateLabel = dto.dueDateLabel,
                            xpReward = dto.xpReward.toInt(),
                            gudepId = dto.gudepId,
                            ownerId = dto.ownerId
                        )
                    }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (currentUidOrNull() == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
            }
    }

    suspend fun saveGudepTask(task: ScoutTask): Result<ScoutTask> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val gudepId = task.gudepId.ifBlank { return Result.failure(IllegalArgumentException("Gudep belum terdaftar.")) }
        val taskId = task.id.ifBlank { "task_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docPath = "gudep/$gudepId/tasks/$taskId"
        return try {
            val docRef = db.collection("gudep").document(gudepId).collection("tasks").document(taskId)
            val existing = docRef.get().await()
            val title = task.title.trim().take(120)
            val desc = task.description.trim().take(2000)
            val target = task.jenjangTarget.ifBlank { "Semua" }
            val due = task.dueDateLabel.trim().take(40).ifBlank { "Minggu Ini" }
            val xp = task.xpReward.coerceIn(0, 1000).toLong()

            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "gudepId" to gudepId,
                        "title" to title,
                        "description" to desc,
                        "jenjangTarget" to target,
                        "dueDateLabel" to due,
                        "xpReward" to xp,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to title,
                        "description" to desc,
                        "jenjangTarget" to target,
                        "dueDateLabel" to due,
                        "xpReward" to xp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(task.copy(id = taskId, ownerId = uid, gudepId = gudepId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
