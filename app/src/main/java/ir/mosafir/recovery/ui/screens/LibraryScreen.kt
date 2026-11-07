package ir.mosafir.recovery.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.mosafir.recovery.data.formatNumber
import ir.mosafir.recovery.data.RemoteContent
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.theme.HeroOrange
import ir.mosafir.recovery.ui.theme.HeroPink
import ir.mosafir.recovery.ui.theme.NavAmber
import ir.mosafir.recovery.ui.theme.NavSlate
import ir.mosafir.recovery.ui.theme.NavTeal
import ir.mosafir.recovery.ui.theme.NavViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

private data class LibItem(
    val id: String,
    val title: String,
    val sub: String,
    val color: Color,
    val text: String
)

/** پیام امروز (تأثیرگذار چرخشی) — از featured.json پیش‌نمایش وب */
private data class FeaturedItem(val id: String, val title: String, val sub: String, val text: String)


/** مثلث آموزشی — هر مثلث دقیقاً سه ضلع دارد که هرکدام روی رأس خودش نوشته می‌شود */
private data class TriDef(val n: Int, val title: String, val sides: List<String>)

/** کتابخانه — دقیقاً مطابق پیش‌نمایش وب (عکس ۶) — همهٔ کارت‌ها خوانش صوتی دارند */
private val LIB_ITEMS = listOf(
    LibItem(
        "lib-01", "چهارده مقاله", "مقالات • مهندس حسین دژاکام •", HeroOrange,
        "کتاب «چهارده مقاله» مجموعهٔ چهارده مقالهٔ آموزشی مهندس حسین دژاکام، بنیان‌گذار کنگره ۶۰ است. مقاله‌ها با توصیف حال امروزِ ما آغاز می‌شوند: ما که امروز در این جمع نشسته‌ایم، روزی با مواد مخدر زندگی می‌کردیم؛ کانال‌های عصبی بدنمان تحت تأثیر مواد قرار گرفته بود و تفکرمان درست عمل نمی‌کرد. با ورود به کنگره ۶۰ و پذیرش بیماری، راه درمان را آغاز کردیم. در ادامهٔ کتاب، سه ضلع درمان — جسم، روان و جهان‌بینی — یک‌به‌یک با راه‌حل‌های عملی شرح داده می‌شود. پیام کتاب روشن است: اعتیاد بیماری است و این بیماری، درمان دارد؛ ما می‌توانیم."
    ),
    LibItem(
        "lib-02", "کنگره ۶۰ چیست؟", "معرفی • سابقه و اهداف کنگرهٔ ۶۰ •", NavTeal,
        "کنگره ۶۰ تشکیلات تحقیقاتی مستقلی است که برای رهایی مصرف‌کنندگان مواد مخدر از دام اعتیاد شکل گرفته است؛ برای کسانی که همواره خواستار ترک بودند، اما راه‌حل منطقی برای درمان خود پیدا نکرده بودند. اساس شکل‌گیری کنگره بر این اصل است: فردی که خودش رها شده، به فردی که هنوز در حال مصرف است کمک می‌کند. اساس کار کنگره ۶۰ بر محبت، عقل و ایمان استوار است. در جلسات، هر عضو از تجربه و دانش خودش در مقولهٔ اعتیاد و دستور جلسه سخن می‌گوید و از نصیحت کردن و شعار دادن پرهیز می‌شود. کنگره به هیچ گروهی وابسته نیست و از نظر مالی به خود متکی است؛ استحکام پایه‌های مالی و علمی، همواره از اهداف آن بوده است."
    ),
    LibItem(
        "lib-03", "راهنمای تازه‌واردین", "راهنما • نخستین روزهای سفر •", HeroPink,
        "راهنمای تازه‌واردین، نخستین نقشهٔ راه برای روزهای اول درمان است. پنج نکتهٔ مهم: ۱) اعتیاد یک بیماری است و درمان دارد؛ برای درمان باید حتماً یک راهنما انتخاب کنی. ۲) نظم، پایهٔ درمان است: خواب و بیداری مشخص، ورزش صبحگاهی، تغذیهٔ درست و حضور به‌موقع در جلسات. ۳) مقدار و ساعت مصرف شربت OT را هیچ‌گاه بدون هماهنگی با راهنما تغییر نده. ۴) در سفر اول، مصرف طبق جدول پروتکل پله‌پله کاهش می‌یابد؛ صبر و پیگیری، کلید عبور از این مرحله است. ۵) سفر دوم بعد از رهایی از مواد آغاز می‌شود: بازسازی شخصیت و شناخت خود. و بدان که در این مسیر تنها نیستی؛ راهنما و همسفران کنار تو هستند. ما می‌توانیم."
    ),
    LibItem(
        "lib-04", "چهارده وادی عشق", "کتاب • ۱۴ وادی • متن و صوت •", NavViolet,
        "کتاب «چهارده وادی عشق» (کتاب عشق) نوشتهٔ مهندس حسین دژاکام است؛ کتابِ سفر دوم، یعنی مرحلهٔ شناخت خویش که بعد از رهایی از مصرف مواد آغاز می‌شود. ترک مواد، نیمی از راه است و نیمهٔ دیگر، ساختن دوبارهٔ انسان. این کتاب این نیمهٔ دوم را در چهارده ایستگاه (وادی) شرح می‌دهد: وادی اول «آغاز» می‌گوید همه‌چیز از یک تصمیم درست شروع می‌شود؛ وادی‌های میانی، تفکر، مسئولیت‌پذیری، صبر، تسلیم و صلح درون را می‌آموزند؛ و وادی پایانی، انسان را به عشق‌ورزی و خدمت به دیگران می‌رساند. هر وادی یک درس برای زندگی سالم است؛ خواندن آن برای مسافران و همسفران هر دو آموزنده است."
    ),
    LibItem(
        "lib-05", "جهان‌بینی", "جهان‌بینی ۱ و ۲ • تفکر درست، زندگی درست •", NavTeal,
        "جهان‌بینی، عینکی است که با آن دنیا را می‌بینیم؛ اگر تفکر ما بگوید «مشکل من حل نمی‌شود»، همین را ساخته‌ایم و مشکل حل نخواهد شد؛ اگر بگوید «بیماری من درمان دارد»، راه درمان باز می‌شود. کتاب‌های جهان‌بینی ۱ و ۲ می‌آموزند که پیش از هر تصمیم باید تفکر کرد؛ تفکر، انتخاب می‌آورد و انتخاب، سرنوشت را می‌سازد. با شناخت خود، ایمان به قدرت مطلق و ساختن تصویر مثبت از آینده، زندگی سالم ساخته می‌شود. برای همین در کنگره ۶۰ کلاس‌های جهان‌بینی، ادامهٔ درمان‌اند: درمان فقط دارو نیست؛ فکرِ سالم هم درمان است."
    ),
    LibItem(
        "dst", "پروتکل DST", "متد درمان اعتیاد با روش DST.", HeroOrange,
        "DST مخفف «تخفیف تدریجی مصرف» است: هر ۱ سی‌سی شربت OT معادل ۱۰۰ میلی‌گرم تریاک خوراکی است؛ سه وعده در ساعت‌های ثابت با فاصله ۸ ساعت؛ هر پله ۲۱ روز و کاهش با ضریب ۰٫۸؛ توقف روی سقف مصرف ۲۲ روز؛ و پله آخر ۰٫۳cc در ۲۴ ساعت. جدول کامل پله‌ها در تب «پروتکل» برنامه است."
    ),
    LibItem(
        "vazayef", "شرح وظایف", "وظایف مرزبانان و خدمتگزاران لژیون.", NavSlate,
        "لژیون مرزبانان نمایندگی همسفران آقا متشکل از ۵ نفر است: نگهبان، دبیر، استاد جلسه، مرزبان امور مالی و مهماندار. طول مدت خدمت مرزبانی ۶ ماه است و برگزاری انتخابات مرزبانی بر عهده راهنمای لژیون مرزبانی نمایندگی می‌باشد. وظیفهٔ هر یک در متن «شرح وظایف» کنگره ۶۰ آمده است."
    ),
    LibItem(
        "ghavanin", "قوانین کنگره ۶۰", "حرمت‌ها و قوانین مجموعه.", NavAmber,
        "کنگره ۶۰ چهارده قانون دارد؛ چند نمونه: کنگره تشکیلات تحقیقاتی مستقلی است که با راهنمایی نیروهای ماوراءالطبیعه و توکل به قدرت مطلق حرکت می‌کند؛ شرط عضویت، پذیرفتن اصول و قوانین و تعهد به رهایی از دام اعتیاد است؛ گردانندگان از بین مسافران و همسفران انتخاب می‌شوند؛ به همهٔ خواهانِ رهایی، مساوات اطلاق می‌گردد؛ منابع مالی، کمک‌های داوطلبانهٔ اعضاست؛ و کنگره در موضوعات خارج از اهدافش نظری ندارد. حرمت‌ها هم جزو قوانین‌اند: در جلسات دروغ، غیبت، بحث سیاسی، قطع حرف دیگران، تلفن بدون مجوز راهنما و دریافت و پرداخت پول بدون هماهنگی ممنوع است. رعایت قوانین، پشتوانهٔ آرامش و امنیت جمع است."
    ),
    LibItem(
        "shast", "۶۰ درجه", "کتاب ۶۰ درجه.", NavViolet,
        "کتاب «۶۰ درجه» روایت سفر مهندس حسین دژاکام و همراهانش برای عبور از «منطقهٔ ۶۰ درجه زیر صفر» — یعنی پایین‌ترین نقطهٔ زندگیِ مصرف — تا بازگشت به زندگی سالم است. نام کتاب از این ایده گرفته شده که گاهی با تغییر ۶۰ درجه‌ای زاویهٔ نگاه، همان مسئلهٔ قبلی معنای دیگری پیدا می‌کند. این کتاب با قصه‌گویی و مثال‌های ساده، مثلث تفکر، تصمیم و اجرا را آموزش می‌دهد و نشان می‌دهد انسان با شناخت خود و ایمان به قدرت مطلق، از هر نقطهٔ تاریکی می‌تواند بازگردد."
    ),
    LibItem(
        "behtarin", "بهترین راه", "سخنان آموزشی کنگرهٔ ۶۰.", NavTeal,
        "جزوهٔ «بهترین راه» پیامی روشن دارد: بسیاری تصور می‌کنند بهترین راه، کوتاه‌ترین راه است و به درستی یا نادرستی آن توجه نمی‌کنند؛ راهی را می‌روند که زودتر به خواسته‌هایشان برساند؛ برای همین مشق‌هایشان ناتمام و بی‌محتوا می‌ماند و مجبورند دوباره بنویسند. راه نیمه‌تمام، مثل غذای نیمه‌پخته یا خام است: قابل خوردن نیست. در درمان اعتیاد هم راه‌های میان‌بُر به بازگشتِ مصرف ختم می‌شوند؛ راه درست، راهِ اصولی درمان است: پذیرش بیماری، درمان با متد DST زیر نظر راهنما، نظم و پیگیری. نتیجهٔ این راه، ماندگار است."
    ),
    LibItem(
        "mosalas", "مثلث‌های آموزشی", "سه‌گانه‌های آموزشی درمان.", HeroPink,
        "مثلث‌های آموزشی کنگره ۶۰، سه‌گانه‌های پایهٔ درمان‌اند: تفکر، تصمیم، اجرا — شناخت، نظم، پیگیری — و آموزش، محبت، کاریابی. هر مثلث، سه ضلع دارد که اگر یکی ضعیف شود، تعادل کل مثلث به هم می‌ریزد؛ مسیر درمان، تمرین مداومِ حفظ تعادل این مثلث‌هاست."
    ),
    LibItem(
        "masraf", "مصرف‌کننده کیست؟", "شناخت بیماری اعتیاد.", HeroOrange,
        "مصرف‌کنندهٔ مواد مخدر، فردی بیمار است که در اثر استفادهٔ مکرر از مواد، در ساختار وجودی خود تخریب پدید آورده و زندگی خود و اطرافیانش را آشفته کرده است؛ چه شروع مصرف دانسته باشد و چه ندانسته، فرقی نمی‌کند، چون هر دو نشانهٔ عدم آگاهی است. این تخریب در جسم، سیستم ضربان قلب، سیستم ایمنی و تولید مخدرهای طبیعی بدن را مختل می‌کند و در روان و جهان‌بینی، جهانی مملو از ترس، اضطراب، نگرانی و حقارت می‌سازد. برای اصلاح ۹۰٪ از این ساخته‌ها، حداقل ۱۰ ماه درمان لازم است. شناخت این واقعیت، اولین قدم درمان است: اعتیاد بیماری است، نه لذت‌جویی؛ و بیماری، درمان دارد."
    )
)

