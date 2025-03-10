package com.example.gprogerapp1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application.applicationContext)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Проверяем состояние входа при инициализации
        _isLoggedIn.value = authManager.isLoggedIn()
    }

    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = authManager.login(username, password)

            result.fold(
                onSuccess = {
                    _loginState.value = LoginState.Success
                    _isLoggedIn.value = true
                },
                onFailure = { exception ->
                    _loginState.value = LoginState.Error(exception.message ?: "Ошибка авторизации")
                }
            )
        }
    }

    fun logout() {
        authManager.logout()
        _isLoggedIn.value = false
        _loginState.value = LoginState.Idle
    }

    fun getUsername(): String {
        return authManager.getUsername()
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}