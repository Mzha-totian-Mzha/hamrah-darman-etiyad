package ir.mosafir.recovery.data

import java.util.Calendar
import java.util.Locale

/**
 * تبدیل تاریخ میلادی ↔ شمسی (الگوریتم استاندارد ۳۳ ساله).
 */
object PersianDate {

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val G_DAY_OF_MONTH = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        var gy0 = gy
        var jy: Int
        var gd2: Int
        if (gy0 > 1600) {
            jy = 979
            gy0 -= 1600
        } else {
            jy = 0
            gy0 -= 621
        }
        gd2 = G_DAY_OF_MONTH[gm - 1] + gd
        if (gm > 2 && ((gy0 % 4 == 0 && gy0 % 100 != 0) || (gy0 % 400 == 0))) gd2++

        var days = 365 * gy0 + ((gy0 + 3) / 4) - ((gy0 + 99) / 100) + ((gy0 + 399) / 400) - 80 + gd2
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }

        var jm: Int
        var jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + (days % 31)
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + ((days - 186) % 30)
        }
        return intArrayOf(jy, jm, jd)
    }

    fun jalaliToGregorian(jy0: Int, jm: Int, jd: Int): IntArray {
        var jy = jy0
        jy += 1595
        var days = -355668 + (365 * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) +
            jd + (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)

        var gy = 400 * (days / 146097)
        days %= 146097

        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }

        gy += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }

        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val monthDays = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        var gm = 0
        while (gm < 13 && gd > monthDays[gm]) {
            gd -= monthDays[gm]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    /** تبدیل millis به آرایه شمسی [سال، ماه، روز] */
    fun fromMillis(millis: Long): IntArray {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return gregorianToJalali(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** قالب کوتاه: ۱۴۰۴/۰۸/۱۱ */
    fun formatShort(millis: Long): String {
        val p = fromMillis(millis)
        val jy = p[0]; val jm = p[1]; val jd = p[2]
        return String.format(Locale.US, "%d/%02d/%02d", jy, jm, jd).toPersianDigits()
    }

    /** قالب بلند: ۱۱ آبان ۱۴۰۴ */
    fun formatLong(millis: Long): String {
        val p = fromMillis(millis)
        val jy = p[0]; val jm = p[1]; val jd = p[2]
        return "${jd.toPersianDigits()} ${MONTH_NAMES[jm - 1]} ${jy.toString().toPersianDigits()}"
    }
}

/** تبدیل ارقام لاتین به فارسی */
fun String.toPersianDigits(): String {
    val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return this.map { if (it in '0'..'9') fa[it - '0'] else it }.joinToString("")
}

/** قالب‌بندی مقدار دوز، مثلا ۰٫۹۵ */
fun formatDose(cc: Double): String {
    val v = kotlin.math.round(cc * 100.0) / 100.0
    val s = if (v == v.toLong().toDouble()) {
        String.format(Locale.US, "%d", v.toLong())
    } else {
        String.format(Locale.US, "%.2f", v)
    }
    return s.replace('.', '٫').toPersianDigits()
}

/** قالب‌بندی عدد ساده */
fun formatNumber(n: Long): String = n.toString().toPersianDigits()

fun formatNumber(n: Int): String = n.toString().toPersianDigits()