/** پیام امروز — ۱۴ مورد؛ دقیقاً همسان با featured.json پیش‌نمایش وب */
private val FEATURED = listOf(
    FeaturedItem(
        "f-emrooz", "توصیف حال امروز ما", "از مقدمهٔ کتاب چهاردم مقاله • مهندس حسین دژاکام",
        "توصیف حال امروز ما — ما که امروز در این جمع نشسته‌ایم، روزی با مواد مخدر زندگی می‌کردیم؛ کانال‌های عصبی بدن ما تحت تأثیر مواد قرار گرفته بود و تفکر ما درست عمل نمی‌کرد. با ورود به کنگره ۶۰ و پذیرش بیماری، راه درمان را آغاز کردیم. امروز می‌توانیم با تکیه بر تجربهٔ همسفران، زندگی سالم و آرام بسازیم؛ ما می‌توانیم."
    ),
    FeaturedItem(
        "f-sabr", "صبر، کلید گنجینه", "از وادی هفتم و هشتم عشق",
        "صبر یعنی اثر گرفتن زمان؛ یعنی اجازه بدهیم هر چیز در زمان خودش به بار بنشیند. کشاورز بذر را می‌کارد و منتظر می‌ماند؛ او نمی‌دود زمین را بکَند تا زودتر سبزه دربیاید. در درمان هم همین است: پله‌ها یکی‌یکی و با نظم طی می‌شوند. آنکه صبور است، در آرامش است و آنکه در آرامش است، درست تصمیم می‌گیرد. ما می‌توانیم؛ با صبر و پیوستگی."
    ),
    FeaturedItem(
        "f-vali-avval", "وادی اول — آغاز", "از کتاب چهارده وادی عشق",
        "مسافر با آغاز سفر، اول ایمان پیدا می‌کند؛ ایمان به اینکه می‌شود. وقتی ایمان آمد، تلاش را آغاز می‌کند و تلاش، راه را باز می‌کند. هیچ سفری بدون اولین قدم به پایان نمی‌رسد؛ اما هیچ قدمی هم بی‌ایمان استوار نمی‌ماند. آغاز، نیمهٔ همهٔ کارهاست — و آغازِ تو، همین امروز است."
    ),
    FeaturedItem(
        "f-jahanbini", "تفکر، سرآغاز تغییر", "از کتاب جهان‌بینی",
        "اگر تفکرِ ما بگوید «مشکل من حل نمی‌شود»، همین را ساخته‌ایم و حل نخواهد شد. باید در وجودمان امید و تصویر مثبت بسازیم؛ جهان‌بینی ما راهنمای انتخاب‌های ماست. با تفکر درست، زندگی درست ساخته می‌شود؛ و تفکر درست از آموزش و تجربه به دست می‌آید. نگاهِ امروزم را انتخاب کن؛ فردای تو ساختهٔ همین نگاه است."
    ),
    FeaturedItem(
        "f-motavazan", "تعادل، سنگ بنای زندگی", "از آموزش‌های کنگرهٔ ۶۰",
        "انسان در تعادل است که سالمی و آرامش را تجربه می‌کند؛ نه در افراط و نه در تفریط. کار، استراحت، تفریح و عبادت — هر کدام جای خود را دارند و هیچ‌کدام نباید جای دیگری را بگیرند. وقتی یکی از اضلاع زندگی قوی و دیگری ضعیف باشد، مثلث زندگی کج می‌شود. وظیفهٔ ما در درمان، برقراری تعادل در جسم، روان و جهان‌بینی است."
    ),
    FeaturedItem(
        "f-nazm", "نظم؛ اولین درس سفر", "از توصیه‌های راهنمایان",
        "نظم در خواب و بیداری، نظم در مصرف، نظم در حضور در جلسات — مسافری که نظم را رعایت کند، خودِ درمان او را به جلو می‌برد. بی‌نظمی، سنگی است که راه رهجو را می‌بندد؛ و نظم، کلیدی است که اولین درها را باز می‌کند. برنامهٔ امروزت را بنویس، به آن وفادار بمان؛ نظم، آزادیِ واقعی را می‌سازد."
    ),
    FeaturedItem(
        "f-mehrabani", "محبت واقعی", "از وادی یازدهم عشق",
        "محبت واقعی، در اول محبت، دوم محبت است؛ یعنی محبتِ بی‌چشمداشت. آنکه محبت می‌کند و منتظر جبران می‌ماند، معامله کرده است نه محبت. همسفرِ خوب، بی‌منت می‌بخشد، بی‌منت خدمت می‌کند و با همین بی‌مندی، دل‌ها را آباد می‌کند. عشقِ ورزیدن، آخرین وادی‌هاست — اما از همین امروز باید تمرینش کرد."
    ),
    FeaturedItem(
        "f-mohem", "مهم آن است که شروع کنی", "از سخنان آموزشی کنگرهٔ ۶۰",
        "کسی که در راه است، از کسی که ایستاده جلوتر است؛ حتی اگر آهسته برود. برای شروع، لازم نیست کامل باشی؛ لازم است شروع کنی و در راه بمانی. اشتباه کردن، بخشی از یادگیری است؛ سقوطِ یک قدم، شکست نیست — نرفتنِ قدم بعدی شکست است. ما می‌توانیم؛ کافی است بخواهیم و تلاش کنیم."
    ),
    FeaturedItem(
        "f-bimari", "اعتیاد، بیماری است", "از مکانیسم اعتیاد • مهندس دژاکام",
        "اعتیاد یک بیماری است؛ نه لذت‌جویی و نه بی‌ارادگی. این بیماری در سه ضلع ظاهر می‌شود: جسم که به ماده وابسته شده، روان که در خدمت مصرف قرار گرفته، و جهان‌بینی که تاریک شده است. بیماری، درمان دارد و درمانِ آن قطع نیست؛ درمان است: تخفیف تدریجی، بازسازی و آموزش. وقتی علت را بشناسی، درمان را می‌پذیری."
    ),
    FeaturedItem(
        "f-dost", "راه نرفتن، تنها راه شکست", "از آموزش‌های لژیون",
        "در این مسیر دو راه بیشتر نیست: یا می‌جنگی و می‌مانی، یا تسلیم می‌شوی و از دست می‌دهی. تجربه نشان داده کسانی که با نظم و صبر پیش می‌روند به آرامش پایدار می‌رسند. حال خوبِ امروزِ تو، ساختهٔ جلسات دیروز است؛ و جلسهٔ فردا، ساختهٔ حضورِ امروز تو. پیوندت را با جمع حفظ کن؛ تنهایی، دشمن رهجوست."
    ),
    FeaturedItem(
        "f-sokut", "پاک بودن، ارزشمندترین سرمایه", "از روز شماری مسافران",
        "هر روز پاک، یک پیروزی بزرگ است؛ که خداوند پاداش نیکی را ضایع نمی‌کند. روزهای پاک تو، عدد نیستند؛ سندهٔ اعتماد‌سازی‌اند: اعتمادِ خانواده، اعتمادِ خودت، و اعتمادِ کسانی که تازه وارد شده‌اند و به تو نگاه می‌کنند. تو الگوی امروزِ دیگری هستی؛ پس امروزت را پاک نگه دار. ما می‌توانیم ✋"
    ),
    FeaturedItem(
        "f-khak", "خدمت، داروی خاموش", "از وادی چهاردهم عشق",
        "آنکه به دیگران خدمت می‌کند، در حقیقت خود را درمان می‌کند؛ خدمت، دارویی است خاموش که زخم‌های کهنه را ترمیم می‌کند. در کنگره، خدمتگزاران کسانی‌اند که روزی خودشان نیازمند بودند و امروز ایستاده‌اند تا دست بگیرند. تو هم می‌توانی روزی از گرفتار شدن، به خدمتگزار شدن برسی — این زیباترین سفرِ دوم است."
    ),
    FeaturedItem(
        "f-emroz", "امروز، تنها چیزی است که داری", "از آموزش‌های جهان‌بینی",
        "حسرتِ دیروز و نگرانیِ فردا، انرژی امروز را می‌بلعد. تجربهٔ دیروز مالِ دیروز بود؛ استفادهٔ امروز، مالِ امروز است و امید به فردا، بذرِ فردا. مثلث خوشبختی همین است: تجربهٔ دیروز، استفادهٔ امروز، امید به فردا. همین امروز را آباد کن؛ فردا، ثمرهٔ همین امروز است."
    ),
    FeaturedItem(
        "f-ommid", "ناامیدی ممنوع", "از سه ضلع جهالت: منیت، ترس، ناامیدی",
        "جهالت سه ضلع دارد: منیت، ترس و ناامیدی. هر سه، دیوارِ راه‌اند و هر سه با یک چیز فرو می‌ریزند: شناخت. وقتی بشناسی که بیماری تو درمان دارد، ترس جای خود را به امید می‌دهد؛ وقتی بپذیری که تنها نیستی، منیت آب می‌شود؛ و وقتی در جمع، رهروانی را ببینی که رسیدند، ناامیدی بی‌اعتبار می‌شود. امید داشته باش؛ راه، باز است."
    ),
    FeaturedItem(
        "nf-01", "سفر اول، مهم‌ترین سفر", "از آموزه‌های کنگره ۶۰",
        "سفر اول مهم‌ترین سفر توست؛ چون بنیان همهٔ درمان در همین سفر گذاشته می‌شود. اگر سفر اول را با تمام وجود اجرا کنی، بقیهٔ راه هموار می‌شود. عجله نکن، پله‌ها را یکی‌یکی برو و دست راهنمایت را رها نکن."
    ),
    FeaturedItem(
        "nf-02", "پیوند انس، تضمین درمان", "از مثلث‌های آموزشی کنگره ۶۰",
        "پیوند انس تضمین‌کنندهٔ درمان است. پیوند انس یعنی رابطهٔ محبت‌آمیز و بدون چشمداشت با همسفران و راهنما؛ وقتی این پیوند شکل بگیرد، هیچ‌وقت در سفر تنها نمی‌مانی و درمان از دل همین پیوندها می‌جوشد."
    ),
    FeaturedItem(
        "nf-03", "استمرار، رمز رهایی", "از آموزه‌های کنگره ۶۰",
        "رهایی یک‌شبه به دست نمی‌آید؛ نتیجهٔ استمرار است. هر روز ثبت روزانه، هر هفته لژیون، هر پله ۲۱ روز. هیچ جلسه‌ای را از دست نده؛ چون حلقه‌های زنجیر رهایی، همین استمرارها هستند."
    ),
    FeaturedItem(
        "nf-04", "وادی هشتم؛ تسلیم", "از چهارده وادی عشق",
        "تسلیم یعنی پذیرفتن قانون جهانی بدون چک‌وچانه؛ دانستن اینکه هر چه برایم رخ می‌دهد از سر حکمت است. در برابر این قانون تسلیم باش، اما در نگهداری از درمان، کوشاترین باش."
    ),
    FeaturedItem(
        "nf-05", "خدمت، معجزهٔ درمان", "از آموزه‌های کنگره ۶۰",
        "وقتی به دیگری خدمت می‌کنی، خودت درمان می‌شوی. خدمت، خودخواهی را می‌شوید و جای خالی مصرف را در وجودت پر می‌کند. دست‌اندرکاران کنگره ۶۰ با همین معجزه، سال‌هاست رها مانده‌اند."
    ),
    FeaturedItem(
        "nf-06", "ما، مصرف‌کنندهٔ جسم و روان", "از چهارده مقاله • مهندس دژاکام",
        "ما با جسم و روانمان مصرف می‌کنیم؛ نه فقط با مواد. غذا، خواب، فکر و احساس هم مصرف می‌شوند. درمان یعنی تعادل در همهٔ این مصرف‌ها؛ واد کردن جسم به آرامش و روان به تعادل."
    ),
    FeaturedItem(
        "nf-07", "سکت؛ قدرت آرامش", "از آموزه‌های کنگره ۶۰",
        "سکت یعنی توقف، سکوت و بازگشت به خود. در دل سکت، آشفتگی‌ها ته‌نشین می‌شوند و راه درست پیدا می‌شود. هر روز چند دقیقه سکت کن؛ با نفس‌های عمیق و ذهنی رها از هیاهو."
    ),
    FeaturedItem(
        "nf-08", "مسئولیت‌پذیری، سنگ بنای رهایی", "از شرح وظایف کنگره ۶۰",
        "رهایی وقتی شروع می‌شود که مسئولیت زندگی‌ات را بپذیری. هیچ‌کس به جای تو سفر نمی‌رود؛ راهنما مسیر را نشان می‌دهد اما قدم‌ها را تو باید برداری. مسئول‌پذیری، اولین قدم بزرگواری است."
    ),
    FeaturedItem(
        "nf-09", "امید؛ اولین هدیهٔ درمان", "از کتاب بهترین راه",
        "اولین چیزی که کنگره ۶۰ به تو می‌دهد، امید است؛ امیدی که از دیدن رهاشدگان می‌آید. آن‌ها هم مثل تو زمین خورده بودند، اما بلند شدند و ادامه دادند. پس تو هم می‌توانی؛ فقط ادامه بده."
    ),
    FeaturedItem(
        "nf-10", "سه‌گانهٔ درمان", "از مثلث درمان: جسم، روان، جهان‌بینی",
        "درمان اعتیاد سه ضلع دارد: جسم با دارو و برنامهٔ DST، روان با خدمت و پیوند انس، جهان‌بینی با آموزش و وادی‌ها. اگر یک ضلع را رها کنی، مثلث ناقص می‌ماند و درمان لنگ می‌زند."
    )
)


