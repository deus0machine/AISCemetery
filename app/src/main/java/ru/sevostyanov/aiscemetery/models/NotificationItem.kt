package ru.sevostyanov.aiscemetery.models

/**
 * Базовый интерфейс для всех типов уведомлений
 */
interface NotificationItem {
    val id: Long
    val createdAt: String
    val isRead: Boolean
    
    fun getDisplayTitle(): String
    fun getDisplayMessage(): String
    fun getDisplayDate(): String
}

/**
 * Обертка для обычных уведомлений
 */
data class RegularNotificationItem(
    val notification: Notification
) : NotificationItem {
    override val id: Long get() = notification.id
    override val createdAt: String get() = notification.createdAt
    override val isRead: Boolean get() = notification.isRead
    
    override fun getDisplayTitle(): String = notification.title ?: "Уведомление"
    override fun getDisplayMessage(): String = notification.message
    override fun getDisplayDate(): String = notification.createdAt
}

/**
 * Обертка для уведомлений о черновиках
 */
data class DraftNotificationItem(
    val draftSubmission: DraftSubmission,
    val isIncoming: Boolean
) : NotificationItem {
    override val id: Long get() = draftSubmission.id
    override val createdAt: String get() = draftSubmission.submittedAt
    override val isRead: Boolean get() = draftSubmission.isReviewed
    
    override fun getDisplayTitle(): String = if (isIncoming) {
        "Предложены изменения в дереве"
    } else {
        "Отправлены изменения на рассмотрение"
    }
    
    override fun getDisplayMessage(): String = draftSubmission.message ?: "Без сообщения"
    override fun getDisplayDate(): String = draftSubmission.submittedAt
} 