package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("username")
    val username: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("fullName")
    val fullName: String? = null,
    
    @SerializedName("fio")
    val fio: String? = null,
    
    @SerializedName("contacts")
    val contacts: String? = null,
    
    @SerializedName("login") 
    val login: String? = null
) : Parcelable 