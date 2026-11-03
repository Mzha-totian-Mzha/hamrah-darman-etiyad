package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.foundation.shape.CircleShape
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.Protocol
import ir.mosafir.recovery.data.SubstanceType
import ir.mosafir.recovery.data.UserSettings
import ir.mosafir.recovery.data.formatDose
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.NavViolet
import java.util.Calendar
import java.util.TimeZone

/** مدل‌های ساعت وعده‌ها بر اساس جدول «فاصله روزانه وعده‌ها در متد DST» — اصلاحیه */
private val DOSE_MODEL_OPTIONS = listOf(
    1 to "مدل ۱ (۵–۱۳–۲۱)",
    2 to "مدل ۲ (۶–۱۴–۲۲)",
    3 to "مدل ۳ (۷–۱۵–۲۳)"
)

/** گزینه‌های مقدار مصرف برای مواد خوراکی (گرم) */
private val GRAM_OPTIONS = listOf(0.5, 1.0, 1.5, 2.0)

/** گزینه‌های مصرف روزانه شربت متادون (سی‌سی) */
private val METHADONE_OPTIONS = listOf(5.0, 10.0, 16.5)

/** گزینه‌های تعداد قرص ترامادول ۱۰۰ میلی‌گرمی در روز */
private val TRAMADOL_OPTIONS = listOf(2.0, 4.0, 6.0, 10.0)

/** رنگ نقطهٔ شماره‌دار هر ماده — الگوی چیپ‌های شماره‌دار «مواد مصرفی» (عکس ۱) */
private val SUBSTANCE_DOTS: Map<SubstanceType, Color> = mapOf(
    SubstanceType.OPIUM_SMOKED to Color(0xFFF97316),
    SubstanceType.OPIUM_ORAL to Color(0xFF14B8A6),
    SubstanceType.HEROIN to Color(0xFFEC4899),
    SubstanceType.ALCOHOL to Color(0xFFE11D48),
    SubstanceType.STIMULANT to Color(0xFF8B5CF6),
    SubstanceType.METHADONE to Color(0xFFF59E0B),
    SubstanceType.TRAMADOL to Color(0xFF10B981),
    SubstanceType.SHIREH_ORAL to Color(0xFF64748B),
    SubstanceType.SOKHTE to Color(0xFFB45309),
    SubstanceType.SLEEPING to Color(0xFF475569),
    SubstanceType.OTHER to Color(0xFF78716C)
)

/**
 * چیپ فشرده مدل ساعت — دقیقاً مثل پیش‌نمایش:
 * متن کامل هر مدل همیشه در «یک خط»، چیپ‌ها مرتب کنار هم،
 * با حاشیه پررنگ دور تا دور (انتخاب‌شده: حاشیه بنفش پررنگ‌تر).
 */
@Composable
private fun ModelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 1.5.dp else 1.25.dp,
                color = if (selected) NavViolet else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** کاشی شماره‌دار مادهٔ مصرفی — نقطهٔ رنگی + برچسب یک‌خطی، همه هم‌اندازه */
