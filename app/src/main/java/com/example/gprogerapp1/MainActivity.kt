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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gprogerapp1.ui.theme.GProgerApp1Theme
import android.app.DatePickerDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GProgerApp1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: OneCViewModel = viewModel()) {
    // Получаем дату из требований (2025-02-18)
    var selectedDate by remember { mutableStateOf(LocalDate.of(2025, 2, 18)) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val formattedDate = selectedDate.format(dateFormatter)

    // Формат даты для API (yyyy-MM-dd)
    val apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val apiFormattedDate = selectedDate.format(apiDateFormatter)

    // Состояния для отслеживания данных
    val operations by viewModel.operations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Исполнитель из требований
    var ispolnitel by remember { mutableStateOf("Дудникова  Наталья Александровна") }

    // Контекст для диалога выбора даты
    val context = LocalContext.current

    // Состояние для отображения данных
    var showResults by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Сдельные наряды",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Поле ввода даты с иконкой календаря
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

            // Поле для ввода исполнителя
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
                        Text(
                            text = "Результаты запроса:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(operations) { operation ->
                                OperationItem(operation)
                                Divider()
                            }
                        }
                    }
                }
            }
        }

        // Кнопка для выполнения запроса, в правом нижнем углу
        Button(
            onClick = {
                showResults = true
                // Вызываем метод загрузки данных из ViewModel
                viewModel.fetchOperations(apiFormattedDate, ispolnitel)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Получить данные")
        }
    }
}

@Composable
fun OperationItem(operation: SdelniyNaryadOperation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Выделяем операцию крупным шрифтом
            Text(
                text = operation.operaciya,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Код операции и заказ покупателя более мелким шрифтом
            Row {
                Text(
                    text = "Код: ${operation.ssylka}",
                    style = MaterialTheme.typography.bodyMedium,
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

            // Показываем информацию о плане, норме и расценке в строку
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