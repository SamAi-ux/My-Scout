package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Mengelola cache offline untuk konten materi dan data tugas yang pernah dibuka pengguna.
 * Menyimpan salinan di Room (`offline_opened_cache`) sekaligus cerminan berkas JSON di penyimpanan internal
 * agar konten tetap terbaca cepat saat aplikasi dijalankan tanpa koneksi internet.
 */
class OfflineContentCacheStore(
    private val context: Context,
    private val cachedDao: CachedOpenedItemDao
) {
    private val cacheDir: File by lazy {
        File(context.applicationContext.filesDir, "offline_cache").apply { mkdirs() }
    }

    private val diskCacheFile: File by lazy {
        File(cacheDir, "opened_materials_tasks_cache.json")
    }

    val openedMaterialsFlow: Flow<List<ScoutMaterial>> =
        cachedDao.observeByType(TYPE_MATERIAL).map { rows -> rows.map { it.toMaterial() } }

    val openedTasksFlow: Flow<List<ScoutTask>> =
        cachedDao.observeByType(TYPE_TASK).map { rows -> rows.map { it.toTask() } }

    val openedMaterialIdsFlow: Flow<Set<String>> =
        cachedDao.observeByType(TYPE_MATERIAL).map { rows -> rows.map { it.itemId }.toSet() }

    val openedTaskIdsFlow: Flow<Set<String>> =
        cachedDao.observeByType(TYPE_TASK).map { rows -> rows.map { it.itemId }.toSet() }

    val totalCachedCountFlow: Flow<Int> =
        cachedDao.observeAll().map { it.size }

    suspend fun cacheOpenedMaterial(material: ScoutMaterial) = withContext(Dispatchers.IO) {
        val entity = CachedOpenedItemEntity(
            cacheKey = "$TYPE_MATERIAL:${material.id}",
            itemType = TYPE_MATERIAL,
            itemId = material.id,
            title = material.title,
            category = material.category,
            jenjangTarget = material.jenjangTarget,
            summary = material.summary,
            body = material.content,
            gudepId = material.gudepId,
            ownerId = material.ownerId,
            fromPembina = material.fromPembina,
            openedAtMillis = System.currentTimeMillis()
        )
        cachedDao.upsert(entity)
        persistDiskSnapshot()
    }

    suspend fun cacheOpenedTask(task: ScoutTask) = withContext(Dispatchers.IO) {
        val entity = CachedOpenedItemEntity(
            cacheKey = "$TYPE_TASK:${task.id}",
            itemType = TYPE_TASK,
            itemId = task.id,
            title = task.title,
            jenjangTarget = task.jenjangTarget,
            body = task.description,
            dueDateLabel = task.dueDateLabel,
            xpReward = task.xpReward,
            gudepId = task.gudepId,
            ownerId = task.ownerId,
            openedAtMillis = System.currentTimeMillis()
        )
        cachedDao.upsert(entity)
        persistDiskSnapshot()
    }

    suspend fun hydrateFromDiskCacheIfNeeded() = withContext(Dispatchers.IO) {
        try {
            if (!diskCacheFile.exists()) return@withContext
            val existingMaterials = cachedDao.getByType(TYPE_MATERIAL)
            val existingTasks = cachedDao.getByType(TYPE_TASK)
            if (existingMaterials.isNotEmpty() || existingTasks.isNotEmpty()) return@withContext

            val raw = diskCacheFile.readText(Charsets.UTF_8)
            val root = JSONObject(raw)
            val arr = root.optJSONArray("items") ?: return@withContext
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cachedDao.upsert(
                    CachedOpenedItemEntity(
                        cacheKey = o.optString("cacheKey"),
                        itemType = o.optString("itemType"),
                        itemId = o.optString("itemId"),
                        title = o.optString("title"),
                        category = o.optString("category"),
                        jenjangTarget = o.optString("jenjangTarget", "Semua"),
                        summary = o.optString("summary"),
                        body = o.optString("body"),
                        dueDateLabel = o.optString("dueDateLabel"),
                        xpReward = o.optInt("xpReward", 0),
                        gudepId = o.optString("gudepId"),
                        ownerId = o.optString("ownerId"),
                        fromPembina = o.optBoolean("fromPembina", false),
                        openedAtMillis = o.optLong("openedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("OfflineCache", "Gagal membaca cache disk: ${e.message}")
        }
    }

    private suspend fun persistDiskSnapshot() {
        try {
            val allItems = cachedDao.getByType(TYPE_MATERIAL) + cachedDao.getByType(TYPE_TASK)
            val jsonArr = JSONArray()
            allItems.forEach { e ->
                jsonArr.put(
                    JSONObject().apply {
                        put("cacheKey", e.cacheKey)
                        put("itemType", e.itemType)
                        put("itemId", e.itemId)
                        put("title", e.title)
                        put("category", e.category)
                        put("jenjangTarget", e.jenjangTarget)
                        put("summary", e.summary)
                        put("body", e.body)
                        put("dueDateLabel", e.dueDateLabel)
                        put("xpReward", e.xpReward)
                        put("gudepId", e.gudepId)
                        put("ownerId", e.ownerId)
                        put("fromPembina", e.fromPembina)
                        put("openedAtMillis", e.openedAtMillis)
                    }
                )
            }
            val root = JSONObject().apply {
                put("cachedAtMillis", System.currentTimeMillis())
                put("items", jsonArr)
            }
            diskCacheFile.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w("OfflineCache", "Gagal menulis cache disk: ${e.message}")
        }
    }

    companion object {
        const val TYPE_MATERIAL = "MATERIAL"
        const val TYPE_TASK = "TASK"
    }
}

private fun CachedOpenedItemEntity.toMaterial() = ScoutMaterial(
    id = itemId,
    title = title,
    category = category.ifBlank { "Materi Tersimpan" },
    jenjangTarget = jenjangTarget,
    summary = summary,
    content = body,
    fromPembina = fromPembina,
    published = true,
    gudepId = gudepId,
    ownerId = ownerId
)

private fun CachedOpenedItemEntity.toTask() = ScoutTask(
    id = itemId,
    title = title,
    description = body,
    jenjangTarget = jenjangTarget,
    dueDateLabel = dueDateLabel.ifBlank { "Minggu Ini" },
    xpReward = xpReward,
    gudepId = gudepId,
    ownerId = ownerId
)
