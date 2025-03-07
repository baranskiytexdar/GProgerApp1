package com.example.gprogerapp1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gprogerapp1.api.OneCRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OneCViewModel : ViewModel() {
    private val repository = OneCRepository()

    // Состояние для списка операций
    private val _operations = MutableStateFlow<List<SdelniyNaryadOperation>>(emptyList())
    val operations: StateFlow<List<SdelniyNaryadOperation>> = _operations.asStateFlow()

    // Состояние загрузки данных
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Состояние для ошибок
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Функция для загрузки данных
    fun fetchOperations(date: String, performer: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = repository.getOperations(date, performer)

                result.fold(
                    onSuccess = { operations ->
                        _operations.value = operations
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Неизвестная ошибка"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Вспомогательные функции для управления состоянием
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun updateOperations(newOperations: List<SdelniyNaryadOperation>) {
        _operations.value = newOperations
    }

    fun setError(errorMessage: String) {
        _error.value = errorMessage
    }

    fun clearError() {
        _error.value = null
    }
}