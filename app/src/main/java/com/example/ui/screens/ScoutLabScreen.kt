package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PackingItem
import com.example.ui.components.BentoCard
import com.example.ui.components.CompassCanvas
import com.example.ui.components.MorseCodec
import com.example.ui.components.SandiCodec
import com.example.ui.components.SectionHeader
import com.example.ui.components.SemaphoreCanvas
import com.example.ui.theme.AmberScout
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.OutlineWarm
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ScoutLabScreen(
    initialTab: Int = 0,
    packingList: List<PackingItem>,
    onTogglePackingItem: (String) -> Unit,
    onAddPackingItem: (name: String, category: String) -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionHeader(
                title = "Scout Lab",
                subtitle = "Penerjemah sandi, semaphore, kompas lapangan & perlengkapan kemah"
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Sandi & Morse", "Semaphore", "Kompas & Kemah")
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("lab_tab_$idx")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> SandiMorseSection()
            1 -> SemaphoreSection()
            else -> CompassAndPackingSection(
                packingList = packingList,
                onTogglePackingItem = onTogglePackingItem,
                onAddPackingItem = onAddPackingItem
            )
        }
    }
}

@Composable
private fun SandiMorseSection() {
    var inputText by remember { mutableStateOf("PRAMUKA JAYA") }
    var isMorseToText by remember { mutableStateOf(false) }
    var selectedSandi by remember { mutableStateOf("Morse") }
    var isPlaying by remember { mutableStateOf(false) }
    var isFlashLit by remember { mutableStateOf(false) }
    var activeSymbol by remember { mutableStateOf<Char?>(null) }
    val scope = rememberCoroutineScope()

    val sandiTypes = listOf("Morse", "Sandi Angka", "Sandi AN", "Sandi AZ", "Sandi Rumput")
    val outputText = remember(inputText, isMorseToText, selectedSandi) {
        when (selectedSandi) {
            "Morse" -> if (isMorseToText) MorseCodec.decode(inputText) else MorseCodec.encode(inputText)
            "Sandi Angka" -> SandiCodec.toSandiAngka(inputText)
            "Sandi AN" -> SandiCodec.toSandiAn(inputText)
            "Sandi AZ" -> SandiCodec.toSandiAz(inputText)
            else -> SandiCodec.toSandiRumput(inputText)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BentoCard {
                Text("Pilih Jenis Sandi", style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sandiTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedSandi == type,
                            onClick = { selectedSandi = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CokelatTua,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sandiTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedSandi == type,
                            onClick = { selectedSandi = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CokelatTua,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedSandi == "Morse") {
                    Spacer(Modifier.height(6.dp))
                    FilterChip(
                        selected = isMorseToText,
                        onClick = { isMorseToText = !isMorseToText },
                        leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                        label = { Text(if (isMorseToText) "Mode: Morse -> Teks" else "Mode: Teks -> Morse") }
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(if (isMorseToText && selectedSandi == "Morse") "Masukkan Kode Morse (.- / ...)" else "Masukkan Teks") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandi_input")
                )

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = KremHangat,
                    border = BorderStroke(1.dp, OutlineWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Hasil ($selectedSandi):",
                            style = MaterialTheme.typography.labelSmall,
                            color = CokelatSeragam
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = outputText.ifBlank { "-" },
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = CokelatTua
                        )
                    }
                }

                if (selectedSandi == "Morse") {
                    Spacer(Modifier.height(12.dp))
                    val morseToPlay = if (isMorseToText) inputText else outputText
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!isPlaying && morseToPlay.isNotBlank()) {
                                    isPlaying = true
                                    scope.launch {
                                        MorseCodec.playAudioAndVisual(morseToPlay) { lit, sym ->
                                            isFlashLit = lit
                                            activeSymbol = sym
                                        }
                                        isPlaying = false
                                    }
                                }
                            },
                            enabled = !isPlaying && morseToPlay.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HijauAlam),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("morse_play_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPlaying) "Memutar..." else "Putar Audio & Sinyal")
                        }

                        // Visual flashlight indicator
                        Surface(
                            shape = CircleShape,
                            color = if (isFlashLit) AmberScout else Color(0xFFE7E0D8),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (activeSymbol != null) {
                                    Text(
                                        text = activeSymbol.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.FlashlightOn,
                                        contentDescription = "Indikator sinyal visual",
                                        tint = if (isFlashLit) Color.White else CokelatTua
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SemaphoreSection() {
    var phrase by remember { mutableStateOf("SCOUT") }
    var selectedLetter by remember { mutableStateOf('S') }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BentoCard {
                Text("Peraga Bendera Semaphore", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { newVal ->
                        phrase = newVal.uppercase().filter { it.isLetter() || it == ' ' }.take(14)
                        phrase.lastOrNull { it.isLetter() }?.let { selectedLetter = it }
                    },
                    label = { Text("Ketik Kata untuk Dilatih") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    phrase.filter { it.isLetter() }.forEachIndexed { idx, ch ->
                        FilterChip(
                            selected = selectedLetter == ch,
                            onClick = { selectedLetter = ch },
                            label = { Text("$ch") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MerahLencana,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(KremHangat),
                    contentAlignment = Alignment.Center
                ) {
                    SemaphoreCanvas(
                        letter = selectedLetter,
                        modifier = Modifier.size(200.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CokelatTua,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Huruf $selectedLetter",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompassAndPackingSection(
    packingList: List<PackingItem>,
    onTogglePackingItem: (String) -> Unit,
    onAddPackingItem: (name: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensorManager != null && rotationSensor != null) {
            sensorAvailable = true
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (deg + 360f) % 360f
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose {}
        }
    }

    val cardinalLabel = remember(azimuth) {
        val dirs = listOf("Utara", "Timur Laut", "Timur", "Tenggara", "Selatan", "Barat Daya", "Barat", "Barat Laut")
        dirs[((azimuth + 22.5f) / 45f).toInt() % 8]
    }
    val backAzimuth = remember(azimuth) {
        val a = azimuth.roundToInt()
        if (a < 180) a + 180 else a - 180
    }

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
                    Column {
                        Text("Kompas Lapangan", style = MaterialTheme.typography.titleMedium, color = CokelatTua)
                        Text(
                            text = if (sensorAvailable) "Sensor magnetik aktif" else "Mode kalibrasi manual (sensor tidak tersedia)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sensorAvailable) HijauAlam else CokelatSeragam
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = HijauAlamSoft) {
                        Text(
                            text = "${azimuth.roundToInt()}° $cardinalLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = HijauAlam,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CompassCanvas(
                        azimuthDegrees = azimuth,
                        modifier = Modifier.size(190.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Back Azimuth: $backAzimuth°",
                    style = MaterialTheme.typography.labelMedium,
                    color = CokelatTua
                )
                if (!sensorAvailable) {
                    Slider(
                        value = azimuth,
                        onValueChange = { azimuth = it },
                        valueRange = 0f..359f
                    )
                }
            }
        }

        item {
            BentoCard {
                val checkedCount = packingList.count { it.checked }
                Text(
                    text = "Daftar Perlengkapan Kemah ($checkedCount/${packingList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = CokelatTua
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = { Text("Tambah perlengkapan...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                onAddPackingItem(newItemName, "Tambahan")
                                newItemName = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CokelatTua)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah perlengkapan", tint = Color.White)
                    }
                }
            }
        }

        items(packingList, key = { it.id }) { item ->
            Surface(
                onClick = { onTogglePackingItem(item.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (item.checked) HijauAlamSoft.copy(alpha = 0.5f) else Color.White,
                border = BorderStroke(1.dp, if (item.checked) HijauAlam.copy(alpha = 0.35f) else OutlineWarm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onTogglePackingItem(item.id) },
                        colors = CheckboxDefaults.colors(checkedColor = HijauAlam)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, color = CokelatTua)
                        Text(item.category, style = MaterialTheme.typography.labelSmall, color = CokelatSeragam)
                    }
                }
            }
        }
    }
}
