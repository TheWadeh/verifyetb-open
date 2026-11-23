package com.verifyetb.app.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AccountDetailUi(
    val institutionId: String = "",
    val displayName: String = "",
    val balance: Double = 0.0,
    val sent: Double = 0.0,
    val received: Double = 0.0,
    val fees: Double = 0.0,
    val recent: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TransactionRepository,
) : ViewModel() {

    private val institutionId: String = checkNotNull(savedStateHandle["institutionId"])
    private val displayName: String =
        EthiopianInstitutions.byId(institutionId)?.displayName ?: institutionId

    val ui: StateFlow<AccountDetailUi> = combine(
        repository.observeLiveBalance(institutionId, displayName),
        repository.observeAll(),
    ) { balance, txs ->
        val (from, to) = DateFormatters.rangeFor(DashboardPeriod.MONTH)
        val mine = txs.filter { it.institution.equals(displayName, ignoreCase = true) }
        val period = mine.filter { it.date >= from && it.date < to }
        val fees = period.sumOf { it.feeAmount }
        AccountDetailUi(
            institutionId = institutionId,
            displayName = displayName,
            balance = balance,
            sent = period.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount } + fees,
            received = period.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount },
            fees = fees,
            recent = mine.take(30),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AccountDetailUi(institutionId = institutionId, displayName = displayName),
    )
}