/** فهرست کامل ۵۸ مثلث جزوهٔ رسمی «مثلث‌های آموزشی کنگره ۶۰» — هر مثلث با سه ضلع مجزا رسم می‌شود */
private val TRIANGLES: List<TriDef> = listOf(
    TriDef(1, "مثلث کنگره ۶۰", listOf("عدالت", "معرفت", "عمل سالم")),
    TriDef(2, "مثلث درمان اعتیاد", listOf("جسم", "روان", "جهان‌بینی")),
    TriDef(3, "مثلث مهندسی اعتیاد", listOf("تغذیه", "اشباع", "سازگاری")),
    TriDef(4, "مثلث دانایی", listOf("تفکر", "تجربه", "آموزش")),
    TriDef(5, "مثلث تفکر", listOf("مسیر حرکت", "مبدأ", "مقصد")),
    TriDef(6, "مثلث آموزش", listOf("متون آموزشی", "شخص آموزش‌دهنده", "شخص آموزش‌گیرنده")),
    TriDef(7, "مثلث جهالت", listOf("منیت", "ترس", "ناامیدی")),
    TriDef(8, "مثلث نفس", listOf("امّاره", "لوّامه", "مطمئنه")),
    TriDef(9, "مثلث خوراک نفس", listOf("تفکر", "صوت", "نور")),
    TriDef(10, "مثلث حیات (دنیا و آخرت)", listOf("برزخ", "جهنم", "بهشت")),
    TriDef(11, "مثلث ساعت (در جهان فیزیکی)", listOf("فضا", "زمان", "مکان")),
    TriDef(12, "مثلث آفرینش", listOf("صوت", "نور", "حس")),
    TriDef(13, "مثلث پنهان‌کاری", listOf("موضوع", "پنهان‌کننده", "کسی که از او پنهان می‌شود")),
    TriDef(14, "مثلث خودکشی", listOf("سیستم ایکس (فیزیولوژی)", "علت", "شناخت")),
    TriDef(15, "مثلث قاضی (قضاوت)", listOf("شاکی", "متشاکی", "دلایل و مدارک")),
    TriDef(16, "مثلث سیستم ایمنی", listOf("تشخیص نیروهای دشمن", "تشخیص نیروهای خودی", "مشکوک (دشمن یا خودی؟)")),
    TriDef(17, "مثلث عمل", listOf("توان فیزیکی (توانایی)", "مهارت و آموزش", "حس")),
    TriDef(18, "مثلث کلام", listOf("راستین", "دروغین", "خنثی")),
    TriDef(19, "مثلث وادی سیزدهم", listOf("تغییر", "تبدیل", "ترخیص")),
    TriDef(20, "مثلث انسان دوبعدی", listOf("عشق", "عقل", "ایمان")),
    TriDef(21, "مثلث عشق", listOf("سایه‌ها", "جاذبه", "حس")),
    TriDef(22, "مثلث تشخیص جایگاه", listOf("خود فرد", "خانواده", "اجتماع")),
    TriDef(23, "مثلث جرقه", listOf("تقدیر (گذشته)", "خواست (حال)", "فرمان الهی یا تفکرات (آینده)")),
    TriDef(24, "مثلث افزایش تکامل", listOf("حرکت", "سرعت", "برکت")),
    TriDef(25, "مثلث نیرو", listOf("القا", "احیا", "تحرک")),
    TriDef(26, "مثلث احیا", listOf("طعام", "احیا شونده", "احیاکننده")),
    TriDef(27, "مثلث یاران", listOf("سابقون", "راست (یمین)", "چپ (شمال)")),
    TriDef(28, "مثلث NGO موفق", listOf("علم", "نیروی انسانی", "منابع مالی")),
    TriDef(29, "مثلث کشفیات جناب آقای مهندس", listOf("OT و DST", "D.sap", "نیکوتین")),
    TriDef(30, "مثلث معما", listOf("صورت‌مسئله", "حل‌کنندهٔ مسئله", "حل مسئله")),
    TriDef(31, "مثلث ادامهٔ حیات", listOf("قدرت تفکر", "قدرت انجام عمل", "قدرت فیزیک بدن")),
    TriDef(32, "مثلث ازدواج", listOf("مذکر", "مؤنث", "کار روی نقاط مشترک")),
    TriDef(33, "مثلث خوشبختی", listOf("تجربهٔ دیروز", "استفادهٔ امروز", "امید به فردا")),
    TriDef(34, "مثلث تباهی", listOf("حسرت دیروز", "اتلاف امروز", "ترس از فردا")),
    TriDef(35, "مثلث ذلت", listOf("افسردگی", "خشم و خشونت", "ترس و اضطراب")),
    TriDef(36, "مثلث رحمت", listOf("مسئولیت", "اخلاق", "واقعیت")),
    TriDef(37, "مثلث تخریب شخصیت", listOf("احساس بی‌لیاقتی", "احساس گناه", "احساس تهدید")),
    TriDef(38, "مثلث جسم سالم و ایده‌آل", listOf("ورزش", "تغذیه", "خواب")),
    TriDef(39, "مثلث پرسشگری", listOf("چرا", "که چه بشود (که چی)", "چگونه")),
    TriDef(40, "مثلث نفس واحده", listOf("روح", "جن", "انسان")),
    TriDef(41, "مثلث تعادل بیوشیمی مغز", listOf("داروی مناسب", "زمان لازم", "روش مناسب")),
    TriDef(42, "مثلث گروه‌بندی معتادان", listOf("نامزدهای اعتیاد", "دوستدار اعتیاد", "خواستار رهایی")),
    TriDef(43, "مثلث برخورد با سه گروه", listOf("پیشگیری", "مهار", "درمان")),
    TriDef(44, "مثلث خواستاران رهایی", listOf("بیکار", "شاغل", "آخر خطی")),
    TriDef(45, "مثلث صحبت", listOf("کلام مناسب", "زمان مناسب", "مکان مناسب")),
    TriDef(46, "مثلث جان", listOf("نفس", "روح", "جن")),
    TriDef(47, "مثلث مراتب دانایی", listOf("علم‌الیقین", "عین‌الیقین", "حق‌الیقین")),
    TriDef(48, "مثلث لذت (جابجایی انرژی)", listOf("از دست دادن انرژی", "به دست آوردن انرژی", "تغییر انرژی به صورتی دیگر")),
    TriDef(49, "مثلث معادله عملکرد", listOf("حس (نیرو)", "مسیر حرکت", "عملکرد")),
    TriDef(50, "مثلث رهایی کامل", listOf("ترک", "درمان", "تعادل")),
    TriDef(51, "مثلث روابط بین انسان‌ها", listOf("خوب", "بد", "مجهول")),
    TriDef(52, "مثلث قیاس ما در موضوعات", listOf("اطلاعات درست", "اطلاعات غلط", "اطلاعات بی‌ربط")),
    TriDef(53, "مثلث وظایف رهجو", listOf("نسبت به راهنما", "نسبت به کنگره", "نسبت به خانواده و کار و زندگی")),
    TriDef(54, "مثلث ورزشکار موفق", listOf("قدرت بدنی", "مهارت", "حس")),
    TriDef(55, "مثلث جسم", listOf("نقشه جسم (ژن)", "تفکر و اندیشه (جهان‌بینی)", "نگهداری جسم")),
    TriDef(56, "مثلث روان", listOf("جسم (فیزیولوژی)", "جهان‌بینی", "ضمیر ناخودآگاه")),
    TriDef(57, "مثلث تشکیل ساختار", listOf("خواسته و حس", "بستر", "تفکر")),
    TriDef(58, "مثلث سه اصل رساندن غذا به شهر وجودی", listOf("غذای مناسب", "میزان مناسب", "زمان مناسب"))
)

