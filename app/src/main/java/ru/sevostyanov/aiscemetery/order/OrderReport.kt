package ru.sevostyanov.aiscemetery.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderReport(
    val id: Long,
    val orderName: String,
    val orderDescription: String,
    val orderCost: Long,
    val orderDate: String, // Формат: "yyyy-MM-dd"
    val guestId: Long,
    val guestName: String,
    val burialId: Long,
    val burialName: String,
    var isCompleted: Boolean
) : Parcelable