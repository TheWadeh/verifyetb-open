package com.verifyetb.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.SmsInboxImporter
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.entity.AccountBalanceEntity
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.worker.SmsProcessingWorker
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val importer: SmsInboxImporter,
    private val settingsRepository: SettingsRepository,
    private val rawSmsDao: RawSmsDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stagedCount: StateFlow<Int> = rawSmsDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val selectedInstitutionIds: StateFlow<Set<String>> = settingsRepository.selectedInstitutionIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val accountBalances: StateFlow<List<AccountBalanceEntity>> = repository.observeAccountBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        viewModelScope.launch { repository.ensureDefaultCategories() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { repository.addCategory(name) }
    }

    fun removeCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun toggleInstitution(id: String) {
        if (id == EthiopianInstitutions.cash.id) return
        viewModelScope.launch {
            val current = settingsRepository.getSelectedInstitutionIds()
            val next = if (id in current) current - id else current + id
            settingsRepository.setSelectedInstitutionIds(next)
            settingsRepository.setAccountsPicked(true)
            SmsProcessingWorker.enqueue(context)
        }
    }

    fun setInstitutions(ids: Set<String>) {
        viewModelScope.launch {
            settingsRepository.setSelectedInstitutionIds(ids)
            settingsRepository.setAccountsPicked(true)
            SmsProcessingWorker.enqueue(context)
        }
    }

    fun saveOpeningBalance(institutionId: String, amount: Double) {
        viewModelScope.launch {
            repository.upsertOpeningBalance(institutionId, amount)
            _statusMessage.value = "Opening balance saved."
        }
    }

    fun scanInbox() {
        viewModelScope.launch {
            _busy.value = true
            _statusMessage.value = "Reading your inbox…"
            val inserted = importer.syncNew()
            _statusMessage.value = if (inserted == 0) {
                "No new bank SMS."
            } else {
                "Read $inserted SMS from your inbox."
            }
            _busy.value = false
        }
    }

    fun reprocessPending() {
        viewModelScope.launch {
            _busy.value = true
            if (!settingsRepository.balancesSet.first()) {
                _statusMessage.value = "Finish setup first, then SMS can be read."
                _busy.value = false
                return@launch
            }
            rawSmsDao.getByStatus(RawSms.STATUS_FAILED).forEach {
                rawSmsDao.updateStatus(it.id, RawSms.STATUS_PENDING)
            }
            rawSmsDao.getByStatus(RawSms.STATUS_SKIPPED).forEach {
                val reason = it.errorMessage.orEmpty()
                if (reason.contains("Cash is manual-only")) return@forEach
                rawSmsDao.updateStatus(it.id, RawSms.STATUS_PENDING)
            }
            importer.syncNew()
            _statusMessage.value = "Reprocessed your inbox."
            _busy.value = false
        }
    }
}
