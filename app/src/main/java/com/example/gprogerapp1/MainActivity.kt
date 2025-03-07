package com.example.gprogerapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.padding

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

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gprogerapp1.ui.theme.GProgerApp1Theme
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

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

// Модель данных для результатов запроса
data class SdelniyNaryadOperation(
    val ssylka: String,
    val zakazPokupatelya: String,
    val nomenklatura: String,
    val operaciya: String,
    val kolichestvoPlan: Double,
    val normaVremeni: Double,
    val rascenka: Double
)

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: OneCViewModel = viewModel()) {
    // Получаем текущую дату
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val formattedDate = selectedDate.format(dateFormatter)

    // Состояния для отслеживания данных
    val operations by viewModel.operations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Исполнитель (в реальном приложении должен быть выбор пользователя)
    var ispolnitel by remember { mutableStateOf("Текущий пользователь") }

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
                viewModel.fetchOperations(ispolnitel)
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
            Text(text = "Номенклатура: ${operation.nomenklatura}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Операция: ${operation.operaciya}")
            Text(text = "Заказ покупателя: ${operation.zakazPokupatelya}")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = "План: ${operation.kolichestvoPlan}", modifier = Modifier.weight(1f))
                Text(text = "Норма: ${operation.normaVremeni}", modifier = Modifier.weight(1f))
                Text(text = "Расценка: ${operation.rascenka} ₽", modifier = Modifier.weight(1f))
            }
        }
    }
}