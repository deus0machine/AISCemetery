package ru.sevostyanov.aiscemetery.user

import com.google.gson.annotations.SerializedName

data class Guest(
    @SerializedName("id")
     val id: Long,
    @SerializedName("balance")
    val balance: Long)
