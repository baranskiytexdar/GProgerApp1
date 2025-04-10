package com.example.gprogerapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gprogerapp1.ui.theme.GProgerApp1Theme
import android.app.DatePickerDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GProgerApp1Theme {
                // Получаем экземпляр AuthViewModel для работы с авторизацией
                val authViewModel: AuthViewModel = viewModel()
                // Отслеживаем состояние авторизации
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Проверяем, авторизован ли пользователь
                    if (isLoggedIn) {
                        // Если авторизован, показываем основной экран с возможностью выхода
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel(),
                            onLogout = { authViewModel.logout() }
                        )
                    } else {
                        // Если не авторизован, показываем экран входа
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: OneCViewModel = viewModel(),
    onLogout: () -> Unit
) {
    // Существующие состояния
    var selectedDate by remember { mutableStateOf(LocalDate.of(2025, 2, 18)) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val formattedDate = selectedDate.format(dateFormatter)
    val apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val apiFormattedDate = selectedDate.format(apiDateFormatter)
    val operations by viewModel.operations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var ispolnitel by remember { mutableStateOf("Дудникова  Наталья Александровна") }
    val context = LocalContext.current
    var showResults by remember { mutableStateOf(false) }

    // Получаем состояние выделения
    val selectedOperations by viewModel.selectedOperations.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()

    // Состояние для диалога частичного выполнения
    var selectedOperationForPartialCompletion by remember { mutableStateOf<SdelniyNaryadOperation?>(null) }

    // Диалог для частичного выполнения
    selectedOperationForPartialCompletion?.let { operation ->
        PartialCompletionDialog(
            operation = operation,
            onDismiss = { selectedOperationForPartialCompletion = null },
            onConfirm = { quantity ->
                viewModel.markSpecificPartialCompletion(operation, quantity)
                selectedOperationForPartialCompletion = null
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Строка с заголовком и кнопкой выхода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сдельные наряды",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onLogout,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Выйти")
                }
            }

            // Поле ввода даты и исполнителя
            OutlinedTextField(
                value = formattedDate,
                onValueChange = { /* Мы не обрабатываем ручной ввод */ },
                readOnly = true,
                label = { Text("Выберите дату") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        )
                        datePickerDialog.show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Выбрать дату"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = ispolnitel,
                onValueChange = { ispolnitel = it },
                label = { Text("Исполнитель") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Отображение результатов запроса
            if (showResults) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                        Text("Загрузка данных...")
                    }
                    error != null -> {
                        Text(
                            text = error ?: "Неизвестная ошибка",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    operations.isEmpty() -> {
                        Text(
                            text = "Нет данных для отображения",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        // Панель групповых действий
                        if (selectionMode) {
                            GroupActionPanel(
                                selectedCount = selectedOperations.size,
                                isLoading = isLoading,
                                onMarkCompleted = { viewModel.markAsCompleted() },
                                onShowPartialCompletionDialog = {
                                    // Логика вызова диалога для первой выбранной операции
                                    val selectedOperations = viewModel.operations.value
                                        .filter { viewModel.selectedOperations.value.contains(it.ssylka) }

                                    if (selectedOperations.isNotEmpty()) {
                                        selectedOperationForPartialCompletion = selectedOperations.first()
                                    }
                                },
                                onCancelSelection = { viewModel.clearSelection() },
                                onCancelCompletion = { viewModel.cancelCompletion() },
                                onStartExecution = { viewModel.startExecution() }
                            )
                        }

                        Text(
                            text = "Результаты запроса:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Список операций с поддержкой выбора
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(operations) { operation ->
                                val isSelected = selectedOperations.contains(operation.ssylka)
                                OperationItem(
                                    operation = operation,
                                    isSelected = isSelected,
                                    onToggleSelection = { viewModel.toggleOperationSelection(it) },
                                    onPartialCompletion = {
                                        selectedOperationForPartialCompletion = it
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }

        // Кнопка для выполнения запроса, в правом нижнем углу
        if (!selectionMode) {
            Button(
                onClick = {
                    viewModel.fetchOperations(
                        date = apiFormattedDate,
                        performer = ispolnitel
                    )
                    showResults = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("Получить задание")
            }
        }

        if (!selectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.fetchOperations(
                            date = apiFormattedDate,
                            performer = ispolnitel
                        )
                        showResults = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Получить данные")
                }

                Button(
                    onClick = {
                        viewModel.fetchStatus(
                            date = apiFormattedDate,
                            performer = ispolnitel
                        )
                        showResults = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Статус")
                }
            }
        }

    }
}

@Composable
fun GroupActionPanel(
    selectedCount: Int,
    isLoading: Boolean,
    onMarkCompleted: () -> Unit,
    onShowPartialCompletionDialog: () -> Unit, // Новый параметр
    onCancelSelection: () -> Unit,
    onCancelCompletion: () -> Unit,
    onStartExecution: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбрано операций: $selectedCount",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartExecution,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Начать выполнение")
                }

                Button(
                    onClick = onMarkCompleted,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Выполнено")
                }

                Button(
                    onClick = onShowPartialCompletionDialog,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Частично")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelSelection,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Отмена выделения")
                }

                OutlinedButton(
                    onClick = onCancelCompletion,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Отмена выполнения")
                }
            }
        }
    }
}

@Composable
fun OperationItem(
    operation: SdelniyNaryadOperation,
    isSelected: Boolean,
    onToggleSelection: (String) -> Unit,
    onPartialCompletion: (SdelniyNaryadOperation) -> Unit
) {
    // Определяем цвет карточки на основе состояния
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.surfaceVariant // Цвет для выделенной карточки
        !operation.isAvailableForExecution -> MaterialTheme.colorScheme.errorContainer // Красный для недоступных операций
        operation.kolichestvoFakt == 0.0 -> MaterialTheme.colorScheme.errorContainer // Красный для факта = 0
        operation.kolichestvoFakt == operation.kolichestvoPlan -> MaterialTheme.colorScheme.primaryContainer // Зеленый для факта = плану
        else -> Color(0xFFFFF9C4) // Желтый для факта != плану и факта != 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = if (operation.isAvailableForExecution) 1f else 0.5f)
            .clickable(operation.isAvailableForExecution) {
                onToggleSelection(operation.ssylka)
            },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Чекбокс и кнопка "Частично" для выбранных операций
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onToggleSelection(operation.ssylka) },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Button(
                        onClick = { onPartialCompletion(operation) },
                        modifier = Modifier.height(40.dp),
                        enabled = operation.isAvailableForExecution,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Частично", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Содержимое карточки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Заголовок с операцией и номером/датой наряда
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = operation.operaciya,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (operation.isAvailableForExecution)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )

                    if (operation.naryadNumber.isNotEmpty()) {
                        Text(
                            text = "Наряд: ${operation.naryadNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Дата наряда
                if (operation.naryadDate.isNotEmpty()) {
                    Text(
                        text = "от ${operation.naryadDate}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Информация о коде операции и номере строки
                Row {
                    if (operation.operationCode.isNotEmpty()) {
                        Text(
                            text = "Код операции: ${operation.operationCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (operation.lineNumber.isNotEmpty()) {
                        Text(
                            text = "Строка №${operation.lineNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Предупреждение о недоступности операции
                if (!operation.isAvailableForExecution) {
                    Text(
                        text = "Операция недоступна. Выполните предыдущие операции заказа.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Код операции и заказ покупателя
                Row {
                    Text(
                        text = "ID: ${operation.ssylka}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (operation.zakazPokupatelya.isNotEmpty()) {
                        Text(
                            text = "Заказ: ${operation.zakazPokupatelya}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Показываем информацию о плане, факте, норме и расценке в строку
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "План: ${operation.kolichestvoPlan}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Факт: ${operation.kolichestvoFakt}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        // Выделяем факт жирным, если он отличается от плана
                        fontWeight = if (operation.kolichestvoFakt != operation.kolichestvoPlan)
                            FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Добавляем вторую строку для нормы времени и расценки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Норма времени: ${operation.normaVremeni}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Расценка: ${operation.rascenka} ₽",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PartialCompletionDialog(
    operation: SdelniyNaryadOperation,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantity by remember { mutableStateOf(operation.kolichestvoFakt.toString()) }
    val maxQuantity = operation.kolichestvoPlan

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Частичное выполнение") },
        text = {
            Column {
                Text("Введите фактическое количество (от 0.001 до ${maxQuantity})")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        // Разрешаем только цифры и точку
                        quantity = it.replace(',', '.').filter { char ->
                            char.isDigit() || char == '.'
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text("Количество") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityValue = quantity.toDoubleOrNull() ?: 0.001
                    val validQuantity = quantityValue.coerceIn(0.001, maxQuantity)
                    onConfirm(validQuantity)
                },
                enabled = quantity.isNotEmpty() &&
                        quantity.toDoubleOrNull()?.let {
                            it in 0.001..maxQuantity
                        } ?: false
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}