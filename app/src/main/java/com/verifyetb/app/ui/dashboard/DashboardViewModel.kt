package com.verifyetb.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.SmsInboxImporter
import com.verifyetb.app.data.local.entity.AccountBalanceEntity
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.parser.InstitutionKind
import com.verifyetb.app.util.BalanceMath
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountRowUi(
    val id: String,
    val displayName: String,
    val kindLabel: String,
    val balance: Double,
    val sent: Double,
    val received: Double,
    val fees: Double,
    val txCount: Int,
)

data class DashboardUi(
    val period: DashboardPeriod = DashboardPeriod.MONTH,
    val customDayMillis: Long? = null,
    val totalBalance: Double = 0.0,
    val sent: Double = 0.0,
    val received: Double = 0.0,
    val fees: Double = 0.0,
    val periodFrom: Long = 0L,
    val periodTo: Long = 0L,
    val accounts: List<AccountRowUi> = emptyList(),
    val recent: List<TransactionEntity> = emptyList(),
    val unresolvedCount: Int = 0,
    val firstUnresolvedId: Long? = null,
    val transactionCount: Int = 0,
    val debitCount: Int = 0,
    val creditCount: Int = 0,
    val avgSent: Double = 0.0,
    val avgReceived: Double = 0.0,
    val largestFee: Double = 0.0,
    val banksBalance: Double = 0.0,
    val walletsBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val accountCount: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: TransactionRepository,
    settingsRepository: SettingsRepository,
    private val importer: SmsInboxImporter,
) : ViewModel() {

    private val _period = MutableStateFlow(DashboardPeriod.MONTH)
    val period: StateFlow<DashboardPeriod> = _period

    private val _customDayMillis = MutableStateFlow<Long?>(null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage

    private val unresolvedBundle = combine(
        repository.observeUnresolvedCount(),
        repository.observeFirstUnresolvedId(),
    ) { count, id -> count to id }

    private val periodBundle = combine(_period, _customDayMillis) { period, customDay ->
        period to customDay
    }

    val ui: StateFlow<DashboardUi> = combine(
        periodBundle,
        settingsRepository.selectedInstitutionIds,
        repository.observeAccountBalances(),
        repository.observeAll(),
        unresolvedBundle,
    ) { periodPair, selectedIds, openings, txs, unresolvedPair ->
        val (period, customDay) = periodPair
        buildUi(
            period = period,
            customDayMillis = customDay,
            selectedIds = selectedIds,
            openings = openings,
            txs = txs,
            unresolved = unresolvedPair.first,
            firstUnresolved = unresolvedPair.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())

    val newestAnimatedId: StateFlow<Long?> = ui
        .map { it.recent.firstOrNull()?.id }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(period: DashboardPeriod) {
        if (period == DashboardPeriod.CUSTOM) return
        _period.value = period
        _customDayMillis.value = null
    }

    fun setCustomDate(utcPickerMillis: Long) {
        _customDayMillis.value = DateFormatters.localDayStartFromUtcPicker(utcPickerMillis)
        _period.value = DashboardPeriod.CUSTOM
    }

    fun refreshFromSms() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            _refreshMessage.value = null
            try {
                val inserted = importer.syncNew()
                delay(700)
                _refreshMessage.value = when {
                    inserted > 0 -> "Read $inserted SMS from your inbox"
                    else -> "No new bank SMS"
                }
            } catch (t: Throwable) {
                _refreshMessage.value = t.message?.takeIf { it.isNotBlank() } ?: "Could not refresh SMS"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    private fun buildUi(
        period: DashboardPeriod,
        customDayMillis: Long?,
        selectedIds: Set<String>,
        openings: List<AccountBalanceEntity>,
        txs: List<TransactionEntity>,
        unresolved: Int,
        firstUnresolved: Long?,
    ): DashboardUi {
        val (from, to) = DateFormatters.rangeFor(period, customDayMillis)
        val openingMap = openings.associate { it.institutionId to it.openingBalance }
        val ids = EthiopianInstitutions.withCashAlways(selectedIds)
        val accounts = EthiopianInstitutions.selectable
            .filter { it.id in ids }
            .map { inst ->
                val instTxs = txs.filter { tx ->
                    tx.institution.equals(inst.displayName, ignoreCase = true)
                }
                val periodTxs = instTxs.filter { it.date >= from && it.date < to }
                val feesPeriod = periodTxs.sumOf { it.feeAmount }
                val sent = periodTxs.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount } + feesPeriod
                val received = periodTxs.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount }
                val opening = openingMap[inst.id] ?: 0.0
                val balance = BalanceMath.compute(
                    opening = opening,
                    txs = instTxs,
                    isCash = inst.kind == InstitutionKind.CASH,
                )
                AccountRowUi(
                    id = inst.id,
                    displayName = inst.displayName,
                    kindLabel = when (inst.kind) {
                        InstitutionKind.BANK -> "Bank"
                        InstitutionKind.WALLET -> "Wallet"
                        InstitutionKind.CASH -> "Cash"
                    },
                    balance = balance,
                    sent = sent,
                    received = received,
                    fees = feesPeriod,
                    txCount = periodTxs.size,
                )
            }
        val periodTxs = txs.filter { it.date >= from && it.date < to }
            .filter { tx ->
                val inst = EthiopianInstitutions.byDisplayName(tx.institution)
                inst == null || inst.id in ids
            }
        val selectedTxs = txs.filter { tx ->
            val inst = EthiopianInstitutions.byDisplayName(tx.institution)
            inst == null || inst.id in ids
        }
        val debits = periodTxs.filter { it.type == TransactionEntity.TYPE_DEBIT }
        val credits = periodTxs.filter { it.type == TransactionEntity.TYPE_CREDIT }
        val fees = periodTxs.sumOf { it.feeAmount }
        val debitAmounts = debits.sumOf { it.amount }
        val sent = debitAmounts + fees
        val received = credits.sumOf { it.amount }
        return DashboardUi(
            period = period,
            customDayMillis = customDayMillis,
            totalBalance = accounts.sumOf { it.balance },
            sent = sent,
            received = received,
            fees = fees,
            periodFrom = from,
            periodTo = to,
            accounts = accounts.filter { account ->
                if (account.kindLabel != "Cash") true
                else {
                    val cashOpening = openingMap[EthiopianInstitutions.cash.id] ?: 0.0
                    account.balance != 0.0 || account.txCount > 0 || cashOpening != 0.0 ||
                        txs.any { it.institution.equals("Cash", ignoreCase = true) }
                }
            },
            recent = selectedTxs.take(20),
            unresolvedCount = unresolved,
            firstUnresolvedId = firstUnresolved,
            transactionCount = periodTxs.size,
            debitCount = debits.size,
            creditCount = credits.size,
            avgSent = if (debits.isNotEmpty()) sent / debits.size else 0.0,
            avgReceived = if (credits.isNotEmpty()) received / credits.size else 0.0,
            largestFee = periodTxs.filter { it.feeAmount > 0.0 }.maxOfOrNull { it.feeAmount } ?: 0.0,
            banksBalance = accounts.filter { it.kindLabel == "Bank" }.sumOf { it.balance },
            walletsBalance = accounts.filter { it.kindLabel == "Wallet" }.sumOf { it.balance },
            cashBalance = run {
                val cash = accounts.find { it.kindLabel == "Cash" }
                cash?.balance ?: 0.0
            },
            accountCount = accounts.count { it.kindLabel != "Cash" } +
                if (accounts.any { it.kindLabel == "Cash" && (it.balance != 0.0 || it.txCount > 0) }) 1 else 0,
        )
    }
}
