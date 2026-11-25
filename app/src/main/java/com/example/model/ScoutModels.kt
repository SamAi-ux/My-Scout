package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class JenjangPramuka(
    val label: String,
    val subtitle: String,
    val isPembinaRole: Boolean
) {
    SIAGA("Siaga", "Usia 7-10 tahun • Barung", false),
    PENGGALANG("Penggalang", "Usia 11-15 tahun • Regu", false),
    PENEGAK("Penegak", "Usia 16-20 tahun • Sangga", false),
    PEMBINA("Pembina", "Pembina Gugus Depan", true),
    PEMBINA_DAMPING("Pembina Damping", "Pendamping Satuan & Gudep", true);

    companion object {
        fun fromLabel(raw: String?): JenjangPramuka {
            return entries.firstOrNull { it.label.equals(raw?.trim(), ignoreCase = true) } ?: PENGGALANG
        }
    }
}

enum class AuthProviderType {
    GOOGLE,
    MANUAL
}

enum class SyncStateStatus {
    IDLE,
    SYNCING,
    QUEUED_OFFLINE,
    SYNCED_CLOUD_AND_DRIVE,
    SYNCED_LOCAL_FALLBACK,
    ERROR
}

data class UserSession(
    val sessionId: String = "",
    val authProvider: AuthProviderType = AuthProviderType.MANUAL,
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val jenjang: JenjangPramuka = JenjangPramuka.PENGGALANG,
    val hasCompletedJenjangSelection: Boolean = true,
    val institutionName: String = "",
    val gudepNumber: String = "",
    val unitName: String = "",
    val xp: Int = 0,
    val lastSyncedMillis: Long = 0L
) {
    val canAccessDashboard: Boolean
        get() = jenjang.isPembinaRole

    val isProfileComplete: Boolean
        get() = fullName.isNotBlank() &&
            username.isNotBlank() &&
            institutionName.isNotBlank() &&
            gudepNumber.isNotBlank() &&
            unitName.isNotBlank()
}

data class GudepProfile(
    val gudepId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val institutionName: String = "",
    val gudepNumber: String = "",
    val address: String = "",
    val logoBase64: String? = null,
    val updatedAtMillis: Long = 0L
)

sealed interface GudepMatchStatus {
    data object Unfilled : GudepMatchStatus
    data class Matched(val gudep: GudepProfile) : GudepMatchStatus
    data class Unmatched(val enteredGudepNumber: String, val enteredInstitution: String) : GudepMatchStatus
}

data class ScoutMaterial(
    val id: String,
    val title: String,
    val category: String,
    val jenjangTarget: String,
    val summary: String,
    val content: String,
    val fromPembina: Boolean = false,
    val published: Boolean = true,
    val gudepId: String = "",
    val ownerId: String = "",
    val readMinutes: Int = 4
)

data class ScoutTask(
    val id: String,
    val title: String,
    val description: String,
    val jenjangTarget: String,
    val dueDateLabel: String,
    val xpReward: Int,
    val gudepId: String = "",
    val ownerId: String = ""
)

data class ScoutNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val sourceLabel: String,
    val dateLabel: String,
    val isGudepAnnouncement: Boolean = false
)

data class ScoutHoliday(
    val name: String,
    val month: Int,
    val day: Int,
    val description: String
)

data class WeatherSnapshot(
    val locationName: String = "Jakarta",
    val temperatureC: Double = 29.0,
    val conditionLabel: String = "Cerah Berawan",
    val humidityPercent: Int = 68,
    val windKmh: Double = 11.0,
    val isLive: Boolean = false
)

data class DasaDarmaItem(
    val number: Int,
    val title: String,
    val practiceTip: String
)

data class SkuItem(
    val id: String,
    val jenjang: JenjangPramuka,
    val tier: String,
    val pointNumber: Int,
    val title: String,
    val description: String
)

data class TkkItem(
    val id: String,
    val name: String,
    val category: String,
    val colorLabel: String,
    val description: String
)

data class QuizQuestion(
    val id: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class PackingItem(
    val id: String,
    val name: String,
    val category: String,
    val checked: Boolean = false
)

data class ReguMember(
    val id: String,
    val name: String,
    val role: String,
    val jenjang: String,
    val present: Boolean = true
)

data class ScoutActivityItem(
    val id: String,
    val title: String,
    val dateLabel: String,
    val location: String,
    val description: String,
    val attended: Boolean = false
)

// Firestore DTOs (annotated for R8 & DocumentId safety)
@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val jenjang: String = JenjangPramuka.PENGGALANG.label,
    val institutionName: String = "",
    val gudepNumber: String = "",
    val unitName: String = "",
    val xp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGudep(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val institutionName: String = "",
    val gudepNumber: String = "",
    val address: String = "",
    val logoBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreMaterial(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val gudepId: String = "",
    val title: String = "",
    val category: String = "",
    val jenjangTarget: String = "Semua",
    val summary: String = "",
    val content: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTask(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val gudepId: String = "",
    val title: String = "",
    val description: String = "",
    val jenjangTarget: String = "Semua",
    val dueDateLabel: String = "",
    val xpReward: Long = 25L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun sanitizeGudepId(gudepNumber: String): String {
    val cleaned = gudepNumber.trim()
        .lowercase()
        .replace(Regex("[^a-z0-9_\\-]+"), "_")
        .trim('_')
    return cleaned.ifBlank { "gudep_default" }.take(64)
}
