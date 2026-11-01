package ir.mosafir.recovery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.mosafir.recovery.R

// ---------- Palette (یاسی — Violet, calm & hopeful) ----------
val VioletPrimary = Color(0xFF6750A4)
val VioletSecondary = Color(0xFF7E66B8)
val AmberAccent = Color(0xFFF9A825)
val SoftBackground = Color(0xFFF9F6FD)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF241056),
    secondary = Color(0xFF7E66B8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE6FA),
    onSecondaryContainer = Color(0xFF1F1440),
    tertiary = Color(0xFFF9A825),
    onTertiary = Color(0xFF2B2000),
    tertiaryContainer = Color(0xFFFFE9B8),
    onTertiaryContainer = Color(0xFF4A3A00),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = SoftBackground,
    onBackground = Color(0xFF1B1723),
    surface = Color.White,
    onSurface = Color(0xFF1B1723),
    surfaceVariant = Color(0xFFEDE6F6),
    onSurfaceVariant = Color(0xFF4A4453),
    outline = Color(0xFF7C7489),
    outlineVariant = Color(0xFFCCC5D6)
)

// ---------- Persian font (Vazirmatn) ----------
val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val Defaults = Typography()

private fun withVazir() = Typography(
    displayLarge = Defaults.displayLarge.copy(fontFamily = Vazir),
    displayMedium = Defaults.displayMedium.copy(fontFamily = Vazir),
    displaySmall = Defaults.displaySmall.copy(fontFamily = Vazir),
    headlineLarge = Defaults.headlineLarge.copy(fontFamily = Vazir),
    headlineMedium = Defaults.headlineMedium.copy(fontFamily = Vazir),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = Vazir),
    titleLarge = Defaults.titleLarge.copy(fontFamily = Vazir),
    titleMedium = Defaults.titleMedium.copy(fontFamily = Vazir),
    titleSmall = Defaults.titleSmall.copy(fontFamily = Vazir),
    bodyLarge = Defaults.bodyLarge.copy(fontFamily = Vazir),
    bodyMedium = Defaults.bodyMedium.copy(fontFamily = Vazir),
    bodySmall = Defaults.bodySmall.copy(fontFamily = Vazir),
    labelLarge = Defaults.labelLarge.copy(fontFamily = Vazir),
    labelMedium = Defaults.labelMedium.copy(fontFamily = Vazir),
    labelSmall = Defaults.labelSmall.copy(fontFamily = Vazir)
)

private val AppTypography = withVazir()

@Composable
fun MosafirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Light theme only for consistency & readability of educational content
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
