package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ir.mosafir.recovery.data.BankMcq
import ir.mosafir.recovery.data.BankQa
import ir.mosafir.recovery.data.NightExamScheduler
import ir.mosafir.recovery.data.NightExamStore
import ir.mosafir.recovery.data.PersianDate
import ir.mosafir.recovery.data.QuestionBank
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.StatTile
import ir.mosafir.recovery.ui.WeCanBadge
import ir.mosafir.recovery.ui.theme.HeroPink
import kotlin.random.Random

/** رنگ‌های بازخورد درست/غلط */
private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)
private val ChartOrange = Color(0xFFF59E0B)
private val NeutralGray = Color(0xFF757575)

/** حداکثر طول پاسخ «سؤال کوتاه‌متن» در تمرین تشریحی */
private const val QA_SHORT_MAX = 120

/** یک سؤال تستیِ یکپارچه (هستهٔ متد DST یا بانک) */
private data class QuizItem(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val cat: String
)

/** سؤال‌های هستهٔ متد DST — با توضیح آموزشی؛ همیشه در دسترس */
private val CORE_DST: List<QuizItem> = listOf(
    QuizItem(
        id = "core-01",
        "در متد DST، حرف S به چه معناست؟",
        listOf("شروع درمان", "پله ۲۱ روزه", "سقف مصرف", "ثبت روزانه"),
        1,
        "S = Step یعنی پله ۲۱ روزه؛ D ضریب ۰٫۸ و T حداقل ۱۰ ماه درمان است.",
        "متد DST"
    ),
    QuizItem(
        id = "core-02",
        "ضریب کاهش مصرف در هر پله متد DST چقدر است؟",
        listOf("۰٫۵", "۰٫۸", "۱٫۲", "۲"),
        1,
        "در هر پله، مقدار مصرف در ضریب ۰٫۸ (D = Dezhakam) ضرب می‌شود.",
        "متد DST"
    ),
    QuizItem(
        id = "core-03",
        "فاصله وعده‌های مصرف در متد DST چند ساعت است؟",
        listOf("۶ ساعت", "۸ ساعت", "۱۲ ساعت", "۲۴ ساعت"),
        1,
        "سه وعده با فاصله ۸ ساعت (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲).",
        "متد DST"
    ),
    QuizItem(
        id = "core-04",
        "سقف OT در برنامه ۶–۳ چند سی‌سی در روز است؟",
        listOf("۶ سی‌سی", "۷٫۵ سی‌سی", "۹ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف برنامه ۶–۳: هر وعده ۲٫۵cc و روزانه ۷٫۵cc؛ سپس ۲۲ روز توقف روی سقف.",
        "متد DST"
    ),
    QuizItem(
        id = "core-05",
        "توقف تثبیت روی سقف مصرف طبق اصلاحیه چند روز است؟",
        listOf("۷ روز", "۱۴ روز", "۲۲ روز", "۳۰ روز"),
        2,
        "عبارت اصلاحیه: «۲۲ روز یا دو پله» توقف روی سقف.",
        "متد DST"
    ),
    QuizItem(
        id = "core-06",
        "هر ۱ سی‌سی شربت اپیوم تینکچر (OT) معادل چند میلی‌گرم تریاک خوراکی است؟",
        listOf("۵۰ میلی‌گرم", "۱۰۰ میلی‌گرم", "۲۰۰ میلی‌گرم", "۵۰۰ میلی‌گرم"),
        1,
        "هر ۱cc OT = ۱۰۰ میلی‌گرم تریاک خوراکی؛ بنابراین ۱ گرم تریاک = ۱۰cc.",
        "متد DST"
    ),
    QuizItem(
        id = "core-07",
        "در برنامه ۶–۳ با مصرف کشیدنی چه باید کرد؟",
        listOf("کاهش تدریجی مصرف کشیدنی", "قطع همان شب هفته اول", "قطع بلافاصله و شروع همزمان OT", "فقط ثبت روزانه کافی است"),
        2,
        "مصرف کشیدنی بلافاصله قطع و همزمان شربت OT شروع می‌شود.",
        "متد DST"
    ),
    QuizItem(
        id = "core-08",
        "پله آخر برنامه (روزانه ۰٫۳cc) چند روز ادامه دارد؟",
        listOf("۳ روز", "۷ روز ثابت", "۲ تا ۲۱ روز", "۲۱ روز ثابت"),
        2,
        "پله آخر طبق توضیح جدول، ۲ تا ۲۱ روز ادامه دارد و سپس با نظر راهنما قطع می‌شود.",
        "متد DST"
    ),
    QuizItem(
        id = "core-09",
        "سقف OT در درمان الکل چند سی‌سی در روز است؟",
        listOf("۴٫۵ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف OT در درمان الکل ۶ سی‌سی روزانه است (هر وعده ۲cc).",
        "متد DST"
    ),
    QuizItem(
        id = "core-10",
        "در درمان هروئین، شروع OT از روز نخست با روزانه چند سی‌سی است؟",
        listOf("۳ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "اصلاحیه ۱۳۹۸: از روز نخست روزانه ۶cc (هر وعده ۲cc)؛ سقف برنامه ۱۰٫۵cc است.",
        "متد DST"
    )
)

private const val CORE_CAT = "متد DST"

private fun BankMcq.toQuizItem(): QuizItem = QuizItem(
    id = id,
    question = q,
    options = opts,
    correctIndex = ansIndex,
    explanation = buildString {
        append("پاسخ درست: ")
        append(opts[ansIndex])
        if (ref.isNotBlank()) {
            append(" · ")
            append(ref)
        }
        append(" · منبع: ")
        append(cat)
    },
    cat = cat
)

/** سؤال‌های پاسخ‌داده‌شده برای مرور نهایی */
private data class AnsweredQuestion(val question: QuizItem, val chosenIndex: Int)

@Composable
private fun FilterChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    accent: Color
) {
    // کاشی ۳تایی در هر ردیف — مثل پیش‌نمایش وب (۳ ستون)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { rowOpts ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOpts.forEach { c ->
                    val on = c in selected
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (on) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (on) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggle(c) }
                    ) {
                        Text(
                            text = c,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 14.dp)
                        )
                    }
                }
                repeat(3 - rowOpts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
/** ذخیره‌گاه سؤال‌های غلط — مبنای کارت «مرور اشتباه‌ها» */
private object WrongStore {
    private const val KEY = "wrong_json"

    fun load(ctx: android.content.Context): List<QuizItem> {
        return try {
            val prefs = ctx.getSharedPreferences("mosafir_wrong", android.content.Context.MODE_PRIVATE)
            val arr = org.json.JSONArray(prefs.getString(KEY, "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val oa = o.optJSONArray("options") ?: return@mapNotNull null
                val opts = (0 until oa.length()).map { oa.optString(it) }
                QuizItem(
                    id = o.optString("id"),
                    o.optString("question"),
                    opts,
                    o.optInt("correctIndex"),
                    o.optString("explanation"),
                    o.optString("cat")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: android.content.Context, items: List<QuizItem>) {
        try {
            val prefs = ctx.getSharedPreferences("mosafir_wrong", android.content.Context.MODE_PRIVATE)
            val arr = org.json.JSONArray()
            items.takeLast(120).forEach { q ->
                val o = org.json.JSONObject()
                o.put("question", q.question)
                val oa = org.json.JSONArray()
                q.options.forEach { oa.put(it) }
                o.put("options", oa)
                o.put("correctIndex", q.correctIndex)
                o.put("explanation", q.explanation)
                o.put("cat", q.cat)
                arr.put(o)
            }
            prefs.edit().putString(KEY, arr.toString()).apply()
        } catch (e: Exception) {
            /* بی‌صدا */
        }
    }
}

/**
 * چرخش هوشمند سؤال‌ها — آخرین سؤال‌های دیده‌شده (تا ۴۰۰) ذخیره می‌شود تا
 * هر بار ورود به آزمون، تا جای ممکن سؤال تازه بیاید و سؤال‌های سشن قبل تکرار نشوند.
 */
private object RecentStore {
    private const val MAX = 400

    private fun prefs(ctx: android.content.Context) =
        ctx.getSharedPreferences("mosafir_exam_recent", android.content.Context.MODE_PRIVATE)

    fun load(ctx: android.content.Context, mode: String): List<String> {
        return try {
            val arr = org.json.JSONArray(prefs(ctx).getString("recent_$mode", "[]") ?: "[]")
            (0 until arr.length()).map { arr.optString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun append(ctx: android.content.Context, mode: String, ids: List<String>) {
        try {
            val merged = (load(ctx, mode) + ids).distinct().takeLast(MAX)
            val arr = org.json.JSONArray()
            merged.forEach { arr.put(it) }
            prefs(ctx).edit().putString("recent_$mode", arr.toString()).apply()
        } catch (e: Exception) {
            /* بی‌صدا */
        }
    }
}

/**
 * «روند آزمون‌ها» — نتیجهٔ هر آزمون/تمرینِ کامل‌شده ذخیره می‌شود تا
 * روند پیشرفت مسافر در بالای صفحهٔ آزمون دیده باشد.
 */
private data class ExamResult(
    val kind: String,   // term | cat | quick | review | written
    val score: Int,     // تستی: تعداد درست — تشریحی: «بلد بودم»
    val total: Int,     // کل سؤال‌های همان جلسه
    val date: Long
)

private object ExamResultStore {
    private const val MAX = 60

    private fun prefs(ctx: android.content.Context) =
        ctx.getSharedPreferences("mosafir_exam_results", android.content.Context.MODE_PRIVATE)

    fun load(ctx: android.content.Context): List<ExamResult> {
        return try {
            val arr = org.json.JSONArray(prefs(ctx).getString("results", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ExamResult(
                    kind = o.optString("kind", "cat"),
                    score = o.optInt("score"),
                    total = o.optInt("total"),
                    date = o.optLong("date")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(ctx: android.content.Context, r: ExamResult) {
        if (r.total <= 0) return
        try {
            val merged = (listOf(r) + load(ctx)).take(MAX)
            val arr = org.json.JSONArray()
            merged.forEach { x ->
                val o = org.json.JSONObject()
                o.put("kind", x.kind)
                o.put("score", x.score)
                o.put("total", x.total)
                o.put("date", x.date)
                arr.put(o)
            }
            prefs(ctx).edit().putString("results", arr.toString()).apply()
        } catch (e: Exception) {
            /* بی‌صدا */
        }
    }
}

private fun resultKindLabel(kind: String): String = when (kind) {
    "term" -> "آزمون ترم"
    "quick" -> "آزمون سریع"
    "review" -> "مرور اشتباه‌ها"
    "written" -> "تمرین تشریحی"
    else -> "تمرین دسته‌ای"
}

/**
 * چینش سشن جدید: اول سؤال‌های تازه (که در سشن‌های اخیر نیامده‌اند)، بعد سؤال‌های دیده‌شده —
 * هر گروه خودش شافل می‌شود تا ترتیب همیشه عوض شود.
 */
private fun <T> rotateFresh(pool: List<Pair<String, T>>, recent: List<String>): List<T> {
    val seen = recent.toHashSet()
    val fresh = pool.filter { it.first !in seen }.map { it.second }.shuffled(Random(System.nanoTime()))
    val old = pool.filter { it.first in seen }.map { it.second }.shuffled(Random(System.nanoTime()))
    return fresh + old
}

/** پنج کارت اصلی صفحهٔ شروع آزمون — طرح مصوب «عکس خوب» + حفظ امکانات فعلی */
private const val QUIZ_LENS_MAX = 100
private data class ExamCardDef(
    val id: String,
    val title: String,
    val sub: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizLenChips(selected: Int, accent: Color, onPick: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        listOf(10, 20, 30, 40, 50, 60, 70, 100).forEach { n ->
            val on = selected == n
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (on) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.clickable { onPick(n) }
            ) {
                Text(
                    text = formatNumber(n),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ExamStartCard(
    title: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    badge: Int?,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selected) color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                if (selected) 1.75.dp else 1.25.dp,
                if (selected) color else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) color else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sub,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(21.dp))
                    if (badge != null && badge > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-7).dp)
                                .size(18.dp)
                                .background(color, CircleShape)
                        ) {
                            Text(
                                text = formatNumber(badge),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
        if (selected) content()
    }
}

/**
 * کارت «روند آزمون‌های تو» — بالای صفحهٔ آزمون، دقیقاً مطابق طرح مرجع:
 * ۴ کارت آمار وسط‌چین (بدون خط بنفش دور کارت‌ها) + نمودار خطی ۶ جلسهٔ اخیر + فهرست آخرین جلسه‌ها.
 */
@Composable
private fun ExamTrendCard(results: List<ExamResult>) {
    val tileBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "روند آزمون‌های تو 📈",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HeroPink
            )
            if (results.isEmpty()) {
                Text(
                    text = "هنوز آزمونی ثبت نشده است. اولین آزمون یا تمرینت را بده؛ نتیجهٔ هر جلسه خودکار همین‌جا ذخیره می‌شود تا روند پیشرفتت را قدم‌به‌قدم ببینی.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val percents = results.map { if (it.total > 0) it.score * 100 / it.total else 0 }
                val avg = kotlin.math.round(percents.average()).toInt()
                val best = percents.max()
                val last = percents.first()
                val prev = percents.getOrNull(1)

                // کارت ۱ و ۲: میانگین کل + آخرین نمره — متن‌ها دقیقاً وسط کارت
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrendTile(
                        modifier = Modifier.weight(1f),
                        value = formatNumber(avg) + "٪",
                        valueColor = ChartOrange,
                        label = "میانگین کل",
                        sub = formatNumber(results.size) + " آزمون گرفته‌ای",
                        subColor = NeutralGray,
                        bg = tileBg
                    )
                    TrendTile(
                        modifier = Modifier.weight(1f),
                        value = formatNumber(last) + "٪",
                        valueColor = CorrectGreen,
                        label = "آخرین نمره",
                        sub = when {
                            prev == null -> "اولین آزمونت"
                            last > prev -> formatNumber(last - prev) + "٪ بهتر از قبلی"
                            last < prev -> formatNumber(prev - last) + "٪ ضعیف‌تر از قبلی"
                            else -> "بدون تغییر نسبت به قبلی"
                        },
                        subColor = when {
                            prev == null -> NeutralGray
                            last > prev -> CorrectGreen
                            last < prev -> WrongRed
                            else -> NeutralGray
                        },
                        bg = tileBg
                    )
                }

                // کارت نمودار: درصد موفقیت در ۶ جلسهٔ اخیر (قدیمی‌ترین جلسه سمت راست — RTL)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tileBg)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "درصد موفقیت در ۶ جلسه اخیر",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val lastSix = results.take(6).reversed() // قدیمی‌ترین اول
                    SuccessChart(values = lastSix.map { if (it.total > 0) it.score * 100 / it.total else 0 })
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        lastSix.forEach { r ->
                            val jm = PersianDate.fromMillis(r.date)[1]
                            Text(
                                text = PersianDate.MONTH_NAMES[jm - 1],
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralGray,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // کارت ۳ و ۴: بهترین درصد + تعداد جلسات
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrendTile(
                        modifier = Modifier.weight(1f),
                        value = formatNumber(best) + "٪",
                        valueColor = CorrectGreen,
                        label = "🏆 بهترین درصد",
                        sub = "بهترین جلسهٔ تو",
                        subColor = NeutralGray,
                        bg = CorrectGreen.copy(alpha = 0.08f)
                    )
                    TrendTile(
                        modifier = Modifier.weight(1f),
                        value = formatNumber(results.size),
                        valueColor = Color(0xFFAD1457),
                        label = "🗓 تعداد جلسات",
                        sub = "همهٔ حالت‌ها ثبت می‌شوند",
                        subColor = NeutralGray,
                        bg = Color(0xFFAD1457).copy(alpha = 0.06f)
                    )
                }

                // فهرست آخرین جلسه‌ها
                results.take(6).forEach { r ->
                    val pct = if (r.total > 0) r.score * 100 / r.total else 0
                    val col = when {
                        pct >= 70 -> CorrectGreen
                        pct >= 50 -> ChartOrange
                        else -> WrongRed
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = resultKindLabel(r.kind) + " · " + PersianDate.formatShort(r.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatNumber(r.score) + " از " + formatNumber(r.total) + " — " + formatNumber(pct) + "٪",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = col
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (pct.coerceIn(0, 100)) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = col,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                if (results.size > 6) {
                    Text(
                        text = "و " + formatNumber(results.size - 6) + " جلسهٔ پیش‌تر در همین روند ثبت شده است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = "این روند روی همین گوشی ذخیره می‌شود؛ آزمون ترم، دسته‌ای، سریع، مرور اشتباه‌ها و تمرین تشریحی همه ثبت می‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** کاشی آمار روند — متن دقیقاً وسط کارت، بدون حاشیهٔ بنفش */
@Composable
private fun TrendTile(
    modifier: Modifier = Modifier,
    value: String,
    valueColor: Color,
    label: String,
    sub: String,
    subColor: Color,
    bg: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = valueColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = subColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** نمودار خطی درصد موفقیت — قدیمی‌ترین جلسه سمت راست (RTL) */
@Composable
private fun SuccessChart(values: List<Int>) {
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val n = values.size
        val padX = 30f
        val padTop = 34f
        val padBottom = 10f
        val chartH = size.height - padTop - padBottom
        val stepX = if (n == 1) 0f else (size.width - 2 * padX) / (n - 1)

        val pts = values.mapIndexed { i, v ->
            val x = size.width - padX - i * stepX // RTL: اولین (قدیمی‌ترین) سمت راست
            val y = padTop + (100 - v) / 100f * chartH
            Offset(x, y)
        }

        // ناحیهٔ پرشدهٔ زیر خط
        val fill = Path()
        fill.moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { fill.lineTo(it.x, it.y) }
        fill.lineTo(pts.last().x, size.height - padBottom)
        fill.lineTo(pts.first().x, size.height - padBottom)
        fill.close()
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(ChartOrange.copy(alpha = 0.30f), ChartOrange.copy(alpha = 0.02f)),
                startY = 0f,
                endY = size.height
            )
        )

        // خط اصلی
        val line = Path()
        line.moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { line.lineTo(it.x, it.y) }
        drawPath(line, color = ChartOrange, style = Stroke(width = 5f))

        // نقطه‌ها و برچسب درصد
        pts.forEach { p ->
            drawCircle(color = ChartOrange, radius = 10f, center = p)
            drawCircle(color = Color.White, radius = 4f, center = p)
        }
        pts.forEachIndexed { i, p ->
            val label = measurer.measure(
                text = formatNumber(values[i]) + "٪",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartOrange,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(p.x - label.size.width / 2f, p.y - label.size.height - 12f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ExamScreen() {
    val context = LocalContext.current
    val bank = remember { QuestionBank.load(context) }

    // mode: quiz = آزمون تستی | flash = تمرین تشریحی
    var mode by remember { mutableStateOf("quiz") }
    var phase by remember { mutableStateOf("start") } // start | running | done

    // ————— حالت آزمون تستی
    var quizList by remember { mutableStateOf(listOf<QuizItem>()) }
    var qIndex by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(listOf<AnsweredQuestion>()) }
    var quizLen by remember { mutableIntStateOf(10) }

    // ————— حالت تمرین تشریحی
    var flashList by remember { mutableStateOf(listOf<BankQa>()) }
    var fIndex by remember { mutableIntStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    var known by remember { mutableIntStateOf(0) }
    var unknown by remember { mutableIntStateOf(0) }
    // فقط سؤال‌های کوتاه‌متن (پیش‌فرض) — بلندها با گزینهٔ «همهٔ سؤال‌ها»
    var flashShortOnly by remember { mutableStateOf(true) }

    // انتخاب دسته‌ها — خالی = همه
    var quizCats by remember { mutableStateOf(setOf<String>()) }
    var flashCats by remember { mutableStateOf(setOf<String>()) }

    // ————— کارت شروع (طرح ۵ کارت) + ذخیره‌گاه غلط‌ها
    var startCard by remember { mutableStateOf<String?>(null) }
    var isReview by remember { mutableStateOf(false) }
    var reviewEmpty by remember { mutableStateOf(false) }
    var wrongCount by remember { mutableStateOf(WrongStore.load(context).size) }

    // ————— روند آزمون‌ها: نوع آخرین سشن + تاریخچهٔ نتایج
    var quizKind by remember { mutableStateOf("cat") }
    var examResults by remember { mutableStateOf(ExamResultStore.load(context)) }

    val quizCatOptions = remember { listOf(CORE_CAT) + bank.mcqCats }
    val flashCatOptions = remember { bank.qaCats }

    fun startQuiz(review: Boolean = false, len: Int = quizLen, cats: Set<String> = quizCats) {
        if (review) {
            val stored = WrongStore.load(context)
            if (stored.isEmpty()) {
                reviewEmpty = true
                return
            }
            reviewEmpty = false
            isReview = true
            quizKind = "review"
            quizList = stored.shuffled(Random(System.nanoTime()))
            qIndex = 0; chosen = null; answered = false; score = 0; history = emptyList()
            phase = "running"
            return
        }
        isReview = false
        quizKind = when (startCard) {
            "term" -> "term"
            "quick" -> "quick"
            "review" -> quizKind // «آزمون دوباره» بعد از مرور — همان نوع سشن قبلی می‌ماند
            else -> "cat"
        }
        val core = if (cats.isEmpty() || CORE_CAT in cats) CORE_DST else emptyList()
        val fromBank = bank.mcq
            .filter { cats.isEmpty() || it.cat in cats }
            .map { it.toQuizItem() }
        val idPool = (core + fromBank).map { it.id to it }
        /* چرخش هوشمند: اول سؤال‌های تازه — سؤال‌های سشن قبل آخر لیست می‌آیند */
        val ordered = rotateFresh(idPool, RecentStore.load(context, "quiz"))
        val picked = ordered.take(len.coerceAtMost(ordered.size))
        RecentStore.append(context, "quiz", picked.map { it.id })
        quizList = picked
        qIndex = 0; chosen = null; answered = false; score = 0; history = emptyList()
        phase = "running"
    }

    /** ثبت نتیجهٔ آزمون تستی در «روند آزمون‌ها» — کل سشن یا تا جای که پاسخ داده شده */
    fun saveQuizResult(partialTotal: Int? = null) {
        val total = partialTotal ?: quizList.size
        if (total <= 0) return
        ExamResultStore.add(
            context,
            ExamResult(kind = if (isReview) "review" else quizKind, score = score, total = total, date = System.currentTimeMillis())
        )
        examResults = ExamResultStore.load(context)
    }

    /** ثبت نتیجهٔ تمرین تشریحی در «روند آزمون‌ها» */
    fun saveFlashResult() {
        val done = known + unknown
        if (done <= 0) return
        ExamResultStore.add(
            context,
            ExamResult(kind = "written", score = known, total = done, date = System.currentTimeMillis())
        )
        examResults = ExamResultStore.load(context)
    }

    /** ذخیرهٔ سؤال‌های غلط — در حالت مرور، فقط غلط‌های دوباره باقی می‌مانند */
    fun recordWrongs() {
        val wrongItems = history.filter { it.chosenIndex != it.question.correctIndex }.map { it.question }
        if (isReview) {
            WrongStore.save(context, wrongItems)
        } else {
            val merged = WrongStore.load(context).toMutableList()
            wrongItems.forEach { w -> if (merged.none { it.question == w.question }) merged.add(w) }
            WrongStore.save(context, merged)
        }
        wrongCount = WrongStore.load(context).size
    }

    fun startFlash() {
        val idPool = bank.qa
            .filter { (flashCats.isEmpty() || it.cat in flashCats) && (!flashShortOnly || it.a.length <= QA_SHORT_MAX) }
            .map { it.id to it }
        /* چرخش هوشمند: سؤال‌های تازه اول — برخلاف دفعهٔ قبل که شافلِ ساده بود */
        flashList = rotateFresh(idPool, RecentStore.load(context, "flash"))
        RecentStore.append(context, "flash", flashList.map { it.id })
        fIndex = 0; reveal = false; known = 0; unknown = 0
        phase = "running"
    }

    /* ——— آزمون شبانه ——— */
    var nightEnabled by remember { mutableStateOf(NightExamStore.load(context).enabled) }
    var nightHour by remember { mutableIntStateOf(NightExamStore.load(context).hour) }
    var nightMinute by remember { mutableIntStateOf(NightExamStore.load(context).minute) }
    var nightDoneToday by remember { mutableStateOf(NightExamStore.isDoneToday(context)) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /* با هر ورود به تب آزمون، زنگ شبانه دوباره تنظیم و روند آزمون‌ها تازه می‌شود */
    LaunchedEffect(Unit) {
        NightExamScheduler.ensureChannel(context)
        NightExamScheduler.scheduleNext(context)
        nightDoneToday = NightExamStore.isDoneToday(context)
        examResults = ExamResultStore.load(context)
    }

    fun setNightExam(enabled: Boolean, hour: Int = nightHour, minute: Int = nightMinute) {
        nightEnabled = enabled; nightHour = hour; nightMinute = minute
        NightExamStore.save(context, NightExamStore.NightExamSettings(enabled, hour, minute))
        NightExamScheduler.ensureChannel(context)
        NightExamScheduler.scheduleNext(context)
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun markExamDoneToday() {
        NightExamStore.markDoneToday(context)
        nightDoneToday = true
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "آزمون",
                subtitle = "آزمون تستی و تمرین تشریحی — از ده‌ها آزمون واقعی کنگره ۶۰",
                accent = HeroPink
            )
        }

        when (phase) {
            "start" -> {
                // روند آزمون‌ها — بالای همهٔ کارت‌ها (همان‌جا که قرار بود باشد)
                item(key = "trend") {
                    ExamTrendCard(examResults)
                }
                val cards = listOf(
                    ExamCardDef("term", "آزمون ترم", "۱۰۰ سؤال · ۴۵ دقیقه — استاندارد جلسهٔ ترم", Icons.Outlined.Assignment, Color(0xFFF97316)),
                    ExamCardDef("cat", "تمرین دسته‌ای", "تمرین روی یک منبع — مثلاً فقط ۱۴ وادی", Icons.Outlined.Layers, Color(0xFF14B8A6)),
                    ExamCardDef("quick", "آزمون سریع", "۵ سؤال — چند دقیقه وقت می‌برد", Icons.Outlined.Timer, Color(0xFFF59E0B)),
                    ExamCardDef("review", "مرور اشتباه‌ها", "فقط سؤال‌هایی که غلط جواب دادی", Icons.Outlined.Refresh, Color(0xFFEC4899)),
                    ExamCardDef("written", "تمرین تشریحی", "سؤال‌های متن کوتاه با پاسخ — اول خودت جواب بده، بعد پاسخ را ببین", Icons.Outlined.MenuBook, Color(0xFFE11D48))
                )
                cards.forEach { cd ->
                    item(key = "card-" + cd.id) {
                        ExamStartCard(
                            title = cd.title,
                            sub = cd.sub,
                            icon = cd.icon,
                            color = cd.color,
                            selected = startCard == cd.id,
                            badge = if (cd.id == "review") wrongCount else null,
                            onClick = {
                                startCard = if (startCard == cd.id) null else cd.id
                                reviewEmpty = false
                                mode = if (cd.id == "written") "flash" else "quiz"
                                if (cd.id == "term") quizLen = 100
                                if (cd.id == "cat" && quizLen > 50) quizLen = 20
                            }
                        ) {
                            when (cd.id) {
                                "term" -> {
                                    SectionCard(title = "تنظیم آزمون ترم", accent = cd.color) {
                                        Text(
                                            text = "همهٔ منابع قاطی — مثل جلسهٔ ترم؛ ۱۰۰ سؤال استاندارد یا هر تعدادی که انتخاب کنی.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                        )
                                        Text(
                                            text = "تعداد سؤال:",
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        )
                                        QuizLenChips(selected = quizLen, accent = cd.color, onPick = { quizLen = it })
                                        Button(
                                            onClick = { startQuiz(len = quizLen, cats = emptySet()) },
                                            colors = ButtonDefaults.buttonColors(containerColor = cd.color),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) { Text("شروع آزمون ترم") }
                                    }
                                }
                                "cat" -> {
                                    SectionCard(title = "انتخاب منبع", accent = cd.color) {
                                        FilterChips(
                                            options = quizCatOptions,
                                            selected = quizCats,
                                            onToggle = { c -> quizCats = if (c in quizCats) quizCats - c else quizCats + c },
                                            accent = cd.color
                                        )
                                        Text(
                                            text = "هیچ دسته‌ای انتخاب نشود = همهٔ منابع",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        Text(
                                            text = "تعداد سؤال:",
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        )
                                        QuizLenChips(selected = quizLen, accent = cd.color, onPick = { quizLen = it })
                                        Button(
                                            onClick = { startQuiz(len = quizLen, cats = quizCats) },
                                            colors = ButtonDefaults.buttonColors(containerColor = cd.color),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) { Text("شروع تمرین دسته‌ای") }
                                    }
                                }
                                "quick" -> {
                                    SectionCard(accent = cd.color) {
                                        Text(
                                            text = "۵ سؤال از همهٔ منابع، بدون هیچ تنظیمی — برای وقتی که فقط چند دقیقه وقت داری.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                        )
                                        Button(
                                            onClick = { startQuiz(len = 5, cats = emptySet()) },
                                            colors = ButtonDefaults.buttonColors(containerColor = cd.color),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) { Text("شروع آزمون ۵ سؤالی") }
                                    }
                                }
                                "review" -> {
                                    SectionCard(accent = cd.color) {
                                        if (wrongCount > 0) {
                                            Text(
                                                text = formatNumber(wrongCount) + " سؤال غلط ذخیره شده است؛ هر سؤالی که این‌جا درست جواب بدهی از فهرست مرور حذف می‌شود.",
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                            )
                                            Button(
                                                onClick = { startQuiz(review = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = cd.color),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) { Text("شروع مرور اشتباه‌ها") }
                                        } else {
                                            Text(
                                                text = "هنوز سؤال غلطی ذخیره نشده — اول یک آزمون بده؛ هر سؤالی که غلط جواب بدهی خودکار این‌جا ذخیره می‌شود تا دفعهٔ بعد بیشتر تکرارش کنی.",
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                            )
                                        }
                                        if (reviewEmpty) {
                                            Text(
                                                text = "هنوز غلطی ذخیره نشده است.",
                                                color = WrongRed,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }
                                "written" -> {
                                    SectionCard(title = "تمرین تشریحی", accent = cd.color) {
                                        val shortCount = bank.qa.count { it.a.length <= QA_SHORT_MAX }
                                        Text(
                                            text = (if (flashShortOnly) formatNumber(shortCount) else formatNumber(bank.qa.size)) +
                                                " سؤال تشریحی پاسخ‌دار" + (if (flashShortOnly) " متن کوتاه" else "") +
                                                " از ۱۴ وادی عشق، ۱۴ مقاله، راهنما و راهنمایی و… اول خودت پاسخ بده، بعد پاسخ را ببین و صادقانه علامت بزن.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                        )
                                        // حالت طول پاسخ: کوتاه (پیش‌فرض) یا همهٔ سؤال‌ها
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            listOf(true to "متن کوتاه", false to "همهٔ سؤال‌ها").forEach { (value, label) ->
                                                val on = flashShortOnly == value
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (on) cd.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, if (on) cd.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                    modifier = Modifier.clickable { flashShortOnly = value }
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 16.sp,
                                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (on) cd.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "در حالت «متن کوتاه» فقط پاسخ‌های کوتاه (تا ۱۲۰ حرف) می‌آید — مناسب مرور سریع؛ پاسخ‌های بلند با گزینهٔ «همهٔ سؤال‌ها» تمرین می‌شوند.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        FilterChips(
                                            options = flashCatOptions,
                                            selected = flashCats,
                                            onToggle = { c -> flashCats = if (c in flashCats) flashCats - c else flashCats + c },
                                            accent = cd.color
                                        )
                                        Text(
                                            text = "هیچ دسته‌ای انتخاب نشود = همهٔ منابع",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        Button(
                                            onClick = { startFlash() },
                                            colors = ButtonDefaults.buttonColors(containerColor = cd.color),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) { Text("شروع تمرین") }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "night") {
                    val nightColor = Color(0xFF7C3AED)
                    SectionCard(title = "آزمون شبانه 🌙", accent = nightColor) {
                        Text(
                            text = "هر شب در ساعت انتخابی، اگر آن روز هنوز آزمون یا تمرینی نداده باشی، یادآوری می‌فرستد: «آزمون شب را هنوز ندادی!». با اولین آزمون یا تمرین همان روز، دیگر پیامی نمی‌آید. آرام و بدون استرس — فقط چند دقیقه مرور.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (nightEnabled) "یادآوری فعال است" else "یادآوری خاموش است",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nightEnabled) nightColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (nightEnabled)
                                        "روزانه ساعت ${formatNumber(nightHour)}:${if (nightMinute == 0) "۰۰" else "۳۰"}" +
                                            if (nightDoneToday) " — امروز آزمون دادی ✅" else " — امروز هنوز آزمون ندادی"
                                    else
                                        "با فعال‌کردن، ساعت یادآوری را انتخاب کن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = nightEnabled,
                                onCheckedChange = { on -> setNightExam(enabled = on) }
                            )
                        }
                        if (nightEnabled) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                listOf(21 to 0, 21 to 30, 22 to 0, 22 to 30, 23 to 0).forEach { (h, m) ->
                                    val on = nightHour == h && nightMinute == m
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (on) nightColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (on) nightColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { setNightExam(enabled = true, hour = h, minute = m) }
                                    ) {
                                        Text(
                                            text = "${formatNumber(h)}:${if (m == 0) "۰۰" else "۳۰"}",
                                            fontSize = 12.sp,
                                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                            color = if (on) nightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "stats") {
                    Text(
                        text = formatNumber(bank.mcq.size) + " سؤال تستی کلیددار + " + formatNumber(CORE_DST.size) + " سؤال متد DST · " + formatNumber(bank.qa.size) + " سؤال تشریحی پاسخ‌دار",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item(key = "info") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFCE7F3),
                        border = BorderStroke(1.dp, Color(0xFFF9A8D4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "هوشمندسازی آزمون: هر بار که وارد آزمون شوی، سؤال‌ها تصادفی چیده می‌شوند و تا جای ممکن از سؤال‌های تازه انتخاب می‌شوند — سؤال‌های سشن قبل، آخرِ صف می‌آیند تا تکرار نشوند. زیر هر سؤال برچسب منبع می‌آید (مثلاً «قوانین کنگره ۶۰») و چیزی که غلط جواب بدی، دفعهٔ بعد بیشتر تکرار می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9D174D),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            "running" -> if (mode == "quiz") {
                val q = quizList[qIndex]
                item {
                    SectionCard {
                        OneLineText(
                            text = "سؤال ${formatNumber(qIndex + 1)} از ${formatNumber(quizList.size)}",
                            baseFontSize = 13,
                            minFontSize = 10,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = HeroPink
                        )
                        LinearProgressIndicator(
                            progress = { (qIndex + 1).toFloat() / quizList.size.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = HeroPink,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = q.cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                item {
                    SectionCard(title = q.question, accent = HeroPink) {
                        q.options.forEachIndexed { i, option ->
                            val isCorrect = i == q.correctIndex
                            val isChosen = chosen == i
                            val bg = when {
                                answered && isCorrect -> CorrectGreen.copy(alpha = 0.14f)
                                answered && isChosen && !isCorrect -> WrongRed.copy(alpha = 0.12f)
                                isChosen -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val border = when {
                                answered && isCorrect -> BorderStroke(1.5.dp, CorrectGreen)
                                answered && isChosen && !isCorrect -> BorderStroke(1.5.dp, WrongRed)
                                isChosen -> BorderStroke(1.5.dp, HeroPink)
                                else -> BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline)
                            }
                            Surface(
                                color = bg,
                                shape = RoundedCornerShape(12.dp),
                                border = border,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !answered) { chosen = i }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isChosen) HeroPink else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatNumber(i + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }
                        }

                        if (answered) {
                            val wasRight = chosen == q.correctIndex
                            Text(
                                text = if (wasRight) "✅ درست بود!" else "❌ پاسخ درست: ${q.options[q.correctIndex]}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (wasRight) CorrectGreen else WrongRed,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        if (qIndex + 1 < quizList.size) {
                                            qIndex += 1
                                            chosen = null
                                            answered = false
                                        } else {
                                            recordWrongs()
                                            saveQuizResult() // ثبت در «روند آزمون‌ها»
                                            markExamDoneToday()
                                            phase = "done"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HeroPink)
                                ) {
                                    Text(
                                        if (qIndex + 1 < quizList.size) "سؤال بعدی" else "دیدن نتیجه"
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        val c = chosen
                                        if (c != null) {
                                            answered = true
                                            if (c == q.correctIndex) score += 1
                                            history = history + AnsweredQuestion(q, c)
                                        }
                                    },
                                    enabled = chosen != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HeroPink,
                                        disabledContainerColor = HeroPink.copy(alpha = 0.4f),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Text("ثبت پاسخ")
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            recordWrongs()
                            // حتی خروجِ وسط کار هم ثبت می‌شود — تا جایی که پاسخ داده شده
                            if (history.isNotEmpty()) saveQuizResult(partialTotal = history.size)
                            phase = "start"
                            startCard = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("پایان و بازگشت") }
                }
            } else {
                val f = flashList.getOrNull(fIndex)
                if (f == null) {
                    item {
                        SectionCard {
                            Text(
                            text = "سؤالی در این دسته‌ها پیدا نشد؛ دسته‌های دیگری را انتخاب کن یا بانک را از منابع تازه به‌روز کن.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                        )
                            OutlinedButton(onClick = { phase = "start" }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("بازگشت")
                            }
                        }
                    }
                } else {
                    item {
                        SectionCard {
                            OneLineText(
                                text = "سؤال ${formatNumber(fIndex + 1)} از ${formatNumber(flashList.size)}",
                                baseFontSize = 13,
                                minFontSize = 10,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HeroPink
                            )
                            LinearProgressIndicator(
                                progress = { (fIndex + 1).toFloat() / flashList.size.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                color = HeroPink,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = f.cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    item {
                        SectionCard(title = f.q, accent = HeroPink) {
                            if (reveal) {
                                Surface(
                                    color = CorrectGreen.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.25.dp, CorrectGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "پاسخ:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CorrectGreen
                                        )
                                        Text(
                                            text = f.a,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                        )
                                        if (f.ref.isNotBlank()) {
                                            Text(
                                                text = f.ref,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                ) {
                                    Button(
                                        onClick = {
                                            known += 1
                                            if (fIndex + 1 < flashList.size) {
                                                fIndex += 1; reveal = false
                                            } else {
                                                saveFlashResult() // ثبت در «روند آزمون‌ها»
                                                markExamDoneToday()
                                                phase = "done"
                                            }
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                                    ) { Text("بلد بودم ✓") }
                                    OutlinedButton(
                                        onClick = {
                                            unknown += 1
                                            if (fIndex + 1 < flashList.size) {
                                                fIndex += 1; reveal = false
                                            } else {
                                                saveFlashResult() // ثبت در «روند آزمون‌ها»
                                                markExamDoneToday()
                                                phase = "done"
                                            }
                                        }
                                    ) { Text("بلد نبودم ✗") }
                                }
                            } else {
                                Button(
                                    onClick = { reveal = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HeroPink)
                                ) {
                                    Text("نمایش پاسخ")
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓ ${formatNumber(known)} · ✗ ${formatNumber(unknown)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 12.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    saveFlashResult() // ثبت در «روند آزمون‌ها»
                                    markExamDoneToday()
                                    phase = "done"
                                }
                            ) { Text("پایان تمرین") }
                        }
                    }
                }
            }

            "done" -> if (mode == "quiz") {
                item {
                    SectionCard(title = "نتیجهٔ آزمون", accent = HeroPink) {
                        OneLineText(
                            text = "${formatNumber(score)} از ${formatNumber(quizList.size)} درست",
                            baseFontSize = 26,
                            minFontSize = 14,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroPink
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WeCanBadge()
                        }
                        if (quizList.isNotEmpty()) {
                            val percent = (score * 100) / quizList.size
                            Text(
                                text = when {
                                    percent >= 90 -> "فوق‌العاده بود! آشنایی‌ات عالی است. ما می‌توانیم ✋"
                                    percent >= 70 -> "خیلی خوب بود! یک مرور کوتاه روی جدول پروتکل، تو را کامل می‌کند."
                                    percent >= 50 -> "بد نبود؛ اما پیشنهاد می‌کنم صفحهٔ «پروتکل» را یک‌بار دیگر با دقت بخوانی."
                                    else -> "نگران نباش؛ هر مسافری اولش این‌جوری است. جزوه و صفحهٔ پروتکل را مرور کن و دوباره آزمون بده. ما می‌توانیم ✋"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                            )
                        }
                        Text(
                            text = "✓ این نتیجه در «روند آزمون‌های تو» بالای صفحه ثبت شد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CorrectGreen,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(onClick = { startQuiz() }, colors = ButtonDefaults.buttonColors(containerColor = HeroPink)) { Text("آزمون دوباره") }
                            OutlinedButton(onClick = { phase = "start" }) { Text("صفحهٔ اول") }
                        }
                    }
                }

                item {
                    SectionCard(title = "مرور پاسخ‌ها", accent = HeroPink) {
                        history.forEachIndexed { idx, h ->
                            val wasRight = h.chosenIndex == h.question.correctIndex
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                OneLineText(
                                    text = "${formatNumber(idx + 1)}. ${h.question.question}",
                                    baseFontSize = 13,
                                    minFontSize = 8,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wasRight) CorrectGreen else WrongRed,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Text(
                                    text = if (wasRight)
                                        "پاسخ تو: ${h.question.options[h.chosenIndex]}"
                                    else
                                        "پاسخ تو: ${h.question.options[h.chosenIndex]} — پاسخ درست: ${h.question.options[h.question.correctIndex]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    SectionCard(title = "خلاصهٔ تمرین", accent = HeroPink) {
                        OneLineText(
                            text = "✓ ${formatNumber(known)} بلد بودم · ✗ ${formatNumber(unknown)} بلد نبودم",
                            baseFontSize = 20,
                            minFontSize = 12,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = HeroPink
                        )
                        Text(
                            text = "سؤال‌های «بلد نبودم» را در جلسهٔ بعدی تمرین، اول مرور کن؛ تکرار، کلید یادگیری است. ما می‌توانیم ✋",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                        )
                        Text(
                            text = "✓ این نتیجه در «روند آزمون‌های تو» بالای صفحه ثبت شد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CorrectGreen,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(onClick = { startFlash() }, colors = ButtonDefaults.buttonColors(containerColor = HeroPink)) { Text("تمرین دوباره") }
                            OutlinedButton(onClick = { phase = "start" }) { Text("صفحهٔ اول") }
                        }
                    }
                }
            }
        }
    }
}
