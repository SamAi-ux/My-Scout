package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ScoutCatalog
import com.example.data.remote.UserProgressPayload
import com.example.model.GudepMatchStatus
import com.example.model.JenjangPramuka
import com.example.model.ScoutNewsItem
import com.example.model.ScoutTask
import com.example.model.UserSession
import com.example.model.WeatherSnapshot
import com.example.ui.components.BentoCard
import com.example.ui.components.GudepLogoView
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberScoutSoft
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatSeragamSoft
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.MerahLencanaSoft
import com.example.ui.theme.OutlineWarm
import kotlin.math.roundToInt

@Composable
fun BerandaScreen(
    session: UserSession,
    progress: UserProgressPayload,
    gudepMatchStatus: GudepMatchStatus,
    tasks: List<ScoutTask>,
    cachedTaskIds: Set<String> = emptySet(),
    weather: WeatherSnapshot,
    onRefreshWeather: () -> Unit,
    onTaskOpened: (ScoutTask) -> Unit = {},
    onToggleTaskComplete: (ScoutTask) -> Unit,
    onNavigateToProfile: () -> Unit,
    onQuickActionSandi: () -> Unit,
    onQuickActionKuis: () -> Unit,
    onQuickActionSku: () -> Unit,
    onQuickActionKompas: () -> Unit
) {
    val todayDate = remember { ScoutCatalog.formattedIndonesianDateToday() }
    val (nearestHoliday, holidayTiming) = remember { ScoutCatalog.currentOrNearestHoliday() }
    val dasaDarmaToday = remember { ScoutCatalog.dasaDarmaForToday() }
    var openedTaskDetail by remember { mutableStateOf<ScoutTask?>(null) }

    val relevantSku = remember(session.jenjang) {
        val targetJenjang = if (session.jenjang.isPembinaRole) JenjangPramuka.PENGGALANG else session.jenjang
        ScoutCatalog.skuCatalog.filter { it.jenjang == targetJenjang }
    }
    val completedSkuCount = relevantSku.count { it.id in progress.completedSkuIds }
    val skuPercent = if (relevantSku.isEmpty()) 0 else ((completedSkuCount * 100f) / relevantSku.size).roundToInt()
    val level = (session.xp / 150) + 1
    val levelProgress = (session.xp % 150) / 150f

    val newsFeed = remember(gudepMatchStatus, tasks) {
        buildList {
            if (gudepMatchStatus is GudepMatchStatus.Matched) {
                add(
                    ScoutNewsItem(
                        id = "gudep_banner_news",
                        title = "Portal Resmi ${gudepMatchStatus.gudep.institutionName}",
                        summary = "Gudep ${gudepMatchStatus.gudep.gudepNumber} • ${gudepMatchStatus.gudep.address.ifBlank { "Aktif terhubung dengan Pembina" }}.",
                        sourceLabel = "Gugus Depan Anda",
                        dateLabel = "Aktif",
                        isGudepAnnouncement = true
                    )
                )
            }
            if (tasks.isNotEmpty()) {
                add(
                    ScoutNewsItem(
                        id = "gudep_task_update",
                        title = "${tasks.size} Tugas Tambahan Tersedia dari Pembina",
                        summary = "Periksa daftar tugas jenjang ${session.jenjang.label} di bagian bawah Beranda.",
                        sourceLabel = "Update Gudep",
                        dateLabel = "Terbaru",
                        isGudepAnnouncement = true
                    )
                )
            }
            addAll(ScoutCatalog.coreNews)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ============================================================
        // BAGIAN 1 (PALING ATAS): BERITA & UPDATE KEPRAMUKAAN
        // ============================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = "Berita & Update Kepramukaan",
                    subtitle = "Informasi Gugus Depan dan kabar Pramuka terkini"
                )

                // Gudep Context Banner or Warning
                when (gudepMatchStatus) {
                    is GudepMatchStatus.Matched -> {
                        val g = gudepMatchStatus.gudep
                        BentoCard(
                            containerColor = Color.White,
                            borderColor = HijauAlam.copy(alpha = 0.35f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GudepLogoView(
                                    logoBase64 = g.logoBase64,
                                    institutionName = g.institutionName,
                                    size = 52.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = g.institutionName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CokelatTua,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Gudep ${g.gudepNumber} • ${g.address.ifBlank { "Alamat Gudep terverifikasi" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = HijauAlamSoft
                                ) {
                                    Text(
                                        text = "Terhubung",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HijauAlam,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    is GudepMatchStatus.Unmatched -> {
                        BentoCard(
                            containerColor = MerahLencanaSoft,
                            borderColor = MerahLencana.copy(alpha = 0.4f),
                            onClick = onNavigateToProfile
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Peringatan Gudep",
                                    tint = MerahLencana
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nomor Gudep ${gudepMatchStatus.enteredGudepNumber} Belum Terdaftar",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MerahLencana
                                    )
                                    Text(
                                        text = "Nomor Gudep belum didaftarkan oleh Pembina. Akses materi & tugas lembaga lain diblokir. Ketuk untuk perbarui Profil.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CokelatTua
                                    )
                                }
                            }
                        }
                    }
                    GudepMatchStatus.Unfilled -> {
                        BentoCard(
                            containerColor = AmberScoutSoft,
                            borderColor = CokelatSeragam.copy(alpha = 0.4f),
                            onClick = onNavigateToProfile
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = "Lengkapi Data Diri",
                                    tint = CokelatTua
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lengkapi Formulir Data Diri & Gudep",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CokelatTua
                                    )
                                    Text(
                                        text = "Masukkan Nama Lembaga dan Nomor Gudep di Profil agar terhubung otomatis ke satuan Anda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CokelatTua
                                    )
                                }
                            }
                        }
                    }
                }

                // Hero Editorial Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scout_hero_1795584338572),
                        contentDescription = "Suasana Perkemahan My Scout",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC2C1A15))
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = HijauAlam
                            ) {
                                Text(
                                    text = "Satyaku Kudarmakan, Darmaku Kubaktikan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Halo, ${session.fullName.ifBlank { session.username }}!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Horizontal News Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(newsFeed, key = { it.id }) { news ->
                        Surface(
                            modifier = Modifier.width(276.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (news.isGudepAnnouncement) HijauAlam.copy(alpha = 0.4f) else OutlineWarm
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Newspaper,
                                            contentDescription = null,
                                            tint = if (news.isGudepAnnouncement) HijauAlam else CokelatTua,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = news.sourceLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (news.isGudepAnnouncement) HijauAlam else CokelatSeragam,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = news.dateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = news.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CokelatTua,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = news.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // BAGIAN 2: CUACA, TANGGAL INDONESIA & HARI BESAR KEPRAMUKAAN
        // ============================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Cuaca, Tanggal & Peringatan",
                    subtitle = "Informasi harian untuk persiapan latihan lapangan"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Weather Bento Card
                    BentoCard(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.WbSunny,
                                    contentDescription = "Cuaca",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = weather.locationName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CokelatTua
                                )
                            }
                            IconButton(
                                onClick = onRefreshWeather,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Muat ulang cuaca",
                                    tint = CokelatSeragam,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${weather.temperatureC.roundToInt()}°C",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CokelatTua
                        )
                        Text(
                            text = weather.conditionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = HijauAlam
                        )
                        Text(
                            text = "Lembap ${weather.humidityPercent}% • Angin ${weather.windKmh.roundToInt()} km/j",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Date & Indonesian Scout Holiday Card
                    BentoCard(
                        modifier = Modifier.weight(1.15f),
                        containerColor = CokelatSeragamSoft
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Tanggal Hari Ini",
                                tint = CokelatTua,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = todayDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = CokelatTua,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MerahLencana
                        ) {
                            Text(
                                text = "Hari Besar • $holidayTiming",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = nearestHoliday.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = CokelatTua
                        )
                        Text(
                            text = nearestHoliday.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ============================================================
        // BAGIAN 3: PROGRES ANGGOTA, DASA DARMA HARI INI & AKSES CEPAT
        // ============================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = "Progres & Dasa Darma",
                    subtitle = "Pantau capaian SKU, XP, dan amalan harian"
                )

                // Member Progress Bento
                BentoCard(containerColor = Color.White) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Level $level • ${session.xp} XP",
                                style = MaterialTheme.typography.titleMedium,
                                color = CokelatTua
                            )
                            Text(
                                text = "SKU ${session.jenjang.label}: $completedSkuCount/${relevantSku.size} poin ($skuPercent%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HijauAlamSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stars,
                                    contentDescription = null,
                                    tint = HijauAlam,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$skuPercent%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HijauAlam
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = HijauAlam,
                        trackColor = OutlineWarm
                    )
                }

                // Dasa Darma Hari Ini Card
                BentoCard(
                    containerColor = CokelatTua,
                    borderColor = CokelatTua
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DASA DARMA HARI INI",
                            style = MaterialTheme.typography.labelMedium,
                            color = CokelatSeragam
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Poin ke-${dasaDarmaToday.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = dasaDarmaToday.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Penerapan: ${dasaDarmaToday.practiceTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KremHangat.copy(alpha = 0.9f)
                    )
                }

                // Quick Access Grid (4 Actions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = "Sandi & Morse",
                        icon = Icons.Filled.GraphicEq,
                        tint = CokelatTua,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_sandi"),
                        onClick = onQuickActionSandi
                    )
                    QuickActionTile(
                        label = "Kuis",
                        icon = Icons.Filled.Quiz,
                        tint = MerahLencana,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_kuis"),
                        onClick = onQuickActionKuis
                    )
                    QuickActionTile(
                        label = "SKU & TKK",
                        icon = Icons.Filled.AssignmentTurnedIn,
                        tint = HijauAlam,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_sku"),
                        onClick = onQuickActionSku
                    )
                    QuickActionTile(
                        label = "Kompas",
                        icon = Icons.Filled.Explore,
                        tint = Color(0xFFD97706),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_kompas"),
                        onClick = onQuickActionKompas
                    )
                }
            }
        }

        // ============================================================
        // BAGIAN 4 (PALING BAWAH): NOTIFIKASI TUGAS SESUAI JENJANG
        // ============================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Notifikasi Tugas (${session.jenjang.label})",
                    subtitle = "Tugas khusus jenjang Anda dari Gugus Depan terdaftar"
                )
            }
        }

        if (tasks.isEmpty()) {
            item {
                BentoCard(containerColor = Color.White) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = CokelatSeragam,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Belum ada tugas tambahan aktif",
                                style = MaterialTheme.typography.titleSmall,
                                color = CokelatTua
                            )
                            Text(
                                text = if (gudepMatchStatus is GudepMatchStatus.Matched) {
                                    "Pembina belum mengirim tugas baru untuk jenjang ${session.jenjang.label}."
                                } else {
                                    "Cocokkan Nomor Gudep di Profil untuk menerima tugas dari Pembina Anda."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                val isDone = task.id in progress.completedTaskIds
                val isCached = task.id in cachedTaskIds
                BentoCard(
                    onClick = {
                        onTaskOpened(task)
                        openedTaskDetail = task
                    },
                    containerColor = if (isDone) HijauAlamSoft.copy(alpha = 0.45f) else Color.White,
                    borderColor = if (isDone) HijauAlam.copy(alpha = 0.4f) else OutlineWarm
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDone) HijauAlam else MerahLencanaSoft
                            ) {
                                Text(
                                    text = if (isDone) "Selesai" else "Belum Selesai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDone) Color.White else MerahLencana,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (isCached) {
                                Icon(
                                    imageVector = Icons.Filled.OfflinePin,
                                    contentDescription = "Tugas tersimpan offline",
                                    tint = HijauAlam,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = CokelatSeragam,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Tenggat: ${task.dueDateLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CokelatTua
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target: ${task.jenjangTarget} • +${task.xpReward} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = HijauAlam
                        )
                        if (isDone) {
                            OutlinedButton(
                                onClick = { onToggleTaskComplete(task) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HijauAlam, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Batalkan", color = HijauAlam)
                            }
                        } else {
                            Button(
                                onClick = { onToggleTaskComplete(task) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HijauAlam)
                            ) {
                                Text("Tandai Selesai")
                            }
                        }
                    }
                }
            }
        }
    }

    openedTaskDetail?.let { task ->
        val isDone = task.id in progress.completedTaskIds
        AlertDialog(
            onDismissRequest = { openedTaskDetail = null },
            containerColor = Color.White,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = HijauAlamSoft) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.OfflinePin, contentDescription = null, tint = HijauAlam, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tersimpan Offline", style = MaterialTheme.typography.labelSmall, color = HijauAlam)
                        }
                    }
                    Text(task.title, style = MaterialTheme.typography.titleLarge, color = CokelatTua)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Target: ${task.jenjangTarget} • Tenggat: ${task.dueDateLabel} • +${task.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = HijauAlam
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CokelatTua
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleTaskComplete(task)
                        openedTaskDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HijauAlam)
                ) {
                    Text(if (isDone) "Batalkan Selesai" else "Tandai Selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { openedTaskDetail = null }) {
                    Text("Tutup", color = CokelatTua)
                }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OutlineWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CokelatTua,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
