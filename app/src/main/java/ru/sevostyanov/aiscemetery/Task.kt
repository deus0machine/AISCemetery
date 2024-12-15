package ru.sevostyanov.aiscemetery

import com.google.gson.annotations.SerializedName

data class Task(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("cost")
    val cost: String,

    @SerializedName("description")
    val description: String,
    )
