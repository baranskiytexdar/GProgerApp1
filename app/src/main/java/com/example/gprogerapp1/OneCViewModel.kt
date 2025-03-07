package com.example.gprogerapp1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OneCViewModel : ViewModel() {
    private val _operations = MutableStateFlow<List<SdelniyNaryadOperation>>(emptyList())
    val operations: StateFlow<List<SdelniyNaryadOperation>> = _operations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchOperations(ispolnitel: String) {
        _isLoading.value = true
        _error.value = null

        // Используем отдельный поток для выполнения сетевого запроса
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = OneCService()
                // При отладке можно использовать тестовые данные вместо реального запроса
                val result = service.executeQuery(ispolnitel)
                // val result = service.getTestData()
                _operations.value = result
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка при выполнении запроса: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}