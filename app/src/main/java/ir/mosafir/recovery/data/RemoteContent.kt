package ir.mosafir.recovery.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * یک قطعهٔ ابری — دقیقاً هم‌ساختار پاساژهای بانک داخلی (assets/passages.json)
 * تا بعد از دریافت، مستقیم وارد چرخش کارت‌های کتابخانه شود.
 */
data class CloudPassage(
    val id: String,
    val src: String,
    val n: Int,
    val text: String
)

/**
 * به‌روزرسانی ابری محتوای کتابخانه از گیت‌هاب.
 *
 * روش کار: فایل library.json در همین ریپو (پوشهٔ server-content) قرار دارد؛
 * اپ با هر ورود به تب کتابخانه آن را می‌خواند، کش می‌کند و قطعه‌هایش به
 * بانک محلی «اضافه» می‌شوند (هیچ‌چیز حذف نمی‌شود).
 *
 * قالب فایل:
 * {
 *   "_info": {"updated": "1404-08-14", "total": 144},
 *   "passages": [
 *     {"id": "cloud-0001", "src": "چهارده مقاله", "n": 1, "text": "..."},
 *     ...
 *   ]
 * }
 *
 * «src» باید دقیقاً یکی از منبع‌های شناخته‌شدهٔ کتابخانه باشد:
 * «چهارده مقاله»، «کتاب ۶۰ درجه»، «چهارده وادی عشق»، «راهنمای تازه‌واردین»،
 * «کنگره ۶۰ چیست؟»، «جهان‌بینی ۱»، «پروتکل DST»، «اصلاحیه پروتکل DST»،
 * «قوانین کنگره ۶۰»، «بهترین راه»، «مثلث‌های آموزشی»، «مصرف‌کننده کیست؟»، «شرح وظایف»
 *
 * اگر اینترنت نبود یا فایل در دسترس نبود، بی‌سروصدا بانک محلی استفاده می‌شود.
 */
object RemoteContent {

    /** آدرس پیش‌فرض: فایل ابری داخل همین ریپو (پوشهٔ server-content) */
    const val DEFAULT_URL =
        "https://raw.githubusercontent.com/Mzha-totian-Mzha/hamrah-darman-etiyad/main/server-content/library.json"

    private const val PREFS = "mosafir_remote"
    private const val KEY_CACHE = "cache"
    private const val KEY_URL = "url"
    private const val KEY_TIME = "time"

    /** آدرس فعلی: اولویت با آدرسی که داخل خود اپ ثبت شده، وگرنه آدرس پیش‌فرض ریپو */
    fun currentUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, null) ?: DEFAULT_URL

    /** آیا آدرس ابری معتبر است؟ */
    fun isConfigured(ctx: Context): Boolean =
        currentUrl(ctx).startsWith("http") && !currentUrl(ctx).contains("USERNAME")

    /**
     * ثبت آدرس داخل خود اپ — بدون نیاز به بیلد جدید.
     * لینک خود صفحهٔ ریپو (github.com/...) هم قبول می‌شود؛ خودکار به raw تبدیل می‌شود.
     */
    fun setUrl(ctx: Context, url: String) {
        val fixed = normalizeUrl(url)
        if (fixed.startsWith("http")) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_URL, fixed).apply()
        }
    }

    /** لینک صفحهٔ github.com/USER/REPO/blob/BRANCH/FILE را به raw.githubusercontent.com تبدیل می‌کند */
    private fun normalizeUrl(u: String): String {
        val t = u.trim().substringBefore("?")
        if (t.contains("github.com") && !t.contains("raw.githubusercontent.com")) {
            val m = Regex("github\\.com/([^/]+)/([^/]+)/(?:blob|raw)/(.+)").find(t)
            if (m != null) {
                return "https://raw.githubusercontent.com/${m.groupValues[1]}/${m.groupValues[2]}/${m.groupValues[3]}"
            }
        }
        return t
    }

    /** قطعه‌های ابریِ کش‌شده از دفعه‌های قبل (حتی آفلاین در دسترس‌اند) */
    fun cachedPassages(ctx: Context): List<CloudPassage> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CACHE, null) ?: return emptyList()
        return runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    fun lastFetchTime(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_TIME, 0L)

    /**
     * دریافت فایل از گیت‌هاب در پس‌زمینه و ذخیرهٔ کش.
     * نتیجه روی ترد اصلی برمی‌گردد: ok / تعداد قطعه‌های دریافتی / پیام خطا.
     */
    fun fetchAndApply(ctx: Context, onDone: (ok: Boolean, received: Int, message: String) -> Unit) {
        val appCtx = ctx.applicationContext
        val main = Handler(Looper.getMainLooper())
        Thread {
            var ok = false
            var received = 0
            var msg = ""
            runCatching {
                val url = currentUrl(appCtx)
                if (!url.startsWith("http")) throw IOException("آدرس گیت‌هاب معتبر نیست")
                val raw = download(url)
                val list = parse(raw)
                received = list.size
                require(received > 0) { "فایل ابری خالی بود" }
                appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHE, raw)
                    .putLong(KEY_TIME, System.currentTimeMillis())
                    .apply()
                ok = true
            }.onFailure { msg = it.message ?: "خطای نامشخص" }
            main.post { onDone(ok, received, msg) }
        }.start()
    }

    private fun download(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Hamrah-Darman-App/1.7")
        try {
            if (conn.responseCode !in 200..299) throw IOException("کد پاسخ سرور: ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** پارس فایل ابری — هم قالب {"passages":[...]} و هم آرایهٔ خام را می‌پذیرد */
    private fun parse(raw: String): List<CloudPassage> {
        val root = JSONObject(raw)
        val arr: JSONArray = root.optJSONArray("passages") ?: JSONArray()
        val out = ArrayList<CloudPassage>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("text").trim()
            val src = o.optString("src").trim()
            if (text.isEmpty() || src.isEmpty()) continue
            out.add(
                CloudPassage(
                    id = o.optString("id").trim().ifEmpty { "cloud-${out.size + 1}" },
                    src = src,
                    n = o.optInt("n", out.size + 1),
                    text = text
                )
            )
        }
        return out
    }
}
