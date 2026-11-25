package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.model.PackingItem
import com.example.model.ReguMember
import com.example.model.ScoutActivityItem
import com.example.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

data class UserProgressPayload(
    val readMaterialIds: Set<String> = emptySet(),
    val completedTaskIds: Set<String> = emptySet(),
    val completedSkuIds: Set<String> = emptySet(),
    val earnedTkkIds: Set<String> = emptySet(),
    val quizBestScore: Int = 0,
    val quizAttempts: Int = 0,
    val packingList: List<PackingItem> = emptyList(),
    val reguMembers: List<ReguMember> = emptyList(),
    val activities: List<ScoutActivityItem> = emptyList()
)

interface GoogleDriveRestApi {
    @GET("drive/v3/files")
    suspend fun listBackupFiles(
        @Header("Authorization") bearerToken: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("q") query: String = "name = 'my_scout_backup.json' and trashed = false",
        @Query("fields") fields: String = "files(id,name,modifiedTime)"
    ): Response<String>
}

sealed interface DriveSyncResult {
    data class CloudSynced(val fileId: String, val timestampMillis: Long) : DriveSyncResult
    data class LocalMirrorSynced(val localPath: String, val timestampMillis: Long) : DriveSyncResult
}

/**
 * Arsitektur sinkronisasi latar belakang ke Google Drive API v3 (`appDataFolder`).
 * Jika token akses OAuth Drive aktif belum tersedia (misalnya login manual atau tanpa izin scope Drive),
 * sistem otomatis menyimpan cerminan JSON identik ke penyimpanan internal aman sebagai fallback lokal.
 */
