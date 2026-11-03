package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.data.JournalEntry
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.NavViolet

private val MOODS = listOf("😞", "😕", "😐", "🙂", "😄")

@Composable
fun JournalScreen(
    settings: UserSettings,
    entries: List<JournalEntry>,
    onSave: (List<JournalEntry>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val todayEntry = entries.firstOrNull { it.dayMillis == Protocol.normalizeDay(System.currentTimeMillis()) }

    // تعداد نوبت‌های امروز بر اساس پله جاری برنامه (TDS/BID/DAILY)
    val plan = remember(settings) { Protocol.buildPlan(settings) }
    val days = remember(settings) { Protocol.daysSinceStart(settings, System.currentTimeMillis()) }
    val stepDoses = remember(settings) {
        Protocol.currentStepIndex(plan, days)?.let { plan.steps[it].dosesPerDay } ?: 3
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "ثبت روزانه",
                    subtitle = "هر شب چند دقیقه وقت بگذار؛ نگاه به گذشته، چراغ راه آینده است",
                    accent = NavViolet
                )
            }

            item {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavViolet)
                ) {
                    Text(if (todayEntry == null) "ثبت وضعیت امروز" else "ویرایش ثبت امروز")
                }
            }

            if (entries.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            text = "هنوز چیزی ثبت نشده. بعد از نوبت مصرف شب، حالت را ثبت کن تا مسیر پیشرفتت را ببینی.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(entries.sortedByDescending { it.dayMillis }, key = { it.id }) { entry ->
                EntryCard(
                    entry = entry,
                    doses = stepDoses,
                    onDelete = {
                        onSave(entries.filterNot { it.id == entry.id })
                    }
                )
            }
        }
    }

    if (showDialog) {
        EntryDialog(
            existing = todayEntry,
            doses = stepDoses,
            onDismiss = { showDialog = false },
            onConfirm = { newEntry ->
                val updated = entries.filterNot { it.id == newEntry.id } + newEntry
                onSave(updated.sortedByDescending { it.dayMillis })
                showDialog = false
            }
        )
    }
}

@Composable
private fun EntryCard(entry: JournalEntry, doses: Int, onDelete: () -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = PersianDate.formatLong(entry.dayMillis),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until doses) {
                val taken = entry.doseTaken.getOrElse(i) { false }
                Text(
                    text = if (taken) "✅" else "⬜️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "   ${MOODS.getOrElse(entry.mood) { "😐" }}",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (entry.note.isNotBlank()) {
            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryDialog(
    existing: JournalEntry?,
    doses: Int,
    onDismiss: () -> Unit,
    onConfirm: (JournalEntry) -> Unit
) {
    val today = Protocol.normalizeDay(System.currentTimeMillis())

    var morning by remember { mutableStateOf(existing?.doseTaken?.getOrElse(0) { false } ?: false) }
    var noon by remember { mutableStateOf(existing?.doseTaken?.getOrElse(1) { false } ?: false) }
    var night by remember { mutableStateOf(existing?.doseTaken?.getOrElse(2) { false } ?: false) }
    var mood by remember { mutableStateOf(existing?.mood ?: 2) }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    val taken = buildList {
        add(morning)
        add(noon)
        add(night)
        repeat((doses - 3).coerceAtLeast(0)) { add(false) }
    }.take(doses)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت امروز") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نوبت‌های مصرف‌شده:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = morning, onCheckedChange = { morning = it })
                    Text("صبح")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = noon, onCheckedChange = { noon = it })
                    Text("ظهر")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = night, onCheckedChange = { night = it })
                    Text("شب")
                }

                Text("حالت امروز چطور بود؟", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOODS.forEachIndexed { index, emoji ->
                        TextButton(onClick = { mood = index }) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (mood == index) NavViolet else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text(
                    text = "تاریخ: ${PersianDate.formatLong(today)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    JournalEntry(
                        id = today.toString(),
                        dayMillis = today,
                        doseTaken = taken,
                        mood = mood,
                        note = note
                    )
                )
            }) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
