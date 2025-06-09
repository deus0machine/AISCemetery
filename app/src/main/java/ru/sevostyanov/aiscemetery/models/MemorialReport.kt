package ru.sevostyanov.aiscemetery.models

data class MemorialReportRequest(
    val memorialId: Long,
    val reason: String
)

data class MemorialReportResponse(
    val status: String,
    val message: String
) 