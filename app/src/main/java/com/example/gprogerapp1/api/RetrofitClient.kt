package com.example.gprogerapp1.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Base64
import android.util.Log

object RetrofitClient {
    private const val BASE_URL = "http://192.168.5.28/unf_5_backup/ws/"
    private var USERNAME: String = "БаранскийИ"
    private var PASSWORD: String = "Qwerty123"

    // Логгер для отладки HTTP запросов и ответов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun updateCredentials(username: String, password: String) {
        USERNAME = username
        PASSWORD = password
    }

    fun getCurrentCredentials(): Pair<String, String> {
        return Pair(USERNAME, PASSWORD)
    }

    // Настраиваем HTTP клиент с таймаутами, логгером и аутентификацией
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val credentials = "${getCurrentCredentials().first}:${getCurrentCredentials().second}"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val request = chain.request().newBuilder()
                .header("Authorization", "Basic $encodedCredentials")
                .build()

            chain.proceed(request)
        }
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    // Создаем Retrofit клиент с XML конвертером
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .build()

    // Создаем экземпляр API сервиса
    val oneCService: OneCService = retrofit.create(OneCService::class.java)

    // Пересоздаем клиент при каждом обновлении учетных данных
    private fun recreateRetrofitClient(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val credentials = "$USERNAME:$PASSWORD"
                val encodedCredentials = Base64.encodeToString(
                    credentials.toByteArray(),
                    Base64.NO_WRAP
                )

                val request = chain.request().newBuilder()
                    .header("Authorization", "Basic $encodedCredentials")
                    .build()

                chain.proceed(request)
            }
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
    }

    // Динамическое обновление сервиса
    val dynamicOneCService: OneCService
        get() = recreateRetrofitClient().create(OneCService::class.java)

    // Вспомогательная функция для создания SOAP-конверта в точном формате, который ожидает 1С
    fun createSoapEnvelope(date: String, performer: String): String {
        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"><soap-env:Body><ns0:GetOperationsQueue xmlns:ns0="http://wsproduction.ru"><ns0:Date>$date</ns0:Date><ns0:performer>$performer</ns0:performer></ns0:GetOperationsQueue></soap-env:Body></soap-env:Envelope>"""
    }

    fun createStatusSoapEnvelope(date: String, performer: String): String {
        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"><soap-env:Body><ns0:GetData xmlns:ns0="http://wsproduction.ru"><ns0:Date>$date</ns0:Date><ns0:performer>$performer</ns0:performer></ns0:GetData></soap-env:Body></soap-env:Envelope>"""
    }

    // Создаем RequestBody из строки SOAP-запроса
    fun createRequestBody(soapEnvelope: String): RequestBody {
        val mediaType = "text/xml; charset=utf-8".toMediaTypeOrNull()
            ?: throw IllegalArgumentException("Неверный тип медиа")
        return RequestBody.create(mediaType, soapEnvelope)
    }

    // Создаем SOAP-конверт для функции SetData
    fun createSetDataSoapEnvelope(naryadNumber: String, naryadDate: String, operationCode: String, lineNumber: Double, actualQuantity: Double): String {
        // Преобразование даты в формат YYYY-MM-DD
        val formattedDate = try {
            // Если дата в формате "17.02.2025"
            if (naryadDate.contains('.')) {
                naryadDate.split('.').reversed().joinToString("-")
            }
            // Если дата в формате "2025-02-17T10:39:02"
            else if (naryadDate.contains('T')) {
                naryadDate.split('T')[0]
            }
            else {
                naryadDate
            }
        } catch (e: Exception) {
            Log.e("DateFormatter", "Ошибка преобразования даты: $naryadDate", e)
            naryadDate
        }

        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns0="http://wsproduction.ru">
    <soap-env:Body>
        <ns0:SetData>
            <ns0:НомерНаряда>$naryadNumber</ns0:НомерНаряда>
            <ns0:ДатаНаряда>$formattedDate</ns0:ДатаНаряда>
            <ns0:КодОперации>$operationCode</ns0:КодОперации>
            <ns0:НомерСтроки>${lineNumber.toInt()}</ns0:НомерСтроки>
            <ns0:КоличествоФакт>$actualQuantity</ns0:КоличествоФакт>
        </ns0:SetData>
    </soap-env:Body>
</soap-env:Envelope>"""
    }
    fun createStuffListSoapEnvelope(date: String): String {
        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
    <soap-env:Body>
        <ns0:GetStuffList xmlns:ns0="http://wsproduction.ru">
            <ns0:Date>$date</ns0:Date>
        </ns0:GetStuffList>
    </soap-env:Body>
</soap-env:Envelope>"""
    }

}