package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AuthProviderType
import com.example.model.GudepMatchStatus
import com.example.model.GudepProfile
import com.example.model.JenjangPramuka
import com.example.model.ScoutMaterial
import com.example.model.SyncStateStatus
import com.example.model.UserSession
import com.example.ui.components.BentoCard
import com.example.ui.components.GudepLogoView
import com.example.ui.components.JenjangBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberScoutSoft
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.MerahLencanaSoft
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    session: UserSession,
    gudepMatchStatus: GudepMatchStatus,
    syncStatus: SyncStateStatus,
    isOnline: Boolean = true,
    pendingSyncCount: Int = 0,
    totalCachedCount: Int = 0,
    onBack: () -> Unit,
    onSaveProfile: (
        fullName: String,
        username: String,
        jenjang: JenjangPramuka,
        institutionName: String,
        gudepNumber: String,
        unitName: String
    ) -> Unit,
    onOpenDashboard: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var fullName by remember(session.sessionId) { mutableStateOf(session.fullName) }
    var username by remember(session.sessionId) { mutableStateOf(session.username) }
    var selectedJenjang by remember(session.sessionId) { mutableStateOf(session.jenjang) }
    var institutionName by remember(session.sessionId) { mutableStateOf(session.institutionName) }
    var gudepNumber by remember(session.sessionId) { mutableStateOf(session.gudepNumber) }
    var unitName by remember(session.sessionId) { mutableStateOf(session.unitName) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = onExportJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!raw.isNullOrBlank()) {
                onImportJson(raw)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        // Subpage Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = CokelatTua)
                }
                Text("Profil & Data Diri", style = MaterialTheme.typography.titleLarge, color = CokelatTua)
            }
            JenjangBadge(jenjang = session.jenjang)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Pembina Dashboard Access Button (STRICTLY visible ONLY for Pembina & Pembina Damping)
            if (session.jenjang.isPembinaRole) {
                item {
                    BentoCard(
                        containerColor = CokelatTua,
                        borderColor = CokelatTua
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Panel Pembina Gugus Depan", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    "Kelola identitas lembaga, nomor Gudep, logo sekolah, materi, dan tugas tambahan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KremHangat.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onOpenDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_dashboard_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = CokelatTua
                            )
                        ) {
                            Text("Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Gudep Auto-Filter Verification Status
            item {
                when (gudepMatchStatus) {
                    is GudepMatchStatus.Matched -> {
                        val g = gudepMatchStatus.gudep
                        BentoCard(
                            containerColor = HijauAlamSoft,
                            borderColor = HijauAlam.copy(alpha = 0.45f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GudepLogoView(
                                    logoBase64 = g.logoBase64,
                                    institutionName = g.institutionName,
                                    size = 54.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Verified, contentDescription = null, tint = HijauAlam, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Gudep Terverifikasi", style = MaterialTheme.typography.labelMedium, color = HijauAlam)
                                    }
                                    Text(g.institutionName, style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                                    Text("Gudep ${g.gudepNumber} • ${g.address}", style = MaterialTheme.typography.bodySmall, color = CokelatTua)
                                }
                            }
                        }
                    }
                    is GudepMatchStatus.Unmatched -> {
                        BentoCard(
                            containerColor = MerahLencanaSoft,
                            borderColor = MerahLencana.copy(alpha = 0.45f)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MerahLencana)
                                Column {
                                    Text("Nomor Gudep Belum Didaftarkan", style = MaterialTheme.typography.titleSmall, color = MerahLencana)
                                    Text(
                                        "Nomor Gudep '${gudepMatchStatus.enteredGudepNumber}' untuk '${gudepMatchStatus.enteredInstitution}' belum didaftarkan oleh Pembina. Akses ke data lembaga lain dicegah.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CokelatTua
                                    )
                                }
                            }
                        }
                    }
                    GudepMatchStatus.Unfilled -> {
                        BentoCard(containerColor = AmberScoutSoft) {
                            Text(
                                text = "Formulir Data Diri Wajib Diisi",
                                style = MaterialTheme.typography.titleSmall,
                                color = CokelatTua
                            )
                            Text(
                                text = "Isi Nama Lembaga dan Nomor Gudep sesuai pendaftaran Pembina untuk mengakses ruang satuan Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CokelatTua
                            )
                        }
                    }
                }
            }

            // 3. Mandatory Personal Data Form
            item {
                BentoCard {
                    Text("Formulir Data Diri Anggota", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_fullname_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_username_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Jenjang Pramuka *", style = MaterialTheme.typography.labelMedium, color = CokelatTua)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JenjangPramuka.entries.forEach { j ->
                            FilterChip(
                                selected = selectedJenjang == j,
                                onClick = { selectedJenjang = j },
                                label = { Text(j.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CokelatTua,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = institutionName,
                        onValueChange = { institutionName = it },
                        label = { Text("Nama Lembaga / Sekolah *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_institution_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = gudepNumber,
                        onValueChange = { gudepNumber = it },
                        label = { Text("Nomor Gudep (mis. 01.001) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_gudep_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("Info Regu / Sangga / Kelas Pendukung") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_unit_input")
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onSaveProfile(
                                fullName,
                                username,
                                selectedJenjang,
                                institutionName,
                                gudepNumber,
                                unitName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("profile_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HijauAlam)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Data Diri & Sinkronkan")
                    }
                }
            }

            // 4. Silent Drive Sync, Offline Cache & JSON Backup Export/Import
            item {
                BentoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = if (pendingSyncCount > 0 || !isOnline) CokelatTua else HijauAlam
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Mode Offline & Antrean Sinkronisasi Drive", style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                            val syncDesc = when (syncStatus) {
                                SyncStateStatus.SYNCING -> "Menyinkronkan antrean ke Google Drive & cloud..."
                                SyncStateStatus.QUEUED_OFFLINE -> "Mode offline: $pendingSyncCount pembaruan masuk antrean otomatis Google Drive."
                                SyncStateStatus.SYNCED_CLOUD_AND_DRIVE -> "Aktif: Lokal + Cloud + Mirror JSON Drive tersinkron."
                                SyncStateStatus.SYNCED_LOCAL_FALLBACK -> "Aktif: Penyimpanan Lokal & Mirror JSON Drive siap."
                                SyncStateStatus.ERROR -> "Sinkronisasi cloud tertunda; antrean dijadwalkan ulang otomatis."
                                SyncStateStatus.IDLE -> if (session.authProvider == AuthProviderType.GOOGLE) {
                                    "Sinkronisasi latar belakang ke Google Drive aktif otomatis saat online."
                                } else {
                                    "Mode Lokal & Mirror Drive JSON otomatis setiap pembaruan data."
                                }
                            }
                            Text(syncDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = HijauAlamSoft) {
                            Text(
                                text = "Cache Offline: $totalCachedCount materi & tugas",
                                style = MaterialTheme.typography.labelSmall,
                                color = HijauAlam,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (pendingSyncCount > 0) AmberScoutSoft else KremHangat
                        ) {
                            Text(
                                text = if (pendingSyncCount > 0) {
                                    "Antrean Drive: $pendingSyncCount menunggu koneksi"
                                } else if (!isOnline) {
                                    "Koneksi Offline • Cache Siap"
                                } else {
                                    "Antrean Drive: Tersinkron"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CokelatTua,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("my_scout_backup.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ekspor JSON")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Impor JSON")
                        }
                    }
                }
            }

            // 5. Logout Button
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MerahLencana,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout dari My Scout")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardPembinaScreen(
    session: UserSession,
    ownedGudep: GudepProfile?,
    gudepMaterials: List<ScoutMaterial>,
    onBack: () -> Unit,
    onSaveGudep: (institutionName: String, gudepNumber: String, address: String, logoBase64: String?) -> Unit,
    onSaveMaterial: (existingId: String, title: String, category: String, jenjangTarget: String, summary: String, content: String, published: Boolean) -> Unit,
    onSendTask: (title: String, description: String, jenjangTarget: String, dueDateLabel: String, xpReward: Int) -> Unit
) {
    BackHandler { onBack() }

    // Hard access block if non-Pembina somehow reaches this screen
    if (!session.canAccessDashboard) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KremHangat)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MerahLencana, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Akses Dashboard Ditolak", style = MaterialTheme.typography.headlineSmall, color = MerahLencana)
            Text(
                "Halaman Dashboard hanya tersedia untuk jenjang Pembina atau Pembina Damping.",
                style = MaterialTheme.typography.bodyMedium,
                color = CokelatTua
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Kembali") }
        }
        return
    }

    val context = LocalContext.current
    var activeSection by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = CokelatTua)
            }
            Column {
                Text("Dashboard Pembina", style = MaterialTheme.typography.titleLarge, color = CokelatTua)
                Text("${session.jenjang.label} • Pengelolaan Gudep", style = MaterialTheme.typography.bodySmall, color = CokelatSeragam)
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val tabs = listOf("Lembaga", "Materi", "Tugas")
            tabs.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = activeSection == idx,
                    onClick = { activeSection = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                    modifier = Modifier.testTag("dashboard_tab_$idx")
                ) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (activeSection) {
            0 -> {
                var instName by remember(ownedGudep) {
                    mutableStateOf(ownedGudep?.institutionName ?: session.institutionName)
                }
                var gudepNum by remember(ownedGudep) {
                    mutableStateOf(ownedGudep?.gudepNumber ?: session.gudepNumber)
                }
                var address by remember(ownedGudep) {
                    mutableStateOf(ownedGudep?.address.orEmpty())
                }
                var logoBase64 by remember(ownedGudep) {
                    mutableStateOf(ownedGudep?.logoBase64)
                }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    if (uri != null) {
                        logoBase64 = encodeResizedLogoBase64(context, uri)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BentoCard {
                            SectionHeader(
                                title = "Identitas Lembaga & Gudep",
                                subtitle = "Nomor Gudep ini menjadi kunci otomatis anggota satuan Anda"
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GudepLogoView(
                                    logoBase64 = logoBase64,
                                    institutionName = instName.ifBlank { "Gudep" },
                                    size = 64.dp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (logoBase64 == null) "Pilih Logo" else "Ganti Logo")
                                        }
                                        if (logoBase64 != null) {
                                            IconButton(onClick = { logoBase64 = null }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Hapus logo", tint = MerahLencana)
                                            }
                                        }
                                    }
                                    Text(
                                        "Pratinjau logo sekolah / Gugus Depan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = instName,
                                onValueChange = { instName = it },
                                label = { Text("Nama Lembaga / Sekolah") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("dash_institution_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = gudepNum,
                                onValueChange = { gudepNum = it },
                                label = { Text("Nomor Gudep (contoh: 01.001)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("dash_gudep_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Alamat Lembaga") },
                                modifier = Modifier.fillMaxWidth().testTag("dash_address_input")
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onSaveGudep(instName, gudepNum, address, logoBase64) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("dash_save_gudep_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HijauAlam)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Daftarkan & Simpan Identitas Gudep")
                            }
                        }
                    }
                }
            }

            1 -> {
                var editingId by remember { mutableStateOf("") }
                var matTitle by remember { mutableStateOf("") }
                var matCategory by remember { mutableStateOf("Materi Gudep") }
                var matTarget by remember { mutableStateOf("Semua") }
                var matSummary by remember { mutableStateOf("") }
                var matContent by remember { mutableStateOf("") }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BentoCard {
                            Text(
                                text = if (editingId.isBlank()) "Tambah Materi Kepramukaan" else "Edit Materi Gudep",
                                style = MaterialTheme.typography.titleMedium,
                                color = CokelatTua
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = matTitle,
                                onValueChange = { matTitle = it },
                                label = { Text("Judul Materi") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = matCategory,
                                onValueChange = { matCategory = it },
                                label = { Text("Kategori (mis. Pionering, Sandi, PBB)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Target Jenjang:", style = MaterialTheme.typography.labelMedium, color = CokelatTua)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Semua", "Siaga", "Penggalang", "Penegak").forEach { t ->
                                    FilterChip(
                                        selected = matTarget == t,
                                        onClick = { matTarget = t },
                                        label = { Text(t) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = matSummary,
                                onValueChange = { matSummary = it },
                                label = { Text("Ringkasan Singkat") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = matContent,
                                onValueChange = { matContent = it },
                                label = { Text("Isi Materi Lengkap") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onSaveMaterial(editingId, matTitle, matCategory, matTarget, matSummary, matContent, false)
                                        editingId = ""; matTitle = ""; matSummary = ""; matContent = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Simpan Draf")
                                }
                                Button(
                                    onClick = {
                                        onSaveMaterial(editingId, matTitle, matCategory, matTarget, matSummary, matContent, true)
                                        editingId = ""; matTitle = ""; matSummary = ""; matContent = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HijauAlam)
                                ) {
                                    Text("Publikasikan")
                                }
                            }
                        }
                    }

                    if (gudepMaterials.isNotEmpty()) {
                        item {
                            Text("Daftar Materi Gudep Anda", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                        }
                        items(gudepMaterials.size) { idx ->
                            val mat = gudepMaterials[idx]
                            BentoCard(containerColor = Color.White) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (mat.published) HijauAlamSoft else AmberScoutSoft
                                    ) {
                                        Text(
                                            text = if (mat.published) "Terbit • ${mat.jenjangTarget}" else "Draf • ${mat.jenjangTarget}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mat.published) HijauAlam else CokelatTua,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingId = mat.id
                                        matTitle = mat.title
                                        matCategory = mat.category
                                        matTarget = mat.jenjangTarget
                                        matSummary = mat.summary
                                        matContent = mat.content
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit materi", tint = CokelatTua)
                                    }
                                }
                                Text(mat.title, style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                                Text(mat.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            else -> {
                var taskTitle by remember { mutableStateOf("") }
                var taskDesc by remember { mutableStateOf("") }
                var taskTarget by remember { mutableStateOf("Penggalang") }
                var taskDue by remember { mutableStateOf("Sabtu Depan") }
                var taskXp by remember { mutableStateOf("40") }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BentoCard {
                            Text("Kirim Tugas Tambahan ke Anggota", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                label = { Text("Judul Tugas") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = taskDesc,
                                onValueChange = { taskDesc = it },
                                label = { Text("Deskripsi & Instruksi Tugas") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Target Jenjang Penerima:", style = MaterialTheme.typography.labelMedium, color = CokelatTua)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Siaga", "Penggalang", "Penegak", "Semua").forEach { t ->
                                    FilterChip(
                                        selected = taskTarget == t,
                                        onClick = { taskTarget = t },
                                        label = { Text(t) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = taskDue,
                                    onValueChange = { taskDue = it },
                                    label = { Text("Tenggat Waktu") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = taskXp,
                                    onValueChange = { taskXp = it.filter(Char::isDigit) },
                                    label = { Text("XP") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onSendTask(
                                        taskTitle,
                                        taskDesc,
                                        taskTarget,
                                        taskDue,
                                        taskXp.toIntOrNull() ?: 30
                                    )
                                    taskTitle = ""
                                    taskDesc = ""
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CokelatTua)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Kirim Tugas ke Jenjang $taskTarget")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun encodeResizedLogoBase64(context: Context, uri: Uri): String? {
    return try {
        val rawStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(rawStream) ?: return null
        rawStream.close()
        val maxDim = 220
        val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}
