package com.example.gprogerapp1.api

import com.example.gprogerapp1.SdelniyNaryadOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.regex.Pattern

class OneCRepository {
    private val api = RetrofitClient.oneCService

    suspend fun getOperations(date: String, performer: String): Result<List<SdelniyNaryadOperation>> {
        return withContext(Dispatchers.IO) {
            try {
                println("ОТЛАДКА: Начинаем выполнение запроса с параметрами: date=$date, performer=$performer")

                val soapEnvelope = RetrofitClient.createSoapEnvelope(date, performer)
                val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

                // Отправляем запрос
                val response = api.getDataRaw(requestBody)

                println("Код ответа: ${response.code()}")
                println("Сообщение: ${response.message()}")

                if (response.isSuccessful) {
                    // Получаем тело ответа как строку
                    val responseBody = response.body()?.string() ?: ""
                    println("Длина ответа: ${responseBody.length} символов")

                    // Парсим XML с помощью регулярных выражений
                    val operations = parseWithRegex(responseBody)
                    println("ОТЛАДКА: Найдено операций с помощью регулярных выражений: ${operations.size}")

                    if (operations.isNotEmpty()) {
                        println("ОТЛАДКА: Пример первой операции: ${operations[0]}")
                    }

                    Result.success(operations)
                } else {
                    // Попытаемся получить тело ответа с ошибкой для дополнительной диагностики
                    val errorBody = response.errorBody()?.string() ?: ""
                    println("Тело ошибки: $errorBody")

                    Result.failure(Exception("Ошибка при получении данных: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                println("ОТЛАДКА: Поймано исключение: ${e.message}")
                e.printStackTrace()
                Result.failure(Exception("Ошибка связи с сервером: ${e.message}"))
            }
        }
    }

    // Парсинг XML с помощью регулярных выражений
    private fun parseWithRegex(xmlResponse: String): List<SdelniyNaryadOperation> {
        val operations = mutableListOf<SdelniyNaryadOperation>()

        try {
            println("ОТЛАДКА: Начинаем парсинг XML с помощью регулярных выражений")

            // Паттерн для поиска блоков <m:Операции>...</m:Операции>
            val operationPattern = Pattern.compile("<m:Операции>.*?</m:Операции>", Pattern.DOTALL)
            val matcher = operationPattern.matcher(xmlResponse)

            var count = 0

            while (matcher.find()) {
                count++
                val operationXml = matcher.group(0)
                println("ОТЛАДКА: Найден блок операции #$count")

                // Извлекаем значения полей
                val operacionKod = extractValue(operationXml, "m:ОперацияКод")
                val zakazPokupatelya = extractValue(operationXml, "m:ЗаказПокупателяНомер")
                val operacionName = extractValue(operationXml, "m:ОперацияНаименование")
                val kolPlan = extractValue(operationXml, "m:КоличествоПлан").toDoubleOrNull() ?: 0.0
                val normVremeni = extractValue(operationXml, "m:НормаВремени").toDoubleOrNull() ?: 0.0
                val rascenka = extractValue(operationXml, "m:Расценка").toDoubleOrNull() ?: 0.0

                println("ОТЛАДКА: Операция $operacionKod, План: $kolPlan, Норма: $normVremeni, Расценка: $rascenka")

                // Создаем объект операции
                val operation = SdelniyNaryadOperation(
                    ssylka = operacionKod,
                    zakazPokupatelya = zakazPokupatelya,
                    nomenklatura = "",  // Нет в ответе
                    operaciya = operacionName,
                    kolichestvoPlan = kolPlan,
                    normaVremeni = normVremeni,
                    rascenka = rascenka
                )

                operations.add(operation)
                println("ОТЛАДКА: Добавлена операция #$count: $operation")
            }

            println("ОТЛАДКА: Всего найдено операций: ${operations.size}")
        } catch (e: Exception) {
            println("Ошибка при парсинге XML с регулярными выражениями: ${e.message}")
            e.printStackTrace()
        }

        return operations
    }

    // Вспомогательная функция для извлечения значения из тега
    private fun extractValue(xml: String, tagName: String): String {
        val pattern = Pattern.compile("<$tagName>(.*?)</$tagName>", Pattern.DOTALL)
        val matcher = pattern.matcher(xml)

        return if (matcher.find()) {
            matcher.group(1)?.trim() ?: ""
        } else {
            ""
        }
    }
}