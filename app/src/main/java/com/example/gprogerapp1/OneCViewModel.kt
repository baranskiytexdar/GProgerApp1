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

    // Добавляем состояние для выбранных операций
    private val _selectedOperations = MutableStateFlow<Set<String>>(emptySet())
    val selectedOperations: StateFlow<Set<String>> = _selectedOperations.asStateFlow()

    // Флаг режима выбора
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    // Функции для управления выбором
    fun toggleOperationSelection(ssylka: String) {
        val operation = _operations.value.find { it.ssylka == ssylka }

        if (operation == null) return

        val currentSelected = _selectedOperations.value.toMutableSet()
        val selectedOps = _operations.value.filter { currentSelected.contains(it.ssylka) }

        // Безопасное преобразование номера строки в число
        val currentLineNumber = operation.lineNumber.toIntOrNull() ?: Int.MAX_VALUE

        val canSelect = selectedOps.isEmpty() ||
                selectedOps.maxOfOrNull { it.lineNumber.toIntOrNull() ?: 0 }
                    ?.let { maxLineNumber ->
                        currentLineNumber > maxLineNumber
                    } ?: false

        if (canSelect) {
            if (currentSelected.contains(ssylka)) {
                currentSelected.remove(ssylka)
            } else {
                currentSelected.add(ssylka)
            }
        }

        _selectedOperations.value = currentSelected
        _selectionMode.value = currentSelected.isNotEmpty()
    }
    fun clearSelection() {
        _selectedOperations.value = emptySet()
        _selectionMode.value = false
    }

    fun showPartialCompletionDialogForSelected() {
        viewModelScope.launch {
            val selectedIds = _selectedOperations.value
            val operationsToUpdate = _operations.value.filter { selectedIds.contains(it.ssylka) }

            // Здесь вы можете реализовать логику группового или индивидуального частичного выполнения
            // Например, можно показать диалог для первой выбранной операции
            if (operationsToUpdate.isNotEmpty()) {
                // Логика вызова диалога для первой операции
                // Это должно быть реализовано в UI слое
            }
        }
    }
    // Обработка групповых действий с обновлением фактического количества на сервере
    fun markAsCompleted() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val selectedIds = _selectedOperations.value
                val operationsToUpdate = _operations.value.filter { selectedIds.contains(it.ssylka) }

                val results = operationsToUpdate.map { operation ->
                    val actualQuantity = operation.kolichestvoPlan
                    repository.setActualQuantity(
                        operation.naryadNumber,
                        operation.naryadDate,  // Добавляем дату наряда
                        operation.operationCode,
                        operation.lineNumber,
                        actualQuantity
                    )
                }

                // Проверяем результаты всех запросов
                val allSuccessful = results.all { it.isSuccess }

                if (allSuccessful) {
                    // Обновляем локальные данные
                    val updatedOperations = _operations.value.map { operation ->
                        if (selectedIds.contains(operation.ssylka)) {
                            operation.copy(kolichestvoFakt = operation.kolichestvoPlan)
                        } else {
                            operation
                        }
                    }
                    _operations.value = updatedOperations
                } else {
                    _error.value = "Не удалось обновить некоторые операции"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
            } finally {
                _isLoading.value = false
                clearSelection()
            }
        }
    }

    fun markAsPartiallyCompleted() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val selectedIds = _selectedOperations.value
                val operationsToUpdate = _operations.value.filter { selectedIds.contains(it.ssylka) }

                val results = operationsToUpdate.map { operation ->
                    val actualQuantity = operation.kolichestvoPlan
                    repository.setActualQuantity(
                        operation.naryadNumber,
                        operation.naryadDate,  // Добавляем дату наряда
                        operation.operationCode,
                        operation.lineNumber,
                        actualQuantity
                    )
                }

                val allSuccessful = results.all { it.isSuccess }

                if (allSuccessful) {
                    val updatedOperations = _operations.value.map { operation ->
                        if (selectedIds.contains(operation.ssylka)) {
                            operation.copy(kolichestvoFakt = operation.kolichestvoPlan * 0.5)
                        } else {
                            operation
                        }
                    }
                    _operations.value = updatedOperations
                } else {
                    _error.value = "Не удалось обновить некоторые операции"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
            } finally {
                _isLoading.value = false
                clearSelection()
            }
        }
    }

    fun cancelCompletion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val selectedIds = _selectedOperations.value
                val operationsToUpdate = _operations.value.filter { selectedIds.contains(it.ssylka) }

                val results = operationsToUpdate.map { operation ->
                    val actualQuantity = 0.0 //operation.kolichestvoPlan
                    repository.setActualQuantity(
                        operation.naryadNumber,
                        operation.naryadDate,  // Добавляем дату наряда
                        operation.operationCode,
                        operation.lineNumber,
                        actualQuantity
                    )
                }

                val allSuccessful = results.all { it.isSuccess }

                if (allSuccessful) {
                    val updatedOperations = _operations.value.map { operation ->
                        if (selectedIds.contains(operation.ssylka)) {
                            operation.copy(kolichestvoFakt = 0.0)
                        } else {
                            operation
                        }
                    }
                    _operations.value = updatedOperations
                } else {
                    _error.value = "Не удалось обновить некоторые операции"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
            } finally {
                _isLoading.value = false
                clearSelection()
            }
        }
    }

    // Функция для обновления фактического количества одной операции
    fun updateActualQuantity(operation: SdelniyNaryadOperation, actualQuantity: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val result = repository.setActualQuantity(
                    operation.naryadNumber,
                    operation.naryadDate,  // Добавляем дату наряда
                    operation.operationCode,
                    operation.lineNumber,
                    actualQuantity
                )

                result.fold(
                    onSuccess = {
                        val updatedOperations = _operations.value.map {
                            if (it.ssylka == operation.ssylka) {
                                it.copy(kolichestvoFakt = actualQuantity)
                            } else {
                                it
                            }
                        }
                        _operations.value = updatedOperations
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Не удалось обновить данные"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // Функция для загрузки данных
    fun fetchOperations(date: String, performer: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = repository.getOperations(date, performer)

                result.fold(
                    onSuccess = { operations ->
                        if (operations.isEmpty()) {
                            _error.value = "Нет данных для выбранного исполнителя"
                            _operations.value = emptyList()
                        } else {
                            _operations.value = operations
                        }
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Неизвестная ошибка"
                        _operations.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
                _operations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun startExecution() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val selectedIds = _selectedOperations.value
                val operationsToUpdate = _operations.value.filter { selectedIds.contains(it.ssylka) }

                val results = operationsToUpdate.map { operation ->
                    val actualQuantity = 0.001 // Минимальное значение для начала выполнения
                    repository.setActualQuantity(
                        operation.naryadNumber,
                        operation.naryadDate,
                        operation.operationCode,
                        operation.lineNumber,
                        actualQuantity
                    )
                }

                val allSuccessful = results.all { it.isSuccess }

                if (allSuccessful) {
                    val updatedOperations = _operations.value.map { operation ->
                        if (selectedIds.contains(operation.ssylka)) {
                            operation.copy(kolichestvoFakt = 0.001)
                        } else {
                            operation
                        }
                    }
                    _operations.value = updatedOperations
                } else {
                    _error.value = "Не удалось начать выполнение некоторых операций"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при начале выполнения: ${e.message}"
            } finally {
                _isLoading.value = false
                clearSelection()
            }
        }
    }

    fun markSpecificPartialCompletion(operation: SdelniyNaryadOperation, actualQuantity: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val result = repository.setActualQuantity(
                    operation.naryadNumber,
                    operation.naryadDate,
                    operation.operationCode,
                    operation.lineNumber,
                    actualQuantity
                )

                result.fold(
                    onSuccess = {
                        val updatedOperations = _operations.value.map {
                            if (it.ssylka == operation.ssylka) {
                                it.copy(kolichestvoFakt = actualQuantity)
                            } else {
                                it
                            }
                        }
                        _operations.value = updatedOperations
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Не удалось обновить данные"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
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