/* ======================= موتور چرخش محتوا (SharedPreferences) ======================= */

private const val MAX_HIST = 8
private const val KEY_FEAT_HIST = "feat_hist"
private const val MAX_FEAT_HIST = 12
private const val KEY_VOICE_FEMALE = "voice_female"

/** تاریخچهٔ موارد نمایش‌داده‌شده — تا هر بار محتوای تازه ببینیم */
private fun readHist(prefs: SharedPreferences, key: String): List<String> =
    prefs.getString(key, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

private fun saveHist(prefs: SharedPreferences, key: String, id: String, max: Int = MAX_HIST) {
    val h = (listOf(id) + readHist(prefs, key).filter { it != id }).take(max)
    prefs.edit().putString(key, h.joinToString(",")).apply()
}

/** انتخاب بدون تکرار: از میان مواردی که اخیراً دیده نشده‌اند */
private fun <T> pickRotating(pool: List<T>, recentIds: List<String>, idOf: (T) -> String): T {
    val fresh = pool.filter { idOf(it) !in recentIds }
    val cand = fresh.ifEmpty { pool }
    return cand[Random.nextInt(cand.size)]
}

/* — بانک پاساژها: قطعه‌های خواندنی استخراج‌شده از کل کتاب‌های کنگره ۶۰ —
   ۱۴ وادی، ۱۴ مقاله، ۶۰ درجه، راهنما و راهنمایی، جهان‌بینی، راهنمای تازه‌واردین،
   پروتکل DST، اصلاحیه، مثلث‌های آموزشی + کتاب‌های اسکنی (OCR) —
   هر قطعه برچسب «منبع: نام کتاب» دارد؛ PDFهای امتحان طبق خواستهٔ کاربر وارد بانک نمی‌شوند */
private data class Passage(val id: String, val src: String, val n: Int, val text: String)

/** هر کارت کتاب از کدام منبع(ها) تغذیه می‌شود */
private val LIB_SRC: Map<String, List<String>> = mapOf(
    "lib-01" to listOf("چهارده مقاله"),
    "lib-02" to listOf("کنگره ۶۰ چیست؟"),
    "lib-03" to listOf("راهنمای تازه‌واردین", "راهنما و راهنمایی"),
    "lib-04" to listOf("چهارده وادی عشق"),
    "lib-05" to listOf("جهان‌بینی ۱", "جهان‌بینی ۲"),
    "dst" to listOf("پروتکل DST", "اصلاحیه پروتکل DST"),
    "vazayef" to listOf("شرح وظایف"),
    "ghavanin" to listOf("قوانین کنگره ۶۰"),
    "shast" to listOf("کتاب ۶۰ درجه"),
    "behtarin" to listOf("بهترین راه"),
    "mosalas" to listOf("مثلث‌های آموزشی"),
    "masraf" to listOf("مصرف‌کننده کیست؟")
)

/** خواندن بانک پاساژها از assets — اگر موجود نبود، فهرست خالی (کارت‌ها متن ثابت خود را نشان می‌دهند) */
private fun loadPassages(context: Context): List<Passage> = try {
    val raw = context.assets.open("passages.json").bufferedReader().use { it.readText() }
    val arr = JSONObject(raw).getJSONArray("passages")
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        Passage(o.optString("id"), o.optString("src"), o.optInt("n"), o.optString("text"))
    }
} catch (_: Exception) {
    emptyList()
}

