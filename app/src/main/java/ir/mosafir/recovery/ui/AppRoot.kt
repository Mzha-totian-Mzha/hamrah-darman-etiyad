package ir.mosafir.recovery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import ir.mosafir.recovery.data.JournalEntry
import ir.mosafir.recovery.data.Store
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.ui.screens.HomeScreen
import ir.mosafir.recovery.ui.screens.JournalScreen
import ir.mosafir.recovery.ui.screens.LibraryScreen
import ir.mosafir.recovery.ui.screens.ProtocolScreen
import ir.mosafir.recovery.ui.screens.SettingsScreen

enum class Tab(val titleFa: String, val icon: ImageVector) {
    HOME("خانه", Icons.Outlined.Home),
    PLAN("پروتکل", Icons.Outlined.EventNote),
    JOURNAL("ثبت روزانه", Icons.Outlined.EditNote),
    LIBRARY("کتابخانه", Icons.Outlined.MenuBook),
    SETTINGS("تنظیمات", Icons.Outlined.Settings)
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { Store(context) }

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

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.titleFa) },
                        label = { Text(t.titleFa) }
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
