package ru.sevostyanov.aiscemetery.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class GuestItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("fio")
    val fio: String,
    
    @SerializedName("contacts")
    val contacts: String?,
    
    @SerializedName("dateOfRegistration")
    val dateOfRegistration: String?,
    
    @SerializedName("login")
    val login: String,
    
    @SerializedName("balance")
    val balance: Long
): Parcelable
