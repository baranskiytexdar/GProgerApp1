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
//import java.util.Base64
import java.util.concurrent.TimeUnit
import android.util.Base64

/**
 * Сервис для работы с 1С УНФ через REST API.
 */
class OneCService {
    companion object {
        private const val BASE_URL = "http://192.168.5.28/unf_5/" // Замените на реальный URL вашего сервера 1С УНФ
        private const val USERNAME = "БаранскийИ" // Замените на реальное имя пользователя
        private const val PASSWORD = "Nhbrjnf4" // Замените на реальный пароль
        private const val TIMEOUT = 30L // Таймаут в секундах
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
        val request = Request.Builder()
            .url("${BASE_URL}query") // Предполагается, что у 1C API есть метод /query для выполнения запросов
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", getBasicAuthHeader(USERNAME, PASSWORD))
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

        val operations = mutableListOf<SdelniyNaryadOperation>()

        for (i in 0 until jsonResults.length()) {
            val jsonResult = jsonResults.getJSONObject(i)

            operations.add(
                SdelniyNaryadOperation(
                    ssylka = jsonResult.getString("Ссылка"),
                    zakazPokupatelya = jsonResult.getString("ЗаказПокупателя"),
                    nomenklatura = jsonResult.getString("Номенклатура"),
                    operaciya = jsonResult.getString("Операция"),
                    kolichestvoPlan = jsonResult.getDouble("КоличествоПлан"),
                    normaVremeni = jsonResult.getDouble("НормаВремени"),
                    rascenka = jsonResult.getDouble("Расценка")
                )
            )
        }

        return operations
    }

    /**
     * Формирование заголовка Basic Authentication.
     */
    private fun getBasicAuthHeader(username: String, password: String): String {
        val credentials = "$username:$password"
        val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
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
                normaVremeni = 2.5,
                rascenka = 1200.0
            ),
            SdelniyNaryadOperation(
                ssylka = "Документ.СдельныйНаряд.12346",
                zakazPokupatelya = "Заказ покупателя №124 от 12.02.2025",
                nomenklatura = "Шкаф купе",
                operaciya = "Монтаж",
                kolichestvoPlan = 2.0,
                normaVremeni = 4.0,
                rascenka = 2500.0
            ),
            SdelniyNaryadOperation(
                ssylka = "Документ.СдельныйНаряд.12347",
                zakazPokupatelya = "Заказ покупателя №125 от 15.02.2025",
                nomenklatura = "Стул офисный",
                operaciya = "Сборка",
                kolichestvoPlan = 10.0,
                normaVremeni = 0.5,
                rascenka = 300.0
            )
        )
    }
}