class GoogleDriveSyncService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Suppress("unused")
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(client)
        .build()

    suspend fun syncBackupSilently(
        session: UserSession,
        progress: UserProgressPayload,
        oauthAccessToken: String? = null
    ): DriveSyncResult = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val backupJson = serializeBackupJson(session, progress, timestamp)

        // Always persist local Drive mirror first so zero data is ever lost
        val mirrorDir = File(context.filesDir, "drive_sync_mirror").apply { mkdirs() }
        val safeKey = session.sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default" }
        val mirrorFile = File(mirrorDir, "my_scout_backup_$safeKey.json")
        mirrorFile.writeText(backupJson, Charsets.UTF_8)

        // Attempt live Google Drive REST v3 upload if OAuth token with drive.appdata scope is provided
        if (!oauthAccessToken.isNullOrBlank()) {
            try {
                val existingFileId = findExistingBackupFileId(oauthAccessToken)
                val uploadedId = uploadOrUpdateDriveFile(oauthAccessToken, existingFileId, backupJson)
                if (uploadedId != null) {
                    return@withContext DriveSyncResult.CloudSynced(uploadedId, timestamp)
                }
            } catch (e: Exception) {
                Log.w("DriveSync", "Google Drive API fallback to local mirror: ${e.message}")
            }
        }

        DriveSyncResult.LocalMirrorSynced(mirrorFile.absolutePath, timestamp)
    }

    suspend fun restoreFromLocalMirror(sessionId: String): Pair<UserSession?, UserProgressPayload?> =
        withContext(Dispatchers.IO) {
            val mirrorDir = File(context.filesDir, "drive_sync_mirror")
            val safeKey = sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default" }
            val mirrorFile = File(mirrorDir, "my_scout_backup_$safeKey.json")
            if (!mirrorFile.exists()) return@withContext null to null
            deserializeBackupJson(mirrorFile.readText(Charsets.UTF_8))
        }

    private fun findExistingBackupFileId(token: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=name%3D%27my_scout_backup.json%27%20and%20trashed%3Dfalse&fields=files(id,name)"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        }
    }

    private fun uploadOrUpdateDriveFile(
        token: String,
        existingFileId: String?,
        jsonPayload: String
    ): String? {
        val metadataJson = JSONObject().apply {
            put("name", "my_scout_backup.json")
            if (existingFileId == null) {
                put("parents", JSONArray(listOf("appDataFolder")))
            }
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .addPart(
                jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        val uploadUrl = if (existingFileId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
        }

        val reqBuilder = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", "Bearer $token")
        val request = if (existingFileId == null) {
            reqBuilder.post(multipartBody).build()
        } else {
            reqBuilder.patch(multipartBody).build()
        }

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            return JSONObject(body).optString("id").takeIf { it.isNotBlank() }
        }
    }

    fun serializeBackupJson(
        session: UserSession,
        progress: UserProgressPayload,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("appName", "My Scout")
        root.put("exportedAtMillis", timestampMillis)

        root.put("profile", JSONObject().apply {
            put("sessionId", session.sessionId)
            put("fullName", session.fullName)
            put("username", session.username)
            put("email", session.email)
            put("jenjang", session.jenjang.label)
            put("hasSelectedJenjang", session.hasCompletedJenjangSelection)
            put("institutionName", session.institutionName)
            put("gudepNumber", session.gudepNumber)
            put("unitName", session.unitName)
            put("xp", session.xp)
        })

        root.put("readMaterialIds", JSONArray(progress.readMaterialIds.toList()))
        root.put("completedTaskIds", JSONArray(progress.completedTaskIds.toList()))
        root.put("completedSkuIds", JSONArray(progress.completedSkuIds.toList()))
        root.put("earnedTkkIds", JSONArray(progress.earnedTkkIds.toList()))
        root.put("quizBestScore", progress.quizBestScore)
        root.put("quizAttempts", progress.quizAttempts)

        root.put("packingList", JSONArray().apply {
            progress.packingList.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("checked", item.checked)
                })
            }
        })

        root.put("reguMembers", JSONArray().apply {
            progress.reguMembers.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("role", m.role)
                    put("jenjang", m.jenjang)
                    put("present", m.present)
                })
            }
        })

        root.put("activities", JSONArray().apply {
            progress.activities.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("dateLabel", a.dateLabel)
                    put("location", a.location)
                    put("description", a.description)
                    put("attended", a.attended)
                })
            }
        })

        return root.toString(2)
    }

    fun deserializeBackupJson(rawJson: String): Pair<UserSession?, UserProgressPayload?> {
        return try {
            val root = JSONObject(rawJson)
            val profileObj = root.optJSONObject("profile")
            val parsedSession = profileObj?.let { p ->
                UserSession(
                    sessionId = p.optString("sessionId"),
                    fullName = p.optString("fullName"),
                    username = p.optString("username"),
                    email = p.optString("email"),
                    jenjang = com.example.model.JenjangPramuka.fromLabel(p.optString("jenjang")),
                    hasCompletedJenjangSelection = p.optBoolean("hasSelectedJenjang", true),
                    institutionName = p.optString("institutionName"),
                    gudepNumber = p.optString("gudepNumber"),
                    unitName = p.optString("unitName"),
                    xp = p.optInt("xp", 0)
                )
            }

            fun readStringSet(key: String): Set<String> {
                val arr = root.optJSONArray(key) ?: return emptySet()
                return buildSet {
                    for (i in 0 until arr.length()) {
                        val v = arr.optString(i)
                        if (v.isNotBlank()) add(v)
                    }
                }
            }

            val packing = buildList {
                val arr = root.optJSONArray("packingList")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            PackingItem(
                                id = o.optString("id"),
                                name = o.optString("name"),
                                category = o.optString("category"),
                                checked = o.optBoolean("checked", false)
                            )
                        )
                    }
                }
            }

            val members = buildList {
                val arr = root.optJSONArray("reguMembers")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            ReguMember(
                                id = o.optString("id"),
                                name = o.optString("name"),
                                role = o.optString("role"),
                                jenjang = o.optString("jenjang"),
                                present = o.optBoolean("present", true)
                            )
                        )
                    }
                }
            }

            val acts = buildList {
                val arr = root.optJSONArray("activities")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            ScoutActivityItem(
                                id = o.optString("id"),
                                title = o.optString("title"),
                                dateLabel = o.optString("dateLabel"),
                                location = o.optString("location"),
                                description = o.optString("description"),
                                attended = o.optBoolean("attended", false)
                            )
                        )
                    }
                }
            }

            val progress = UserProgressPayload(
                readMaterialIds = readStringSet("readMaterialIds"),
                completedTaskIds = readStringSet("completedTaskIds"),
                completedSkuIds = readStringSet("completedSkuIds"),
                earnedTkkIds = readStringSet("earnedTkkIds"),
                quizBestScore = root.optInt("quizBestScore", 0),
                quizAttempts = root.optInt("quizAttempts", 0),
                packingList = packing,
                reguMembers = members,
                activities = acts
            )

            parsedSession to progress
        } catch (e: Exception) {
            Log.w("DriveSync", "Failed to parse backup JSON: ${e.message}")
            null to null
        }
    }
}
