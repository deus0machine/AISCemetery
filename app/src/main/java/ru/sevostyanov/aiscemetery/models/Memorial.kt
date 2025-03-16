package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.sevostyanov.aiscemetery.user.GuestItem

@Parcelize
data class Memorial(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("fio")
    val fio: String,
    
    @SerializedName("birthDate")
    val birthDate: String?,
    
    @SerializedName("deathDate")
    val deathDate: String?,
    
    @SerializedName("biography")
    val biography: String?,
    
    @SerializedName("mainLocation")
    val mainLocation: Location?,
    
    @SerializedName("burialLocation")
    val burialLocation: Location?,
    
    @SerializedName("photoUrl")
    val photoUrl: String? = null,
    
    @SerializedName("is_public")
    val isPublic: Boolean = false,
    
    @SerializedName("treeId")
    val treeId: Long? = null,
    
    @SerializedName("createdBy")
    val createdBy: GuestItem? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class Location(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("address")
    val address: String?
) : Parcelable

data class Media(
    val id: Long,
    val url: String,
    val type: MediaType,
    val description: String? = null,
    val createdAt: String
)

enum class MediaType {
    PHOTO,
    VIDEO,
    AUDIO
}

data class Comment(
    val id: Long,
    val memorialId: Long,
    val userId: Long,
    val text: String,
    val createdAt: String,
    val updatedAt: String? = null
)

data class PrivacyUpdateRequest(
    @SerializedName("isPublic")
    val isPublic: Boolean
) 