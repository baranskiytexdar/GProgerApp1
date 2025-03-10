package com.example.gprogerapp1

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.gprogerapp1.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AuthManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "auth_prefs", Context.MODE_PRIVATE
    )

    suspend fun login(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                Log.d("AuthManager", "Параметры запроса:")
                Log.d("AuthManager", "Дата: $currentDate")
                Log.d("AuthManager", "Исполнитель: $username")

                val soapEnvelope = """<?xml version='1.0' encoding='utf-8'?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/" 
    xmlns:ns0="http://wsproduction.ru">
    <soap-env:Body>
        <ns0:GetData>
            <ns0:Date>$currentDate</ns0:Date>
            <ns0:performer>$username</ns0:performer>
        </ns0:GetData>
    </soap-env:Body>
</soap-env:Envelope>"""

                val requestBody = RetrofitClient.createRequestBody(soapEnvelope)

                Log.d("AuthManager", "SOAP-конверт: $soapEnvelope")

                val response = RetrofitClient.oneCService.login(requestBody)

                Log.d("AuthManager", "Код ответа: ${response.code()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""

                    Log.d("AuthManager", "Тело ответа: $responseBody")

                    val isAuthenticated = checkAuthenticationResponse(responseBody)

                    if (isAuthenticated) {
                        saveAuthData(username, password)
                        Result.success(true)
                    } else {
                        Log.e("AuthManager", "Аутентификация не прошла")
                        Result.failure(Exception("Не удалось пройти аутентификацию"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Пустое тело ошибки"
                    Log.e("AuthManager", "Ошибка входа: ${response.code()} ${response.message()}")
                    Log.e("AuthManager", "Тело ошибки: $errorBody")

                    Result.failure(Exception("Ошибка сервера: ${response.code()} $errorBody"))
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Критическая ошибка при входе", e)
                Result.failure(Exception("Ошибка связи с сервером: ${e.message}"))
            }
        }
    }

    private fun checkAuthenticationResponse(responseBody: String): Boolean {
        return try {
            // Проверяем наличие элементов в ответе
            responseBody.contains("<m:return") &&
                    !responseBody.contains("xsi:nil=\"true\"")
        } catch (e: Exception) {
            Log.e("AuthManager", "Ошибка проверки ответа", e)
            false
        }
    }

    // Сохранение данных авторизации
    private fun saveAuthData(username: String, password: String) {
        sharedPreferences.edit().apply {
            putString("username", username)
            putString("password", password) // Храните пароль с осторожностью
            putLong("expiry", System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24 часа
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        val expiry = sharedPreferences.getLong("expiry", 0)
        return expiry > System.currentTimeMillis()
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    fun getUsername(): String {
        return sharedPreferences.getString("username", "") ?: ""
    }

    fun getToken(): String {
        // В вашем случае может быть пустым или возвращать что-то другое
        return ""
    }

    // Получение сохраненного пароля (используйте с осторожностью)
    fun getSavedPassword(): String {
        return sharedPreferences.getString("password", "") ?: ""
    }
}