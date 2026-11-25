package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AuthProviderType
import com.example.model.JenjangPramuka
import com.example.model.UserSession
import com.example.ui.components.BentoCard
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.OutlineWarm

@Composable
fun JenjangOnboardingScreen(
    session: UserSession,
    onConfirmJenjang: (JenjangPramuka) -> Unit,
    onSwitchAccount: () -> Unit
) {
    var selectedJenjang by remember { mutableStateOf(JenjangPramuka.PENGGALANG) }
    val isGoogle = session.authProvider == AuthProviderType.GOOGLE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BentoCard(containerColor = Color.White) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = HijauAlam,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pendaftaran Berhasil!",
                            style = MaterialTheme.typography.labelLarge,
                            color = HijauAlam
                        )
                        Text(
                            text = "Pilih Jenjang Pramuka Anda",
                            style = MaterialTheme.typography.headlineSmall,
                            color = CokelatTua
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Halo, ${session.fullName.ifBlank { session.username }} (${session.email.ifBlank { session.username }}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CokelatTua,
                    fontWeight = FontWeight.Medium
                )
            }

            // One-time notice card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HijauAlamSoft,
                border = BorderStroke(1.dp, HijauAlam.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jenjang_once_notice")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = HijauAlam,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isGoogle) {
                            "Pemilihan jenjang hanya tampil sekali saja saat pendaftaran dan data langsung tersimpan ke akun Google yang terdaftar."
                        } else {
                            "Pemilihan jenjang hanya tampil sekali saja saat pendaftaran dan data tersimpan di akun Anda yang terdaftar."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = CokelatTua
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                JenjangPramuka.entries.forEach { jenjang ->
                    val selected = selectedJenjang == jenjang
                    Surface(
                        onClick = { selectedJenjang = jenjang },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) CokelatTua else Color.White,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) CokelatTua else OutlineWarm
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_jenjang_${jenjang.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = jenjang.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) Color.White else CokelatTua,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = jenjang.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) KremHangat.copy(alpha = 0.9f) else CokelatSeragam
                                )
                            }
                            Icon(
                                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (selected) "Terpilih" else "Pilih ${jenjang.label}",
                                tint = if (selected) Color.White else CokelatSeragam
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onConfirmJenjang(selectedJenjang) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_jenjang_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HijauAlam,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Simpan Jenjang (${selectedJenjang.label}) & Masuk",
                    fontWeight = FontWeight.Bold
                )
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(
                    onClick = onSwitchAccount,
                    modifier = Modifier.testTag("onboarding_logout_button")
                ) {
                    Text("Gunakan akun lain", color = CokelatSeragam)
                }
            }
        }
    }
}