/* — پاک‌سازی بانک پاساژها برای نمایش در کتابخانه —
   قطعه‌های خامِ استخراج‌شده از PDF گاهی از وسط جمله شروع می‌شوند یا فهرستِ
   منابع انگلیسیِ آخر کتاب‌اند؛ اینجا متنِ هر کتاب یک‌بار از سرِ جمله برش
   می‌خورد تا هیچ قطعه‌ای «نیمه‌شروع» نباشد و هیچ قطعهٔ انگلیسیِ منابع نمایش نیاید. */
private const val CLEAN_TARGET = 620 // طول هدف هر قطعهٔ تمیز (نویسه)
private const val CLEAN_MIN = 140    // کمینهٔ طول قطعه — کوتاه‌ترها به قطعهٔ قبلی می‌چسبند

/** آیا متن بیشترِ آن انگلیسی است (فهرست منابع/ارجاعات)؟ */
private fun isLatinHeavy(t: String): Boolean {
    var latin = 0
    var letters = 0
    for (c in t) {
        if (c.isLetter()) {
            letters++
            if (c in 'A'..'Z' || c in 'a'..'z') latin++
        }
    }
    return letters > 40 && latin * 100 / letters > 35
}

/** شکستن متن به جمله‌ها — فقط در پایان جمله (. ؟ ! ؛ ! و خط جدید) */
private fun splitSentences(text: String): List<String> {
    val out = ArrayList<String>()
    val sb = StringBuilder()
    for (c in text) {
        sb.append(c)
        if (c == '.' || c == '؟' || c == '?' || c == '!' || c == '؛' || c == '\n') {
            val s = sb.toString().trim()
            if (s.length > 2) out.add(s)
            sb.clear()
        }
    }
    val rest = sb.toString().trim()
    if (rest.length > 2) out.add(rest)
    return out
}

