package com.example.gprogerapp1.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.5.28/unf_5/ws/"
    private const val USERNAME = "БаранскийИ"
    private const val PASSWORD = "Nhbrjnf4"

    // Создаем логгер для отладки HTTP запросов и ответов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Настраиваем HTTP клиент с таймаутами, логгером и аутентификацией
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            // Кодируем логин и пароль в base64
            val credentials = "$USERNAME:$PASSWORD"
            val encodedCredentials = android.util.Base64.encodeToString(
                credentials.toByteArray(),
                android.util.Base64.NO_WRAP
            )

            // Добавляем заголовок Authorization к каждому запросу
            val request = chain.request().newBuilder()
                .header("Authorization", "Basic $encodedCredentials")
                .build()

            chain.proceed(request)
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Создаем Retrofit клиент с XML конвертером
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .build()

    // Создаем экземпляр API сервиса
    val oneCService: OneCService = retrofit.create(OneCService::class.java)

    // Вспомогательная функция для создания SOAP-конверта в точном формате, который ожидает 1С
    fun createSoapEnvelope(date: String, performer: String): String {
        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"><soap-env:Body><ns0:GetData xmlns:ns0="http://wsproduction.ru"><ns0:Date>$date</ns0:Date><ns0:performer>$performer</ns0:performer></ns0:GetData></soap-env:Body></soap-env:Envelope>"""
    }

    // Создаем RequestBody из строки SOAP-запроса
    fun createRequestBody(soapEnvelope: String): RequestBody {
        val mediaType = "text/xml; charset=utf-8".toMediaTypeOrNull()
            ?: throw IllegalArgumentException("Неверный тип медиа")
        return RequestBody.create(mediaType, soapEnvelope)
    }
}