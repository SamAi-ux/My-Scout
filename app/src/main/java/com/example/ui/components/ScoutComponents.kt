package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.JenjangPramuka
import com.example.ui.theme.AmberScout
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.MerahLencanaSoft
import com.example.ui.theme.OutlineWarm
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceWhite,
    borderColor: Color = OutlineWarm,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun JenjangBadge(jenjang: JenjangPramuka, modifier: Modifier = Modifier) {
    val (bg, fg) = when (jenjang) {
        JenjangPramuka.SIAGA -> HijauAlamSoft to HijauAlam
        JenjangPramuka.PENGGALANG -> MerahLencanaSoft to MerahLencana
        JenjangPramuka.PENEGAK -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        JenjangPramuka.PEMBINA, JenjangPramuka.PEMBINA_DAMPING -> Color(0xFFEFE9E6) to CokelatTua
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
    ) {
        Text(
            text = jenjang.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GudepLogoView(
    logoBase64: String?,
    institutionName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val bitmap: Bitmap? = remember(logoBase64) {
        if (logoBase64.isNullOrBlank()) null
        else try {
            val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(CokelatTua)
            .border(1.dp, OutlineWarm, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Logo $institutionName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Lambang Gugus Depan",
                tint = KremHangat,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = CokelatTua
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

object MorseCodec {
    private val map = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )
    private val reverseMap = map.entries.associate { (k, v) -> v to k }

    fun encode(text: String): String {
        return text.uppercase()
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" / ") { word ->
                word.mapNotNull { ch -> map[ch] }.joinToString(" ")
            }
    }

    fun decode(morse: String): String {
        return morse.trim()
            .split("/")
            .joinToString(" ") { word ->
                word.trim()
                    .split(Regex("\\s+"))
                    .mapNotNull { code -> reverseMap[code] }
                    .joinToString("")
            }
    }

    suspend fun playAudioAndVisual(
        morse: String,
        onSignalActive: (Boolean, Char?) -> Unit
    ) {
        val tone = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            null
        }
        try {
            for (ch in morse) {
                when (ch) {
                    '.' -> {
                        onSignalActive(true, '.')
                        tone?.startTone(ToneGenerator.TONE_DTMF_1, 130)
                        delay(150)
                        onSignalActive(false, null)
                        delay(110)
                    }
                    '-' -> {
                        onSignalActive(true, '-')
                        tone?.startTone(ToneGenerator.TONE_DTMF_1, 360)
                        delay(380)
                        onSignalActive(false, null)
                        delay(110)
                    }
                    ' ' -> delay(200)
                    '/' -> delay(420)
                }
            }
        } finally {
            onSignalActive(false, null)
            tone?.release()
        }
    }
}

object SandiCodec {
    fun toSandiAngka(text: String): String {
        return text.uppercase().map { ch ->
            when {
                ch in 'A'..'Z' -> (ch - 'A' + 1).toString()
                ch == ' ' -> "/"
                else -> ch.toString()
            }
        }.joinToString(" ")
    }

    fun toSandiAn(text: String): String {
        return text.uppercase().map { ch ->
            when (ch) {
                in 'A'..'M' -> (ch + 13)
                in 'N'..'Z' -> (ch - 13)
                else -> ch
            }
        }.joinToString("")
    }

    fun toSandiAz(text: String): String {
        return text.uppercase().map { ch ->
            if (ch in 'A'..'Z') ('Z' - (ch - 'A')) else ch
        }.joinToString("")
    }

    fun toSandiRumput(text: String): String {
        return MorseCodec.encode(text)
            .replace(".", "^")
            .replace("-", "^^^")
            .replace(" ", "_")
    }
}

object SemaphorePositions {
    // Angles in degrees where 0 = down (6 o'clock), clockwise:
    // 45 = bottom-left, 90 = left, 135 = top-left, 180 = up, 225 = top-right, 270 = right, 315 = bottom-right
    private val letterAngles: Map<Char, Pair<Float, Float>> = mapOf(
        'A' to (45f to 0f),
        'B' to (90f to 0f),
        'C' to (135f to 0f),
        'D' to (180f to 0f),
        'E' to (0f to 225f),
        'F' to (0f to 270f),
        'G' to (0f to 315f),
        'H' to (90f to 45f),
        'I' to (135f to 45f),
        'J' to (180f to 270f),
        'K' to (45f to 180f),
        'L' to (45f to 225f),
        'M' to (45f to 270f),
        'N' to (45f to 315f),
        'O' to (90f to 135f),
        'P' to (90f to 180f),
        'Q' to (90f to 225f),
        'R' to (90f to 270f),
        'S' to (90f to 315f),
        'T' to (135f to 180f),
        'U' to (135f to 225f),
        'V' to (180f to 315f),
        'W' to (270f to 225f),
        'X' to (315f to 225f),
        'Y' to (135f to 270f),
        'Z' to (315f to 270f)
    )

    fun anglesFor(letter: Char): Pair<Float, Float> =
        letterAngles[letter.uppercaseChar()] ?: (45f to 315f)
}

@Composable
fun SemaphoreCanvas(
    letter: Char,
    modifier: Modifier = Modifier
) {
    val (leftDeg, rightDeg) = SemaphorePositions.anglesFor(letter)
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.50f
        val bodyHeight = size.height * 0.28f
        val armLen = size.minDimension * 0.34f

        // Background guide circle
        drawCircle(
            color = OutlineWarm,
            radius = size.minDimension * 0.44f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // Scout silhouette head & torso
        drawCircle(
            color = CokelatTua,
            radius = size.minDimension * 0.08f,
            center = Offset(cx, cy - bodyHeight * 0.35f)
        )
        drawLine(
            color = CokelatTua,
            start = Offset(cx, cy),
            end = Offset(cx, cy + bodyHeight),
            strokeWidth = 10.dp.toPx(),
            cap = StrokeCap.Round
        )

        fun drawArmWithFlag(clockDeg: Float, primaryColor: Color) {
            val rad = Math.toRadians((clockDeg + 90f).toDouble())
            val shoulder = Offset(cx, cy + bodyHeight * 0.12f)
            val hand = Offset(
                x = shoulder.x + (cos(rad) * armLen).toFloat(),
                y = shoulder.y + (sin(rad) * armLen).toFloat()
            )
            drawLine(
                color = CokelatSeragam,
                start = shoulder,
                end = hand,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Flag stick extension
            val tip = Offset(
                x = shoulder.x + (cos(rad) * (armLen + 18.dp.toPx())).toFloat(),
                y = shoulder.y + (sin(rad) * (armLen + 18.dp.toPx())).toFloat()
            )
            drawLine(
                color = CokelatTua,
                start = hand,
                end = tip,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Red-yellow semaphore flag square
            val perpRad = rad + Math.PI / 2
            val flagSize = 18.dp.toPx()
            val p1 = hand
            val p2 = tip
            val p3 = Offset(
                tip.x + (cos(perpRad) * flagSize).toFloat(),
                tip.y + (sin(perpRad) * flagSize).toFloat()
            )
            val p4 = Offset(
                hand.x + (cos(perpRad) * flagSize).toFloat(),
                hand.y + (sin(perpRad) * flagSize).toFloat()
            )
            val redHalf = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }
            val yellowHalf = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }
            drawPath(redHalf, color = primaryColor)
            drawPath(yellowHalf, color = AmberScout)
        }

        drawArmWithFlag(leftDeg, MerahLencana)
        drawArmWithFlag(rightDeg, MerahLencana)
    }
}

@Composable
fun CompassCanvas(
    azimuthDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f

        // Outer dial ring
        drawCircle(
            color = KremHangat,
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = CokelatTua,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )

        // Tick marks every 15 deg
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg - 90).toDouble())
            val isCardinal = deg % 90 == 0
            val inner = if (isCardinal) r * 0.78f else r * 0.88f
            val start = Offset(cx + (cos(rad) * inner).toFloat(), cy + (sin(rad) * inner).toFloat())
            val end = Offset(cx + (cos(rad) * (r * 0.96f)).toFloat(), cy + (sin(rad) * (r * 0.96f)).toFloat())
            drawLine(
                color = if (deg == 0) MerahLencana else CokelatSeragam,
                start = start,
                end = end,
                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx()
            )
        }

        // Needle rotated by -azimuthDegrees (pointing North relative to dial)
        val needleRad = Math.toRadians((-azimuthDegrees - 90f).toDouble())
        val tipN = Offset(cx + (cos(needleRad) * (r * 0.68f)).toFloat(), cy + (sin(needleRad) * (r * 0.68f)).toFloat())
        val tipS = Offset(cx - (cos(needleRad) * (r * 0.56f)).toFloat(), cy - (sin(needleRad) * (r * 0.56f)).toFloat())
        val sideRad = needleRad + Math.PI / 2
        val sideW = r * 0.11f
        val left = Offset(cx + (cos(sideRad) * sideW).toFloat(), cy + (sin(sideRad) * sideW).toFloat())
        val right = Offset(cx - (cos(sideRad) * sideW).toFloat(), cy - (sin(sideRad) * sideW).toFloat())

        val northTriangle = Path().apply {
            moveTo(tipN.x, tipN.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        val southTriangle = Path().apply {
            moveTo(tipS.x, tipS.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(northTriangle, color = MerahLencana)
        drawPath(southTriangle, color = CokelatTua)
        drawCircle(color = KremHangat, radius = 6.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = CokelatTua, radius = 6.dp.toPx(), center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
    }
}
