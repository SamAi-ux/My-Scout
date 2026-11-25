package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.data.remote.UserProgressPayload
import com.example.model.UserSession
import com.example.ui.components.BentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencanaSoft
import com.example.ui.theme.OutlineWarm

@Composable
fun KegiatanScreen(
    session: UserSession,
    progress: UserProgressPayload,
    onToggleMemberPresent: (String) -> Unit,
    onAddMember: (name: String, role: String) -> Unit,
    onToggleActivityAttended: (String) -> Unit,
    onAddActivity: (title: String, dateLabel: String, location: String, description: String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionHeader(
                title = "Kegiatan & Regu",
                subtitle = session.unitName.ifBlank { "Manajemen anggota satuan, presensi & agenda latihan" }
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Regu & Presensi")
                }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Agenda Kegiatan")
                }
            }
        }

        if (activeTab == 0) {
            var memberName by remember { mutableStateOf("") }
            var memberRole by remember { mutableStateOf("") }
            val presentCount = progress.reguMembers.count { it.present }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BentoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Group, contentDescription = null, tint = CokelatTua)
                                Spacer(Modifier.width(8.dp))
                                Text("Daftar Anggota Satuan", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                            }
                            Surface(shape = RoundedCornerShape(50), color = HijauAlamSoft) {
                                Text(
                                    text = "Hadir: $presentCount/${progress.reguMembers.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HijauAlam,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = memberName,
                                onValueChange = { memberName = it },
                                label = { Text("Nama Anggota") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = memberRole,
                                onValueChange = { memberRole = it },
                                label = { Text("Jabatan") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (memberName.isNotBlank()) {
                                    onAddMember(memberName, memberRole)
                                    memberName = ""
                                    memberRole = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CokelatTua),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Tambah Anggota Regu/Sangga")
                        }
                    }
                }

                items(progress.reguMembers, key = { it.id }) { m ->
                    Surface(
                        onClick = { onToggleMemberPresent(m.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.present) HijauAlamSoft.copy(alpha = 0.4f) else Color.White,
                        border = BorderStroke(1.dp, if (m.present) HijauAlam.copy(alpha = 0.35f) else OutlineWarm)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = m.present,
                                onCheckedChange = { onToggleMemberPresent(m.id) },
                                colors = CheckboxDefaults.colors(checkedColor = HijauAlam)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.name, style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                                Text("${m.role} • ${m.jenjang}", style = MaterialTheme.typography.bodySmall, color = CokelatSeragam)
                            }
                            Text(
                                text = if (m.present) "Hadir" else "Absen",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (m.present) HijauAlam else CokelatSeragam
                            )
                        }
                    }
                }
            }
        } else {
            var actTitle by remember { mutableStateOf("") }
            var actDate by remember { mutableStateOf("") }
            var actLoc by remember { mutableStateOf("") }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BentoCard {
                        Text("Tambah Jadwal Kegiatan", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actTitle,
                            onValueChange = { actTitle = it },
                            label = { Text("Nama Kegiatan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = actDate,
                                onValueChange = { actDate = it },
                                label = { Text("Hari / Waktu") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = actLoc,
                                onValueChange = { actLoc = it },
                                label = { Text("Lokasi") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (actTitle.isNotBlank()) {
                                    onAddActivity(actTitle, actDate, actLoc, "Agenda latihan kepramukaan.")
                                    actTitle = ""
                                    actDate = ""
                                    actLoc = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CokelatTua),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Simpan Agenda")
                        }
                    }
                }

                items(progress.activities, key = { it.id }) { act ->
                    BentoCard(
                        containerColor = if (act.attended) HijauAlamSoft.copy(alpha = 0.45f) else Color.White,
                        borderColor = if (act.attended) HijauAlam.copy(alpha = 0.4f) else OutlineWarm
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Event, contentDescription = null, tint = CokelatTua, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(act.dateLabel, style = MaterialTheme.typography.labelMedium, color = CokelatTua)
                            }
                            Surface(
                                onClick = { onToggleActivityAttended(act.id) },
                                shape = RoundedCornerShape(50),
                                color = if (act.attended) HijauAlam else MerahLencanaSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (act.attended) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (act.attended) "Hadir (+20 XP)" else "Check-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (act.attended) Color.White else CokelatTua
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(act.title, style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = CokelatSeragam, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(act.location, style = MaterialTheme.typography.labelSmall, color = CokelatSeragam)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(act.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
