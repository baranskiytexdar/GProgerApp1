package com.example.gprogerapp1

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.util.Base64
import android.util.Log
import com.example.gprogerapp1.api.RetrofitClient
//import com.example.gprogerapp1.api.StuffViewModelle
import java.util.regex.Pattern
import java.util.regex.Matcher
/**
 * Сервис для работы с 1С УНФ через REST API.
 */

class OneCService {
    companion object {
        private const val BASE_URL = "http://192.168.5.28/unf_5_backup/ws/"
//        private const val USERNAME = "БаранскийИ" // Замените на реальное имя пользователя
//        private const val PASSWORD = "Nhbrjnf4" // Замените на реальный пароль
        private const val TIMEOUT = 30L // Таймаут в секундах
    }
    private fun parseStuffListResponse(responseBody: String): List<String> {
        val pattern = Pattern.compile("<d4p1:ФИОСотрудника>(.*?)</d4p1:ФИОСотрудника>", Pattern.DOTALL)
        val matcher = pattern.matcher(responseBody)

        val stuffList = mutableListOf<String>()
        while (matcher.find()) {
            val stuffName = matcher.group(1)?.trim()
            if (!stuffName.isNullOrEmpty()) {
                stuffList.add(stuffName)
            }
        }

        return stuffList
    }
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()

