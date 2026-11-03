package ir.mosafir.recovery.ui.screens

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.data.JournalEntry
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.PlanStep
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.WeCanBadge
import ir.mosafir.recovery.ui.theme.HeroOrange
import ir.mosafir.recovery.ui.theme.HeroPink
import ir.mosafir.recovery.ui.theme.NavAmber
import ir.mosafir.recovery.ui.theme.NavTeal
import ir.mosafir.recovery.ui.theme.NavViolet
import java.util.Calendar

private val QUOTES = listOf(
    "ما می‌توانیم؛ کافیست بخواهیم و تلاش کنیم.",
    "راه سخت است، اما رسیدن به آرامش، ارزش هر قدم را دارد.",
    "هر روز پاک، یک پیروزی بزرگ است.",
    "نظم و صبر، دو بال پرواز به سوی سلامتی‌اند.",
    "درمان یک سفر است؛ قدم به قدم و با عشق.",
    "با ورزش، نظم و حضور در جلسات، سفرت را محکم‌تر کن."
)

/** چیپ گرد رنگی با آیکون — سبک «پلنر من» */
@Composable
private fun ChipIcon(color: Color, icon: ImageVector, chipSize: Int = 36, iconSize: Int = 19) {
    Box(
        modifier = Modifier
            .size(chipSize.dp)
            .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

/** کارت آمار کوچک با چیپ رنگی — برای شبکهٔ ۲×۲ خانه */
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipIcon(color, icon)
            Spacer(Modifier.width(10.dp))
            Column {
                OneLineText(
                    text = value,
                    baseFontSize = 17,
                    minFontSize = 9,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start
                )
                OneLineText(
                    text = label,
                    baseFontSize = 11,
                    minFontSize = 8,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/** دکمهٔ شیشه‌ای داخل کارت گرادیانی هیرو */
@Composable
private fun HeroPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HomeScreen(
    settings: UserSettings,
    entries: List<JournalEntry>,
    onGoToSettings: () -> Unit,
    onGoToExam: () -> Unit,
    onGoToJournal: () -> Unit
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

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ۱) سلام + مُهر «ما می‌توانیم»
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سلام مسافر 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "امروز روز ${formatNumber(days + 1)} سفر توست",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                WeCanBadge()
            }
        }

        // ۲) کارت گرادیانی هیرو — نارنجی → صورتی (سبک «پلنر من»)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(HeroOrange, HeroPink)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (settings.startDateMillis == null) {
                        Text(
                            text = "سفرت را شروع کن!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "برای فعال شدن برنامه DST، اول نوع مادهٔ مصرفی، تاریخ شروع و مقدار مصرف را در تنظیمات ثبت کن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Justify
                        )
                        Spacer(Modifier.height(14.dp))
                        HeroPill(
                            label = "ثبت اطلاعات در تنظیمات",
                            icon = Icons.Outlined.Settings,
                            onClick = onGoToSettings
                        )
                    } else {
                        Text(
                            text = "روز پاک من",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        Text(
                            text = formatNumber(days + 1),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "از رهایی تا رهایی — متد DST",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HeroPill(
                                label = "شروع آزمون",
                                icon = Icons.Outlined.Quiz,
                                modifier = Modifier.weight(1f),
                                onClick = onGoToExam
                            )
                            Spacer(Modifier.width(10.dp))
                            HeroPill(
                                label = "ثبت روزانه",
                                icon = Icons.Outlined.EditNote,
                                modifier = Modifier.weight(1f),
                                onClick = onGoToJournal
                            )
                        }
                    }
                }
            }
        }

        if (settings.startDateMillis != null) {
            // ۳) شبکهٔ ۲×۲ کارت‌های آمار با چیپ‌های رنگی
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "روز سفر",
                        value = formatNumber(days + 1),
                        icon = Icons.Outlined.CalendarMonth,
                        color = HeroOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "پلهٔ جاری",
                        value = step?.title ?: "پایان",
                        icon = Icons.Outlined.Flag,
                        color = NavTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "هر وعده",
                        value = if (step != null && step.perDoseCc > 0) "${formatDose(step.perDoseCc)}cc" else "—",
                        icon = Icons.Outlined.WaterDrop,
                        color = NavViolet,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "پیشرفت سفر",
                        value = progress?.let { "${formatNumber((it * 100).toInt())}٪" } ?: "—",
                        icon = Icons.Outlined.TrendingUp,
                        color = HeroPink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ۴) وضعیت درمان — نوار پیشرفت + تاریخ شروع
            item {
                SectionCard(title = "وضعیت درمان", accent = HeroOrange) {
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = HeroOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                // ۵) نوبت‌های امروز
                item {
                    SectionCard(title = "نوبت‌های امروز", accent = HeroOrange) {
                        for (i in 0 until step.dosesPerDay.coerceIn(1, 3)) {
                            val done = entries.firstOrNull { it.dayMillis == startOfToday() }
                                ?.doseTaken
                                ?.getOrElse(i) { false } ?: false
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = if (done) NavTeal else NavAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = times.getOrElse(i) { "نوبت ${formatNumber(i + 1)}" },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = if (done)
                                        "${formatDose(step.perDoseCc)}cc مصرف شد"
                                    else
                                        "${formatDose(step.perDoseCc)}cc باقی است",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (done) NavTeal else HeroOrange
                                )
                            }
                        }
                        if (step.note.isNotBlank()) {
                            Text(
                                text = step.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionCard(title = "پایان برنامه", accent = HeroOrange) {
                        Text(
                            text = "همه پله‌های این برنامه طی شده است. طبق جزوه، پله آخر (۰٫۳cc در ۲۴ ساعت) ۲ تا ۲۱ روز ادامه دارد و پس از دیدن شرایط پله آخر، مصرف با نظر راهنما قطع می‌شود. خدا قوت — سفر دوم آغاز شد. 🌱",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            // ۶) کارت دعوت به آزمون
            item {
                Card(
                    onClick = onGoToExam,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChipIcon(NavTeal, Icons.Outlined.Quiz, chipSize = 40, iconSize = 21)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "آزمون امروزت رو بده!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "۱۰ سؤال — چند دقیقه بیشتر وقت نمی‌گیره",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // ۷) نقل‌قول همیشه یک‌خطی
        item {
            SectionCard {
                OneLineQuote(text = "«$quote»")
            }
        }

        // ۸) یادآوری
        item {
            NoticeCard(
                text = "این برنامه همراه آموزشی است و به‌هیچ‌وجه جایگزین راهنما و لژیون نیست. هر تغییری در پله‌ها فقط با هماهنگی راهنمای خود انجام بده."
            )
        }
    }
}

private fun startOfToday(): Long = Protocol.normalizeDay(System.currentTimeMillis())

/**
 * نقل‌قول همیشه در «یک خط» — اگر متن در عرض صفحه جا نشود،
 * اندازه فونت خودکار کمی کوچک می‌شود تا هرگز دوخطی نشود.
 */
@Composable
private fun OneLineQuote(text: String) {
    var fontSizeSp by remember(text) { mutableStateOf(15) }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeSp.sp),
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSizeSp > 10) fontSizeSp = fontSizeSp - 1
        }
    )
}
