package ir.mosafir.recovery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.mosafir.recovery.R

// ---------------------------------------------------------------
// حالت‌های تم — انتخاب کاربر
//   بنفش (بنفش واقعی) | ارغوانی | ارکیده | خاکستری آرام | خودکار
// «خودکار» بر اساس روشن/تاریک بودن خود گوشی عوض می‌شود.
// ---------------------------------------------------------------
enum class ThemeMode(val labelFa: String, val swatch: Color, val followsSystem: Boolean = false) {
    BANAFSH("بنفش", Color(0xFF6A0DAD)),
    ARGHAVANI("ارغوانی", Color(0xFFA21CAF)),
    ORCHID("ارکیده", Color(0xFF9C3794)),
    KHAKESTARI("خاکستری آرام", Color(0xFF52525B)),
    AUTO("خودکار", Color(0xFF6A0DAD), followsSystem = true);

    companion object {
        fun from(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: BANAFSH
    }
}

// ---------------------------------------------------------------
// رنگ‌های پویا — با هر تم عوض می‌شوند.
// VioletPrimary در همهٔ کدهای قدیمی استفاده شده؛ حالا از تم فعلی می‌آید.
// ---------------------------------------------------------------
val LocalThemePrimary = staticCompositionLocalOf { Color(0xFF6A0DAD) }
val LocalTopBarColor = staticCompositionLocalOf { Color(0xFF6A0DAD) }

/** رنگ اصلی تم فعلی (تیترها، حاشیه کارت‌ها، نشان‌ها) */
val VioletPrimary: Color @Composable get() = LocalThemePrimary.current

/** رنگ نوار بالای صفحه (TopAppBar و نوار وضعیت) */
val AppTopBarColor: Color @Composable get() = LocalTopBarColor.current

/** رنگ کهربایی (ثابت — با همهٔ تم‌ها سازگار است) */
val AmberAccent = Color(0xFFF9A825)

private val SoftBackground = Color(0xFFF9F6FD)

// ---------------------------------------------------------------
// ۱) بنفش — بنفش واقعی و پرجان (نه یاسی)
// ---------------------------------------------------------------
private val BanafshLight = lightColorScheme(
    primary = Color(0xFF6A0DAD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E3FF),
    onPrimaryContainer = Color(0xFF2A0A55),
    secondary = Color(0xFF7B1FA2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF301440),
    tertiary = AmberAccent,
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
    onSurfaceVariant = Color(0xFF38323F),
    outline = Color(0xFF655D75),
    outlineVariant = Color(0xFFCCC5D6)
)

// ---------------------------------------------------------------
// ۲) ارغوانی — بنفشِ مایل به سرخ (مگنتی)
// ---------------------------------------------------------------
private val ArghavaniLight = lightColorScheme(
    primary = Color(0xFFA21CAF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFAE3F7),
    onPrimaryContainer = Color(0xFF4A0A44),
    secondary = Color(0xFFB1549F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E4F3),
    onSecondaryContainer = Color(0xFF401438),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF2B2000),
    tertiaryContainer = Color(0xFFFFE9B8),
    onTertiaryContainer = Color(0xFF4A3A00),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFDF7FB),
    onBackground = Color(0xFF221A21),
    surface = Color.White,
    onSurface = Color(0xFF221A21),
    surfaceVariant = Color(0xFFF4E4F1),
    onSurfaceVariant = Color(0xFF3A2F38),
    outline = Color(0xFF7F657A),
    outlineVariant = Color(0xFFDCC9D6)
)

