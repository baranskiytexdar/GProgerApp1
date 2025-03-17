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
                    modifier = Modifier
                        //.align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text("Выйти")
                }
            }

            // Поле ввода даты и исполнителя (существующий код)
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
                        // Показываем диалог выбора даты при нажатии на иконку
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
                                onMarkPartiallyCompleted = { viewModel.markAsPartiallyCompleted() },
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
                                    onToggleSelection = { viewModel.toggleOperationSelection(it) }
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
                Text("Получить данные")
            }
        }
    }
}
@Composable
fun GroupActionPanel(
    selectedCount: Int,
    isLoading: Boolean,
    onMarkCompleted: () -> Unit,
    onMarkPartiallyCompleted: () -> Unit,
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
                    onClick = onStartExecution, // Новая кнопка
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
                    onClick = onMarkPartiallyCompleted,
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
    onToggleSelection: (String) -> Unit
) {
    // Определяем цвет карточки на основе соотношения плана и факта и выделения
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.surfaceVariant // Цвет для выделенной карточки
        operation.kolichestvoFakt == 0.0 -> MaterialTheme.colorScheme.errorContainer // Красный для факта = 0
        operation.kolichestvoFakt == operation.kolichestvoPlan -> MaterialTheme.colorScheme.primaryContainer // Зеленый для факта = плану
        else -> Color(0xFFFFF9C4) // Желтый для факта != плану и факта != 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggleSelection(operation.ssylka) },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Чекбокс для выбора
            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onToggleSelection(operation.ssylka) },
                    modifier = Modifier.padding(start = 8.dp)
                )
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
                        color = MaterialTheme.colorScheme.primary,
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