@Composable
private fun SubstanceTile(
    index: Int,
    type: SubstanceType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dot = SUBSTANCE_DOTS[type] ?: NavViolet
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) dot.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 1.75.dp else 1.25.dp,
                color = if (selected) dot else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(dot, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatNumber(index + 1),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = type.label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) dot else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSaveSettings: (UserSettings) -> Unit,
    onResetAll: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "تنظیمات درمان",
                subtitle = "نوع ماده و برنامه درمانی را فقط با هماهنگی راهنمای خود انتخاب کن",
                accent = NavViolet
            )
        }

        item {
            SectionCard(accent = NavViolet) {
                // تاریخ شروع درمان همیشه در «یک خط» — متن پایین به متن بالا می‌چسبد و ادامهٔ همان خط است
                OneLineText(
                    text = if (settings.startDateMillis != null)
                        "تاریخ شروع درمان: ${PersianDate.formatLong(settings.startDateMillis!!)}"
                    else "تاریخ شروع درمان: هنوز ثبت نشده است",
                    baseFontSize = 15,
                    minFontSize = 9,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavViolet,
                    textAlign = TextAlign.Start
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavViolet)
                    ) {
                        Text("انتخاب تاریخ")
                    }
                    OutlinedButton(onClick = {
                        onSaveSettings(settings.copy(startDateMillis = Protocol.normalizeDay(System.currentTimeMillis())))
                    }) {
                        Text("از امروز")
                    }
                }
                Text(
                    text = "تاریخ شروع، روز اول پله نخستِ برنامه ماده انتخابی است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
                )
            }
        }

        item {
            SectionCard(title = "نوع ماده مصرفی", accent = NavViolet) {
                // کاشی‌های شماره‌دار ۲تایی در ردیف‌های ۲ستونه — الگوی چیپ‌های «مواد مصرفی» (عکس ۱)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubstanceType.entries.chunked(2).forEachIndexed { rowIdx, rowTypes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowTypes.forEachIndexed { colIdx, type ->
                                SubstanceTile(
                                    index = rowIdx * 2 + colIdx,
                                    type = type,
                                    selected = settings.substance == type,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onSaveSettings(
                                            settings.copy(
                                                substance = type,
                                                consumption = Protocol.defaultConsumption(type)
                                            )
                                        )
                                    }
                                )
                            }
                            if (rowTypes.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                // تأیید ثبت انتخاب — بلافاصله زیر همان لیست، در همان صفحه (طراحی مصوب)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF16A34A).copy(alpha = 0.08f))
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFF16A34A).copy(alpha = 0.33f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).background(Color(0xFF16A34A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✓", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = "ثبت شد — «${settings.substance.label}» انتخاب و ذخیره شد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "برنامه اجرایی: ${settings.substance.program}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            when (settings.substance.consumptionLabel) {
                null -> {
                    SectionCard(title = "مقدار مصرف", accent = NavViolet) {
                        Text(
                            text = settings.substance.consumptionHint,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
                else -> {
                    val options = when (settings.substance) {
                        SubstanceType.OPIUM_ORAL, SubstanceType.SHIREH_ORAL -> GRAM_OPTIONS
                        SubstanceType.METHADONE -> METHADONE_OPTIONS
                        SubstanceType.TRAMADOL -> TRAMADOL_OPTIONS
                        else -> emptyList()
                    }
                    SectionCard(title = settings.substance.consumptionLabel, accent = NavViolet) {
                        if (options.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                options.forEach { option ->
                                    val selectedOption = settings.consumption == option
                                    FilterChip(
                                        selected = selectedOption,
                                        onClick = {
                                            onSaveSettings(settings.copy(consumption = option))
                                        },
                                        label = {
                                            Text(
                                                when (settings.substance) {
                                                    SubstanceType.TRAMADOL -> "${formatNumber(option.toInt())} قرص"
                                                    else -> formatDose(option)
                                                }
                                            )
                                        },
                                        border = BorderStroke(
                                            1.25.dp,
                                            if (selectedOption) NavViolet else MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            text = settings.substance.consumptionHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "مدل ساعت وعده‌ها", accent = NavViolet) {
                // سه مدل مثل پیش‌نمایش: چیپ‌های فشرده کنار هم، متن کامل هر مدل در یک خط
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DOSE_MODEL_OPTIONS.forEach { (model, label) ->
                        ModelChip(
                            label = label,
                            selected = settings.doseModel == model,
                            onClick = { onSaveSettings(settings.copy(doseModel = model)) }
                        )
                    }
                }
                Text(
                    text = "چطور انتخاب کنم؟ سه وعده همیشه ۸ ساعت فاصله دارند؛ وعده اول بعد از بیداری و وعده آخر نزدیک خواب است. " +
                        "اگر حدود ۵ صبح بیدار می‌شوی مدل ۱، حدود ۶ صبح مدل ۲ و حدود ۷ صبح مدل ۳ مناسب است. " +
                        "ساعت وعده شب با تشخیص راهنما می‌تواند تا یک ساعت پایین‌تر بیاید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
                )
            }
        }

        item {
            NoticeCard(
                text = "همه پله‌ها و مقادیر این برنامه از «اصلاحیه پروتکل DST (۱۳۹۸/۳/۱)» و راهنمای ویرایش ۹۹ گرفته شده است. انتخاب پله شروع و هر تغییر دیگری فقط با صلاحدید راهنمای تو انجام می‌شود."
            )
        }

        item {
            SectionCard(title = "داده‌ها", accent = NavViolet) {
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("پاک کردن همه داده‌ها")
                }
                Text(
                    text = "همه اطلاعات فقط روی همین گوشی ذخیره می‌شود و به هیچ سروری ارسال نمی‌گردد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
                )
            }
        }

        item {
            SectionCard(title = "درباره", accent = NavViolet) {
                Text(
                    text = "مسافر — همراه درمان اعتیاد",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "نسخه ۱٫۵٫۴ — اپلیکیشن آفلاین و متن‌باز\nپروتکل بر اساس اصلاحیه DST (۱۳۹۸/۳/۱)، پروتکل ۱۳۸۸ و راهنمای ویرایش ۹۹ کنگره ۶۰",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
                )
                Text(
                    text = "ما می‌توانیم ✋",
                    style = MaterialTheme.typography.titleSmall,
                    color = NavViolet,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = settings.startDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis
                    if (picked != null) {
                        // DatePicker مقدار UTC نیمه‌شب برمی‌گرداند؛ به نیمه‌شب محلی تبدیل می‌کنیم
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utc.timeInMillis = picked
                        val y = utc.get(Calendar.YEAR)
                        val m = utc.get(Calendar.MONTH)
                        val d = utc.get(Calendar.DAY_OF_MONTH)
                        val local = Calendar.getInstance().apply {
                            set(y, m, d, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onSaveSettings(settings.copy(startDateMillis = local.timeInMillis))
                    }
                    showDatePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("پاک کردن همه داده‌ها؟") },
            text = { Text("همه ثبت‌های روزانه و تنظیمات درمان حذف می‌شوند. این عمل قابل بازگشت نیست.", textAlign = TextAlign.Justify) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAll()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، پاک کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("انصراف") }
            }
        )
    }
}
