package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.PlanStep
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.SubstanceType
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.AmberAccent
import ir.mosafir.recovery.ui.theme.VioletPrimary

@Composable
fun ProtocolScreen(settings: UserSettings) {
    val plan = remember(settings) { Protocol.buildPlan(settings) }
    val now = remember { System.currentTimeMillis() }
    val days = remember(settings) { Protocol.daysSinceStart(settings, now) }
    val currentIndex = remember(settings) { Protocol.currentStepIndex(plan, days) }
    val usesOtLadder = settings.substance != SubstanceType.STIMULANT &&
        settings.substance != SubstanceType.ALCOHOL

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "پروتکل درمان (DST)",
                subtitle = "پله‌های برنامه ${settings.substance.program} — اصلاحیه ۱۳۹۸/۳/۱"
            )
        }

        item {
            SectionCard(title = "مشخصات برنامه") {
                ParamRow("نوع ماده", settings.substance.label)
                ParamRow("برنامه اجرایی", settings.substance.program)
                ParamRow("OT روزانه در پله اول", "${formatDose(plan.startDailyCc)} سی‌سی")
                ParamRow("سقف OT روزانه", "${formatDose(plan.capDailyCc)} سی‌سی")
                ParamRow(
                    "طول کل برنامه",
                    "${formatNumber(plan.totalDays)} روز (حدود ${formatNumber((plan.totalDays / 30.4).toInt() + 1)} ماه)"
                )
            }
        }

        item {
            // جدول پله‌ها
            SectionCard(title = "پله‌های درمان") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "پله",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(96.dp)
                    )
                    Text(
                        "مصرف",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        "مدت",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()

                itemsIndexed(plan.steps) { index, step ->
                    StepRow(
                        step = step,
                        isCurrent = index == currentIndex,
                        endMillis = settings.startDateMillis?.let { start ->
                            start + (Protocol.stepStartDay(plan, index) + step.days) * 86_400_000L
                        }
                    )
                }
            }
        }

        if (usesOtLadder) {
            item {
                SectionCard(title = "توضیح جدول OT") {
                    Protocol.OT_LADDER_NOTES.forEach { note ->
                        Text(
                            text = "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            NoticeCard(
                text = "واحد درمان: شربت اپیوم تینکچر (سی‌سی). ورود به جدول کاهش و تغییر پله‌ها فقط با تشخیص راهنما انجام می‌شود؛ تکرار پله در کل سفر فقط یک‌بار و افزایش پله حداکثر تا ۲ پله مجاز است."
            )
        }

        item {
            SectionCard {
                Text(
                    text = "فاصله وعده‌ها ۸ ساعت است (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲). ساعت وعده شب بنا به تشخیص راهنما می‌تواند تا یک ساعت پایین‌تر بیاید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = VioletPrimary
        )
    }
}

@Composable
private fun StepRow(step: PlanStep, isCurrent: Boolean, endMillis: Long?) {
    val container = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    Surface(
        color = container,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(VioletPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "الان",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Text(
                    text = if (step.kind == StepKind.FINAL) "۲ تا ۲۱ روز"
                    else "${formatNumber(step.days)} روز",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (step.perDoseCc > 0)
                    "هر وعده ${formatDose(step.perDoseCc)}cc × ${formatNumber(step.dosesPerDay)} = روزانه ${formatDose(step.dailyCc)}cc"
                else step.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (step.note.isNotBlank() && step.perDoseCc > 0) {
                Text(
                    text = step.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCurrent && endMillis != null) {
                Text(
                    text = "پایان این پله: ${PersianDate.formatLong(endMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
