package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("type")
    val type: NotificationType,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("relatedId")
    val relatedId: Long,
    
    @SerializedName("isRead")
    val isRead: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String
)

enum class NotificationType {
    @SerializedName("TREE_ACCESS_REQUEST")
    TREE_ACCESS_REQUEST,     // Запрос на доступ к древу
    TREE_ACCESS_RESPONSE,    // Ответ на запрос доступа
    TREE_EDIT_REQUEST,       // Запрос на подтверждение изменений
    @SerializedName("MEMORIAL_COMMENT")
    MEMORIAL_COMMENT,        // Новый комментарий
    @SerializedName("ANNIVERSARY")
    ANNIVERSARY,             // Годовщина
    @SerializedName("SYSTEM")
    SYSTEM                   // Системное уведомление
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