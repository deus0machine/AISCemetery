package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.sevostyanov.aiscemetery.user.GuestItem

@Parcelize
data class Notification(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("senderId")
    val senderId: Long,
    
    @SerializedName("receiverId")
    val receiverId: Long,
    
    @SerializedName("senderName")
    val senderName: String? = null,
    
    @SerializedName("receiverName")
    val receiverName: String? = null,
    
    @SerializedName("type")
    val type: NotificationType,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("status")
    val status: NotificationStatus,
    
    @SerializedName("read")
    val isRead: Boolean,
    
    @SerializedName("relatedEntityId")
    val relatedEntityId: Long? = null,
    
    @SerializedName("relatedEntityName")
    val relatedEntityName: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("moderation")
    val moderation: ModerationInfo? = null,
    
    @SerializedName("urgent")
    val urgent: Boolean = false
) : Parcelable {
    // Проверяет, является ли уведомление связанным с модерацией мемориала или семейного дерева
    fun isRelatedToModeration(): Boolean {
        // Защита от null в типе уведомления
        if (type == null) return false
        
        // Прямые типы модерации
        if (type == NotificationType.MODERATION || 
            type == NotificationType.FAMILY_TREE_MODERATION) return true
        
        // Системные уведомления с заголовком/сообщением о модерации
        if (type == NotificationType.SYSTEM) {
            val titleLower = title?.lowercase() ?: ""
            val messageLower = message.lowercase()
            
            return titleLower.contains("опубликован") || 
                   titleLower.contains("не опубликован") ||
                   titleLower.contains("отклонен") ||
                   messageLower.contains("публикац") ||
                   messageLower.contains("модерац")
        }
        
        return false
    }
    
    // Определяет, был ли мемориал или семейное дерево одобрено (для уведомлений о модерации)
    fun isMemorialApproved(): Boolean {
        // Защита от null
        if (type == null) return false
        
        // Прямой тип одобрения семейного дерева
        if (type == NotificationType.FAMILY_TREE_APPROVED) return true
        
        // Для других типов проверяем заголовок
        if (isRelatedToModeration()) {
            val titleLower = title?.lowercase() ?: ""
            return titleLower.contains("опубликован") && !titleLower.contains("не опубликован")
        }
        
        return false
    }
}

@Parcelize
data class ModerationInfo(
    @SerializedName("moderationStatus")
    val moderationStatus: String? = null,
    
    @SerializedName("moderationMessage")
    val moderationMessage: String? = null,
    
    @SerializedName("moderatedAt")
    val moderatedAt: String? = null
) : Parcelable

enum class NotificationType {
    @SerializedName("MEMORIAL_OWNERSHIP")
    MEMORIAL_OWNERSHIP,
    
    @SerializedName("MEMORIAL_CHANGES")
    MEMORIAL_CHANGES,
    
    @SerializedName("SYSTEM")
    SYSTEM,
    
    @SerializedName("INFO")
    INFO,
    
    @SerializedName("MEMORIAL_EDIT")
    MEMORIAL_EDIT,
    
    @SerializedName("MODERATION")
    MODERATION,
    
    @SerializedName("CHANGES_REJECTED")
    CHANGES_REJECTED,
    
    @SerializedName("MEMORIAL_MODERATION")
    MEMORIAL_MODERATION,
    
    @SerializedName("CHANGES_MODERATION")
    CHANGES_MODERATION,
    
    @SerializedName("MEMORIAL_APPROVED")
    MEMORIAL_APPROVED,
    
    @SerializedName("MEMORIAL_REJECTED")
    MEMORIAL_REJECTED,
    
    @SerializedName("TECHNICAL")
    TECHNICAL,
    
    @SerializedName("ADMIN_INFO")
    ADMIN_INFO,
    
    @SerializedName("MASS_ANNOUNCEMENT")
    MASS_ANNOUNCEMENT,
    
    @SerializedName("ADMIN_SYSTEM")
    ADMIN_SYSTEM,
    
    @SerializedName("ADMIN_WARNING")
    ADMIN_WARNING,
    
    @SerializedName("MEMORIAL_REPORT")
    MEMORIAL_REPORT,
    
    // Типы уведомлений для семейных деревьев
    @SerializedName("FAMILY_TREE_MODERATION")
    FAMILY_TREE_MODERATION,
    
    @SerializedName("FAMILY_TREE_APPROVED")
    FAMILY_TREE_APPROVED,
    
    @SerializedName("FAMILY_TREE_REJECTED")
    FAMILY_TREE_REJECTED,
    
    @SerializedName("FAMILY_TREE_ACCESS_GRANTED")
    FAMILY_TREE_ACCESS_GRANTED,
    
    @SerializedName("FAMILY_TREE_ACCESS_REVOKED")
    FAMILY_TREE_ACCESS_REVOKED
}

enum class NotificationStatus {
    @SerializedName("PENDING")
    PENDING,
    
    @SerializedName("ACCEPTED")
    ACCEPTED,
    
    @SerializedName("REJECTED")
    REJECTED,
    
    @SerializedName("PROCESSED")
    PROCESSED
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