package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class MemorialOwnershipRequest(
    @SerializedName("receiverId")
    val receiverId: String,
    
    @SerializedName("memorialId")
    val memorialId: String,
    
    @SerializedName("message")
    val message: String
) 