/** چسباندن قطعه‌های خامِ هر کتاب و برشِ دوباره از سرِ جمله + حذف قطعه‌های انگلیسی */
private fun cleanPassages(raw: List<Passage>): List<Passage> {
    val bySrc = raw.filter { !isLatinHeavy(it.text) }.groupBy { it.src }
    val out = ArrayList<Passage>()
    for ((src, list) in bySrc) {
        // چسباندن پیوستهٔ قطعه‌های خام به ترتیب، بعد اصلاح فاصله‌های نشانه‌گذاری
        val full = list.sortedBy { it.n }.joinToString(" ") {
            it.text.replace("\n", " ").trim()
        }
            .replace(" .", ".")
            .replace(" ،", "،")
            .replace(" ؛", "؛")
            .replace(" ؟", "؟")
            .replace(" !", "!")
            .replace(" :", ":")
        var buf = StringBuilder()
        var idx = 0
        fun flush() {
            val t = buf.toString().trim()
            if (t.isNotEmpty()) {
                idx++
                out.add(Passage("clean-${src}-$idx", src, idx, t))
            }
            buf = StringBuilder()
        }
        for (s in splitSentences(full)) {
            if (buf.isNotEmpty() && buf.length + s.length + 1 > CLEAN_TARGET) flush()
            if (buf.isNotEmpty()) buf.append(' ')
            buf.append(s)
        }
        flush()
        // تکهٔ کوتاه آخر به قطعهٔ قبلی همان منبع می‌چسبد تا هیچ قطعهٔ لرزانی نماند
        if (out.isNotEmpty() && out.last().src == src) {
            val lastIdx = out.lastIndex
            val last = out[lastIdx]
            if (last.text.length < CLEAN_MIN && lastIdx > 0 && out[lastIdx - 1].src == src) {
                out[lastIdx - 1] = out[lastIdx - 1].copy(text = out[lastIdx - 1].text + " " + last.text)
                out.removeAt(lastIdx)
            }
        }
    }
    return out
}

/* صدای مرد/زن فقط با فایل‌های ضبط‌شدهٔ خود برنامه (res/raw) پخش می‌شود؛
   موتور گفتار گوشی هرگز صدای انتخابی را عوض نمی‌کند — همان رفتار نسخه‌های سالم */
private val SRC_FALLBACK_AUDIO: Map<String, String> = mapOf(
    "چهارده مقاله" to "lib_01",
    "کنگره ۶۰ چیست؟" to "lib_02",
    "راهنمای تازه‌واردین" to "lib_03",
    "راهنما و راهنمایی" to "lib_03",
    "چهارده وادی عشق" to "lib_04",
    "جهان‌بینی ۱" to "lib_05",
    "جهان‌بینی ۲" to "lib_05",
    "پروتکل DST" to "dst",
    "اصلاحیه پروتکل DST" to "dst",
    "شرح وظایف" to "vazayef",
    "قوانین کنگره ۶۰" to "ghavanin",
    "کتاب ۶۰ درجه" to "shast",
    "بهترین راه" to "behtarin",
    "مثلث‌های آموزشی" to "mosalas",
    "مصرف‌کننده کیست؟" to "masraf"
)

/* ================================ کتابخانهٔ صوتی ================================ */

