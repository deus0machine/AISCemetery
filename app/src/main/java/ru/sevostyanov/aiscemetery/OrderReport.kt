package ru.sevostyanov.aiscemetery

data class OrderReport(
    val id: Long,
    val orderName: String,
    val orderDescription: String,
    val orderCost: Long,
    val orderDate: String, // Формат: "yyyy-MM-dd"
    val guest: Long,
    val burial: Long
)