package ru.sevostyanov.aiscemetery

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date
@Parcelize
data class GuestItem(
    val id: Long,
    val fio: String,
    val contacts: String?,
    val dateOfRegistration: Date,
    val login: String,
    val balance: Long
): Parcelable
