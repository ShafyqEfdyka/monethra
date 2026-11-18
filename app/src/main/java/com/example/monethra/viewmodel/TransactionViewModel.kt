package com.example.monethra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.utils.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionUiState(
    val searchQuery: String = "",
    val typeFilter: String = "ALL", // "ALL", "INCOME", "EXPENSE"
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val transactionToDelete: TransactionEntity? = null,
    val isAddEditSheetOpen: Boolean = false,
    val isDeleteConfirmationOpen: Boolean = false,
    val currencySymbol: String = "Rp",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

private data class FilterState(
    val query: String,
    val filter: String,
    val selectedTx: TransactionEntity?,
    val txToDelete: TransactionEntity?,
    val isAddEditOpen: Boolean,
    val isDeleteOpen: Boolean,
    val errorMsg: String?,
    val saveSuccess: Boolean
)

class TransactionViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow("ALL")
    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _transactionToDelete = MutableStateFlow<TransactionEntity?>(null)
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _isDeleteConfirmationOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _saveSuccess = MutableStateFlow(false)

    private val _filterState = combine(
        _searchQuery,
        _typeFilter,
        _selectedTransaction,
        _transactionToDelete,
        _isAddEditSheetOpen
    ) { query, filter, selectedTx, txToDelete, addEditOpen ->
        Tuple5(query, filter, selectedTx, txToDelete, addEditOpen)
    }.combine(_isDeleteConfirmationOpen) { tuple, deleteOpen ->
        Tuple6(tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, deleteOpen)
    }.combine(_errorMessage) { tuple, errorMsg ->
        Tuple7(tuple.v1, tuple.v2, tuple.v3, tuple.v4, tuple.v5, tuple.v6, errorMsg)
    }.combine(_saveSuccess) { tuple, success ->
        FilterState(
            query = tuple.v1,
            filter = tuple.v2,
            selectedTx = tuple.v3,
            txToDelete = tuple.v4,
            isAddEditOpen = tuple.v5,
            isDeleteOpen = tuple.v6,
            errorMsg = tuple.v7,
            saveSuccess = success
        )
    }

    val uiState: StateFlow<TransactionUiState> = combine(
        repository.allTransactions,
        repository.currencySymbol,
        _filterState
    ) { allTx, symbol, filterState ->
        val filtered = allTx.filter { tx ->
            val typeMatches = when (filterState.filter.uppercase()) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE"
                else -> true
            }

            val queryMatches = if (filterState.query.isBlank()) {
                true
            } else {
                val q = filterState.query.trim().lowercase()
                tx.title.lowercase().contains(q) ||
                        tx.category.lowercase().contains(q) ||
                        (tx.notes?.lowercase()?.contains(q) == true)
            }

            typeMatches && queryMatches
        }.sortedByDescending { it.date }

        TransactionUiState(
            searchQuery = filterState.query,
            typeFilter = filterState.filter,
            transactions = allTx,
            filteredTransactions = filtered,
            selectedTransaction = filterState.selectedTx,
            transactionToDelete = filterState.txToDelete,
            isAddEditSheetOpen = filterState.isAddEditOpen,
            isDeleteConfirmationOpen = filterState.isDeleteOpen,
            currencySymbol = symbol,
            isLoading = false,
            errorMessage = filterState.errorMsg,
            saveSuccess = filterState.saveSuccess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(type: String) {
        _typeFilter.value = type
    }

    fun selectTransaction(transaction: TransactionEntity?) {
        _selectedTransaction.value = transaction
    }

    fun openAddTransactionSheet(preselectedType: String = "EXPENSE") {
        _selectedTransaction.value = null
        _errorMessage.value = null
        _saveSuccess.value = false
        if (preselectedType in listOf("INCOME", "EXPENSE")) {
            _typeFilter.value = preselectedType
        }
        _isAddEditSheetOpen.value = true
    }

    fun openEditTransactionSheet(transaction: TransactionEntity) {
        _selectedTransaction.value = transaction
        _errorMessage.value = null
        _saveSuccess.value = false
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _selectedTransaction.value = null
        _errorMessage.value = null
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun openDeleteConfirmation(transaction: TransactionEntity) {
        _transactionToDelete.value = transaction
        _isDeleteConfirmationOpen.value = true
    }

    fun closeDeleteConfirmation() {
        _isDeleteConfirmationOpen.value = false
        _transactionToDelete.value = null
    }

    fun validateTransaction(title: String, amountText: String, category: String): String? {
        if (title.isBlank()) {
            return "Title is required"
        }
        val amount = CurrencyFormatter.parseAmount(amountText)
        if (amount == null || amount <= 0) {
            return "Amount must be greater than 0"
        }
        if (category.isBlank()) {
            return "Category selection is required"
        }
        return null
    }

    fun addTransaction(
        title: String,
        amount: Long,
        type: String,
        category: String,
        date: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            val validationError = validateTransaction(title, amount.toString(), category)
            if (validationError != null) {
                _errorMessage.value = validationError
                return@launch
            }

            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type.uppercase(),
                category = category.trim(),
                date = date,
                notes = notes?.trim().takeIf { !it.isNullOrBlank() }
            )
            repository.insertTransaction(entity)
            _saveSuccess.value = true
            closeAddEditSheet()
        }
    }

    fun updateTransaction(
        id: Long,
        title: String,
        amount: Long,
        type: String,
        category: String,
        date: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            val validationError = validateTransaction(title, amount.toString(), category)
            if (validationError != null) {
                _errorMessage.value = validationError
                return@launch
            }

            val existingTx = _selectedTransaction.value
            val currentUserId = repository.activeUserId.first()
            val entity = if (existingTx != null && existingTx.id == id) {
                existingTx.copy(
                    title = title.trim(),
                    amount = amount,
                    type = type.uppercase(),
                    category = category.trim(),
                    date = date,
                    notes = notes?.trim().takeIf { !it.isNullOrBlank() },
                    userId = if (existingTx.userId > 0L) existingTx.userId else currentUserId
                )
            } else {
                TransactionEntity(
                    id = id,
                    title = title.trim(),
                    amount = amount,
                    type = type.uppercase(),
                    category = category.trim(),
                    date = date,
                    notes = notes?.trim().takeIf { !it.isNullOrBlank() },
                    userId = currentUserId
                )
            }
            repository.updateTransaction(entity)
            _saveSuccess.value = true
            closeAddEditSheet()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val currentUserId = repository.activeUserId.first()
            val entityToUpdate = if (transaction.userId == 0L && currentUserId > 0L) {
                transaction.copy(userId = currentUserId)
            } else {
                transaction
            }
            repository.updateTransaction(entityToUpdate)
            _saveSuccess.value = true
            closeAddEditSheet()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            closeDeleteConfirmation()
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            closeDeleteConfirmation()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

private data class Tuple5<A, B, C, D, E>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E)
private data class Tuple6<A, B, C, D, E, F>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F)
private data class Tuple7<A, B, C, D, E, F, G>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E, val v6: F, val v7: G)

class TransactionViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
