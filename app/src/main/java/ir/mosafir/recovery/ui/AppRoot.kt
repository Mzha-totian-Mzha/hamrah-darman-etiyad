package ir.mosafir.recovery.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.data.JournalEntry
import ir.mosafir.recovery.data.Store
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.ui.screens.ExamScreen
import ir.mosafir.recovery.ui.screens.HomeScreen
import ir.mosafir.recovery.ui.screens.JournalScreen
import ir.mosafir.recovery.ui.screens.LibraryScreen
import ir.mosafir.recovery.ui.screens.ProtocolScreen
import ir.mosafir.recovery.ui.screens.SettingsScreen
import ir.mosafir.recovery.ui.theme.AppTopBarColor
import ir.mosafir.recovery.ui.theme.MosafirTheme
import ir.mosafir.recovery.ui.theme.ThemeMode

/**
 * تب‌های پایین صفحه — آیکون‌های قبلی دست‌نخورده؛ فقط تب «امتحان» اضافه شده است.
 */
enum class Tab(val titleFa: String, val icon: ImageVector) {
    HOME("خانه", Icons.Outlined.Home),
    PLAN("پروتکل", Icons.Outlined.EventNote),
    JOURNAL("ثبت روزانه", Icons.Outlined.EditNote),
    LIBRARY("کتابخانه", Icons.Outlined.MenuBook),
    EXAM("امتحان", Icons.Outlined.Quiz),
    SETTINGS("تنظیمات", Icons.Outlined.Settings)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { Store(context) }

    // وضعیت تم — با SharedPreferences ماندگار می‌شود
    val themePrefs = remember { context.getSharedPreferences("mosafir_theme", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(ThemeMode.from(themePrefs.getString("mode", null))) }
    var themeMenuOpen by remember { mutableStateOf(false) }

    var settings by remember { mutableStateOf(store.loadSettings()) }
    var entries by remember { mutableStateOf(store.loadEntries()) }
    var tab by remember { mutableStateOf(Tab.HOME) }

    fun updateSettings(s: UserSettings) {
        settings = s
        store.saveSettings(s)
    }

    fun updateEntries(list: List<JournalEntry>) {
        entries = list
        store.saveEntries(list)
    }

    MosafirTheme(themeMode = themeMode) {
        // رنگ نوار وضعیت گوشی هماهنگ با نوار بالا
        val view = LocalView.current
        val topBarColor = AppTopBarColor
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = topBarColor.toArgb()
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = false
            }
        }

        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "همراه درمان اعتیاد",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                // انتخابگر تم — سمت چپ، درست زیر آیکون باتری
                                Box {
                                    IconButton(onClick = { themeMenuOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Palette,
                                            contentDescription = "انتخاب تم"
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = themeMenuOpen,
                                        onDismissRequest = { themeMenuOpen = false }
                                    ) {
                                        ThemeMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = mode.labelFa,
                                                        fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(
                                                                brush = if (mode == ThemeMode.AUTO)
                                                                    Brush.verticalGradient(
                                                                        listOf(
                                                                            topBarColor,
                                                                            MaterialTheme.colorScheme.background
                                                                        )
                                                                    )
                                                                else
                                                                    Brush.verticalGradient(
                                                                        listOf(mode.swatch, mode.swatch)
                                                                    ),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (mode == themeMode) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    themeMode = mode
                                                    themePrefs.edit().putString("mode", mode.name).apply()
                                                    themeMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = AppTopBarColor,
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White,
                                navigationIconContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    icon = { Icon(t.icon, contentDescription = t.titleFa) },
                                    label = {
                                        Text(
                                            t.titleFa,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (tab) {
                            Tab.HOME -> HomeScreen(
                                settings = settings,
                                entries = entries,
                                onGoToSettings = { tab = Tab.SETTINGS }
                            )
                            Tab.PLAN -> ProtocolScreen(settings = settings)
                            Tab.JOURNAL -> JournalScreen(
                                settings = settings,
                                entries = entries,
                                onSave = { updateEntries(it) }
                            )
                            Tab.LIBRARY -> LibraryScreen()
                            Tab.EXAM -> ExamScreen()
                            Tab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onSaveSettings = { updateSettings(it) },
                                onResetAll = {
                                    updateSettings(UserSettings())
                                    updateEntries(emptyList())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
