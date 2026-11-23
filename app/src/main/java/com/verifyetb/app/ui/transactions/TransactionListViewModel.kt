package com.verifyetb.app.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TransactionRepository,
) : ViewModel() {

    private val initialFrom: Long = savedStateHandle["from"] ?: 0L
    private val initialTo: Long = savedStateHandle["to"] ?: 0L
    private val initialInstitution: String = decode(savedStateHandle["institution"] ?: "")
    private val initialType: String = decode(savedStateHandle["type"] ?: "")
    private val initialFeesOnly: Boolean = (savedStateHandle["feesOnly"] as? Int ?: 0) == 1

    private val _query = MutableStateFlow("")
    private val _type = MutableStateFlow(initialType)
    private val _institution = MutableStateFlow(initialInstitution)
    private val _from = MutableStateFlow(initialFrom)
    private val _to = MutableStateFlow(initialTo)
    private val _feesOnly = MutableStateFlow(initialFeesOnly)
    private val _period = MutableStateFlow(inferPeriod(initialFrom, initialTo))
    private val _customDayMillis = MutableStateFlow(
        if (_period.value == DashboardPeriod.CUSTOM && initialFrom > 0L) initialFrom else null,
    )

    val query: StateFlow<String> = _query
    val typeFilter: StateFlow<String> = _type
    val institutionFilter: StateFlow<String> = _institution
    val fromFilter: StateFlow<Long> = _from
    val toFilter: StateFlow<Long> = _to
    val feesOnly: StateFlow<Boolean> = _feesOnly
    val period: StateFlow<DashboardPeriod?> = _period
    val customDayMillis: StateFlow<Long?> = _customDayMillis
    val hasPeriodFilter: Boolean = initialFrom > 0L || initialTo > 0L || initialFeesOnly

    val institutions: StateFlow<List<String>> = repository.observeInstitutions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<com.verifyetb.app.data.local.entity.TransactionEntity>> =
        combine(
            combine(_query, _type, _institution) { q, t, i -> Triple(q, t, i) },
            combine(_from, _to, _feesOnly) { f, t, fees -> Triple(f, t, fees) },
        ) { a, b ->
            FilterArgs(
                query = a.first,
                type = a.second,
                institution = a.third,
                from = b.first,
                to = b.second,
                feesOnly = b.third,
            )
        }.flatMapLatest { args ->
            repository.observeFiltered(
                query = args.query,
                type = args.type,
                institution = args.institution,
                fromInclusive = args.from,
                toExclusive = args.to,
                feesOnly = args.feesOnly,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setTypeFilter(value: String) {
        _type.value = value
    }

    fun setInstitutionFilter(value: String) {
        _institution.value = value
    }

    fun clearFeesOnly() {
        _feesOnly.value = false
    }

    fun setPeriod(period: DashboardPeriod) {
        if (period == DashboardPeriod.CUSTOM) return
        val (from, to) = DateFormatters.rangeFor(period)
        _period.value = period
        _customDayMillis.value = null
        _from.value = from
        _to.value = to
    }

    fun setCustomDate(utcPickerMillis: Long) {
        val day = DateFormatters.localDayStartFromUtcPicker(utcPickerMillis)
        val (from, to) = DateFormatters.dayRange(day)
        _period.value = DashboardPeriod.CUSTOM
        _customDayMillis.value = day
        _from.value = from
        _to.value = to
    }

    fun clearPeriodFilter() {
        _from.value = 0L
        _to.value = 0L
        _feesOnly.value = false
        _period.value = null
        _customDayMillis.value = null
    }

    private data class FilterArgs(
        val query: String,
        val type: String,
        val institution: String,
        val from: Long,
        val to: Long,
        val feesOnly: Boolean,
    )

    private fun decode(value: String): String {
        if (value.isEmpty()) return value
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            value
        }
    }

    private fun inferPeriod(from: Long, to: Long): DashboardPeriod? {
        if (from <= 0L && to <= 0L) return null
        for (period in listOf(
            DashboardPeriod.TODAY,
            DashboardPeriod.WEEK,
            DashboardPeriod.MONTH,
            DashboardPeriod.YEAR,
        )) {
            val (rangeFrom, rangeTo) = DateFormatters.rangeFor(period)
            if (rangeFrom == from && rangeTo == to) return period
        }
        return DashboardPeriod.CUSTOM
    }
}
