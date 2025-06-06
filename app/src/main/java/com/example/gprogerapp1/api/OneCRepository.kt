package com.example.gprogerapp1.api

import com.example.gprogerapp1.SdelniyNaryadOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.regex.Pattern
import android.util.Log


// OneCRepository.kt
class OneCRepository {
    private val api = RetrofitClient.oneCService
    suspend fun getStatus(date: String, performer: String): Result<List<SdelniyNaryadOperation>> {
        return withContext(Dispatchers.IO) {
            try {
                val soapEnvelope = RetrofitClient.createStatusSoapEnvelope(date, performer)
                val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

                val response = api.getDataRaw(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("OneCRepository", "Полный ответ: $responseBody")

                    val operations = parseXmlResponseForStatus(responseBody)

                    if (operations.isEmpty()) {
                        Log.d("OneCRepository", "Операции не найдены")
                        Result.failure(Exception("Нет данных для выбранного исполнителя"))
                    } else {
                        Log.d("OneCRepository", "Найдено операций: ${operations.size}")
                        Result.success(operations)
                    }
                } else {
                    Log.e("OneCRepository", "Ошибка запроса: ${response.code()} ${response.message()}")
                    Result.failure(Exception("Ошибка получения данных: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("OneCRepository", "Исключение при получении данных", e)
                Result.failure(Exception("Ошибка связи с сервером: ${e.message}"))
            }
        }
    }

    private fun parseXmlResponseForStatus(xmlResponse: String): List<SdelniyNaryadOperation> {
        val operations = mutableListOf<SdelniyNaryadOperation>()

        try {
            val operationPattern = Pattern.compile("<m:Операции[^>]*>(.*?)</m:Операции>", Pattern.DOTALL)
            val matcher = operationPattern.matcher(xmlResponse)

            while (matcher.find()) {
                val operationXml = matcher.group(1) ?: continue

                val operation = SdelniyNaryadOperation(
                    ssylka = extractValue(operationXml, "m:ОперацияКод") ?: "",
                    zakazPokupatelya = extractValue(operationXml, "m:ЗаказПокупателяНомер") ?: "",
                    nomenklatura = extractValue(operationXml, "m:Номенклатура") ?: "",
                    operaciya = extractValue(operationXml, "m:ОперацияНаименование") ?: "",
                    kolichestvoPlan = extractValue(operationXml, "m:КоличествоПлан")?.toDoubleOrNull() ?: 0.0,
                    kolichestvoFakt = extractValue(operationXml, "m:КоличествоФакт")?.toDoubleOrNull() ?: 0.0,
                    normaVremeni = extractValue(operationXml, "m:НормаВремени")?.toDoubleOrNull() ?: 0.0,
                    rascenka = extractValue(operationXml, "m:Расценка")?.toDoubleOrNull() ?: 0.0,
                    naryadNumber = extractValue(operationXml, "m:Номер") ?: "",
                    naryadDate = extractValue(operationXml, "m:Дата") ?: "",
                    lineNumber = extractValue(operationXml, "m:НомерСтроки") ?: "",
                    operationCode = extractValue(operationXml, "m:ОперацияКод") ?: "",
                    isAvailableForExecution = true
                )

                operations.add(operation)
            }

            Log.d("OneCRepository", "Распарсено операций: ${operations.size}")
        } catch (e: Exception) {
            Log.e("OneCRepository", "Ошибка парсинга XML", e)
        }

        return operations
    }
    suspend fun getOperations(date: String, performer: String): Result<List<SdelniyNaryadOperation>> {
        return withContext(Dispatchers.IO) {
            try {
                val soapEnvelope = RetrofitClient.createSoapEnvelope(date, performer)
                val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

                val response = api.getDataRaw(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("OneCRepository", "Полный ответ: $responseBody")

                    val operations = parseXmlResponse(responseBody)

                    if (operations.isEmpty()) {
                        Log.d("OneCRepository", "Операции не найдены")
                        Result.failure(Exception("Нет данных для выбранного исполнителя"))
                    } else {
                        Log.d("OneCRepository", "Найдено операций: ${operations.size}")
                        Result.success(operations)
                    }
                } else {
                    Log.e("OneCRepository", "Ошибка запроса: ${response.code()} ${response.message()}")
                    Result.failure(Exception("Ошибка получения данных: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("OneCRepository", "Исключение при получении данных", e)
                Result.failure(Exception("Ошибка связи с сервером: ${e.message}"))
            }
        }
    }

    private fun parseXmlResponse(xmlResponse: String): List<SdelniyNaryadOperation> {
        val operations = mutableListOf<SdelniyNaryadOperation>()
        val operationsByOrder = mutableMapOf<String, MutableList<SdelniyNaryadOperation>>()

        try {
            // Паттерн для поиска блоков <m:Операции>
            val operationPattern = Pattern.compile("<m:Операции[^>]*>(.*?)</m:Операции>", Pattern.DOTALL)
            val matcher = operationPattern.matcher(xmlResponse)

            while (matcher.find()) {
                val operationXml = matcher.group(1) ?: continue

                // Извлечение значений с более гибким подходом
                val operation = SdelniyNaryadOperation(
                    ssylka = extractValue(operationXml, "m:ОперацияКод") ?: "",
                    zakazPokupatelya = extractValue(operationXml, "m:ЗаказПокупателяНомер") ?: "",
                    nomenklatura = extractValue(operationXml, "m:Номенклатура") ?: "",
                    operaciya = extractValue(operationXml, "m:ОперацияНаименование") ?: "",
                    kolichestvoPlan = extractValue(operationXml, "m:КоличествоПлан")?.toDoubleOrNull() ?: 0.0,
                    kolichestvoFakt = extractValue(operationXml, "m:КоличествоФакт")?.toDoubleOrNull() ?: 0.0,
                    normaVremeni = extractValue(operationXml, "m:НормаВремени")?.toDoubleOrNull() ?: 0.0,
                    rascenka = extractValue(operationXml, "m:Расценка")?.toDoubleOrNull() ?: 0.0,
                    naryadNumber = extractValue(operationXml, "m:Номер") ?: "",
                    naryadDate = extractValue(operationXml, "m:Дата") ?: "",
                    lineNumber = extractValue(operationXml, "m:НомерСтроки") ?: "",
                    operationCode = extractValue(operationXml, "m:ОперацияКод") ?: "",
                    isAvailableForExecution = false
                )

                // Группируем операции по заказу
                val orderKey = operation.zakazPokupatelya
                operationsByOrder.getOrPut(orderKey) { mutableListOf() }.add(operation)
            }

            // Обработка последовательности операций
            val availableOperations = mutableSetOf<SdelniyNaryadOperation>()
            val unavailableOperations = mutableSetOf<SdelniyNaryadOperation>()

            operationsByOrder.forEach { (_, orderOperations) ->
                // Сортируем операции по номеру строки
                val sortedOperations = orderOperations.sortedBy {
                    it.lineNumber.replace(".", "").toIntOrNull() ?: Int.MAX_VALUE
                }

                sortedOperations.forEachIndexed { index, operation ->
                    // Первая операция в заказе всегда доступна
                    val isAvailable = index == 0 ||
                            // Предыдущие операции выполнены
                            sortedOperations.take(index)
                                .all { it.kolichestvoFakt > 0 }

                    if (isAvailable) {
                        availableOperations.add(operation)
                    } else {
                        unavailableOperations.add(operation)
                    }
                }
            }

            // Помечаем доступные операции
            val processedOperations = (availableOperations + unavailableOperations).map { operation ->
                operation.copy(
                    isAvailableForExecution = availableOperations.contains(operation)
                )
            }

            operations.addAll(processedOperations)

            Log.d("OneCRepository", "Распарсено операций: ${operations.size}")
            Log.d("OneCRepository", "Доступные операции: ${availableOperations.size}")
        } catch (e: Exception) {
            Log.e("OneCRepository", "Ошибка парсинга XML", e)
        }

        return operations
    }

    private fun extractValue(xml: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName>(.*?)</$tagName>", Pattern.DOTALL)
        val matcher = pattern.matcher(xml)

        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else {
            null
        }
    }

    suspend fun setActualQuantity(naryadNumber: String, naryadDate: String, operationCode: String, lineNumber: String, actualQuantity: Double): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val lineNumberDouble = lineNumber.toDoubleOrNull() ?: 0.0
                Log.d("OneCRepository", "Отправка данных: Наряд=$naryadNumber, Дата=$naryadDate, Операция=$operationCode, Строка=$lineNumberDouble, Количество=$actualQuantity")

                val soapEnvelope = RetrofitClient.createSetDataSoapEnvelope(
                    naryadNumber, naryadDate, operationCode, lineNumberDouble, actualQuantity
                )
                val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

                val response = api.setDataRaw(requestBody)

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("OneCRepository", "Ответ на установку данных: $responseBody")

                    val success = responseBody.contains("<m:return") && responseBody.contains(">0</m:return>")

                    if (success) {
                        Log.d("OneCRepository", "Данные успешно обновлены")
                        Result.success(true)
                    } else {
                        Log.e("OneCRepository", "Ошибка обновления данных в 1С")
                        Result.failure(Exception("Не удалось обновить данные"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e("OneCRepository", "Ошибка запроса: ${response.code()} ${response.message()}")
                    Log.e("OneCRepository", "Тело ошибки: $errorBody")
                    Result.failure(Exception("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("OneCRepository", "Исключение при отправке данных", e)
                Result.failure(Exception("Ошибка связи с сервером: ${e.message}"))
            }
        }
    }
}