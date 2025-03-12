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

object RetrofitClient {
    private const val BASE_URL = "http://192.168.5.28/unf_5/ws/"
    private const val USERNAME = "БаранскийИ"
    private const val PASSWORD = "Nhbrjnf9"

    // Создаем логгер для отладки HTTP запросов и ответов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Настраиваем HTTP клиент с таймаутами, логгером и аутентификацией
    private val okHttpClient = OkHttpClient.Builder()
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
    // Создаем SOAP-конверт для функции SetData
    fun createSetDataSoapEnvelope(naryadNumber: String, naryadDate: String, operationCode: String, lineNumber: Double, actualQuantity: Double): String {
        return """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
    <soap-env:Body>
        <ns0:SetData xmlns:ns0="http://wsproduction.ru">
            <ns0:NaryadNumber>$naryadNumber</ns0:NaryadNumber>
            <ns0:NaryadDate>$naryadDate</ns0:NaryadDate>
            <ns0:OperationCode>$operationCode</ns0:OperationCode>
            <ns0:LineNumber>$lineNumber</ns0:LineNumber>
            <ns0:ActualQuantity>$actualQuantity</ns0:ActualQuantity>
        </ns0:SetData>
    </soap-env:Body>
</soap-env:Envelope>"""
    }
}