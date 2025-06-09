package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.sevostyanov.aiscemetery.user.UserManager

@Parcelize
data class Memorial(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("user")
    val user: Long? = null,
    
    @SerializedName("fio")
    val fio: String,
    
    @SerializedName("firstName")
    val firstName: String? = null,
    
    @SerializedName("lastName")
    val lastName: String? = null,
    
    @SerializedName("middleName")
    val middleName: String? = null,
    
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
    
    @SerializedName("pendingFio")
    val pendingFio: String? = null,
    
    @SerializedName("pendingFirstName")
    val pendingFirstName: String? = null,
    
    @SerializedName("pendingLastName")
    val pendingLastName: String? = null,
    
    @SerializedName("pendingMiddleName")
    val pendingMiddleName: String? = null,
    
    @SerializedName("pendingBiography")
    val pendingBiography: String? = null,
    
    @SerializedName("pendingBirthDate")
    val pendingBirthDate: String? = null,
    
    @SerializedName("pendingDeathDate")
    val pendingDeathDate: String? = null,
    
    @SerializedName("pendingIsPublic")
    val pendingIsPublic: Boolean? = null,
    
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
    val createdBy: User? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    
    @SerializedName("editors")
    val editors: List<Long>? = null,
    
    @SerializedName("is_editor")
    val isEditor: Boolean = false,
    
    @SerializedName("pendingChanges")
    val pendingChanges: Boolean = false,
    
    @SerializedName("changesUnderModeration")
    val changesUnderModeration: Boolean = false
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
        
        // Если изменения мемориала находятся на модерации, также запрещаем редактирование
        if (changesUnderModeration) {
            return false
        }
        
        return isUserOwner || isEditor || isUserEditor
    }
    
    // Метод для получения статуса публикации в виде строки
    fun getPublicationStatusText(): String {
        return when {
            // Показываем статус "Изменения на модерации" только владельцу
            changesUnderModeration && isUserOwner -> "Изменения на модерации"
            publicationStatus == PublicationStatus.PUBLISHED -> "Опубликован"
            publicationStatus == PublicationStatus.PENDING_MODERATION -> "На модерации"
            publicationStatus == PublicationStatus.REJECTED -> "Отклонен"
            publicationStatus == PublicationStatus.DRAFT -> "Черновик"
            isPublic -> "Опубликован"
            else -> "Черновик"
        }
    }
    
    // Метод для получения цвета статуса публикации
    fun getPublicationStatusColor(): Int {
        return when {
            // Цвет для изменений на модерации показываем только владельцу
            changesUnderModeration && isUserOwner -> android.R.color.holo_orange_dark
            publicationStatus == PublicationStatus.PUBLISHED -> android.R.color.holo_green_dark
            publicationStatus == PublicationStatus.PENDING_MODERATION -> android.R.color.holo_orange_dark
            publicationStatus == PublicationStatus.REJECTED -> android.R.color.holo_red_dark
            publicationStatus == PublicationStatus.DRAFT -> android.R.color.darker_gray
            isPublic -> android.R.color.holo_green_dark
            else -> android.R.color.darker_gray
        }
    }
    
    // ========== Методы для работы с разделенным ФИО ==========
    
    /**
     * Получает полное ФИО, приоритезируя отдельные поля над объединенным
     */
    fun getFullName(): String {
        return if (!firstName.isNullOrBlank() && !lastName.isNullOrBlank()) {
            buildString {
                append(lastName)
                append(" ")
                append(firstName)
                if (!middleName.isNullOrBlank()) {
                    append(" ")
                    append(middleName)
                }
            }
        } else {
            fio
        }
    }
    
    /**
     * Получает краткое ФИО (Фамилия И.О.)
     */
    fun getShortName(): String {
        return if (!firstName.isNullOrBlank() && !lastName.isNullOrBlank()) {
            buildString {
                append(lastName)
                append(" ")
                append(firstName.first().uppercase())
                append(".")
                if (!middleName.isNullOrBlank()) {
                    append(middleName.first().uppercase())
                    append(".")
                }
            }
        } else {
            // Пытаемся сократить объединенное ФИО
            val parts = fio.split(" ", limit = 3)
            when (parts.size) {
                1 -> parts[0] // Только одно слово
                2 -> "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}."
                else -> "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}.${parts[2].firstOrNull()?.uppercase() ?: ""}."
            }
        }
    }
    
    /**
     * Проверяет, заполнены ли отдельные поля ФИО
     */
    fun hasSeparateNameFields(): Boolean {
        return !firstName.isNullOrBlank() && !lastName.isNullOrBlank()
    }
    
    companion object {
        /**
         * Утилитный метод для разбора ФИО на составляющие
         * Возвращает тройку: (firstName, lastName, middleName)
         */
        fun parseFio(fio: String): Triple<String?, String?, String?> {
            val parts = fio.trim().split("\\s+".toRegex())
            return when (parts.size) {
                1 -> Triple(parts[0], null, null) // Только имя
                2 -> Triple(parts[1], parts[0], null) // Фамилия Имя
                3 -> Triple(parts[1], parts[0], parts[2]) // Фамилия Имя Отчество
                else -> {
                    // Более трех частей - берем первые три
                    Triple(parts[1], parts[0], parts[2])
                }
            }
        }
        
        /**
         * Утилитный метод для сборки ФИО из отдельных частей
         */
        fun buildFio(firstName: String?, lastName: String?, middleName: String?): String {
            return buildString {
                if (!lastName.isNullOrBlank()) {
                    append(lastName)
                }
                if (!firstName.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(firstName)
                }
                if (!middleName.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(middleName)
                }
            }
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