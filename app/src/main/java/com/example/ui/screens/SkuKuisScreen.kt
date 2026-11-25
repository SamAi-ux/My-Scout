package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ScoutCatalog
import com.example.data.remote.UserProgressPayload
import com.example.model.JenjangPramuka
import com.example.ui.components.BentoCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.MerahLencanaSoft
import com.example.ui.theme.OutlineWarm
import kotlin.math.roundToInt

@Composable
fun SkuKuisScreen(
    initialTab: Int = 0,
    userJenjang: JenjangPramuka,
    progress: UserProgressPayload,
    onToggleSku: (String) -> Unit,
    onToggleTkk: (String) -> Unit,
    onRecordQuizResult: (Int) -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionHeader(
                title = "SKU, TKK & Kuis",
                subtitle = "Syarat Kecakapan Umum, Tanda Kecakapan Khusus & Latihan Soal"
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("SKU", "TKK", "Kuis")
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("sku_tab_$idx")
                    ) {
                        Text(label)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> SkuChecklistTab(
                defaultJenjang = if (userJenjang.isPembinaRole) JenjangPramuka.PENGGALANG else userJenjang,
                completedSkuIds = progress.completedSkuIds,
                onToggleSku = onToggleSku
            )
            1 -> TkkCatalogTab(
                earnedTkkIds = progress.earnedTkkIds,
                onToggleTkk = onToggleTkk
            )
            else -> QuizPracticeTab(
                bestScore = progress.quizBestScore,
                attempts = progress.quizAttempts,
                onQuizFinished = onRecordQuizResult
            )
        }
    }
}

@Composable
private fun SkuChecklistTab(
    defaultJenjang: JenjangPramuka,
    completedSkuIds: Set<String>,
    onToggleSku: (String) -> Unit
) {
    var selectedJenjang by remember(defaultJenjang) { mutableStateOf(defaultJenjang) }
    val jenjangOptions = listOf(JenjangPramuka.SIAGA, JenjangPramuka.PENGGALANG, JenjangPramuka.PENEGAK)
    val items = remember(selectedJenjang) {
        ScoutCatalog.skuCatalog.filter { it.jenjang == selectedJenjang }
    }
    val doneCount = items.count { it.id in completedSkuIds }
    val pct = if (items.isEmpty()) 0f else doneCount.toFloat() / items.size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                jenjangOptions.forEach { j ->
                    FilterChip(
                        selected = selectedJenjang == j,
                        onClick = { selectedJenjang = j },
                        label = { Text(j.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CokelatTua,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres SKU ${selectedJenjang.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CokelatTua
                    )
                    Text(
                        text = "$doneCount/${items.size} (${(pct * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = HijauAlam
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = HijauAlam,
                    trackColor = OutlineWarm
                )
            }
        }

        items(items, key = { it.id }) { sku ->
            val checked = sku.id in completedSkuIds
            Surface(
                onClick = { onToggleSku(sku.id) },
                shape = RoundedCornerShape(18.dp),
                color = if (checked) HijauAlamSoft.copy(alpha = 0.45f) else Color.White,
                border = BorderStroke(1.dp, if (checked) HijauAlam.copy(alpha = 0.4f) else OutlineWarm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleSku(sku.id) },
                        colors = CheckboxDefaults.colors(checkedColor = HijauAlam)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(50), color = KremHangat) {
                                Text(
                                    text = "${sku.tier} • Poin ${sku.pointNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CokelatTua,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text("+25 XP", style = MaterialTheme.typography.labelSmall, color = HijauAlam)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(sku.title, style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                        Text(
                            sku.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TkkCatalogTab(
    earnedTkkIds: Set<String>,
    onToggleTkk: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BentoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scout_badge_1795584338295),
                        contentDescription = "Ilustrasi TKK Pramuka",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text("Katalog TKK (${earnedTkkIds.size}/${ScoutCatalog.tkkCatalog.size} Diraih)", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                        Text(
                            "Tanda Kecakapan Khusus sesuai bidang keterampilan anggota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(ScoutCatalog.tkkCatalog, key = { it.id }) { tkk ->
            val earned = tkk.id in earnedTkkIds
            BentoCard(
                onClick = { onToggleTkk(tkk.id) },
                containerColor = if (earned) HijauAlamSoft.copy(alpha = 0.5f) else Color.White,
                borderColor = if (earned) HijauAlam.copy(alpha = 0.45f) else OutlineWarm
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.MilitaryTech,
                            contentDescription = null,
                            tint = if (earned) HijauAlam else CokelatSeragam,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(tkk.name, style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                            Text("${tkk.category} • ${tkk.colorLabel}", style = MaterialTheme.typography.labelSmall, color = CokelatSeragam)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (earned) HijauAlam else KremHangat
                    ) {
                        Text(
                            text = if (earned) "Tercapai" else "Klaim +40 XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (earned) Color.White else CokelatTua,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(tkk.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuizPracticeTab(
    bestScore: Int,
    attempts: Int,
    onQuizFinished: (Int) -> Unit
) {
    val questions = ScoutCatalog.quizQuestions
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Quiz, contentDescription = null, tint = MerahLencana)
                        Spacer(Modifier.width(8.dp))
                        Text("Kuis Kepramukaan", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                    }
                    Text(
                        text = "Skor Terbaik: $bestScore% ($attempts×)",
                        style = MaterialTheme.typography.labelMedium,
                        color = HijauAlam
                    )
                }
            }
        }

        if (finished) {
            val scorePct = ((correctCount * 100f) / questions.size).roundToInt()
            item {
                BentoCard(containerColor = Color.White) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selesai",
                            tint = HijauAlam,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Nilai Akhir: $scorePct%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CokelatTua
                        )
                        Text(
                            text = "Anda menjawab benar $correctCount dari ${questions.size} soal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                currentIndex = 0
                                selectedOption = null
                                correctCount = 0
                                finished = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CokelatTua)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ulangi Kuis")
                        }
                    }
                }
            }
        } else {
            val q = questions[currentIndex]
            item {
                BentoCard {
                    Text(
                        text = "Soal ${currentIndex + 1} dari ${questions.size} • ${q.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CokelatSeragam
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium,
                        color = CokelatTua
                    )
                    Spacer(Modifier.height(12.dp))

                    q.options.forEachIndexed { idx, opt ->
                        val answered = selectedOption != null
                        val isChosen = selectedOption == idx
                        val isCorrect = idx == q.correctIndex
                        val bg = when {
                            !answered -> KremHangat
                            isCorrect -> HijauAlamSoft
                            isChosen -> MerahLencanaSoft
                            else -> KremHangat
                        }
                        val border = when {
                            !answered -> OutlineWarm
                            isCorrect -> HijauAlam
                            isChosen -> MerahLencana
                            else -> OutlineWarm
                        }

                        Surface(
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = idx
                                    if (idx == q.correctIndex) correctCount++
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = BorderStroke(1.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${('A' + idx)}. $opt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChosen || (answered && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                color = CokelatTua,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (selectedOption != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HijauAlamSoft.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Pembahasan:", style = MaterialTheme.typography.labelLarge, color = HijauAlam)
                                Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = CokelatTua)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (currentIndex < questions.lastIndex) {
                                    currentIndex++
                                    selectedOption = null
                                } else {
                                    val finalScore = ((correctCount * 100f) / questions.size).roundToInt()
                                    finished = true
                                    onQuizFinished(finalScore)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CokelatTua)
                        ) {
                            Text(if (currentIndex < questions.lastIndex) "Soal Berikutnya" else "Lihat Skor Akhir")
                        }
                    }
                }
            }
        }
    }
}
