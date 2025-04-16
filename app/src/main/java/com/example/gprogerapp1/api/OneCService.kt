package com.example.gprogerapp1.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OneCService {
    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "Accept: text/xml"
    )
    @POST("prod.1cws")
    suspend fun login(
        @Body soapRequestBody: RequestBody
    ): Response<ResponseBody>

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "Accept: text/xml"
    )
    @POST("prod.1cws")
    suspend fun getDataRaw(
        @Body soapRequestBody: RequestBody
    ): Response<ResponseBody>

    @POST("prod.1cws")
    suspend fun setDataRaw(
        @Body soapRequestBody: RequestBody
    ): Response<ResponseBody>

    @Headers(
        "Content-Type: text/xml; charset=utf-8",
        "SOAPAction: http://wsproduction.ru#wsProduction:GetStuffList"
    )
    @POST("prod.1cws")
    suspend fun getStuffList(
        @Body soapRequestBody: RequestBody
    ): Response<ResponseBody>
}