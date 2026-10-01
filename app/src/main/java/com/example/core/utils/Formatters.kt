package com.example.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object PersianFormatter {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Long): String {
        return toPersianDigits(number.toString())
    }

    fun toPersianDigits(number: Int): String {
        return toPersianDigits(number.toString())
    }

    fun formatMoney(amount: Long, currency: String = "تومان", hide: Boolean = false): String {
        if (hide) {
            return "••••••••"
        }
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = formatter.format(amount)
        return "${toPersianDigits(formatted)} $currency"
    }

    fun formatNumberWithCommas(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return toPersianDigits(formatter.format(amount))
    }

    fun formatPercent(percent: Int): String {
        return "${toPersianDigits(percent)}٪"
    }
}

/**
 * Solar Hijri (Jalali) Calendar Utilities
 */
object JalaliCalendarHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun timestampToJalali(timestamp: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    fun formatJalaliDate(timestamp: Long): String {
        val j = timestampToJalali(timestamp)
        val monthName = PERSIAN_MONTH_NAMES.getOrElse(j.month - 1) { "" }
        return "${PersianFormatter.toPersianDigits(j.day)} $monthName ${PersianFormatter.toPersianDigits(j.year)}"
    }

    fun formatRelativeOrDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "امروز"
            isYesterday -> "دیروز"
            else -> formatJalaliDate(timestamp)
        }
    }

    fun formatTime(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val hStr = if (hour < 10) "0$hour" else "$hour"
        val mStr = if (minute < 10) "0$minute" else "$minute"
        return PersianFormatter.toPersianDigits("$hStr:$mStr")
    }

    /**
     * Start of the current Jalali month (1st day, 00:00:00.000).
     * "این ماه" must follow the Persian calendar; Gregorian month bounds
     * cut off the Jalali month's tail and include the previous month's head.
     */
    fun getStartOfCurrentJalaliMonth(): Long {
        val j = timestampToJalali(System.currentTimeMillis())
        return jalaliToStartOfDay(j.year, j.month, 1)
    }

    /** End of the current Jalali month (last day, 23:59:59). */
    fun getEndOfCurrentJalaliMonth(): Long {
        val j = timestampToJalali(System.currentTimeMillis())
        return jalaliToEndOfDay(j.year, j.month, jalaliDaysInMonth(j.year, j.month))
    }

    fun getCurrentPersianMonthName(): String {
        val j = timestampToJalali(System.currentTimeMillis())
        return PERSIAN_MONTH_NAMES.getOrElse(j.month - 1) { "این ماه" }
    }

    fun getDaysPassedInCurrentMonth(): Int {
        val j = timestampToJalali(System.currentTimeMillis())
        return j.day.coerceAtLeast(1)
    }

    /** Start of today (00:00:00.000) in the device timezone. */
    fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Milliseconds until the next local midnight — used to re-evaluate day-scoped windows. */
    fun millisUntilTomorrow(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
    }

    /**
     * Start of the current week (00:00:00.000).
     * The Iranian week starts on Saturday (شنبه).
     */
    fun getStartOfCurrentWeek(): Long {
        val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SATURDAY }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** End of the current Iranian week (Friday, 23:59:59.999). */
    fun getEndOfCurrentWeek(): Long {
        val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SATURDAY }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Convert Jalali (Solar Hijri) date to Gregorian date.
     * Day-count method that is the exact inverse of [gregorianToJalali]
     * (the jdf.scr.ir algorithm pair). The previous JDN-based version was
     * mathematically wrong and produced dates thousands of years in the past.
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): JalaliDate {
        // Canonical day-count algorithm (jdf.scr.ir) — exact inverse of gregorianToJalali above.
        val gy0 = if (jy > 979) 1600 else 621
        val y = if (jy > 979) jy - 979 else jy
        var days = 365L * y + (y / 33) * 8 + ((y % 33) + 3) / 4 + 78 + jd +
                (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        var gy = gy0 + 400 * (days / 146097).toInt()
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524).toInt()
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            gy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var gd = (days + 1).toInt()
        val isLeapG = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val monthLengths = intArrayOf(0, 31, if (isLeapG) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12 && gd > monthLengths[gm]) {
            gd -= monthLengths[gm]
            gm++
        }
        return JalaliDate(gy, gm, gd)
    }

    /**
     * Convert a Jalali date to a Unix timestamp (milliseconds).
     */
    fun jalaliToTimestamp(jy: Int, jm: Int, jd: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Long {
        val g = jalaliToGregorian(jy, jm, jd)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, g.year)
            set(Calendar.MONTH, g.month - 1)
            set(Calendar.DAY_OF_MONTH, g.day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Start of a Jalali day as timestamp.
     */
    fun jalaliToStartOfDay(jy: Int, jm: Int, jd: Int): Long =
        jalaliToTimestamp(jy, jm, jd, 0, 0, 0)

    /**
     * End of a Jalali day as timestamp (inclusive of the whole final second).
     */
    fun jalaliToEndOfDay(jy: Int, jm: Int, jd: Int): Long =
        jalaliToTimestamp(jy, jm, jd, 23, 59, 59) + 999 // + .999ms

    /**
     * Number of days in a Jalali month.
     */
    fun jalaliDaysInMonth(jy: Int, jm: Int): Int {
        return if (jm <= 6) 31 else if (jm <= 11) 30 else if (isJalaliLeapYear(jy)) 30 else 29
    }

    /**
     * Check if a Jalali year is a leap year (33-year cycle).
     */
    fun isJalaliLeapYear(jy: Int): Boolean {
        val remainder = jy % 33
        val patterns = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)
        return remainder in patterns
    }

    /**
     * Format a Jalali date range for display.
     */
    fun formatDateRange(startTimestamp: Long, endTimestamp: Long): String {
        val s = timestampToJalali(startTimestamp)
        val e = timestampToJalali(endTimestamp)
        val startStr = "${PersianFormatter.toPersianDigits(s.day)} ${PERSIAN_MONTH_NAMES[s.month - 1]} ${PersianFormatter.toPersianDigits(s.year)}"
        val endStr = "${PersianFormatter.toPersianDigits(e.day)} ${PERSIAN_MONTH_NAMES[e.month - 1]} ${PersianFormatter.toPersianDigits(e.year)}"
        return "$startStr تا $endStr"
    }
}
