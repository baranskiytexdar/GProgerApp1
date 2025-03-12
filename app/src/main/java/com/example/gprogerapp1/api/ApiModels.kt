package com.example.gprogerapp1.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import com.example.gprogerapp1.SdelniyNaryadOperation

// Если сервис возвращает XML, используйте аннотации SimpleXML
@Root(name = "Operation", strict = false)
data class SdelniyNaryadOperationResponse(
    @field:Element(name = "Ssylka", required = false)
    var ssylka: String = "",

    @field:Element(name = "ZakazPokupatelya", required = false)
    var zakazPokupatelya: String = "",

    @field:Element(name = "Nomenklatura", required = false)
    var nomenklatura: String = "",

    @field:Element(name = "Operaciya", required = false)
    var operaciya: String = "",

    @field:Element(name = "KolichestvoPlan", required = false)
    var kolichestvoPlan: Double = 0.0,

    @field:Element(name = "kolichestvoFakt", required = false)
    var kolichetstvoFakt: Double = 0.0,

    @field:Element(name = "NormaVremeni", required = false)
    var normaVremeni: Double = 0.0,

    @field:Element(name = "Rascenka", required = false)
    var rascenka: Double = 0.0,

    @field:Element(name = "NaryadNumber", required = false)
    var naryadNumber: String = "",

    @field:Element(name = "NaryadDate", required = false)
    var naryadDate: String = "",

    @field:Element(name = "LineNumber", required = false)
    var lineNumber: String = "",

    @field:Element(name = "OperationCode", required = false)
    var operationCode: String = ""
)

// Для корневого элемента ответа с массивом операций
@Root(name = "GetDataResponse", strict = false)
data class GetDataResponse(
    @field:Element(name = "return", required = false)
    var operations: List<SdelniyNaryadOperationResponse> = emptyList()
)

// Функция для конвертации ответа API в модель вашего приложения
fun SdelniyNaryadOperationResponse.toOperation(): SdelniyNaryadOperation {
    return SdelniyNaryadOperation(
        ssylka = this.ssylka,
        zakazPokupatelya = this.zakazPokupatelya,
        nomenklatura = this.nomenklatura,
        operaciya = this.operaciya,
        kolichestvoPlan = this.kolichestvoPlan,
        kolichestvoFakt = this.kolichetstvoFakt,
        normaVremeni = this.normaVremeni,
        rascenka = this.rascenka,
        naryadNumber = this.naryadNumber,
        naryadDate = this.naryadDate,
        lineNumber = this.lineNumber,
        operationCode = this.operationCode
    )
}