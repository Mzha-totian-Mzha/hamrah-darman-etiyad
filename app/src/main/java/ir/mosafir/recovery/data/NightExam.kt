package ir.mosafir.recovery.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ir.mosafir.recovery.MainActivity
import ir.mosafir.recovery.R
import java.util.Calendar

/**
 * «آزمون شبانه» — یادآورِ روزانهٔ آزمون
 *
 * هر روز در ساعت تعیین‌شده (پیش‌فرض ۲۱:۳۰) بررسی می‌شود:
 * اگر آن روز هنوز هیچ آزمون یا تمرینی کامل نشده باشد، نوتیفیکیشن
 * «امشب هنوز آزمون ندادی!» فرستاده می‌شود؛ و اگر آزمون داده شده باشد،
 * آن شب دیگر پیامی نمی‌آید. برنامهٔ روز بعد، بلافاصله تنظیم می‌شود.
 */
object NightExamStore {

    data class NightExamSettings(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("mosafir_night_exam", Context.MODE_PRIVATE)

    fun load(ctx: Context): NightExamSettings {
        val p = prefs(ctx)
        return NightExamSettings(
            enabled = p.getBoolean("enabled", false),
            hour = p.getInt("hour", 21),
            minute = p.getInt("minute", 30)
        )
    }

    fun save(ctx: Context, s: NightExamSettings) {
        prefs(ctx).edit()
            .putBoolean("enabled", s.enabled)
            .putInt("hour", s.hour)
            .putInt("minute", s.minute)
            .apply()
    }

    /** کلید امروز — مثلاً 2026-11-04 */
    fun todayKey(): String {
        val c = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isDoneToday(ctx: Context): Boolean =
        prefs(ctx).getString("last_done_date", null) == todayKey()

    /** پایان یک آزمون یا تمرین → همان شب دیگر یادآوری نمی‌شود */
    fun markDoneToday(ctx: Context) {
        prefs(ctx).edit().putString("last_done_date", todayKey()).apply()
    }

    fun lastDoneLabel(ctx: Context): String? =
        prefs(ctx).getString("last_done_date", null)

    fun isNotifiedToday(ctx: Context): Boolean =
        prefs(ctx).getString("last_notif_date", null) == todayKey()

    fun markNotifiedToday(ctx: Context) {
        prefs(ctx).edit().putString("last_notif_date", todayKey()).apply()
    }
}

object NightExamScheduler {

    const val CHANNEL_ID = "night_exam"
    private const val REQUEST_CODE = 1403

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                "آزمون شبانه",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآور روزانهٔ آزمون — اگر آن روز آزمون نداده باشی"
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun alarmPendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, NightExamReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** تنظیم زنگ فردا (یا امروز اگر هنوز نرسیده) — با هر تغییر تنظیمات و هر بار باز شدن برنامه */
    fun scheduleNext(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(ctx)
        runCatching { am.cancel(pi) }
        val s = NightExamStore.load(ctx)
        if (!s.enabled) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, s.hour)
            set(Calendar.MINUTE, s.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val canExact =
            Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun sendNotification(ctx: Context) {
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        if (!granted) return

        ensureChannel(ctx)
        val open = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, REQUEST_CODE, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 آزمون شبانه")
            .setContentText("امشب هنوز آزمون ندادی! چند دقیقه وقت بگذار — ما می‌توانیم ✋")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("امشب هنوز آزمون ندادی! چند دقیقه وقت بگذار و آزمون شب را بده؛ سؤال‌های امشب با دفعهٔ قبل فرق دارند. ما می‌توانیم ✋")
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1403, notif)
    }
}

/** دریافت زنگ روزانه: یادآوری (اگر لازم باشد) + تنظیم زنگ فردا */
class NightExamReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = NightExamStore.load(context)
        if (!s.enabled) return
        if (!NightExamStore.isDoneToday(context) && !NightExamStore.isNotifiedToday(context)) {
            NightExamScheduler.sendNotification(context)
            NightExamStore.markNotifiedToday(context)
        }
        NightExamScheduler.scheduleNext(context)
    }
}

/** بعد از ری‌استارت گوشی، زنگ دوباره تنظیم می‌شود */
class NightExamBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NightExamScheduler.scheduleNext(context)
        }
    }
}
