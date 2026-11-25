package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ScoutCatalog
import com.example.model.ScoutMaterial
import com.example.ui.components.BentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.OutlineWarm

@Composable
fun MateriScreen(
    gudepMaterials: List<ScoutMaterial>,
    readMaterialIds: Set<String>,
    cachedMaterialIds: Set<String> = emptySet(),
    onMaterialOpened: (ScoutMaterial) -> Unit = {},
    onToggleRead: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var openedMaterial by remember { mutableStateOf<ScoutMaterial?>(null) }

    LaunchedEffect(openedMaterial?.id) {
        openedMaterial?.let { onMaterialOpened(it) }
    }

    val filters = listOf("Semua", "Siaga", "Penggalang", "Penegak", "Dari Pembina")

    val combinedMaterials = remember(gudepMaterials) {
        gudepMaterials.filter { it.published } + ScoutCatalog.builtInMaterials
    }

    val filtered = remember(combinedMaterials, searchQuery, selectedFilter) {
        combinedMaterials.filter { mat ->
            val matchesQuery = searchQuery.isBlank() ||
                mat.title.contains(searchQuery, ignoreCase = true) ||
                mat.category.contains(searchQuery, ignoreCase = true) ||
                mat.summary.contains(searchQuery, ignoreCase = true)
            val matchesChip = when (selectedFilter) {
                "Semua" -> true
                "Dari Pembina" -> mat.fromPembina
                else -> mat.jenjangTarget.equals("Semua", true) ||
                    mat.jenjangTarget.equals(selectedFilter, true)
            }
            matchesQuery && matchesChip
        }
    }

    if (openedMaterial != null) {
        val current = openedMaterial!!
        val isRead = current.id in readMaterialIds
        BackHandler { openedMaterial = null }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KremHangat)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { openedMaterial = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke daftar materi",
                            tint = CokelatTua
                        )
                    }
                    Text(
                        text = "Detail Materi Pramuka",
                        style = MaterialTheme.typography.titleMedium,
                        color = CokelatTua
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = HijauAlamSoft) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OfflinePin,
                            contentDescription = null,
                            tint = HijauAlam,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Tersimpan Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = HijauAlam
                        )
                    }
                }
            }

            BentoCard(containerColor = Color.White) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(50), color = KremHangat, border = BorderStroke(1.dp, OutlineWarm)) {
                        Text(
                            text = current.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = CokelatTua,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = HijauAlamSoft) {
                        Text(
                            text = if (current.fromPembina) "Materi Pembina" else current.jenjangTarget,
                            style = MaterialTheme.typography.labelSmall,
                            color = HijauAlam,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CokelatTua
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = current.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CokelatSeragam
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = current.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CokelatTua
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onToggleRead(current.id) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRead) HijauAlam else CokelatTua
                    )
                ) {
                    Icon(
                        imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRead) "Selesai Dibaca (+15 XP)" else "Tandai Selesai Dibaca")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Akademi Pramuka",
                subtitle = "Materi kepramukaan terstruktur & pembaruan dari Pembina"
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari materi (PBB, P3K, Tunas Kelapa, Kompas...)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("materi_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { chip ->
                    val selected = selectedFilter == chip
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = chip },
                        label = { Text(chip) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CokelatTua,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                BentoCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = CokelatSeragam,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Materi tidak ditemukan", style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                        Text(
                            "Coba ubah kata kunci pencarian atau pilih filter Semua.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { mat ->
                val isRead = mat.id in readMaterialIds
                val isCached = mat.id in cachedMaterialIds
                BentoCard(
                    onClick = {
                        onMaterialOpened(mat)
                        openedMaterial = mat
                    },
                    containerColor = Color.White,
                    borderColor = if (isRead) HijauAlam.copy(alpha = 0.4f) else OutlineWarm
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (mat.fromPembina) HijauAlamSoft else KremHangat
                            ) {
                                Text(
                                    text = if (mat.fromPembina) "Pembina • ${mat.category}" else mat.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (mat.fromPembina) HijauAlam else CokelatTua,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = KremHangat) {
                                Text(
                                    text = mat.jenjangTarget,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CokelatSeragam,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCached) {
                                Icon(
                                    imageVector = Icons.Filled.OfflinePin,
                                    contentDescription = "Tersimpan di cache offline",
                                    tint = HijauAlam,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (isRead) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Sudah dibaca",
                                    tint = HijauAlam,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = mat.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CokelatTua
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = mat.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
