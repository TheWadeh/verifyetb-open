package com.verifyetb.app.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle["id"])

    val transaction: StateFlow<TransactionEntity?> = repository.observeById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveNote(reason: String, category: String?) {
        viewModelScope.launch {
            repository.updateNote(id, reason, category)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
