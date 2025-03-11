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
                    rascenka = extractValue(operationXml, "m:Расценка")?.toDoubleOrNull() ?: 0.0
                )

                operations.add(operation)
            }

            Log.d("OneCRepository", "Распарсено операций: ${operations.size}")
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
}