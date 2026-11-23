package com.verifyetb.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class InsightsPeriod { WEEK, MONTH, YEAR }

data class FlowBar(
    val start: Long,
    val label: String,
    val net: Double,
)

data class CategorySlice(
    val name: String,
    val amount: Double,
    val fraction: Float,
)

data class HeatCell(
    val start: Long,
    val count: Int,
    val net: Double,
    val inFuture: Boolean,
)

data class InsightsUi(
    val period: InsightsPeriod = InsightsPeriod.MONTH,
    val net: Double = 0.0,
    val incoming: Double = 0.0,
    val outgoing: Double = 0.0,
    val fees: Double = 0.0,
    val count: Int = 0,
    val bars: List<FlowBar> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val weeks: List<List<HeatCell>> = emptyList(),
    val maxHeat: Int = 0,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    private val period = MutableStateFlow(InsightsPeriod.MONTH)

    val ui: StateFlow<InsightsUi> = combine(
        period,
        repository.observeAll(),
    ) { selected, txs ->
        build(selected, txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUi())

    fun setPeriod(next: InsightsPeriod) {
        period.value = next
    }
}

private fun build(period: InsightsPeriod, txs: List<TransactionEntity>): InsightsUi {
    val (from, to) = when (period) {
        InsightsPeriod.WEEK -> DateFormatters.rangeFor(DashboardPeriod.WEEK)
        InsightsPeriod.MONTH -> DateFormatters.rangeFor(DashboardPeriod.MONTH)
        InsightsPeriod.YEAR -> DateFormatters.rangeFor(DashboardPeriod.YEAR)
    }
    val inRange = txs.filter { it.date in from until to }
    val incoming = inRange.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount }
    val outgoing = inRange.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount }
    val fees = inRange.sumOf { it.feeAmount }
    val bars = barsFor(period, from, to, inRange)
    val categories = categorySlices(inRange)
    val weeks = heatmap(txs)
    return InsightsUi(
        period = period,
        net = incoming - outgoing - fees,
        incoming = incoming,
        outgoing = outgoing,
        fees = fees,
        count = inRange.size,
        bars = bars,
        categories = categories,
        weeks = weeks,
        maxHeat = weeks.flatten().maxOfOrNull { it.count } ?: 0,
    )
}

private fun barsFor(
    period: InsightsPeriod,
    from: Long,
    to: Long,
    txs: List<TransactionEntity>,
): List<FlowBar> {
    val buckets = mutableListOf<FlowBar>()
    val cal = Calendar.getInstance().apply { timeInMillis = from }
    when (period) {
        InsightsPeriod.WEEK, InsightsPeriod.MONTH -> {
            while (cal.timeInMillis < to) {
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis
                val label = if (period == InsightsPeriod.WEEK) {
                    java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(java.util.Date(start))
                } else {
                    calLabel(start, day = true)
                }
                buckets += FlowBar(start, label, netBetween(txs, start, end))
            }
        }
        InsightsPeriod.YEAR -> {
            while (cal.timeInMillis < to) {
                val start = cal.timeInMillis
                val label = java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(cal.time)
                cal.add(Calendar.MONTH, 1)
                buckets += FlowBar(start, label, netBetween(txs, start, cal.timeInMillis))
            }
        }
    }
    return buckets
}

private fun categorySlices(txs: List<TransactionEntity>): List<CategorySlice> {
    val debits = txs.filter { it.type == TransactionEntity.TYPE_DEBIT }
    if (debits.isEmpty()) return emptyList()
    val grouped = debits.groupBy { it.category?.trim().takeUnless { name -> name.isNullOrEmpty() } ?: "Uncategorized" }
    val ranked = grouped.map { (name, rows) -> name to rows.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .take(6)
    val max = ranked.maxOf { it.second }.coerceAtLeast(1.0)
    return ranked.map { (name, amount) ->
        CategorySlice(name, amount, (amount / max).toFloat().coerceIn(0.04f, 1f))
    }
}

private fun heatmap(txs: List<TransactionEntity>): List<List<HeatCell>> {
    val todayStart = DateFormatters.dayRange(System.currentTimeMillis()).first
    val monday = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.WEEK_OF_YEAR, -11)
    }
    val byDay = txs.groupBy { DateFormatters.dayRange(it.date).first }
    return List(12) {
        List(7) {
            val start = monday.timeInMillis
            val rows = byDay[start].orEmpty()
            val incoming = rows.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount }
            val outgoing = rows.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount }
            val cell = HeatCell(
                start = start,
                count = rows.size,
                net = incoming - outgoing - rows.sumOf { it.feeAmount },
                inFuture = start > todayStart,
            )
            monday.add(Calendar.DAY_OF_MONTH, 1)
            cell
        }
    }
}

private fun netBetween(txs: List<TransactionEntity>, from: Long, to: Long): Double {
    val slice = txs.filter { it.date in from until to }
    val incoming = slice.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount }
    val outgoing = slice.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount }
    return incoming - outgoing - slice.sumOf { it.feeAmount }
}

private fun calLabel(millis: Long, day: Boolean): String {
    val fmt = if (day) java.text.SimpleDateFormat("d", java.util.Locale.US) else java.text.SimpleDateFormat("MMM", java.util.Locale.US)
    return fmt.format(java.util.Date(millis))
}
