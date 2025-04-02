package com.example.gprogerapp1

data class SdelniyNaryadOperation(
    val ssylka: String,
    val zakazPokupatelya: String,
    val nomenklatura: String,
    val operaciya: String,
    val kolichestvoPlan: Double,
    val kolichestvoFakt: Double,
    val normaVremeni: Double,
    val rascenka: Double,
    val naryadNumber: String = "ТПНФ-000520",
    val naryadDate: String = "17.02.2025",
    val lineNumber: String = "1",
    val operationCode: String = "НФ-00003142",
    val isAvailableForExecution: Boolean = false // Флаг доступности операции
)