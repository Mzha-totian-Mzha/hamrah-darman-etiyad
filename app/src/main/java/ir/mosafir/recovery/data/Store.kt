package ir.mosafir.recovery.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ذخیره‌سازی محلی و آفلاین با SharedPreferences + Gson.
 */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("mosafir_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadSettings(): UserSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return UserSettings()
        return runCatching {
            gson.fromJson(raw, UserSettings::class.java)
        }.getOrNull() ?: UserSettings()
    }

    fun saveSettings(settings: UserSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun loadEntries(): List<JournalEntry> {
        val raw = prefs.getString(KEY_JOURNAL, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<JournalEntry>>() {}.type
            gson.fromJson<List<JournalEntry>>(raw, type) ?: emptyList()
        }.getOrNull() ?: emptyList()
    }

    fun saveEntries(entries: List<JournalEntry>) {
        prefs.edit().putString(KEY_JOURNAL, gson.toJson(entries)).apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        /** با تغییر ساختار UserSettings، کلید هم عوض می‌شود تا JSON قدیمی مشکلی نسازد */
        private const val KEY_SETTINGS = "settings_v3"
        private const val KEY_JOURNAL = "journal"
    }
}
