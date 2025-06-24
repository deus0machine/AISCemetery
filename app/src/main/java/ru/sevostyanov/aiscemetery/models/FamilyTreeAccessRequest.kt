package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class FamilyTreeAccessRequest(
    @SerializedName("familyTreeId")
    val familyTreeId: Long,
    
    @SerializedName("requestedAccessLevel")
    val requestedAccessLevel: String, // "VIEWER", "EDITOR", "ADMIN"
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("action")
    val action: String = "request"
) 