/** وضعیت پنجرهٔ «خوانش متن» — با کلیک روی آیکون کتاب (📖) هر کارت */
private data class ReaderState(
    val id: String,
    val title: String,
    val sub: String?,
    val text: String,
    val triangles: Boolean,
    val color: Color,
    val audioId: String? = null,
    val fallbackAudioId: String? = null
)

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hamrah_lib", Context.MODE_PRIVATE) }

    // — محتوای چرخشی: پیام امروز + بانک پاساژهای کل کتاب‌ها + متن جاری هر کارت —
    var feat by remember { mutableStateOf(FEATURED.first()) }
    var passages by remember { mutableStateOf<List<Passage>>(emptyList()) }
    var cardTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val lastPicks = remember { mutableMapOf<String, List<String>>() }

    // — گوینده —
    var voiceFemale by remember { mutableStateOf(prefs.getBoolean(KEY_VOICE_FEMALE, false)) }

    // — پخش‌کنندهٔ فایل صوتی ضبط‌شده (MP3 داخل خود برنامه — صدای تضمینی و بدون نیاز به موتور گفتار) —
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    // — موتور TTS (فارسی) — فقط برای متون بدون فایل ضبط‌شده —
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var loadingId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var spokenText by remember { mutableStateOf("") }
    var spokenSlow by remember { mutableStateOf(false) }

    // — وضعیت‌های رابط —
    var expandedId by remember { mutableStateOf<String?>("feat") } // پیام امروز به‌صورت پیش‌فرض باز است
    var reader by remember { mutableStateOf<ReaderState?>(null) }

    // — به‌روزرسانی ابری گیت‌هاب: کاملاً بی‌صدا و بی‌واسطه —
    //   هیچ پیام، خط وضعیت یا پنجرهٔ آدرس نمایش داده نمی‌شود؛
    //   اگر اینترنت/منبع در دسترس نبود، همین بانک محلی پاک‌سازی‌شده کار می‌کند.
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // — دکمهٔ «بروزرسانی دیتاها»: چرخ‌دندهٔ کوچک روی خود دکمه + یک خط نتیجه که خودش محو می‌شود —
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudMsg by remember { mutableStateOf<String?>(null) }

    fun stopSpeak() {
        player?.let { p ->
            try { p.stop() } catch (_: Exception) {}
            try { p.release() } catch (_: Exception) {}
        }
        player = null
        try { tts?.stop() } catch (_: Exception) {}
        playingId = null
        loadingId = null
        progress = 0f
    }

    fun speak(id: String, text: String, slow: Boolean = false, audioId: String = id, fallbackAudioId: String? = null) {
        if (playingId == id) {
            stopSpeak()
            return
        }
        stopSpeak()

        /* — اول: فایل صوتی ضبط‌شده (MP3 داخل خود برنامه) —
           صدای واقعی مرد/زن که در res/raw بسته شده و روی هر گوشی کار می‌کند.
           اگر برای همین متن فایل جدا نداشتیم (قطعهٔ چرخشی)، فایل ضبط‌شدهٔ خودِ کارت/کتاب
           پخش می‌شود — دقیقاً مثل نسخه‌های قبلی که همیشه صدا داشتند. */
        fun rawIdOf(rawName: String?): Int =
            if (rawName.isNullOrBlank()) 0
            else context.resources.getIdentifier(
                if (voiceFemale) "${rawName}f" else rawName,
                "raw",
                context.packageName
            )
        val resId = rawIdOf(audioId.replace("-", "_").lowercase())
            .takeIf { it != 0 }
            ?: rawIdOf(fallbackAudioId?.replace("-", "_")?.lowercase())
        if (resId != 0) {
            loadingId = id
            try {
                val mp = MediaPlayer.create(context, resId)
                if (mp != null) {
                    player = mp
                    spokenText = text
                    spokenSlow = slow
                    mp.setOnCompletionListener { done ->
                        mainHandler.post {
                            if (player == done) player = null
                            try { done.release() } catch (_: Exception) {}
                            if (playingId == id) {
                                playingId = null
                                loadingId = null
                                progress = 0f
                            }
                        }
                    }
                    mp.start()
                    playingId = id
                    loadingId = null
                    return
                }
            } catch (_: Exception) {
                loadingId = null
            }
        }

        /* — دوم: موتور گفتار گوشی — فقط برای متون بدون فایل ضبط‌شده؛
           بدون هیچ پیام، توست یا پنجره‌ای: اگر موتور نبود، ساکت رد می‌شود */
        val engine = tts ?: return
        spokenText = text
        spokenSlow = slow
        loadingId = id
        try {
            engine.setSpeechRate(if (slow) 0.85f else 1.0f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: Exception) {
            loadingId = null
        }
    }

    fun pickVoice(female: Boolean) {
        if (voiceFemale == female) return
        stopSpeak()
        voiceFemale = female
        prefs.edit().putBoolean(KEY_VOICE_FEMALE, female).apply()
    }

    // — چرخش هوشمند: پیام بالای کتابخانه از کل بانک (آموزه‌ها + قطعه‌های همهٔ کتاب‌ها با برچسب منبع)
    //   و متن هر کارت کتاب از بانک همان کتاب — تصادفی و بدون ترتیب مشخص، با تاریخچهٔ ضدتکرار ۱۲تایی.
    //   دکمهٔ «پیام دیگر» حذف شد؛ چرخش کاملاً خودکار است. —
    fun rotateAll() {
        stopSpeak()
        val passageFeat = passages.map {
            FeaturedItem(it.id, it.src, "منبع: ${it.src} — قطعهٔ ${it.n}", it.text)
        }
        val fromAmoozeh = FEATURED.isNotEmpty() && Random.nextInt(100) < 40
        val pool = when {
            fromAmoozeh -> FEATURED
            passageFeat.isNotEmpty() -> passageFeat
            else -> FEATURED
        }
        val f = pickRotating(pool, readHist(prefs, KEY_FEAT_HIST) + feat.id) { it.id }
        saveHist(prefs, KEY_FEAT_HIST, f.id, max = 12)
        feat = f
        // — متن تازهٔ هر کارت کتاب: معرفی تمیز کتاب (همیشه خوانا) + قطعهٔ چرخشی از متن اصلی همان کتاب —
        val next = mutableMapOf<String, String>()
        for (item in LIB_ITEMS) {
            val srcs = LIB_SRC[item.id]
            val cand = if (srcs == null) emptyList() else passages.filter { p -> p.src in srcs }
            if (cand.isEmpty()) {
                next[item.id] = item.text
                continue
            }
            val recent = lastPicks[item.id] ?: emptyList()
            val fresh = cand.filter { p -> p.id !in recent }
            val from = fresh.ifEmpty { cand }
            val pick = from[Random.nextInt(from.size)]
            lastPicks[item.id] = (listOf(pick.id) + recent).take(3)
            next[item.id] = "${item.text}\n\n— قطعه‌ای از متن اصلی «${item.title}»:\n${pick.text}"
        }
        cardTexts = next
    }

    // — انگشت‌نگاری بانک برای تشخیص تغییر واقعی (id + متن هر قطعه) —
    fun bankFingerprint(list: List<Passage>): Int =
        list.sortedBy { it.id }.joinToString("|") { "${it.id}:${it.text.hashCode()}" }.hashCode()

    // — بانک ابری کامل: کش ابری با همان قانون متن‌های داخلی پاک‌سازی می‌شود (برش از سرِ جمله) —
    fun cleanedCloudBank(): List<Passage> =
        cleanPassages(RemoteContent.cachedPassages(context).map { Passage(it.id, it.src, it.n, it.text) })

    // — اعمال بانک ابری: جایگزینی کامل بانک + چرخش تازهٔ پیام امروز و همهٔ ۱۲ کارت —
    fun applyCloudBank(): Int {
        val fresh = cleanedCloudBank()
        if (fresh.isEmpty()) return 0
        passages = fresh
        rotateAll()
        return fresh.size
    }

    // — دکمهٔ «بروزرسانی دیتاها»: همیشه دریافت واقعی از گیت‌هاب؛ بدون هیچ پنجره و آدرسی —
    fun startCloudRefresh() {
        if (cloudBusy) return
        cloudBusy = true
        RemoteContent.fetchAndApply(context) { ok, _, errMsg ->
            cloudBusy = false
            cloudMsg = if (ok) {
                val n = applyCloudBank()
                if (n > 0) "دریافت شد — $n قطعه از همهٔ منابع — همهٔ کارت‌ها تازه شد"
                else "فایل ابری خالی بود — بانک محلی ماند"
            } else "دریافت نشد — ${errMsg ?: "خطای نامشخص"}"
            mainHandler.postDelayed({ cloudMsg = null }, 4500)
        }
    }

    // — با هر ورود به کتابخانه: بانک پاساژها خوانده و چرخش تازه انجام می‌شود —
    //   دریافت ابری بی‌صدا حداکثر هر ۱۰ دقیقه یک‌بار؛ فقط اگر بانک ابری واقعاً تغییر کرده باشد اعمال می‌شود
    LaunchedEffect(Unit) {
        if (passages.isEmpty()) passages = cleanPassages(loadPassages(context))
        rotateAll()
        if (RemoteContent.isConfigured(context) &&
            System.currentTimeMillis() - RemoteContent.lastFetchTime(context) > 10 * 60 * 1000L
        ) {
            RemoteContent.fetchAndApply(context) { ok, _, _ ->
                if (ok) {
                    val fresh = cleanedCloudBank()
                    if (fresh.isNotEmpty() && bankFingerprint(fresh) != bankFingerprint(passages)) {
                        passages = fresh
                        rotateAll()
                    }
                }
            }
        }
    }

    // — با بازگشت به برنامه (از پس‌زمینه) هم چرخش تازه انجام می‌شود —
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) rotateAll()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // — ساخت موتور TTS —
    DisposableEffect(Unit) {
        var engineRef: TextToSpeech? = null
        val engine = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    engineRef?.let { eng ->
                        try {
                            eng.setLanguage(Locale("fa", "IR"))
                            eng.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    mainHandler.post {
                                        if (utteranceId != null) {
                                            playingId = utteranceId
                                            loadingId = null
                                        }
                                    }
                                }

                                override fun onDone(utteranceId: String?) {
                                    mainHandler.post {
                                        if (utteranceId != null && playingId == utteranceId) {
                                            playingId = null
                                            loadingId = null
                                            progress = 0f
                                        }
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    mainHandler.post {
                                        if (utteranceId != null && playingId == utteranceId) {
                                            playingId = null
                                            loadingId = null
                                            progress = 0f
                                        }
                                    }
                                }
                            })
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        engineRef = engine
        tts = engine
        onDispose {
            engineRef = null
            try {
                engine.stop()
                engine.shutdown()
            } catch (_: Exception) {}
            player?.let { p ->
                try { p.stop() } catch (_: Exception) {}
                try { p.release() } catch (_: Exception) {}
            }
            player = null
            tts = null
            playingId = null
            loadingId = null
        }
    }

    // — نوار پیشرفت خوانش: برای فایل ضبط‌شده از خود فایل؛ برای موتور گفتار تخمینی از طول متن —
    LaunchedEffect(playingId) {
        if (playingId == null) {
            progress = 0f
        } else {
            val p = player
            if (p != null) {
                val dur = (if (p.duration > 0) p.duration else 1).toFloat()
                while (playingId != null && isActive) {
                    try {
                        progress = (p.currentPosition.toFloat() / dur).coerceIn(0f, 0.98f)
                    } catch (_: Exception) {
                        break
                    }
                    delay(200)
                }
            } else {
                val estimateMs = ((spokenText.length * 70L) * (if (spokenSlow) 1.2 else 1.0)).toLong()
                    .coerceAtLeast(2000L)
                val start = System.currentTimeMillis()
                progress = 0f
                while (playingId != null && isActive) {
                    val fraction = (System.currentTimeMillis() - start).toFloat() / estimateMs.toFloat()
                    progress = fraction.coerceIn(0f, 0.98f)
                    if (fraction >= 1f) break
                    delay(120)
                }
            }
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                ScreenHeader(
                    title = "کتابخانه",
                    subtitle = "متن و خوانش کتاب‌ها و متون کنگرهٔ ۶۰ — با صدای مرد و زن",
                    accent = NavAmber
                )
                // دکمهٔ «بروزرسانی دیتاها» — گوشهٔ مقابل عنوان
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, NavAmber),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !cloudBusy) { startCloudRefresh() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (cloudBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 1.8.dp,
                                color = NavAmber
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "در حال دریافت…",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavAmber
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "بروزرسانی دیتاها",
                                tint = NavAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "بروزرسانی دیتاها",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavAmber
                            )
                        }
                    }
                }
            }
            // خط نتیجهٔ دریافت — خودش بعد از چند ثانیه محو می‌شود؛ بدون پنجره و بدون آدرس
            AnimatedVisibility(
                visible = cloudMsg != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = cloudMsg.orEmpty(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // پیام امروز — محتوای تأثیرگذار چرخشی (کادر قرمز عکس ۱)
        item {
            LibCard(
                title = feat.title,
                sub = feat.sub,
                text = feat.text,
                color = NavAmber,
                open = expandedId == "feat",
                playing = playingId == "feat",
                loading = loadingId == "feat",
                progress = progress,
                badge = "صوت = خوانش همین متن",
                onToggle = { expandedId = if (expandedId == "feat") null else "feat" },
                onPlay = { speak("feat", feat.text, audioId = feat.id, fallbackAudioId = SRC_FALLBACK_AUDIO[feat.title]) },
                onRead = {
                    reader = ReaderState("feat", feat.title, feat.sub, feat.text, false, NavAmber, feat.id, SRC_FALLBACK_AUDIO[feat.title])
                }
            )
        }


        // انتخاب گوینده — روی همهٔ خوانش‌ها اعمال می‌شود و به‌خاطر سپرده می‌شود
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "گویندهٔ مرد", true to "گویندهٔ زن").forEach { (female, label) ->
                    val on = voiceFemale == female
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (on) NavAmber else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (on) NavAmber else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pickVoice(female) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = if (on) Color.White else NavViolet,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // کارت‌های کتابخانه — چیدمان عکس ۶: کتاب سمت راست، پخش سمت چپ، شرون پایین
        items(LIB_ITEMS.size) { index ->
            val item = LIB_ITEMS[index]
            // متن چرخشی کارت — با هر ورود یک قطعهٔ تازه از همان کتاب؛ اگر بانک نداشت، متن ثابت
            val txt = cardTexts[item.id] ?: item.text
            val audioId = if (txt == item.text) item.id else "${item.id}__pas"
            LibCard(
                title = item.title,
                sub = item.sub,
                text = txt,
                color = item.color,
                open = expandedId == item.id,
                playing = playingId == item.id,
                loading = loadingId == item.id,
                progress = progress,
                badge = "صوت = خوانش همین متن",
                triangles = item.id == "mosalas",
                onToggle = { expandedId = if (expandedId == item.id) null else item.id },
                onPlay = { speak(item.id, txt, audioId = audioId, fallbackAudioId = item.id) },
                onRead = {
                    reader = ReaderState(
                        item.id,
                        item.title,
                        item.sub,
                        txt,
                        item.id == "mosalas",
                        item.color,
                        audioId,
                        item.id
                    )
                }
            )
        }

        item {
            NoticeCard(
                text = "مطالب این کتابخانه جنبه یادآوری و آموزش دارد؛ برای شرح کامل به نسخه اصلی کتاب‌ها مراجعه کن."
            )
        }
    }

    // — پنجرهٔ «خوانش متن» —
    reader?.let { r ->
        Dialog(
            onDismissRequest = { reader = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val config = LocalConfiguration.current
            val maxHeight = config.screenHeightDp.dp * 0.85f
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = maxHeight)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // سربرگ: عنوان + دکمهٔ بستن
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = r.color
                            )
                            if (r.sub != null) {
                                Text(
                                    text = r.sub,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { reader = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "بستن پنجره",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (r.triangles) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            TrianglesSection(color = r.color)
                        }
                    } else {
                        Text(
                            text = r.text,
                            fontSize = 12.5.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }

                    // دکمهٔ پخش تمام‌عرض — رنگ کارت
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = r.color,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable { speak(r.id, r.text, audioId = r.audioId ?: r.id, fallbackAudioId = r.fallbackAudioId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (playingId == r.id) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    loadingId == r.id -> "در حال آماده‌سازی…"
                                    playingId == r.id -> "توقف پخش"
                                    else -> "پخش خوانش متن"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

}

/**
 * کارت کتابخانه — دقیقاً مطابق عکس ۱ و ۲:
 *  • 📖 خوانش متن — فرزند اول ردیف = سمت راست (RTL)؛ پنجرهٔ کامل متن را باز می‌کند
 *  • ستون وسط: عنوان + زیرعنوان وسط‌چین، «متن + صوت» و نشانِ رنگی
 *  • ▶ پخش/توقف — فرزند آخر ردیف = سمت چپ (RTL)
 *  • شرون ▼ وسطِ پایین کارت برای باز/بسته کردن متن (توجیه‌شده)
 *  • نوار پیشرفت هنگام خوانش
 */
@Composable
private fun LibCard(
    title: String,
    sub: String,
    text: String,
    color: Color,
    open: Boolean,
    playing: Boolean,
    loading: Boolean,
    progress: Float,
    badge: String,
    triangles: Boolean = false,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    onRead: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 📖 — فرزند اول = سمت راست (RTL)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f))
                        .clickable(onClick = onRead),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = "خوانش متن در پنجره",
                        tint = color,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sub,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "متن + صوت",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    // نشان «صوت = خوانش همین متن» — از سمت راستِ ردیف
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = badge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = color
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // ▶ پخش — فرزند آخر = سمت چپ (RTL)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (playing) color else color.copy(alpha = 0.12f))
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = when {
                            loading -> "در حال آماده‌سازی"
                            playing -> "توقف پخش"
                            else -> "پخش خوانش"
                        },
                        tint = if (playing) Color.White else color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // نوار پیشرفت هنگام خوانش
            if (playing) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }

            // شرون ▼ — پایین و وسط کارت؛ با چرخش نرم باز/بسته می‌شود
            val chevronRotation by animateFloatAsState(
                targetValue = if (open) 180f else 0f,
                label = "chevronRotation"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (open) "بستن متن" else "نمایش متن",
                    tint = color,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                        .clickable(onClick = onToggle)
                )
            }

            // متن باز‌شده — همسطح و توجیه‌شده (یا مثلث‌های آموزشی)
            AnimatedVisibility(visible = open) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (triangles) {
                        TrianglesSection(color = color)
                    } else {
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            trailing?.invoke()
        }
    }
}

