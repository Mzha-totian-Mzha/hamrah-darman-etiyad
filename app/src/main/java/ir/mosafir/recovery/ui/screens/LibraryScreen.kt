package ir.mosafir.recovery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.SectionCard
import ir.mosafir.recovery.ui.theme.VioletPrimary

private data class Article(val title: String, val body: String)

private val ARTICLES = listOf(
    Article(
        title = "۱) روش DST چیست؟",
        body = """DST مخفف «تخفیف تدریجی مصرف» (تیپرینگ) است؛ روش درمان اعتیاد با شربت اپیوم تینکچر (OT) که در کنگره ۶۰ به‌کار گرفته می‌شود.

سه رکن متد:
• D (ضریب دهژاکام): کاهش یا افزایش برابر با ضریب ۰٫۸ در هر مرحله
• S (پله): هر پله ۲۱ روز است
• T (زمان): حداقل ۱۰ ماه درمان

هر ۱ سی‌سی OT معادل ۱۰۰ میلی‌گرم تریاک است. شروع برنامه به نوع ماده بستگی دارد (برنامه ۶–۳ برای کشیدنی‌ها، معادل‌سازی برای خوراکی، جدول محرک‌ها و...) و بعد از تثبیت، کاهش پله‌به‌پله طبق جدول OT انجام می‌شود تا مصرف به ۰٫۳cc در ۲۴ ساعت برسد.

همیشه سه وعده در ساعت‌های ثابت؛ در پایان کاهش، یکی‌یکی وعده‌ها حذف می‌شود (۳ → ۲ → ۱ وعده).

قانون طلایی: هیچ‌وقت مقدار و ساعت مصرف را بدون هماهنگی با راهنما تغییر نده."""
    ),
    Article(
        title = "۲) سه مقصد: خدمت، عشق‌ورزی، مهربانی",
        body = """در فرهنگ کنگره ۶۰، سه مقصد همیشگی مطرح است: خدمت، عشق‌ورزی و مهربانی.

خدمت: کمک بی‌منت به دیگر مسافران؛ خدمت‌کردن، تجربه‌های تازه به تو می‌دهد و انگیزه سفر را چند برابر می‌کند.

عشق‌ورزی: دوست‌داشتن خود، خانواده و مسیرِ درمان. عشق‌ورزی است که روزهای سخت را تحمل‌پذیر می‌کند.

مهربانی: با خودت هم مهربان باش. اشتباه، بخشی از سفر است؛ مهم این است که دوباره برخیزیم و ادامه دهیم.

این سه، در کنار نظم و پیگیری درمان، موتور محرک سفر به‌سوی آرامش‌اند."""
    ),
    Article(
        title = "۳) نقش راهنما و لژیون",
        body = """در سفر درمان، هیچ‌کس قرار نیست تنها مسیر را طی کند.

راهنما: مسافری است که خودش سفر درمان را با موفقیت پشت سر گذاشته و اکنون به مسافران تِازه‌وارد کمک می‌کند. راهنما مقدار مصرف، سرعت تخفیف و برنامه هفته‌های تو را بر اساس شرایط بدن و سوابق مصرفت تعیین می‌کند.

لژیون: گروهی از مسافران که هر هفته دور هم جمع می‌شوند، تجربه می‌کنند و هم‌سفر می‌شوند. حضور منظم در لژیون، احتمال موفقیت سفر را به‌شدت بالا می‌برد.

توصیه: این اپلیکیشن فقط دفترچه یادداشت و همراه آموزشی توست؛ تصمیم‌های درمانی همیشه با راهنما و لژیون گرفته شود."""
    ),
    Article(
        title = "۴) عشق: ۱۴ وادی برای رسیدن به خود",
        body = """«وادی»، واژهٔ درست همان مفهومی است که در متن‌های کنگره ۶۰ می‌آید: کتاب «عشق: ۱۴ وادی برای رسیدن به خود» نوشتهٔ مهندس حسین دژاکام، مسیر سفر دوم (از قطع مواد تا رسیدن به خود) را در قالب عبور از چهارده وادی شرح می‌دهد؛ کتاب «مسافر» هم روایت سفر اول (از مصرف تا قطع) است.

مفهوم وادی‌ها: زندگی مسافر مانند عبور از وادی‌های پی‌درپی است؛ از وادی شناخت و آگاهی گرفته تا عشق‌ورزی، تسلیم، صلح و آرامش، مسئولیت‌پذیری و در نهایت خدمت به دیگران. هر وادی نماد یک ایستگاه رشد و تعادل در زندگی است.

پیام اصلی: درمان اعتیاد فقط قطع مصرف نیست؛ بازسازی شخصیت و یافتن معنا در زندگی است. هر وادی، تو را برای وادی بعدی آماده می‌کند.

شرح کامل هر وادی را حتماً از خودِ کتاب «عشق: ۱۴ وادی برای رسیدن به خود» بخوان؛ این متن فقط یادآور کوتاهی از مفهوم آن است."""
    ),
    Article(
        title = "۵) توصیه‌های مهم در دوره درمان",
        body = """• نظم: ساعت مصرف، خواب و بیداری را ثابت نگه دار.
• ورزش صبحگاهی: حتی نیم‌ساعت پیاده‌روی صبح، حال جسم و روان را دگرگون می‌کند.
• تغذیه: بدنی که سال‌ها در رنج بوده، به پروتئین و ویتامین نیاز دارد.
• پرهیز مطلق: در طول درمان، هیچ ماده دیگری (حتی الکل و قرص‌های خواب‌آور) جایگزین نشود.
• حضور در جلسات: جمع هفتگی، پشتوانه روانی سفر توست.
• صبر: روزهایی خسته‌کننده خواهند بود؛ با راهنما و هم‌سفران‌ات حرف بزن، نه با تنهایی.
• ثبت روزانه: همین بخش «ثبت روزانه» در اپ را جدی بگیر؛ دیدن پیشرفت، قوی‌ترین انگیزه است."""
    ),
    Article(
        title = "۶) هشدار و سلب مسئولیت",
        body = """این اپلیکیشن یک ابزار آموزشی و یادداشت‌برداری شخصی است و به‌هیچ‌وجه توصیه پزشکی نیست.

• مقدار شروع مصرف، سرعت تخفیف و پایان درمان را فقط راهنمای تربیت‌شده یا پزشک تعیین می‌کند.
• اگر به بیماری زمینه‌ای (قلبی، کبدی، روان‌پزشکی و...) داری یا داروی دیگری مصرف می‌کنی، حتماً با پزشک مشورت کن.
• در موارد اورژانسی (تشنج، درد شدید قفسه سینه، افکار آسیب‌زننده) بلافاصله به اورژانس مراجعه کن.

منابع محتوایی: مفاهیم این اپ خلاصه‌ای آموزشی از روش کنگره ۶۰ بر اساس کتاب مسافر و راهنمای ویرایش ۹۹ است."""
    )
)

@Composable
fun LibraryScreen() {
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "کتابخانه",
                subtitle = "خلاصه‌ای از مفاهیم کتاب مسافر و راهنمای ویرایش ۹۹"
            )
        }

        items(ARTICLES.size) { index ->
            val article = ARTICLES[index]
            val expanded = expandedId == article.title
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedId = if (expanded) null else article.title },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VioletPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "بستن" else "باز کردن"
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = article.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            NoticeCard(
                text = "مطالب این کتابخانه جنبه یادآوری و آموزش دارد؛ برای شرح کامل به نسخه اصلی کتاب‌ها مراجعه کن."
            )
        }
    }
}
