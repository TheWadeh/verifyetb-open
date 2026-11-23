package com.verifyetb.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MoneyFormat {
    private val et = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun birr(amount: Double?): String {
        if (amount == null) return "—"
        return "ETB ${et.format(amount)}"
    }

    fun signed(amount: Double, isDebit: Boolean): String {
        val sign = if (isDebit) "−" else "+"
        return "$sign${et.format(amount)}"
    }
}

enum class DashboardPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM,
}

object DateFormatters {
    private val day = SimpleDateFormat("d MMM yyyy", Locale.US)
    private val dayShort = SimpleDateFormat("d MMM", Locale.US)
    private val dayTime = SimpleDateFormat("d MMM · HH:mm", Locale.US)

    fun day(millis: Long): String = day.format(Date(millis))
    fun dayShort(millis: Long): String = dayShort.format(Date(millis))
    fun dayTime(millis: Long): String = dayTime.format(Date(millis))

    fun rangeFor(period: DashboardPeriod, customDayMillis: Long? = null): Pair<Long, Long> = when (period) {
        DashboardPeriod.TODAY -> todayRange()
        DashboardPeriod.WEEK -> thisWeekRange()
        DashboardPeriod.MONTH -> currentMonthRange()
        DashboardPeriod.YEAR -> thisYearRange()
        DashboardPeriod.CUSTOM -> dayRange(customDayMillis ?: System.currentTimeMillis())
    }

    /** Material3 DatePicker returns UTC midnight; convert to local calendar day. */
    fun localDayStartFromUtcPicker(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utc.get(Calendar.YEAR))
            set(Calendar.MONTH, utc.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun localDayStartToUtcPicker(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, local.get(Calendar.YEAR))
            set(Calendar.MONTH, local.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun dayRange(dayMillis: Long): Pair<Long, Long> {
        val start = startOfDay(Calendar.getInstance().apply { timeInMillis = dayMillis })
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        return start to end
    }

    fun todayRange(): Pair<Long, Long> {
        val start = startOfDay(Calendar.getInstance())
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        return start to end
    }

    fun thisWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = startOfDay(cal)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_MONTH, 7)
        }
        return start to endCal.timeInMillis
    }

    fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = startOfDay(cal)
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    fun thisYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        val start = startOfDay(cal)
        cal.add(Calendar.YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun startOfDay(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
