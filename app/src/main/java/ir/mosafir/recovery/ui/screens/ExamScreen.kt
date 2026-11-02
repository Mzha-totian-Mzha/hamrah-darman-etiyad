package ir.mosafir.recovery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.ui.OneLineText
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.WeCanBadge
import ir.mosafir.recovery.ui.theme.VioletPrimary

/** رنگ‌های بازخورد درست/غلط */
private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)

/** یک سؤال امتحان */
private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * سؤال‌های امتحان — همه بر پایهٔ همین برنامه و اسناد مرجع:
 * «اصلاحیه پروتکل DST (۱۳۹۸/۳/۱)» و راهنمای ویرایش ۹۹ کنگره ۶۰.
 */
private val QUIZ_QUESTIONS = listOf(
    QuizQuestion(
        "در متد DST، حرف S به چه معناست؟",
        listOf("شروع درمان", "پله ۲۱ روزه", "سقف مصرف", "ثبت روزانه"),
        1,
        "S = Step یعنی پله ۲۱ روزه؛ D ضریب ۰٫۸ و T حداقل ۱۰ ماه درمان است."
    ),
    QuizQuestion(
        "ضریب کاهش مصرف در هر پله متد DST چقدر است؟",
        listOf("۰٫۵", "۰٫۸", "۱٫۲", "۲"),
        1,
        "در هر پله، مقدار مصرف در ضریب ۰٫۸ (D = Dezhakam) ضرب می‌شود."
    ),
    QuizQuestion(
        "فاصله وعده‌های مصرف در متد DST چند ساعت است؟",
        listOf("۶ ساعت", "۸ ساعت", "۱۲ ساعت", "۲۴ ساعت"),
        1,
        "سه وعده با فاصله ۸ ساعت (مثلاً ۶ صبح، ۲ بعدازظهر و ۱۰ شب — مدل ۲)."
    ),
    QuizQuestion(
        "سقف OT در برنامه ۶–۳ چند سی‌سی در روز است؟",
        listOf("۶ سی‌سی", "۷٫۵ سی‌سی", "۹ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف برنامه ۶–۳: هر وعده ۲٫۵cc و روزانه ۷٫۵cc؛ سپس ۲۲ روز توقف روی سقف."
    ),
    QuizQuestion(
        "توقف تثبیت روی سقف مصرف طبق اصلاحیه چند روز است؟",
        listOf("۷ روز", "۱۴ روز", "۲۲ روز", "۳۰ روز"),
        2,
        "عبارت اصلاحیه: «۲۲ روز یا دو پله» توقف روی سقف."
    ),
    QuizQuestion(
        "هر ۱ سی‌سی شربت اپیوم تینکچر (OT) معادل چند میلی‌گرم تریاک خوراکی است؟",
        listOf("۵۰ میلی‌گرم", "۱۰۰ میلی‌گرم", "۲۰۰ میلی‌گرم", "۵۰۰ میلی‌گرم"),
        1,
        "هر ۱cc OT = ۱۰۰ میلی‌گرم تریاک خوراکی؛ بنابراین ۱ گرم تریاک = ۱۰cc."
    ),
    QuizQuestion(
        "در برنامه ۶–۳ با مصرف کشیدنی چه باید کرد؟",
        listOf(
            "کاهش تدریجی مصرف کشیدنی",
            "قطع همان شب هفته اول",
            "قطع بلافاصله و شروع همزمان OT",
            "فقط ثبت روزانه کافی است"
        ),
        2,
        "مصرف کشیدنی بلافاصله قطع و همزمان شربت OT شروع می‌شود."
    ),
    QuizQuestion(
        "پله آخر برنامه (روزانه ۰٫۳cc) چند روز ادامه دارد؟",
        listOf("۳ روز", "۷ روز ثابت", "۲ تا ۲۱ روز", "۲۱ روز ثابت"),
        2,
        "پله آخر طبق توضیح جدول، ۲ تا ۲۱ روز ادامه دارد و سپس با نظر راهنما قطع می‌شود."
    ),
    QuizQuestion(
        "سقف OT در درمان الکل چند سی‌سی در روز است؟",
        listOf("۴٫۵ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "سقف OT در درمان الکل ۶ سی‌سی روزانه است (هر وعده ۲cc)."
    ),
    QuizQuestion(
        "در درمان هروئین، شروع OT از روز نخست با روزانه چند سی‌سی است؟",
        listOf("۳ سی‌سی", "۶ سی‌سی", "۱۰٫۵ سی‌سی", "۱۶٫۵ سی‌سی"),
        1,
        "اصلاحیه ۱۳۹۸: از روز نخست روزانه ۶cc (هر وعده ۲cc)؛ سقف برنامه ۱۰٫۵cc است."
    )
)

/** سؤال‌های پاسخ‌داده‌شده برای مرور نهایی */
private data class AnsweredQuestion(
    val question: QuizQuestion,
    val chosenIndex: Int
)

@Composable
fun ExamScreen() {
    var phase by remember { mutableStateOf("start") } // start | running | done
    var qIndex by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(listOf<AnsweredQuestion>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "امتحان",
                subtitle = "سنجش آشنایی تو با متد DST و اصلاحیه پروتکل کنگره ۶۰"
            )
        }

        when (phase) {
            "start" -> {
                item {
                    SectionCard(title = "امتحان متد DST") {
                        Text(
                            text = "این امتحان ${formatNumber(QUIZ_QUESTIONS.size)} سؤال دارد و از همان مطالبی پرسیده می‌شود که در پروتکل و کتابخانهٔ برنامه آمده است. " +
                                "پاسخ درست بلافاصله با توضیح کوتاه نشان داده می‌شود؛ پس نگران اشتباه نباش.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                qIndex = 0; chosen = null; answered = false; score = 0; history = emptyList()
                                phase = "running"
                            }) {
                                Text("شروع امتحان")
                            }
                        }
                    }
                }
                item {
                    SectionCard {
                        Text(
                            text = "💡 نکته: پاسخ همهٔ سؤال‌ها در صفحهٔ «پروتکل» و بخش‌های آموزشی برنامه وجود دارد؛ اگر بخواهی می‌توانی اول مرور کنی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            "running" -> {
                val q = QUIZ_QUESTIONS[qIndex]
                item {
                    SectionCard {
                        // پیشرفت امتحان — همیشه یک خط
                        OneLineText(
                            text = "سؤال ${formatNumber(qIndex + 1)} از ${formatNumber(QUIZ_QUESTIONS.size)}",
                            baseFontSize = 13,
                            minFontSize = 10,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = VioletPrimary
                        )
                        LinearProgressIndicator(
                            progress = { (qIndex + 1).toFloat() / QUIZ_QUESTIONS.size.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    SectionCard(title = q.question) {
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
                                isChosen -> BorderStroke(1.5.dp, VioletPrimary)
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
                                            .background(if (isChosen) VioletPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${i + 1}",
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
                                color = if (wasRight) CorrectGreen else WrongRed
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
                                Button(onClick = {
                                    if (qIndex + 1 < QUIZ_QUESTIONS.size) {
                                        qIndex += 1
                                        chosen = null
                                        answered = false
                                    } else {
                                        phase = "done"
                                    }
                                }) {
                                    Text(
                                        if (qIndex + 1 < QUIZ_QUESTIONS.size)
                                            "سؤال بعدی"
                                        else
                                            "دیدن نتیجه"
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
                                    enabled = chosen != null
                                ) {
                                    Text("ثبت پاسخ")
                                }
                            }
                        }
                    }
                }
            }

            "done" -> {
                item {
                    SectionCard(title = "نتیجهٔ امتحان") {
                        // نمرهٔ بزرگ — همیشه یک خط
                        OneLineText(
                            text = "${formatNumber(score)} از ${formatNumber(QUIZ_QUESTIONS.size)} درست",
                            baseFontSize = 26,
                            minFontSize = 14,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = VioletPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WeCanBadge()
                        }
                        val percent = (score * 100) / QUIZ_QUESTIONS.size
                        Text(
                            text = when {
                                percent >= 90 -> "فوق‌العاده بود! آشنایی‌ات با متد DST عالی است. ما می‌توانیم ✋"
                                percent >= 70 -> "خیلی خوب بود! یک مرور کوتاه روی جدول پروتکل، تو را کامل می‌کند."
                                percent >= 50 -> "بد نبود؛ اما پیشنهاد می‌کنم صفحهٔ «پروتکل» را یک‌بار دیگر با دقت بخوانی."
                                else -> "نگران نباش؛ هر مسافری اولش این‌جوری است. جزوه و صفحهٔ پروتکل را مرور کن و دوباره امتحان بده. ما می‌توانیم ✋"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(onClick = {
                                qIndex = 0; chosen = null; answered = false; score = 0; history = emptyList()
                                phase = "running"
                            }) {
                                Text("امتحان دوباره")
                            }
                            OutlinedButton(onClick = {
                                phase = "start"
                            }) {
                                Text("صفحهٔ اول")
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "مرور پاسخ‌ها") {
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
            }
        }
    }
}
