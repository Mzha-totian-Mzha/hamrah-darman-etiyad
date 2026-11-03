package ir.mosafir.recovery.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ir.mosafir.recovery.data.formatNumber
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
        "توصیف حال امروز ما — ما که امروز در این جمع نشسته‌ایم، روزی با مواد مخدر زندگی می‌کردیم؛ کانال‌های عصبی بدن ما تحت تأثیر مواد قرار گرفته بود و تفکر ما درست عمل نمی‌کرد. با ورود به کنگره ۶۰ و پذیرش بیماری، راه درمان را آغاز کردیم. امروز می‌توانیم با تکیه بر تجربهٔ همسفران، زندگی سالم و آرام بسازیم؛ ما می‌توانیم."
    ),
    LibItem(
        "lib-02", "کنگره ۶۰ چیست؟", "معرفی • سابقه و اهداف کنگرهٔ ۶۰ •", NavTeal,
        "کنگره ۶۰ محفلی است برای آموزش و درمان اعتیاد. این مجموعه توسط مهندس حسین دژاکام بنیان گذاشته شد و درمان در آن بر پایهٔ متد DST یعنی تخفیف تدریجی مصرف انجام می‌شود. در کنگره ۶۰، اعتیاد یک بیماری در نظر گرفته می‌شود که با نظم، محبت و پیگیری درمان، قابل کنترل است. هدف، ساختن مسافری است که با رهایی از مواد، به ارزشمندی و آرامش برسد."
    ),
    LibItem(
        "lib-03", "راهنمای تازه‌واردان", "راهنما • نخستین روزهای سفر •", HeroPink,
        "راهنمای تازه‌واردان؛ نخستین روزهای سفر. تازه‌واردی که وارد کنگره می‌شود، اول باید بداند که اعتیاد بیماری است و درمان دارد. در روزهای نخست، نظم در خواب و بیداری، ورزش صبحگاهی، حضور در جلسات و شروع پروتکل درمان زیر نظر راهنما، پایه‌های سفر را می‌سازد. هیچ‌گاه مقدار و ساعت مصرف را بدون هماهنگی با راهنما تغییر نده. سفر اول از قطع مصرف می‌گذرد و سفر دوم، رسیدن به خویشتن است."
    ),
    LibItem(
        "lib-04", "چهارده وادی عشق", "کتاب • ۱۴ وادی • متن و صوت •", NavViolet,
        "کتاب عشق؛ چهارده وادی برای رسیدن به خود. سفر دوم مسافر، بعد از قطع مواد آغاز می‌شود و از چهارده وادی می‌گذرد. هر وادی، ایستگاهی برای رشد و تعادل است؛ از وادی آغاز و تفکر، تا عشق‌ورزی، صلح و آرامش و در پایان خدمت به دیگران. درمان اعتیاد فقط قطع مصرف نیست؛ بازسازی شخصیت و یافتن معنا در زندگی است."
    ),
    LibItem(
        "lib-05", "جهان‌بینی", "جهان‌بینی ۱ و ۲ • تفکر درست، زندگی درست •", NavTeal,
        "اگر تفکرِ ما این بگوئیم که «مشکل من حل نمی‌شود»، همین را ساخته‌ایم و حل نخواهد شد؛ اگر تفکرِ ما این بگوئیم که «بیماری من خوب نمی‌شود»، خوب نخواهد شد. باید در وجودمان امید و تصویر مثبت بسازیم. جهان‌بینی ما، راهنمای انتخاب‌های ماست؛ با تفکر درست، زندگی درست ساخته می‌شود."
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
        "حرمت‌های کنگره ۶۰: احترام به رأی اکثریت لژیون، نظم در حضور و غیبت، عدم قضاوت دربارهٔ همسفران، پرهیز از سیاست‌بازی و حفظ اسرار افراد داخل مجموعه. قوانین، پشتوانهٔ آرامش و امنیت جمع است و رعایت آن‌ها شرط ادامهٔ سفر در لژیون است."
    ),
    LibItem(
        "shast", "۶۰ درجه", "کتاب ۶۰ درجه.", NavViolet,
        "کتاب «۶۰ درجه» مجموعه سخنان و آموزش‌های مهندس دژاکام است؛ زاویهٔ نگاه ما به مسائل زندگی را تغییر می‌دهد. همان ماجرایی که با یک نگاهِ ۶۰ درجه‌ای دیده می‌شود، معنای تازه‌ای پیدا می‌کند؛ تفکر، تصمیم و اجرا — مثلث آموزشی کنگره ۶۰ — کلید تغییر نگاه است."
    ),
    LibItem(
        "behtarin", "بهترین راه", "سخنان آموزشی کنگرهٔ ۶۰.", NavTeal,
        "بهترین راه برای عبور از اعتیاد، راهِ میانه و اصولی است: نه خشونت و نه تسلیم؛ بلکه شناخت، نظم، پیگیری درمان زیر نظر راهنما و حضور پیوسته در جلسات. تجربه نشان داده کسانی که با نظم و صبر پیش می‌روند، به آرامش پایدار می‌رسند."
    ),
    LibItem(
        "mosalas", "مثلث‌های آموزشی", "سه‌گانه‌های آموزشی درمان.", HeroPink,
        "مثلث‌های آموزشی کنگره ۶۰، سه‌گانه‌های پایهٔ درمان‌اند: تفکر، تصمیم، اجرا — شناخت، نظم، پیگیری — و آموزش، محبت، کاریابی. هر مثلث، سه ضلع دارد که اگر یکی ضعیف شود، تعادل کل مثلث به هم می‌ریزد؛ مسیر درمان، تمرین مداومِ حفظ تعادل این مثلث‌هاست."
    ),
    LibItem(
        "masraf", "مصرف‌کننده کیست؟", "شناخت بیماری اعتیاد.", HeroOrange,
        "مصرف‌کننده کسی است که در به‌کارگیری مواد مخدر، اختیار خود را از دست داده است؛ جسم به ماده وابسته شده و تفکر نیز در خدمت ادامهٔ مصرف قرار می‌گیرد. شناخت این وضعیت، اولین قدم درمان است؛ اعتیاد بیماری است، نه لذت‌جویی — و بیماری، درمان دارد."
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
private const val KEY_VOICE_FEMALE = "voice_female"

/** تاریخچهٔ موارد نمایش‌داده‌شده — تا هر بار محتوای تازه ببینیم */
private fun readHist(prefs: SharedPreferences, key: String): List<String> =
    prefs.getString(key, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

private fun saveHist(prefs: SharedPreferences, key: String, id: String) {
    val h = (listOf(id) + readHist(prefs, key).filter { it != id }).take(MAX_HIST)
    prefs.edit().putString(key, h.joinToString(",")).apply()
}

/** انتخاب بدون تکرار: از میان مواردی که اخیراً دیده نشده‌اند */
private fun <T> pickRotating(pool: List<T>, recentIds: List<String>, idOf: (T) -> String): T {
    val fresh = pool.filter { idOf(it) !in recentIds }
    val cand = fresh.ifEmpty { pool }
    return cand[Random.nextInt(cand.size)]
}

/** انتخاب صدای فارسی مرد/زن — اگر موتور TTS صدای مناسب نداشت، پیش‌فرض می‌ماند (بی‌صدا) */
private fun selectVoice(engine: TextToSpeech, female: Boolean) {
    try {
        val voices = engine.voices ?: return
        val faVoices = voices.filter { v ->
            v.locale?.language?.startsWith("fa") == true || v.name.contains("fa", ignoreCase = true)
        }
        if (faVoices.isEmpty()) return
        val chosen = if (female) {
            faVoices.firstOrNull { it.name.contains("Dilara", ignoreCase = true) || it.name.contains("female", ignoreCase = true) }
        } else {
            faVoices.firstOrNull { it.name.contains("Farid", ignoreCase = true) || (it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true)) }
        } ?: return
        engine.setVoice(chosen)
    } catch (_: Exception) {
        // بی‌صدا — اگر انتخاب صدا ممکن نشد، صدای پیش‌فرض استفاده می‌شود
    }
}

/* ================================ کتابخانهٔ صوتی ================================ */

/** وضعیت پنجرهٔ «خوانش متن» — با کلیک روی آیکون کتاب (📖) هر کارت */
private data class ReaderState(
    val id: String,
    val title: String,
    val sub: String?,
    val text: String,
    val triangles: Boolean,
    val color: Color
)

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hamrah_lib", Context.MODE_PRIVATE) }

    // — محتوای چرخشی: پیام امروز —
    var feat by remember { mutableStateOf(FEATURED.first()) }

    // — گوینده —
    var voiceFemale by remember { mutableStateOf(prefs.getBoolean(KEY_VOICE_FEMALE, false)) }

    // — پخش‌کنندهٔ فایل صوتی ضبط‌شده (MP3 داخل خود برنامه — صدای تضمینی و بدون نیاز به موتور گفتار) —
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    // — موتور TTS (فارسی) — فقط برای متون بدون فایل ضبط‌شده —
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var loadingId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var spokenText by remember { mutableStateOf("") }
    var spokenSlow by remember { mutableStateOf(false) }
    var ttsAsked by remember { mutableStateOf(false) }
    var ttsFailed by remember { mutableStateOf(false) }
    var showTtsHelp by remember { mutableStateOf(false) }

    // — وضعیت‌های رابط —
    var expandedId by remember { mutableStateOf<String?>("feat") } // پیام امروز به‌صورت پیش‌فرض باز است
    var reader by remember { mutableStateOf<ReaderState?>(null) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

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

    fun speak(id: String, text: String, slow: Boolean = false) {
        if (playingId == id) {
            stopSpeak()
            return
        }
        stopSpeak()

        /* — اول: فایل صوتی ضبط‌شده (MP3 داخل خود برنامه) —
           صدای واقعی مرد/زن که در res/raw بسته شده و روی هر گوشی کار می‌کند؛
           موتور گفتار گوشی فقط برای متون بدون فایل ضبط‌شده استفاده می‌شود. */
        val base = id.replace("-", "_").lowercase()
        val resId = context.resources.getIdentifier(
            if (voiceFemale) "${base}f" else base,
            "raw",
            context.packageName
        )
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

        /* — دوم: موتور گفتار گوشی (برای متون بدون فایل ضبط‌شده) — */
        if (!ttsAsked) {
            Toast.makeText(context, "موتور گفتار دارد آماده می‌شود؛ چند لحظهٔ دیگر دوباره بزن", Toast.LENGTH_SHORT).show()
            return
        }
        val engine = tts
        if (engine == null || ttsFailed) {
            showTtsHelp = true // نه فایل ضبط‌شده داریم و نه موتور گفتار — راهنمای نصب باز می‌شود
            return
        }
        spokenText = text
        spokenSlow = slow
        loadingId = id
        try {
            engine.setSpeechRate(if (slow) 0.85f else 1.0f)
            selectVoice(engine, voiceFemale)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: Exception) {
            loadingId = null
            showTtsHelp = true
        }
    }

    fun pickVoice(female: Boolean) {
        if (voiceFemale == female) return
        stopSpeak()
        voiceFemale = female
        prefs.edit().putBoolean(KEY_VOICE_FEMALE, female).apply()
        tts?.let { selectVoice(it, female) }
    }

    fun rotateFeat() {
        stopSpeak()
        val f = pickRotating(FEATURED, readHist(prefs, KEY_FEAT_HIST) + feat.id) { it.id }
        saveHist(prefs, KEY_FEAT_HIST, f.id, max = 12)
        feat = f
    }

    // — چرخش هوشمند: با هر ورود به کتابخانه (باز کردن برنامه یا جابجایی بین تب‌ها) —
    // محتوای تازه انتخاب می‌شود؛ تاریخچهٔ SharedPreferences مانع تکرار موارد اخیر می‌شود.
    LaunchedEffect(Unit) {
        val f = pickRotating(FEATURED, readHist(prefs, KEY_FEAT_HIST)) { it.id }
        saveHist(prefs, KEY_FEAT_HIST, f.id, max = 12)
        feat = f
    }

    // — ساخت موتور TTS —
    DisposableEffect(Unit) {
        var engineRef: TextToSpeech? = null
        val engine = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                ttsAsked = true
                if (status == TextToSpeech.SUCCESS) {
                    ttsFailed = false
                    ttsReady = true
                    engineRef?.let { eng ->
                        try {
                            eng.setLanguage(Locale("fa", "IR"))
                            selectVoice(eng, voiceFemale)
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
                                            showTtsHelp = true // موتور نتوانست متن را بخواند — راهنمای نصب باز می‌شود
                                        }
                                    }
                                }
                            })
                        } catch (_: Exception) {
                        }
                    }
                    ttsReady = true
                } else {
                    ttsFailed = true
                }
                // اگر راه‌اندازی ناموفق بود، با زدن دکمهٔ پخش راهنمای نصب باز می‌شود
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
            ScreenHeader(
                title = "کتابخانه",
                subtitle = "متن و خوانش کتاب‌ها و متون کنگرهٔ ۶۰ — با صدای مرد و زن",
                accent = NavAmber
            )
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
                onPlay = { speak("feat", feat.text) },
                onRead = {
                    reader = ReaderState("feat", feat.title, feat.sub, feat.text, false, NavAmber)
                },
                trailing = {
                    RotatePill(
                        label = "پیام دیگر",
                        color = NavAmber,
                        onClick = { rotateFeat() }
                    )
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

        // راهنمای صدا — وقتی موتور گفتار فارسی روی گوشی نصب نیست، کاربر راهنمایی می‌شود
        item {
            Text(
                text = "اگر صدا پخش نشد، یعنی موتور گفتار فارسی (Google Speech Services) روی گوشی نصب نیست؛ دکمهٔ پخش را بزن تا راهنمای نصب باز شود.",
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // کارت‌های کتابخانه — چیدمان عکس ۶: کتاب سمت راست، پخش سمت چپ، شرون پایین
        items(LIB_ITEMS.size) { index ->
            val item = LIB_ITEMS[index]
            LibCard(
                title = item.title,
                sub = item.sub,
                text = item.text,
                color = item.color,
                open = expandedId == item.id,
                playing = playingId == item.id,
                loading = loadingId == item.id,
                progress = progress,
                badge = "صوت = خوانش همین متن",
                triangles = item.id == "mosalas",
                onToggle = { expandedId = if (expandedId == item.id) null else item.id },
                onPlay = { speak(item.id, item.text) },
                onRead = {
                    reader = ReaderState(
                        item.id,
                        item.title,
                        item.sub,
                        item.text,
                        item.id == "mosalas",
                        item.color
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
                            .clickable { speak(r.id, r.text) }
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

    // — راهنمای نصب موتور گفتار فارسی (وقتی صدایی پخش نمی‌شود) —
    if (showTtsHelp) {
        AlertDialog(
            onDismissRequest = { showTtsHelp = false },
            title = { Text("موتور گفتار فارسی یافت نشد") },
            text = {
                Text("برای پخش صدای خوانش، «Google Speech Services» باید روی گوشی نصب و به‌روز باشد. با دکمهٔ پایین نصب یا به‌روزرسانی کن؛ بعد برنامه را ببند و دوباره باز کن.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showTtsHelp = false
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                }) { Text("نصب موتور گفتار") }
            },
            dismissButton = {
                TextButton(onClick = { showTtsHelp = false }) { Text("بستن") }
            }
        )
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

/** دکمهٔ گرد «پیام دیگر / آرام‌سازی دیگر» — چرخش به محتوای تازه */
@Composable
private fun RotatePill(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
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

/**
 * رسم مثلث — هر ضلع روی رأس خودش (بالا، پایین‌راست، پایین‌چپ).
 * داخل یک ناحیهٔ LTR رسم می‌شود تا مختصات راست/چپ مستقل از RTL کل صفحه ثابت بماند.
 */
@Composable
private fun TriangleFigure(tri: TriDef, color: Color) {
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
                // پرش ملایم + خط دور مثلث
                drawPath(path = triangle, color = color.copy(alpha = 0.05f))
                drawPath(
                    path = triangle,
                    color = color,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // سه دایرهٔ رأس‌ها
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = apex)
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = rightBase)
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = leftBase)
            }
            // ضلع اول — بالای رأس
            TriChip(
                text = tri.sides[0],
                color = color,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // ضلع دوم — پایینِ راست (در LTR: BottomEnd)
            TriChip(
                text = tri.sides[1],
                color = color,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            // ضلع سوم — پایینِ چپ (در LTR: BottomStart)
            TriChip(
                text = tri.sides[2],
                color = color,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

/** بخش مثلث‌ها — مثلث واقعی رسم می‌شود، نه متن پشت سر هم */
@Composable
private fun TrianglesSection(color: Color) {
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
                        .border(1.dp, color.copy(alpha = 0.33f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${formatNumber(t.n)}. ${t.title}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                    TriangleFigure(tri = t, color = color)
                }
            }
        }
    }
}
