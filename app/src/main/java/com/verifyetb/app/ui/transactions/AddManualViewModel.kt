package com.verifyetb.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.parser.EthiopianInstitution
import com.verifyetb.app.parser.EthiopianInstitutions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddManualViewModel @Inject constructor(
    private val repository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<EthiopianInstitution>> = settingsRepository.selectedInstitutionIds
        .map { ids ->
            val selected = EthiopianInstitutions.selectable.filter { it.id in ids }
            if (selected.isEmpty()) listOf(EthiopianInstitutions.cash) else selected
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf(EthiopianInstitutions.cash),
        )

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch { repository.ensureDefaultCategories() }
    }

    fun clearError() {
        _error.value = null
    }

    fun save(
        type: String,
        amountText: String,
        institution: String,
        category: String?,
        reason: String?,
        feeText: String,
    ) {
        if (_saving.value) return
        val amount = amountText.replace(",", "").trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _error.value = "Enter a valid amount."
            return
        }
        if (institution.isBlank()) {
            _error.value = "Choose an account."
            return
        }
        if (type != TransactionEntity.TYPE_DEBIT && type != TransactionEntity.TYPE_CREDIT) {
            _error.value = "Choose Send or Receive."
            return
        }
        val feeAmount = when (type) {
            TransactionEntity.TYPE_DEBIT -> feeText.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        if (feeAmount < 0.0) {
            _error.value = "Fee cannot be negative."
            return
        }

        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                val id = repository.addManual(
                    type = type,
                    amount = amount,
                    institution = institution.trim(),
                    counterparty = null,
                    reason = reason?.trim()?.ifBlank { null },
                    category = category?.trim()?.ifBlank { null },
                    date = System.currentTimeMillis(),
                    feeAmount = feeAmount,
                )
                if (id <= 0L) {
                    _error.value = "Could not save — try again."
                } else {
                    _saved.value = true
                }
            } catch (t: Throwable) {
                _error.value = t.message?.takeIf { it.isNotBlank() } ?: "Could not save transaction."
            } finally {
                _saving.value = false
            }
        }
    }
}
