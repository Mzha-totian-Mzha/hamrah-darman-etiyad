package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import ir.mosafir.recovery.ui.FabScrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.data.BankMcq
import ir.mosafir.recovery.data.BankQa
import ir.mosafir.recovery.data.QuestionBank
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.WeCanBadge
import ir.mosafir.recovery.ui.theme.HeroPink
import kotlin.random.Random

/** رنگ‌های بازخورد درست/غلط */
private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)

/** یک سؤال تستیِ یکپارچه (هستهٔ متد DST یا بانک) */
private data class QuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val cat: String
)

/** سؤال‌های هستهٔ متد DST — با توضیح آموزشی؛ همیشه در دسترس */
private val CORE_DST: List<QuizItem> = listOf(
    QuizItem(
        "در متد DST، حرف S به چه معناست؟",
        listOf("شروع درمان", "پله ۲۱ روزه", "سقف مصرف", "ثبت روزانه"),
        1,
        "S = Step یعنی پله ۲۱ روزه؛ D ضریب ۰٫۸ و T حداقل ۱۰ ماه درمان است.",
        "متد DST"
    ),
    QuizItem(
        "ضریب کاهش مصرف در هر پله متد DST چقدر است؟",
        listOf("۰٫۵", "۰٫۸", "۱٫۲", "۲"),
        1,
        "در هر پله، مقدار مصرف در ضریب ۰٫۸ (D = Dezhakam) ضرب می‌شود.",
        "متد DST"
    ),
    QuizItem(
        "فاصله وعده‌های مصرف در متد DST چند ساعت است؟",
        listOf("۶ ساعت", "۸ ساعت", "۱۲ ساعت", "۲۴ ساعت"),
        1,
        "سه وعده با فاصله ۸ ساعت (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲).",
        "متد DST"
    ),
    QuizItem(
        "سقف OT در برنامه ۶–۳ چند سی‌سی در روز است؟",
        listOf("۶ سی‌سی", "۷٫۵ سی‌سی", "۹ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف برنامه ۶–۳: هر وعده ۲٫۵cc و روزانه ۷٫۵cc؛ سپس ۲۲ روز توقف روی سقف.",
        "متد DST"
    ),
    QuizItem(
        "توقف تثبیت روی سقف مصرف طبق اصلاحیه چند روز است؟",
        listOf("۷ روز", "۱۴ روز", "۲۲ روز", "۳۰ روز"),
        2,
        "عبارت اصلاحیه: «۲۲ روز یا دو پله» توقف روی سقف.",
        "متد DST"
    ),
    QuizItem(
        "هر ۱ سی‌سی شربت اپیوم تینکچر (OT) معادل چند میلی‌گرم تریاک خوراکی است؟",
        listOf("۵۰ میلی‌گرم", "۱۰۰ میلی‌گرم", "۲۰۰ میلی‌گرم", "۵۰۰ میلی‌گرم"),
        1,
        "هر ۱cc OT = ۱۰۰ میلی‌گرم تریاک خوراکی؛ بنابراین ۱ گرم تریاک = ۱۰cc.",
        "متد DST"
    ),
    QuizItem(
        "در برنامه ۶–۳ با مصرف کشیدنی چه باید کرد؟",
        listOf("کاهش تدریجی مصرف کشیدنی", "قطع همان شب هفته اول", "قطع بلافاصله و شروع همزمان OT", "فقط ثبت روزانه کافی است"),
        2,
        "مصرف کشیدنی بلافاصله قطع و همزمان شربت OT شروع می‌شود.",
        "متد DST"
    ),
    QuizItem(
        "پله آخر برنامه (روزانه ۰٫۳cc) چند روز ادامه دارد؟",
        listOf("۳ روز", "۷ روز ثابت", "۲ تا ۲۱ روز", "۲۱ روز ثابت"),
        2,
        "پله آخر طبق توضیح جدول، ۲ تا ۲۱ روز ادامه دارد و سپس با نظر راهنما قطع می‌شود.",
        "متد DST"
    ),
    QuizItem(
        "سقف OT در درمان الکل چند سی‌سی در روز است؟",
        listOf("۴٫۵ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف OT در درمان الکل ۶ سی‌سی روزانه است (هر وعده ۲cc).",
        "متد DST"
    ),
    QuizItem(
        "در درمان هروئین، شروع OT از روز نخست با روزانه چند سی‌سی است؟",
        listOf("۳ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "اصلاحیه ۱۳۹۸: از روز نخست روزانه ۶cc (هر وعده ۲cc)؛ سقف برنامه ۱۰٫۵cc است.",
        "متد DST"
    )
)

private const val CORE_CAT = "متد DST"

private fun BankMcq.toQuizItem(): QuizItem = QuizItem(
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
@Composable
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

    // انتخاب دسته‌ها — خالی = همه
    var quizCats by remember { mutableStateOf(setOf<String>()) }
    var flashCats by remember { mutableStateOf(setOf<String>()) }

    val quizCatOptions = remember { listOf(CORE_CAT) + bank.mcqCats }
    val flashCatOptions = remember { bank.qaCats }

    fun startQuiz() {
        val core = if (quizCats.isEmpty() || CORE_CAT in quizCats) CORE_DST else emptyList()
        val fromBank = bank.mcq
            .filter { quizCats.isEmpty() || it.cat in quizCats }
            .map { it.toQuizItem() }
        val pool = (core + fromBank).shuffled(Random(System.nanoTime()))
        quizList = pool.take(quizLen.coerceAtMost(pool.size))
        qIndex = 0; chosen = null; answered = false; score = 0; history = emptyList()
        phase = "running"
    }

    fun startFlash() {
        flashList = bank.qa
            .filter { flashCats.isEmpty() || it.cat in flashCats }
            .shuffled(Random(System.nanoTime()))
        fIndex = 0; reveal = false; known = 0; unknown = 0
        phase = "running"
    }

    val listState = rememberLazyListState()
    FabScrollEffect(listState)

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
                item {
                    SectionCard {
                        // انتخاب حالت
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (mode == "quiz") HeroPink.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.25.dp, if (mode == "quiz") HeroPink else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { mode = "quiz" }
                            ) {
                                Text(
                                    text = "🎯 آزمون تستی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mode == "quiz") HeroPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (mode == "flash") HeroPink.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.25.dp, if (mode == "flash") HeroPink else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { mode = "flash" }
                            ) {
                                Text(
                                    text = "📖 تمرین تشریحی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mode == "flash") HeroPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (mode == "quiz") {
                    item {
                        SectionCard(title = "آزمون تستی با کلید", accent = HeroPink) {
                            Text(
                                text = "${formatNumber(CORE_DST.size)} سؤال هستهٔ متد DST + ${formatNumber(bank.mcq.size)} سؤال تستیِ بانک با کلید تأییدشده. " +
                                    "پاسخ درست بلافاصله با توضیح نشان داده می‌شود.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "تعداد سؤال:",
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                listOf(10, 20, 30, 40, 50, 60, 70).forEach { n ->
                                    val on = quizLen == n
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (on) HeroPink.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (on) HeroPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { quizLen = n }
                                    ) {
                                        Text(
                                            text = formatNumber(n),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                            color = if (on) HeroPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SectionCard(title = "دستهٔ منابع (اختیاری)", accent = HeroPink) {
                            FilterChips(
                                options = quizCatOptions,
                                selected = quizCats,
                                onToggle = { c -> quizCats = if (c in quizCats) quizCats - c else quizCats + c },
                                accent = HeroPink
                            )
                            Text(
                                text = "هیچ دسته‌ای انتخاب نشود = همهٔ منابع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { startQuiz() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroPink)
                        ) {
                            Text("شروع آزمون")
                        }
                    }
                } else {
                    item {
                        SectionCard(title = "تمرین با سؤال‌های تشریحی", accent = HeroPink) {
                            Text(
                                text = "${formatNumber(bank.qa.size)} سؤال تشریحی پاسخ‌دار از ۱۴ وادی عشق، ۱۴ مقاله، راهنما و راهنمایی، شرح وظایف و… " +
                                    "اول خودت پاسخ بده، بعد پاسخ را ببین و صادقانه علامت بزن.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item {
                        SectionCard(title = "دستهٔ منابع (اختیاری)", accent = HeroPink) {
                            FilterChips(
                                options = flashCatOptions,
                                selected = flashCats,
                                onToggle = { c -> flashCats = if (c in flashCats) flashCats - c else flashCats + c },
                                accent = HeroPink
                            )
                            Text(
                                text = "هیچ دسته‌ای انتخاب نشود = همهٔ منابع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { startFlash() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroPink)
                        ) {
                            Text("شروع تمرین")
                        }
                    }
                }

                item {
                    SectionCard {
                        Text(
                            text = "💡 نکته: پاسخ همهٔ سؤال‌های هسته در صفحهٔ «پروتکل» و بخش‌های آموزشی برنامه وجود دارد؛ اگر بخواهی می‌توانی اول مرور کنی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onClick = { phase = "start" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("پایان و بازگشت") }
                }
            } else {
                val f = flashList.getOrNull(fIndex)
                if (f == null) {
                    item {
                        SectionCard {
                            Text("سؤالی در این دسته‌ها پیدا نشد؛ دسته‌های دیگری را انتخاب کن یا بانک را از منابع تازه به‌روز کن.")
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
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (f.ref.isNotBlank()) {
                                            Text(
                                                text = f.ref,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            } else phase = "done"
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                                    ) { Text("بلد بودم ✓") }
                                    OutlinedButton(
                                        onClick = {
                                            unknown += 1
                                            if (fIndex + 1 < flashList.size) {
                                                fIndex += 1; reveal = false
                                            } else phase = "done"
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
                            OutlinedButton(onClick = { phase = "done" }) { Text("پایان تمرین") }
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            style = MaterialTheme.typography.bodyMedium
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
