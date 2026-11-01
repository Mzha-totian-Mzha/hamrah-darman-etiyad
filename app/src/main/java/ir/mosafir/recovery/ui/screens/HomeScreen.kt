package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.data.JournalEntry
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.PlanStep
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.StatTile
import ir.mosafir.recovery.ui.WeCanBadge
import ir.mosafir.recovery.ui.theme.VioletPrimary
import java.util.Calendar

private val QUOTES = listOf(
    "ما می‌توانیم؛ کافیست بخواهیم و تلاش کنیم.",
    "راه سخت است، اما رسیدن به آرامش، ارزش هر قدم را دارد.",
    "هر روز پاک، یک پیروزی بزرگ است.",
    "نظم و صبر، دو بال پرواز به سوی سلامتی‌اند.",
    "درمان یک سفر است؛ قدم به قدم و با عشق.",
    "با ورزش، نظم و حضور در جلسات، سفرت را محکم‌تر کن."
)

@Composable
fun HomeScreen(
    settings: UserSettings,
    entries: List<JournalEntry>,
    onGoToSettings: () -> Unit
) {
    val plan = remember(settings) { Protocol.buildPlan(settings) }
    val now = remember { System.currentTimeMillis() }
    val days = remember(settings) { Protocol.daysSinceStart(settings, now) }
    val stepIndex = remember(settings) { Protocol.currentStepIndex(plan, days) }
    val step: PlanStep? = stepIndex?.let { plan.steps[it] }
    val times = remember(settings) { Protocol.doseTimes(settings.doseModel) }
    val progress = if (plan.totalDays > 0) {
        ((days + 1).toFloat() / plan.totalDays.toFloat()).coerceIn(0f, 1f)
    } else null

    val quote = remember {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        QUOTES[dayOfYear % QUOTES.size]
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سلام مسافر 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = VioletPrimary
                    )
                    Text(
                        text = "امروز روز ${formatNumber(days + 1)} سفر توست",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                WeCanBadge()
            }
        }

        if (settings.startDateMillis == null) {
            item {
                SectionCard(title = "شروع سفر") {
                    Text(
                        text = "برای فعال شدن برنامه DST، اول نوع ماده مصرفی، تاریخ شروع و مقدار مصرف را در تنظیمات ثبت کن؛ پله‌های برنامه همان‌جا ساخته می‌شود.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onGoToSettings) {
                        Text("ثبت اطلاعات در تنظیمات")
                    }
                }
            }
        } else {
            item {
                SectionCard(title = "وضعیت درمان") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            label = "روز سفر",
                            value = formatNumber(days + 1),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "پله جاری",
                            value = step?.title ?: "پایان",
                            modifier = Modifier.weight(1.4f)
                        )
                        StatTile(
                            label = "هر وعده",
                            value = if (step != null && step.perDoseCc > 0) "${formatDose(step.perDoseCc)}cc" else "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "پیشرفت سفر: ${formatNumber((progress * 100).toInt())}٪ از کل ${formatNumber(plan.totalDays)} روز",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "تاریخ شروع: ${PersianDate.formatLong(settings.startDateMillis!!)} · ${settings.substance.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (step != null) {
                item {
                    SectionCard(title = "نوبت‌های امروز") {
                        for (i in 0 until step.dosesPerDay.coerceIn(1, 3)) {
                            val done = entries.firstOrNull { it.dayMillis == startOfToday() }
                                ?.doseTaken
                                ?.getOrElse(i) { false } ?: false
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = times.getOrElse(i) { "نوبت ${formatNumber(i + 1)}" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (done)
                                        "✅ ${formatDose(step.perDoseCc)}cc مصرف شد"
                                    else
                                        "⏳ ${formatDose(step.perDoseCc)}cc باقی است",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (done) VioletPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (step.note.isNotBlank()) {
                            Text(
                                text = step.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionCard(title = "پایان برنامه") {
                        Text(
                            text = "همه پله‌های این برنامه طی شده است. طبق جزوه، پله آخر (۰٫۳cc در ۲۴ ساعت) ۲ تا ۲۱ روز ادامه دارد و پس از دیدن شرایط پله آخر، مصرف با نظر راهنما قطع می‌شود. خدا قوت — سفر دوم آغاز شد. 🌱",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            SectionCard {
                Text(
                    text = "«$quote»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            NoticeCard(
                text = "این برنامه همراه آموزشی است و به‌هیچ‌وجه جایگزین راهنما، لژیون و نظر پزشک نیست. هر تغییری در پله‌ها فقط با هماهنگی راهنمای خود انجام بده."
            )
        }
    }
}

private fun startOfToday(): Long = Protocol.normalizeDay(System.currentTimeMillis())