// ---------------------------------------------------------------
// ۳) ارکیده — بنفش ارکیده‌ای (#9C3794)
// ---------------------------------------------------------------
private val OrchidLight = lightColorScheme(
    primary = Color(0xFF9C3794),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E1F3),
    onPrimaryContainer = Color(0xFF3B1038),
    secondary = Color(0xFFAF5CA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5E3F2),
    onSecondaryContainer = Color(0xFF381535),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF2B2000),
    tertiaryContainer = Color(0xFFFFE9B8),
    onTertiaryContainer = Color(0xFF4A3A00),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFCF7FB),
    onBackground = Color(0xFF211B21),
    surface = Color.White,
    onSurface = Color(0xFF211B21),
    surfaceVariant = Color(0xFFF2E3EF),
    onSurfaceVariant = Color(0xFF372F36),
    outline = Color(0xFF7A6176),
    outlineVariant = Color(0xFFD8C6D4)
)

// ---------------------------------------------------------------
// ۴) خاکستری آرام — بی‌رنگ، ساده و آرام
// ---------------------------------------------------------------
private val KhakestariLight = lightColorScheme(
    primary = Color(0xFF52525B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8EB),
    onPrimaryContainer = Color(0xFF232327),
    secondary = Color(0xFF6B7280),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECECEE),
    onSecondaryContainer = Color(0xFF2B2B30),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF2B2000),
    tertiaryContainer = Color(0xFFFFE9B8),
    onTertiaryContainer = Color(0xFF4A3A00),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF1C1C1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1F),
    surfaceVariant = Color(0xFFEEEEF0),
    onSurfaceVariant = Color(0xFF3A3A3F),
    outline = Color(0xFF71717A),
    outlineVariant = Color(0xFFD9D9DE)
)

// ---------------------------------------------------------------
// ۵) تم تاریک — فقط برای حالت «خودکار» وقتی تم گوشی تاریک است
// ---------------------------------------------------------------
private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD3B8FF),
    onPrimary = Color(0xFF2E0A5C),
    primaryContainer = Color(0xFF452080),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFC7AEEF),
    onSecondary = Color(0xFF35104A),
    secondaryContainer = Color(0xFF3A2350),
    onSecondaryContainer = Color(0xFFEFE1FA),
    tertiary = Color(0xFFFFC94D),
    onTertiary = Color(0xFF2B2000),
    tertiaryContainer = Color(0xFF4A3A00),
    onTertiaryContainer = Color(0xFFFFE9B8),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121016),
    onBackground = Color(0xFFE6E1EC),
    surface = Color(0xFF1A1721),
    onSurface = Color(0xFFE6E1EC),
    surfaceVariant = Color(0xFF2B2634),
    onSurfaceVariant = Color(0xFFC9C2D4),
    outline = Color(0xFF948CA3),
    outlineVariant = Color(0xFF3B3546)
)

// ---------------------------------------------------------------
// فونت فارسی (وزیرمتن)
// ---------------------------------------------------------------
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

/**
 * تم اصلی اپلیکیشن.
 * @param themeMode حالت انتخابی کاربر؛ «خودکار» از تم روشن/تاریک گوشی پیروی می‌کند.
 */
@Composable
fun MosafirTheme(
    themeMode: ThemeMode = ThemeMode.BANAFSH,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = themeMode == ThemeMode.AUTO && systemDark

    val scheme = when (themeMode) {
        ThemeMode.BANAFSH -> BanafshLight
        ThemeMode.ARGHAVANI -> ArghavaniLight
        ThemeMode.ORCHID -> OrchidLight
        ThemeMode.KHAKESTARI -> KhakestariLight
        ThemeMode.AUTO -> if (isDark) PurpleDark else BanafshLight
    }

    // رنگ تاکیدها: در تم‌های روشن = رنگ اصلی تم؛ در تم تاریک = بنفش روشن برای خوانایی
    val accent = if (isDark) Color(0xFFD3B8FF) else scheme.primary
    // رنگ نوار بالا: در تم تاریک، سطح تیره؛ در بقیه، رنگ اصلی تم
    val topBar = if (isDark) Color(0xFF1A1721) else scheme.primary

    CompositionLocalProvider(
        LocalThemePrimary provides accent,
        LocalTopBarColor provides topBar
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
