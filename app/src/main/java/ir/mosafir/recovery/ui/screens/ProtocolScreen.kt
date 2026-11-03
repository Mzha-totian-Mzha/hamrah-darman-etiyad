package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.PlanStep
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.StepKind
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.AmberAccent
import ir.mosafir.recovery.ui.theme.NavTeal

/** سبز تیک پله‌های تمام‌شده — مثل پیش‌نمایش */
private val GreenOK = Color(0xFF2E7D32)

/**
 * صفحهٔ پروتکل — مطابق پیش‌نمایش وب (عکس ۹):
 *  • کارت بالا: توضیح پلهٔ جاری یا پلهٔ انتخاب‌شده با کلیک
 *  • کاشی‌های ۱۵ پله — ۳ ستون × ۵ ردیف؛ با کلیک هایلایت و توضیح در کارت بالا
 */
@Composable
fun ProtocolScreen(settings: UserSettings) {
    val plan = remember(settings) { Protocol.buildPlan(settings) }
    val now = remember { System.currentTimeMillis() }
    val days = remember(settings) { Protocol.daysSinceStart(settings, now) }
    val currentIndex = remember(settings) { Protocol.currentStepIndex(plan, days) }
    val usesOtLadder = settings.substance != ir.mosafir.recovery.data.SubstanceType.STIMULANT &&
        settings.substance != ir.mosafir.recovery.data.SubstanceType.ALCOHOL
    val hasStart = settings.startDateMillis != null

    val currentStep = currentIndex?.let { plan.steps[it] }
    val currentLadderIdx = currentStep
        ?.let { step -> Protocol.OT_LADDER.indexOfFirst { it.title == step.title } }
        ?: -1

    // پلهٔ انتخاب‌شده با کلیک — هایلایت کاشی + توضیح در کارت بالا
    var selStep by remember { mutableIntStateOf(-1) }

    val ladderStartDay = { i: Int ->
        Protocol.OT_LADDER.take(i).sumOf { it.days }
    }
    val ladderTotal = Protocol.OT_LADDER.sumOf { it.days }

    // محتوای کارت بالا — انتخاب‌شده یا جاری
    val viewStep: PlanStep? = if (selStep >= 0) Protocol.OT_LADDER[selStep] else currentStep
    val viewTitle = when {
        selStep >= 0 ->
            "پلهٔ ${formatNumber(selStep + 1)} از ${formatNumber(Protocol.OT_LADDER.size)} — " +
                "روز ${formatNumber(ladderStartDay(selStep) + 1)} تا ${formatNumber(ladderStartDay(selStep) + Protocol.OT_LADDER[selStep].days)} سفر"
        currentStep != null ->
            "${currentStep.title} — روز ${formatNumber(Protocol.stepStartDay(plan, currentIndex!!) + 1)} تا ${formatNumber(Protocol.stepStartDay(plan, currentIndex) + currentStep.days)} سفر"
        else -> "برای دیدن پلهٔ جاری، تاریخ شروع درمان را در تنظیمات ثبت کن"
    }
    val viewNote = if (selStep >= 0) Protocol.OT_LADDER[selStep].note
    else "هر پله ۲۱ روز • کاهش ۲۰٪ مصرف نسبت به پلهٔ قبل"
    val viewProgress = when {
        selStep >= 0 -> ladderStartDay(selStep).toFloat() / ladderTotal.toFloat()
        plan.totalDays > 0 -> (days.toFloat() / plan.totalDays.toFloat()).coerceIn(0.02f, 1f)
        else -> 0f
    }
    val viewEndDays: Int = if (selStep >= 0) ladderStartDay(selStep) + Protocol.OT_LADDER[selStep].days
    else currentIndex?.let { idx -> (Protocol.stepStartDay(plan, idx) + plan.steps[idx].days).toInt() } ?: 0

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "پروتکل",
                subtitle = "پله‌های کاهش مصرف — متد DST (اصلاحیهٔ ۱۳۹۸/۳/۱)",
                accent = NavTeal
            )
        }

        // کارت بالای درمان — با کلیک روی هر پله، توضیح همان پله همین‌جا نمایش داده می‌شود
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = viewNote,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NavTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = NavTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // نوار پیشرفت
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(viewProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(NavTeal)
                    )
                }

                // جعبهٔ مصرف هر وعده
                if (viewStep != null && viewStep.perDoseCc > 0) {
                    Surface(
                        color = NavTeal.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = null,
                                tint = NavTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "مصرف هر وعده: ${formatDose(viewStep.perDoseCc)}cc — " +
                                    (when (viewStep.dosesPerDay) {
                                        3 -> "سه"
                                        2 -> "دو"
                                        else -> "یک"
                                    }) + " وعده در شبانه‌روز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = NavTeal
                            )
                        }
                    }
                }

                // پایان این پله
                if (viewStep != null && settings.startDateMillis != null && viewEndDays > 0) {
                    Text(
                        text = "پایان این پله: ${PersianDate.formatLong(settings.startDateMillis + viewEndDays * 86_400_000L)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // راهنمای بازگشت به پلهٔ جاری
                if (selStep >= 0 && selStep != currentLadderIdx) {
                    Text(
                        text = "برای بازگشت به پلهٔ جاری، دوباره روی همان پله بزن.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // پله‌ها — کاشی ۳تایی × ۵ ردیف (۱۵ پلهٔ DST)
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پله‌ها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${formatNumber(Protocol.OT_LADDER.size)} پله تا رهایی • هر پله ۲۱ روز",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Protocol.OT_LADDER.chunked(3).forEachIndexed { rowIdx, rowSteps ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSteps.forEachIndexed { colIdx, step ->
                            val i = rowIdx * 3 + colIdx
                            val isCurrent = hasStart && i == currentLadderIdx
                            val isDone = hasStart && currentLadderIdx >= 0 && i < currentLadderIdx
                            val isSelected = selStep == i
                            val teal = isSelected || (selStep < 0 && isCurrent)

                            val bg = when {
                                teal -> NavTeal
                                isDone -> GreenOK.copy(alpha = 0.1f)
                                isCurrent -> NavTeal.copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val border = when {
                                teal -> BorderStroke(1.dp, NavTeal)
                                isDone -> BorderStroke(1.dp, GreenOK.copy(alpha = 0.35f))
                                isCurrent -> BorderStroke(1.dp, NavTeal)
                                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = bg,
                                border = border,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selStep = if (isSelected) -1 else i }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isDone && !isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = GreenOK,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.height(14.dp))
                                    }
                                    Text(
                                        text = "پلهٔ ${formatNumber(i + 1)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (teal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (step.kind == StepKind.FINAL) "۲ تا ۲۱ روز"
                                        else "${formatNumber(step.days)} روز",
                                        fontSize = 9.5.sp,
                                        color = if (teal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                        // پرکردن ردیف ناقص — جدول ۱۵تایی همیشه ۳تایی است ولی برای اطمینان
                        repeat(3 - rowSteps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = "جدول تیپرینگ OT — پیوست ۱ (۱۵ پله) • با کلیک روی هر پله، توضیح آن بالا نمایش داده می‌شود",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }
        }

        item {
            SectionCard(title = "مشخصات برنامه", accent = NavTeal) {
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

        if (usesOtLadder) {
            item {
                SectionCard(title = "توضیح جدول OT", accent = NavTeal) {
                    Protocol.OT_LADDER_NOTES.forEach { note ->
                        Text(
                            text = "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Justify
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
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
            color = NavTeal
        )
    }
}
