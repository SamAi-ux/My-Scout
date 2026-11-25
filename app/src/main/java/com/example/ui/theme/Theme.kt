package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MyScoutLightColors = lightColorScheme(
    primary = CokelatTua,
    onPrimary = Color.White,
    primaryContainer = CokelatSeragamSoft,
    onPrimaryContainer = CokelatTua,
    secondary = HijauAlam,
    onSecondary = Color.White,
    secondaryContainer = HijauAlamSoft,
    onSecondaryContainer = HijauAlam,
    tertiary = CokelatSeragam,
    onTertiary = Color.White,
    tertiaryContainer = SurfaceWarm,
    onTertiaryContainer = CokelatTua,
    error = MerahLencana,
    onError = Color.White,
    errorContainer = MerahLencanaSoft,
    onErrorContainer = MerahLencana,
    background = KremHangat,
    onBackground = CokelatTua,
    surface = SurfaceWhite,
    onSurface = CokelatTua,
    surfaceVariant = SurfaceWarm,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = KremHangat,
    surfaceContainer = SurfaceWarm,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = OutlineWarm,
    outlineVariant = OutlineWarm
)

private val MyScoutDarkColors = darkColorScheme(
    primary = Color(0xFFD7CCC8),
    onPrimary = CokelatTuaDark,
    primaryContainer = CokelatTua,
    onPrimaryContainer = KremHangat,
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0A2E0D),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = CokelatSeragam,
    onTertiary = Color(0xFF231613),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF4E0A0A),
    errorContainer = MerahLencana,
    onErrorContainer = Color(0xFFFFEBEE),
    background = Color(0xFF1C1614),
    onBackground = Color(0xFFF5EFEA),
    surface = Color(0xFF261E1B),
    onSurface = Color(0xFFF5EFEA),
    surfaceVariant = Color(0xFF332925),
    onSurfaceVariant = Color(0xFFCBBDB6),
    outline = Color(0xFF4A3D37),
    outlineVariant = Color(0xFF3C302B)
)

val MyScoutShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyScoutTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MyScoutDarkColors else MyScoutLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyScoutTypography,
        shapes = MyScoutShapes,
        content = content
    )
}