    /**
     * Выполнение запроса к базе 1С УНФ и получение данных по сдельным нарядам.
     *
     * @param ispolnitel Исполнитель для фильтрации данных
     * @return Список операций сдельных нарядов
     */
    fun executeQuery(ispolnitel: String): List<SdelniyNaryadOperation> {
        // Формируем запрос
        val query = """
            ВЫБРАТЬ
                СдельныйНарядОперации.Ссылка КАК Ссылка,
                СдельныйНарядОперации.ЗаказПокупателя КАК ЗаказПокупателя,
                СдельныйНарядОперации.Номенклатура КАК Номенклатура,
                СдельныйНарядОперации.Операция КАК Операция,
                СдельныйНарядОперации.КоличествоПлан КАК КоличествоПлан,
                СдельныйНарядОперации.КоличествоФакт КАК КоличествоФакт,
                СдельныйНарядОперации.НормаВремени КАК НормаВремени,
                СдельныйНарядОперации.Расценка КАК Расценка
            ИЗ
                Документ.СдельныйНаряд.Операции КАК СдельныйНарядОперации
            ГДЕ
                СдельныйНарядОперации.Исполнитель = &Исполнитель
        """.trimIndent()

        // Параметры запроса
        val params = mapOf("Исполнитель" to ispolnitel)

        // Формируем тело запроса в формате JSON
        val requestBody = JSONObject().apply {
            put("query", query)
            put("params", JSONObject(params))
        }.toString()

        // Создаем HTTP запрос
        // Получаем текущие учетные данные из RetrofitClient
        val (username, password) = RetrofitClient.getCurrentCredentials()
        val request = Request.Builder()
            .url("${BASE_URL}query") // Предполагается, что у 1C API есть метод /query для выполнения запросов
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", getBasicAuthHeader( username, password))
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            // Выполняем запрос
            val response = client.newCall(request).execute()

            // Проверяем ответ
            if (!response.isSuccessful) {
                println("Код ответа: ${response.code}")
                println("Тело ответа: ${response.body?.string()}")
                when (response.code) {
                    404 -> throw Exception("Ресурс не найден. Проверьте URL.")
                    500 -> throw Exception("Внутренняя ошибка сервера")

                }
                throw IOException("Ошибка при выполнении запроса: ${response.code} ${response.message}")
            }

            // Разбираем ответ
            val responseBody = response.body?.string() ?: throw IOException("Пустой ответ от сервера")

            return parseResponse(responseBody)
        } catch (e: Exception) {
            // В реальном приложении здесь следует добавить логирование ошибки
            throw e
        }
    }

    /**
     * Разбор ответа от сервера и преобразование в список объектов.
     */
    private fun parseResponse(responseBody: String): List<SdelniyNaryadOperation> {
        val jsonResponse = JSONObject(responseBody)
        val jsonResults = jsonResponse.getJSONArray("results")

        // Логируем всю структуру JSON для анализа
        Log.d("OneCService", "JSON Response: $responseBody")

        val operations = mutableListOf<SdelniyNaryadOperation>()

        for (i in 0 until jsonResults.length()) {
            val jsonResult = jsonResults.getJSONObject(i)

            // Выводим все ключи в JSON-объекте
            val keys = jsonResult.keys()
            val keysStr = StringBuilder("JSON keys: ")
            while (keys.hasNext()) {
                val key = keys.next()
                keysStr.append("$key, ")
            }
            Log.d("OneCService", keysStr.toString())

            // Проверяем наличие нужных ключей
            Log.d("OneCService", "Содержит 'Номер': ${jsonResult.has("Номер")}")
            Log.d("OneCService", "Содержит 'Дата': ${jsonResult.has("Дата")}")
            Log.d("OneCService", "Содержит 'ОперацияКод': ${jsonResult.has("ОперацияКод")}")
            Log.d("OneCService", "Содержит 'НомерСтроки': ${jsonResult.has("НомерСтроки")}")

            // Если ключи есть, выводим их значения
            if (jsonResult.has("Номер")) {
                Log.d("OneCService", "Номер: ${jsonResult.getString("Номер")}")
            }
            if (jsonResult.has("Дата")) {
                Log.d("OneCService", "Дата: ${jsonResult.getString("Дата")}")
            }

            // Пытаемся безопасно получить значения
            val naryadNumber = if (jsonResult.has("Номер")) jsonResult.getString("Номер") else ""
            val naryadDate = if (jsonResult.has("Дата")) jsonResult.getString("Дата") else ""
            val operationCode = if (jsonResult.has("ОперацияКод")) jsonResult.getString("ОперацияКод") else ""
            val lineNumber = if (jsonResult.has("НомерСтроки")) jsonResult.getString("НомерСтроки") else ""

            operations.add(
                SdelniyNaryadOperation(
                    ssylka = jsonResult.getString("Ссылка"),
                    zakazPokupatelya = jsonResult.getString("ЗаказПокупателя"),
                    nomenklatura = jsonResult.getString("Номенклатура"),
                    operaciya = jsonResult.getString("Операция"),
                    kolichestvoPlan = jsonResult.getDouble("КоличествоПлан"),
                    kolichestvoFakt = jsonResult.getDouble("КоличествоФакт"),
                    normaVremeni = jsonResult.getDouble("НормаВремени"),
                    rascenka = jsonResult.getDouble("Расценка"),
                    lineNumber = lineNumber,
                    naryadNumber = naryadNumber,
                    naryadDate = naryadDate,
                    operationCode = operationCode
                )
            )

            // Логируем созданный объект для проверки
            Log.d("OneCService", "Создан объект: naryadNumber=$naryadNumber, naryadDate=$naryadDate, operationCode=$operationCode, lineNumber=$lineNumber")
        }

        return operations
    }

    /**
     * Формирование заголовка Basic Authentication.
     */
    private fun getBasicAuthHeader(username: String, password: String): String {
        val credentials = "$username:$password"
        Log.d("OneCService", "Credentials (before encoding): $credentials")

        // Явно указываем кодировку UTF-8 для кириллических символов
        val encodedCredentials = Base64.encodeToString(
            credentials.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        Log.d("OneCService", "Encoded credentials: $encodedCredentials")

        return "Basic $encodedCredentials"
    }

    /**
     * Имитация запроса для тестирования (используется, если соединение с 1С недоступно).
     */
    fun getTestData(): List<SdelniyNaryadOperation> {
        // Тестовые данные для отладки
        return listOf(
            SdelniyNaryadOperation(
                ssylka = "Документ.СдельныйНаряд.12345",
                zakazPokupatelya = "Заказ покупателя №123 от 10.02.2025",
                nomenklatura = "Стол письменный",
                operaciya = "Сборка",
                kolichestvoPlan = 5.0,
                kolichestvoFakt = 5.0,
                normaVremeni = 2.5,
                rascenka = 1200.0,
                naryadNumber = "НР-001",
                naryadDate = "18.02.2025",
                lineNumber = "1",
                operationCode = "СБ001"
            ),
            SdelniyNaryadOperation(
                ssylka = "Документ.СдельныйНаряд.12346",
                zakazPokupatelya = "Заказ покупателя №124 от 12.02.2025",
                nomenklatura = "Шкаф купе",
                operaciya = "Монтаж",
                kolichestvoPlan = 2.0,
                kolichestvoFakt = 1.0,
                normaVremeni = 4.0,
                rascenka = 2500.0,
                naryadNumber = "НР-002",
                naryadDate = "19.02.2025",
                lineNumber = "2",
                operationCode = "МТ001"
            ),
            SdelniyNaryadOperation(
                ssylka = "Документ.СдельныйНаряд.12347",
                zakazPokupatelya = "Заказ покупателя №125 от 15.02.2025",
                nomenklatura = "Стул офисный",
                operaciya = "Сборка",
                kolichestvoPlan = 10.0,
                kolichestvoFakt = 0.0,
                normaVremeni = 0.5,
                rascenka = 300.0,
                naryadNumber = "НР-003",
                naryadDate = "20.02.2025",
                lineNumber = "3",
                operationCode = "СБ002"
            )
        )
    }
    fun getStuffList(date: String): List<String> {
        val currentDate = date

        val soapEnvelope = """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
    <soap-env:Body>
        <GetStuffList xmlns="http://wsproduction.ru">
            <Date>$currentDate</Date>
        </GetStuffList>
    </soap-env:Body>
</soap-env:Envelope>"""

        Log.e("OneCService", "Full SOAP Request: $soapEnvelope")

        val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

        try {
            // Получаем текущие учетные данные из RetrofitClient
            val (username, password) = RetrofitClient.getCurrentCredentials()

            val request = Request.Builder()
                .url("${BASE_URL}prod.1cws")
                .post(requestBody)
                .addHeader("Content-Type", "text/xml; charset=utf-8")
                .addHeader("SOAPAction", "http://wsproduction.ru#wsProduction:GetStuffList")
                // Добавляем авторизацию
                .addHeader("Authorization", getBasicAuthHeader(username, password))
                .build()

            Log.e("OneCService", "Request URL: ${request.url}")
            Log.e("OneCService", "Request Headers: ${request.headers}")

            val response = client.newCall(request).execute()

            Log.e("OneCService", "Response Code: ${response.code}")
            Log.e("OneCService", "Response Message: ${response.message}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e("OneCService", "Error Body: $errorBody")
                throw IOException("Ошибка при выполнении запроса: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Пустой ответ от сервера")

            Log.e("OneCService", "Full Response Body: $responseBody")

            // Парсинг списка сотрудников
//            val pattern = Pattern.compile("<m:return>(.*?)</m:return>", Pattern.DOTALL)
//            val matcher = pattern.matcher(responseBody)
//
//            val stuffList = mutableListOf<String>()
//            while (matcher.find()) {
//                val stuffName = matcher.group(1)?.trim()
//                if (!stuffName.isNullOrEmpty()) {
//                    stuffList.add(stuffName)
//                }
//            }
            return parseStuffListResponse(responseBody)
            //return stuffList
        } catch (e: Exception) {
            Log.e("OneCService", "Detailed Error:", e)
            throw e
        }
    }
}