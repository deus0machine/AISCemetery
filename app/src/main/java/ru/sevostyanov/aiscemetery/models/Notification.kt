package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notification(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("senderId")
    val senderId: Long? = null,
    
    @SerializedName("senderName")
    val senderName: String? = null,
    
    @SerializedName("type")
    val type: NotificationType,
    
    @SerializedName("status")
    val status: NotificationStatus = NotificationStatus.PENDING,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("relatedEntityId")
    val relatedEntityId: Long? = null,
    
    @SerializedName("relatedEntityName")
    val relatedEntityName: String? = null,
    
    @SerializedName("isRead")
    val isRead: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String
) : Parcelable

enum class NotificationType {
    @SerializedName("INFO")
    INFO,                     // Информационное уведомление
    
    @SerializedName("MEMORIAL_OWNERSHIP")
    MEMORIAL_OWNERSHIP,       // Запрос на совместное владение мемориалом
    
    @SerializedName("TREE_ACCESS_REQUEST")
    TREE_ACCESS_REQUEST,      // Запрос на доступ к древу
    
    @SerializedName("MEMORIAL_COMMENT")
    MEMORIAL_COMMENT,         // Новый комментарий
    
    @SerializedName("ANNIVERSARY")
    ANNIVERSARY,              // Годовщина
    
    @SerializedName("SYSTEM")
    SYSTEM                    // Системное уведомление
}

enum class NotificationStatus {
    @SerializedName("PENDING")
    PENDING,    // Ожидает ответа
    
    @SerializedName("ACCEPTED")
    ACCEPTED,   // Принято
    
    @SerializedName("REJECTED")
    REJECTED,   // Отклонено
    
    @SerializedName("INFO")
    INFO        // Информационное (не требует ответа)
}

data class Favorite(
    val id: Long,
    val userId: Long,
    val type: FavoriteType,
    val itemId: Long,        // ID избранного объекта
    val addedAt: String
)

enum class FavoriteType {
    TREE,       // Генеалогическое древо
    MEMORIAL,   // Мемориал
    USER        // Пользователь
} 