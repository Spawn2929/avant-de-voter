package fr.avantdevoter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// Une seule couleur, gris-bleu neutre, pour toute l'appli et tous les candidats : aucune couleur de parti.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F5A73),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3F3),
    onPrimaryContainer = Color(0xFF0E1D2B),
    secondary = Color(0xFF55606B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Color(0xFF141C24),
    tertiary = Color(0xFF55606B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF181C20),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F7),
    surfaceContainer = Color(0xFFEBEFF3),
    surfaceContainerHigh = Color(0xFFE5E9EE),
    surfaceContainerHighest = Color(0xFFDFE3E8),
    onSurfaceVariant = Color(0xFF424A53),
    outline = Color(0xFF727A84),
    outlineVariant = Color(0xFFC2C8D0),
    error = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA9C7E3),
    onPrimary = Color(0xFF103248),
    primaryContainer = Color(0xFF2A4560),
    onPrimaryContainer = Color(0xFFD3E3F3),
    secondary = Color(0xFFBEC7D1),
    onSecondary = Color(0xFF28313A),
    secondaryContainer = Color(0xFF3E4852),
    onSecondaryContainer = Color(0xFFDCE3EA),
    tertiary = Color(0xFFBEC7D1),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E6),
    surfaceContainerLowest = Color(0xFF0C0F12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    onSurfaceVariant = Color(0xFFC2C8D0),
    outline = Color(0xFF8C939C),
    outlineVariant = Color(0xFF424A53),
    error = Color(0xFFFFB4AB),
)

/** Textes un peu plus grands que la normale : lisibles d'un coup d'œil. */
private val BigTypography = Typography().let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        titleMedium = t.titleMedium.copy(fontSize = 18.sp),
        labelLarge = t.labelLarge.copy(fontSize = 15.sp),
    )
}

@Composable
fun AvantDeVoterTheme(sombre: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (sombre) DarkScheme else LightScheme,
        typography = BigTypography,
        content = content,
    )
}