/** چیپ ضلع مثلث — کپسول رنگی ملایم روی هر رأس */
@Composable
private fun TriChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        modifier = modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

/** رنگ مشکی مثلث‌های آموزشی — بنا به درخواست کاربر (قبلاً صورتی/سرخابی بود) */
private val TriangleInk = Color(0xFF111111)

/**
 * رسم مثلث — هر ضلع روی رأس خودش (بالا، پایین‌راست، پایین‌چپ).
 * داخل یک ناحیهٔ LTR رسم می‌شود تا مختصات راست/چپ مستقل از RTL کل صفحه ثابت بماند.
 */
@Composable
private fun TriangleFigure(tri: TriDef, color: Color) {
    val ink = TriangleInk
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val apex = Offset(x = w / 2f, y = 50.dp.toPx())
                val rightBase = Offset(x = w - 22.dp.toPx(), y = 160.dp.toPx())
                val leftBase = Offset(x = 22.dp.toPx(), y = 160.dp.toPx())
                val triangle = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(rightBase.x, rightBase.y)
                    lineTo(leftBase.x, leftBase.y)
                    close()
                }
                // پرش ملایم + خط دور مثلث — با رنگ مشکی ثابت
                drawPath(path = triangle, color = ink.copy(alpha = 0.05f))
                drawPath(
                    path = triangle,
                    color = ink,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // سه دایرهٔ رأس‌ها
                drawCircle(color = ink, radius = 4.5.dp.toPx(), center = apex)
                drawCircle(color = ink, radius = 4.5.dp.toPx(), center = rightBase)
                drawCircle(color = ink, radius = 4.5.dp.toPx(), center = leftBase)
            }
            // ضلع اول — بالای رأس
            TriChip(
                text = tri.sides[0],
                color = ink,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // ضلع دوم — پایینِ راست (در LTR: BottomEnd)
            TriChip(
                text = tri.sides[1],
                color = ink,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            // ضلع سوم — پایینِ چپ (در LTR: BottomStart)
            TriChip(
                text = tri.sides[2],
                color = ink,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

/** بخش مثلث‌ها — مثلث واقعی رسم می‌شود، نه متن پشت سر هم — رنگ مشکی ثابت */
@Composable
private fun TrianglesSection(color: Color) {
    val ink = TriangleInk
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "هر مثلث سه ضلع دارد؛ اگر یک ضلع ضعیف شود، تعادل کل مثلث به هم می‌ریزد. " +
                "هر ضلع روی رأس خودش نوشته شده است. (هر ۵۸ مثلث جزوهٔ «مثلث‌های آموزشی کنگره ۶۰»)",
            fontSize = 11.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
        // ارتفاع ثابت با اسکرول — مثل max-h-96 پیش‌نمایش وب
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TRIANGLES.size) { index ->
                val t = TRIANGLES[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ink.copy(alpha = 0.33f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${formatNumber(t.n)}. ${t.title}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                    TriangleFigure(tri = t, color = ink)
                }
            }
        }
    }
}
