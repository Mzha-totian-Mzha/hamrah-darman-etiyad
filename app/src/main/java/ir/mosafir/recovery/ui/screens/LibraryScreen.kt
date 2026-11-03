package ir.mosafir.recovery.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mosafir.recovery.ui.NoticeCard
import ir.mosafir.recovery.ui.ScreenHeader
import ir.mosafir.recovery.ui.theme.HeroOrange
import ir.mosafir.recovery.ui.theme.HeroPink
import ir.mosafir.recovery.ui.theme.NavAmber
import ir.mosafir.recovery.ui.theme.NavSlate
import ir.mosafir.recovery.ui.theme.NavTeal
import ir.mosafir.recovery.ui.theme.NavViolet
import kotlinx.coroutines.delay

private data class LibItem(
    val id: String,
    val title: String,
    val sub: String,
    val color: Color,
    val audio: Boolean,
    val text: String
)

/** کتابخانه — دقیقاً مطابق پیش‌نمایش وب (عکس ۶) */
private val LIB_ITEMS = listOf(
    LibItem(
        "lib-01", "چهارده مقاله", "مقالات • مهندس حسین دژاکام •", HeroOrange, true,
        "توصیف حال امروز ما — ما که امروز در این جمع نشسته‌ایم، روزی با مواد مخدر زندگی می‌کردیم؛ کانال‌های عصبی بدن ما تحت تأثیر مواد قرار گرفته بود و تفکر ما درست عمل نمی‌کرد. با ورود به کنگره ۶۰ و پذیرش بیماری، راه درمان را آغاز کردیم. امروز می‌توانیم با تکیه بر تجربهٔ همسفران، زندگی سالم و آرام بسازیم؛ ما می‌توانیم."
    ),
    LibItem(
        "lib-02", "کنگره ۶۰ چیست؟", "معرفی • سابقه و اهداف کنگرهٔ ۶۰ •", NavTeal, true,
        "کنگره ۶۰ محفلی است برای آموزش و درمان اعتیاد. این مجموعه توسط مهندس حسین دژاکام بنیان گذاشته شد و درمان در آن بر پایهٔ متد DST یعنی تخفیف تدریجی مصرف انجام می‌شود. در کنگره ۶۰، اعتیاد یک بیماری در نظر گرفته می‌شود که با نظم، محبت و پیگیری درمان، قابل کنترل است. هدف، ساختن مسافری است که با رهایی از مواد، به ارزشمندی و آرامش برسد."
    ),
    LibItem(
        "lib-03", "راهنمای تازه‌واردان", "راهنما • نخستین روزهای سفر •", HeroPink, true,
        "راهنمای تازه‌واردان؛ نخستین روزهای سفر. تازه‌واردی که وارد کنگره می‌شود، اول باید بداند که اعتیاد بیماری است و درمان دارد. در روزهای نخست، نظم در خواب و بیداری، ورزش صبحگاهی، حضور در جلسات و شروع پروتکل درمان زیر نظر راهنما، پایه‌های سفر را می‌سازد. هیچ‌گاه مقدار و ساعت مصرف را بدون هماهنگی با راهنما تغییر نده. سفر اول از قطع مصرف می‌گذرد و سفر دوم، رسیدن به خویشتن است."
    ),
    LibItem(
        "lib-04", "چهارده وادی عشق", "کتاب • ۱۴ وادی • متن و صوت •", NavViolet, true,
        "کتاب عشق؛ چهارده وادی برای رسیدن به خود. سفر دوم مسافر، بعد از قطع مواد آغاز می‌شود و از چهارده وادی می‌گذرد. هر وادی، ایستگاهی برای رشد و تعادل است؛ از وادی آغاز و تفکر، تا عشق‌ورزی، صلح و آرامش و در پایان خدمت به دیگران. درمان اعتیاد فقط قطع مصرف نیست؛ بازسازی شخصیت و یافتن معنا در زندگی است."
    ),
    LibItem(
        "lib-05", "جهان‌بینی", "جهان‌بینی ۱ و ۲ • تفکر درست، زندگی درست •", NavTeal, true,
        "اگر تفکرِ ما این بگوئیم که «مشکل من حل نمی‌شود»، همین را ساخته‌ایم و حل نخواهد شد؛ اگر تفکرِ ما این بگوئیم که «بیماری من خوب نمی‌شود»، خوب نخواهد شد. باید در وجودمان امید و تصویر مثبت بسازیم. جهان‌بینی ما، راهنمای انتخاب‌های ماست؛ با تفکر درست، زندگی درست ساخته می‌شود."
    ),
    LibItem(
        "dst", "پروتکل DST", "متد درمان اعتیاد با روش DST.", HeroOrange, false,
        "DST مخفف «تخفیف تدریجی مصرف» است: هر ۱ سی‌سی شربت OT معادل ۱۰۰ میلی‌گرم تریاک خوراکی است؛ سه وعده در ساعت‌های ثابت با فاصله ۸ ساعت؛ هر پله ۲۱ روز و کاهش با ضریب ۰٫۸؛ توقف روی سقف مصرف ۲۲ روز؛ و پله آخر ۰٫۳cc در ۲۴ ساعت. جدول کامل پله‌ها در تب «پروتکل» برنامه است."
    ),
    LibItem(
        "vazayef", "شرح وظایف", "وظایف مرزبانان و خدمتگزاران لژیون.", NavSlate, false,
        "لژیون مرزبانان نمایندگی همسفران آقا متشکل از ۵ نفر است: نگهبان، دبیر، استاد جلسه، مرزبان امور مالی و مهماندار. طول مدت خدمت مرزبانی ۶ ماه است و برگزاری انتخابات مرزبانی بر عهده راهنمای لژیون مرزبانی نمایندگی می‌باشد. وظیفهٔ هر یک در متن «شرح وظایف» کنگره ۶۰ آمده است."
    ),
    LibItem(
        "ghavanin", "قوانین کنگره ۶۰", "حرمت‌ها و قوانین مجموعه.", NavAmber, false,
        "حرمت‌های کنگره ۶۰: احترام به رأی اکثریت لژیون، نظم در حضور و غیبت، عدم قضاوت دربارهٔ همسفران، پرهیز از سیاست‌بازی و حفظ اسرار افراد داخل مجموعه. قوانین، پشتوانهٔ آرامش و امنیت جمع است و رعایت آن‌ها شرط ادامهٔ سفر در لژیون است."
    ),
    LibItem(
        "shast", "۶۰ درجه", "کتاب ۶۰ درجه.", NavViolet, false,
        "کتاب «۶۰ درجه» مجموعه سخنان و آموزش‌های مهندس دژاکام است؛ زاویهٔ نگاه ما به مسائل زندگی را تغییر می‌دهد. همان ماجرایی که با یک نگاهِ ۶۰ درجه‌ای دیده می‌شود، معنای تازه‌ای پیدا می‌کند؛ تفکر، تصمیم و اجرا — مثلث آموزشی کنگره ۶۰ — کلید تغییر نگاه است."
    ),
    LibItem(
        "behtarin", "بهترین راه", "سخنان آموزشی کنگرهٔ ۶۰.", NavTeal, false,
        "بهترین راه برای عبور از اعتیاد، راهِ میانه و اصولی است: نه خشونت و نه تسلیم؛ بلکه شناخت، نظم، پیگیری درمان زیر نظر راهنما و حضور پیوسته در جلسات. تجربه نشان داده کسانی که با نظم و صبر پیش می‌روند، به آرامش پایدار می‌رسند."
    ),
    LibItem(
        "mosalas", "مثلث‌های آموزشی", "سه‌گانه‌های آموزشی درمان.", HeroPink, false,
        "مثلث‌های آموزشی کنگره ۶۰، سه‌گانه‌های پایهٔ درمان‌اند: تفکر، تصمیم، اجرا — شناخت، نظم، پیگیری — و آموزش، محبت، کاریابی. هر مثلث، سه ضلع دارد که اگر یکی ضعیف شود، تعادل کل مثلث به هم می‌ریزد؛ مسیر درمان، تمرین مداومِ حفظ تعادل این مثلث‌هاست."
    ),
    LibItem(
        "masraf", "مصرف‌کننده کیست؟", "شناخت بیماری اعتیاد.", HeroOrange, false,
        "مصرف‌کننده کسی است که در به‌کارگیری مواد مخدر، اختیار خود را از دست داده است؛ جسم به ماده وابسته شده و تفکر نیز در خدمت ادامهٔ مصرف قرار می‌گیرد. شناخت این وضعیت، اولین قدم درمان است؛ اعتیاد بیماری است، نه لذت‌جویی — و بیماری، درمان دارد."
    )
)

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    var voiceFemale by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stop() {
        player?.release()
        player = null
        playingId = null
        progress = 0f
    }

    fun play(id: String) {
        stop()
        val base = if (id == "forest") "forest" else id.replace("-", "_")
        val resName = if (voiceFemale) "${base}f" else base
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return
        val mp = MediaPlayer.create(context, resId) ?: return
        mp.setOnCompletionListener { stop() }
        player = mp
        playingId = id
        mp.start()
    }

    // به‌روزرسانی نوار پیشرفت پخش
    LaunchedEffect(playingId) {
        while (playingId != null) {
            val p = player
            if (p != null && p.isPlaying) {
                progress = p.currentPosition.toFloat() / p.duration.coerceAtLeast(1).toFloat()
            }
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { player?.release() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = "کتابخانه",
                subtitle = "متن و خوانشِ کتاب‌ها و متون کنگره ۶۰ — با صدای مرد و زن"
            )
        }

        // آرام‌سازی فورست
        item {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (playingId == "forest") stop() else play("forest") }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "آرام‌سازی فورست",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            if (playingId == "forest") {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
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
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // انتخاب گوینده
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "گویندهٔ مرد", true to "گویندهٔ زن").forEach { (female, label) ->
                    val on = voiceFemale == female
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (voiceFemale != female) {
                                    stop()
                                    voiceFemale = female
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = if (on) MaterialTheme.colorScheme.onPrimary else NavViolet,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // کارت‌های کتابخانه — چیدمان عکس ۶: کتاب سمت راست، پخش سمت چپ، شرون پایین
        items(LIB_ITEMS.size) { index ->
            val item = LIB_ITEMS[index]
            val expanded = expandedId == item.id
            val isPlaying = playingId == item.id
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // آیکون کتاب — سمت راست
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(item.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.sub,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = if (item.audio) "متن + صوت" else "متن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (item.audio) {
                                // «اپیز = خوانش متن» — آیکون کوچکِ کتاب سمت چپِ عبارت
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "اپیز = خوانش متن",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = item.color
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = item.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        if (item.audio) {
                            Spacer(Modifier.width(10.dp))
                            // دکمهٔ پخش — سمت چپ
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) item.color else item.color.copy(alpha = 0.12f))
                                    .clickable { if (isPlaying) stop() else play(item.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "توقف پخش" else "پخش صوت",
                                    tint = if (isPlaying) Color.White else item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isPlaying) {
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
                                    .background(item.color)
                            )
                        }
                    }

                    // شرون ▼ — پایین و وسط کارت (عکس ۶)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "بستن متن" else "نمایش متن",
                            tint = item.color,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { expandedId = if (expanded) null else item.id }
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = item.text,
                            fontSize = 12.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(top = 6.dp)
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
