package ru.sevostyanov.aiscemetery.models

data class Notification(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedId: Long? = null, // ID связанного объекта (мемориала, древа и т.д.)
    val isRead: Boolean = false,
    val createdAt: String
)

enum class NotificationType {
    TREE_ACCESS_REQUEST,     // Запрос на доступ к древу
    TREE_ACCESS_RESPONSE,    // Ответ на запрос доступа
    TREE_EDIT_REQUEST,       // Запрос на подтверждение изменений
    MEMORIAL_COMMENT,        // Новый комментарий
    ANNIVERSARY,             // Годовщина
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