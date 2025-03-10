package com.example.gprogerapp1

data class AuthResult(
    val isSuccess: Boolean,
    val token: String = "",
    val errorMessage: String = ""
)