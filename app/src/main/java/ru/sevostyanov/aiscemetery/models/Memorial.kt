package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.sevostyanov.aiscemetery.user.GuestItem
import ru.sevostyanov.aiscemetery.user.UserManager

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
    
    @SerializedName("pendingPhotoUrl")
    val pendingPhotoUrl: String? = null,
    
    @SerializedName("pendingBiography")
    val pendingBiography: String? = null,
    
    @SerializedName("pendingBirthDate")
    val pendingBirthDate: String? = null,
    
    @SerializedName("pendingDeathDate")
    val pendingDeathDate: String? = null,
    
    @SerializedName("pendingMainLocation")
    val pendingMainLocation: Location? = null,
    
    @SerializedName("pendingBurialLocation")
    val pendingBurialLocation: Location? = null,
    
    @SerializedName("is_public")
    val isPublic: Boolean = false,
    
    @SerializedName("publicationStatus")
    val publicationStatus: PublicationStatus? = null,
    
    @SerializedName("treeId")
    val treeId: Long? = null,
    
    @SerializedName("createdBy")
    val createdBy: GuestItem? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    
    @SerializedName("editors")
    val editors: List<Long>? = null,
    
    @SerializedName("is_editor")
    val isEditor: Boolean = false,
    
    @SerializedName("pendingChanges")
    val pendingChanges: Boolean = false
) : Parcelable {
    // Вычисляемое свойство для проверки, является ли текущий пользователь редактором
    // независимо от флага is_editor, присланного с сервера
    val isUserEditor: Boolean
        get() {
            val currentUser = UserManager.getCurrentUser()
            if (currentUser == null || editors == null) {
                return false
            }
            return editors.contains(currentUser.id)
        }
        
    // Вычисляемое свойство для проверки, является ли текущий пользователь владельцем
    val isUserOwner: Boolean
        get() {
            val currentUser = UserManager.getCurrentUser()
            return currentUser?.id == createdBy?.id
        }
        
    // Метод для проверки, имеет ли пользователь права на редактирование
    fun canEdit(): Boolean {
        // Если мемориал на модерации, запрещаем его редактирование независимо от прав пользователя
        if (publicationStatus == PublicationStatus.PENDING_MODERATION) {
            return false
        }
        return isUserOwner || isEditor || isUserEditor
    }
    
    // Метод для получения статуса публикации в виде строки
    fun getPublicationStatusText(): String {
        return when (publicationStatus) {
            PublicationStatus.PUBLISHED -> "Опубликован"
            PublicationStatus.PENDING_MODERATION -> "На модерации"
            PublicationStatus.REJECTED -> "Отклонен"
            PublicationStatus.DRAFT -> "Черновик"
            null -> if (isPublic) "Опубликован" else "Черновик"
        }
    }
    
    // Метод для получения цвета статуса публикации
    fun getPublicationStatusColor(): Int {
        return when (publicationStatus) {
            PublicationStatus.PUBLISHED -> android.R.color.holo_green_dark
            PublicationStatus.PENDING_MODERATION -> android.R.color.holo_orange_dark
            PublicationStatus.REJECTED -> android.R.color.holo_red_dark
            PublicationStatus.DRAFT -> android.R.color.darker_gray
            null -> if (isPublic) android.R.color.holo_green_dark else android.R.color.darker_gray
        }
    }
}

@Parcelize
enum class PublicationStatus : Parcelable {
    @SerializedName("DRAFT")
    DRAFT,
    
    @SerializedName("PENDING_MODERATION")
    PENDING_MODERATION,
    
    @SerializedName("PUBLISHED")
    PUBLISHED,
    
    @SerializedName("REJECTED")
    REJECTED
}

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

// Запрос на совместное редактирование
data class EditorRequest(
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("action")
    val action: String // "add" или "remove"
)

// Запрос на одобрение изменений
data class ApproveChangesRequest(
    @SerializedName("approve")
    val approve: Boolean
) 