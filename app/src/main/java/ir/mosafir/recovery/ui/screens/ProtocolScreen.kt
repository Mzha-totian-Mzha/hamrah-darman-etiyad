package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.PlanStep
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.SubstanceType
import ir.mosafir.recovery.data.StepKind
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.AmberAccent
import ir.mosafir.recovery.ui.theme.VioletPrimary

/** خط سبز جدول — مرز ستون «مصرف» و «مدت»؛ اعداد از روی این خط چپ‌چین می‌شوند */
private val GreenRule = Color(0xFF43A047)

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
            // جدول پله‌ها — سه ستون: پله (راست) | مصرف (وسط) | مدت (چپ)
            SectionCard(title = "پله‌های درمان") {
                // سرستون
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "پله",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "مصرف",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.55f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(2.dp)
                            .height(18.dp)
                            .background(GreenRule)
                    )
                    Text(
                        "مدت",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                HorizontalDivider()

                plan.steps.forEachIndexed { index, step ->
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

/**
 * یک ردیف جدول — دقیقاً مثل جدول مرجع (عکس ۸۸):
 *  • پله: سمت راست
 *  • مصرف: وسط — عبارت «هر وعده ۲cc × ۳ = روزانه ۶cc» دست‌نخورده،
 *    اعداد چپ‌چین و چسبیده به خط سبز
 *  • مدت: سمت چپ — هیچ‌وقت روی اعداد مصرف نمی‌افتد
 */
@Composable
private fun StepRow(step: PlanStep, isCurrent: Boolean, endMillis: Long?) {
    val container = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    Surface(
        color = container,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ستون پله — راست
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OneLineText(
                        text = step.title,
                        baseFontSize = 12,
                        minFontSize = 7,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
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

                // ستون مصرف — وسط؛ اعداد چپ‌چین از خط سبز
                if (step.perDoseCc > 0) {
                    OneLineText(
                        text = "هر وعده ${formatDose(step.perDoseCc)}cc × ${formatNumber(step.dosesPerDay)} = روزانه ${formatDose(step.dailyCc)}cc",
                        baseFontSize = 12,
                        minFontSize = 7,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.55f)
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.55f)
                    )
                }

                // خط سبز جدول
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(GreenRule)
                )

                // ستون مدت — چپ
                OneLineText(
                    text = if (step.kind == StepKind.FINAL) "۲ تا ۲۱ روز"
                    else "${formatNumber(step.days)} روز",
                    baseFontSize = 11,
                    minFontSize = 7,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.6f)
                )
            }

            // یادداشت پله — زیر ردیف، بدون تداخل با ستون‌ها
            if (step.note.isNotBlank()) {
                Text(
                    text = step.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isCurrent && endMillis != null) {
                Text(
                    text = "پایان این پله: ${PersianDate.formatLong(endMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
