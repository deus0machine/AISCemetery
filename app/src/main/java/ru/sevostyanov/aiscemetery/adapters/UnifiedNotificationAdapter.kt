package ru.sevostyanov.aiscemetery.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.*
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.JsonParser

class UnifiedNotificationAdapter(
    private val isIncoming: Boolean,
    private val onRegularNotificationClick: (Notification) -> Unit = {},
    private val onRegularAcceptClick: (Notification) -> Unit = {},
    private val onRegularRejectClick: (Notification) -> Unit = {},
    private val onRegularDeleteClick: (Notification) -> Unit = {},
    private val onDraftNotificationClick: (DraftSubmission) -> Unit = {},
    private val onDraftApproveClick: (DraftSubmission) -> Unit = {},
    private val onDraftRejectClick: (DraftSubmission) -> Unit = {}
) : ListAdapter<NotificationItem, RecyclerView.ViewHolder>(NotificationItemDiffCallback()) {

    companion object {
        private const val TYPE_REGULAR = 0
        private const val TYPE_DRAFT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RegularNotificationItem -> TYPE_REGULAR
            is DraftNotificationItem -> TYPE_DRAFT
            else -> TYPE_REGULAR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_REGULAR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification, parent, false)
                RegularNotificationViewHolder(view, isIncoming, onRegularNotificationClick, onRegularAcceptClick, onRegularRejectClick, onRegularDeleteClick)
            }
            TYPE_DRAFT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_draft_submission, parent, false)
                DraftNotificationViewHolder(view, isIncoming, onDraftNotificationClick, onDraftApproveClick, onDraftRejectClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RegularNotificationItem -> (holder as RegularNotificationViewHolder).bind(item.notification)
            is DraftNotificationItem -> (holder as DraftNotificationViewHolder).bind(item.draftSubmission)
        }
    }

    // ViewHolder для обычных уведомлений
    class RegularNotificationViewHolder(
        itemView: View,
        private val isIncoming: Boolean,
        private val onItemClick: (Notification) -> Unit,
        private val onAcceptClick: (Notification) -> Unit,
        private val onRejectClick: (Notification) -> Unit,
        private val onDeleteClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val titleTextView: TextView? = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView? = itemView.findViewById(R.id.text_message)
        private val dateTextView: TextView? = itemView.findViewById(R.id.text_date)
        private val statusTextView: TextView? = itemView.findViewById(R.id.text_status)
        private val userInfoTextView: TextView? = itemView.findViewById(R.id.text_user_info)
        private val memorialInfoTextView: TextView? = itemView.findViewById(R.id.text_memorial_info)
        private val typeBadgeTextView: TextView? = itemView.findViewById(R.id.text_type_badge)
        private val acceptButton: Button? = itemView.findViewById(R.id.btn_accept)
        private val rejectButton: Button? = itemView.findViewById(R.id.btn_reject)
        
        fun bind(notification: Notification) {
            android.util.Log.d("UnifiedAdapter", "Binding regular notification: id=${notification.id}, status=${notification.status}, type=${notification.type}")
            
            // Заголовок
            titleTextView?.text = notification.title ?: getDefaultTitle(notification)
            
            // Сообщение
            messageTextView?.text = notification.message
            
            // Дата
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(notification.createdAt)
                if (date != null) outputFormat.format(date) else notification.createdAt
            } catch (e: Exception) {
                notification.createdAt
            }
            dateTextView?.text = formattedDate
            
            // Информация о пользователе
            configureUserInfo(notification)
            
            // Информация о мемориале
            memorialInfoTextView?.text = if (notification.relatedEntityName != null) {
                "Мемориал: ${notification.relatedEntityName}"
            } else null
            memorialInfoTextView?.visibility = if (notification.relatedEntityName != null) View.VISIBLE else View.GONE
            
            // Тип уведомления (бейдж)
            configureTypeBadge(notification)
            
            // Статус
            configureStatus(notification)
            
            // Стиль карточки по типу
            styleNotificationByType(notification)
            
            // Кнопки действий
            configureActionButtons(notification)
            
            // Обработчики кликов
            itemView.setOnClickListener { onItemClick(notification) }
        }
        
        private fun getDefaultTitle(notification: Notification): String {
            return when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> "Запрос на совместное владение"
                NotificationType.MEMORIAL_CHANGES -> "Запрос на изменение мемориала"
                NotificationType.MEMORIAL_EDIT -> "Изменения в мемориале"
                NotificationType.FAMILY_TREE_ACCESS_REQUEST -> "Запрос на доступ к дереву"
                NotificationType.FAMILY_TREE_ACCESS_GRANTED -> "Доступ к дереву предоставлен"
                NotificationType.FAMILY_TREE_ACCESS_REVOKED -> "Доступ к дереву отозван"
                NotificationType.SYSTEM -> "Системное уведомление"
                NotificationType.INFO -> "Информационное уведомление"
                NotificationType.MODERATION -> "Уведомление о модерации"
                else -> "Уведомление"
            }
        }
        
        private fun configureUserInfo(notification: Notification) {
            if (isIncoming) {
                // Для входящих показываем отправителя только для запросов
                if (notification.status == NotificationStatus.PENDING) {
                    userInfoTextView?.text = if (notification.senderName != null) {
                        "От: ${notification.senderName}"
                    } else {
                        "От: Система"
                    }
                    userInfoTextView?.visibility = View.VISIBLE
                } else {
                    // Для информационных уведомлений не показываем отправителя
                    userInfoTextView?.visibility = View.GONE
                }
            } else {
                // Для исходящих показываем получателя
                userInfoTextView?.text = if (notification.receiverName != null) {
                    "Кому: ${notification.receiverName}"
                } else {
                    "Отправлено"
                }
                userInfoTextView?.visibility = View.VISIBLE
            }
        }
        
        private fun configureTypeBadge(notification: Notification) {
            val badgeText = when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> "СОВМЕСТНОЕ ВЛАДЕНИЕ"
                NotificationType.MEMORIAL_CHANGES -> "ИЗМЕНЕНИЕ МЕМОРИАЛА"
                NotificationType.MEMORIAL_EDIT -> "РЕДАКТИРОВАНИЕ"
                NotificationType.FAMILY_TREE_ACCESS_REQUEST -> "ЗАПРОС НА ДОСТУП"
                NotificationType.FAMILY_TREE_ACCESS_GRANTED -> "ДОСТУП ПРЕДОСТАВЛЕН"
                NotificationType.FAMILY_TREE_ACCESS_REVOKED -> "ДОСТУП ОТОЗВАН"
                NotificationType.SYSTEM -> "СИСТЕМА"
                NotificationType.INFO -> "ИНФОРМАЦИЯ"
                NotificationType.MODERATION -> "МОДЕРАЦИЯ"
                else -> "УВЕДОМЛЕНИЕ"
            }
            typeBadgeTextView?.text = badgeText
        }
        
        private fun configureStatus(notification: Notification) {
            val statusText = when (notification.status) {
                NotificationStatus.PENDING -> "Ожидает рассмотрения"
                NotificationStatus.ACCEPTED -> "Принято"
                NotificationStatus.REJECTED -> "Отклонено"
                NotificationStatus.PROCESSED -> "Обработано"
                null -> "Неизвестно"
            }
            statusTextView?.text = statusText
            
            // Цвет статуса
            val statusColor = when (notification.status) {
                NotificationStatus.PENDING -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                NotificationStatus.ACCEPTED -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                NotificationStatus.REJECTED -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                NotificationStatus.PROCESSED -> ContextCompat.getColor(itemView.context, R.color.teal_700)
                null -> ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
            }
            statusTextView?.setTextColor(statusColor)
        }
        
        private fun styleNotificationByType(notification: Notification) {
            var backgroundColorHex: String
            var borderColor: Int
            
            when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> {
                    backgroundColorHex = "#FFF8E1" // Light amber
                    borderColor = ContextCompat.getColor(itemView.context, R.color.gold)
                }
                NotificationType.FAMILY_TREE_ACCESS_REQUEST -> {
                    backgroundColorHex = "#FFF8E1" // Light amber
                    borderColor = ContextCompat.getColor(itemView.context, R.color.gold)
                }
                NotificationType.FAMILY_TREE_ACCESS_GRANTED -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                }
                NotificationType.FAMILY_TREE_ACCESS_REVOKED -> {
                    backgroundColorHex = "#FFEBEE" // Light red
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                }
                NotificationType.MEMORIAL_CHANGES -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                }
                NotificationType.MEMORIAL_EDIT -> {
                    backgroundColorHex = "#FFECB3" // Light orange
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                }
                NotificationType.INFO -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(itemView.context, R.color.teal_700)
                }
                NotificationType.SYSTEM -> {
                    backgroundColorHex = "#E3F2FD" // Light blue
                    borderColor = ContextCompat.getColor(itemView.context, R.color.teal_700)
                }
                NotificationType.MODERATION -> {
                    backgroundColorHex = "#FFF3E0" // Light orange 
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                }
                else -> {
                    backgroundColorHex = "#F5F5F5" // Light grey
                    borderColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                }
            }
            
            cardView.setCardBackgroundColor(Color.parseColor(backgroundColorHex))
            cardView.strokeColor = borderColor
            cardView.strokeWidth = 4
        }
        
        private fun configureActionButtons(notification: Notification) {
            // Показываем кнопки только для входящих уведомлений со статусом PENDING
            // И только для определенных типов уведомлений
            val showActionButtons = isIncoming && 
                                   notification.status == NotificationStatus.PENDING &&
                                   isActionableNotificationType(notification.type)
            
            acceptButton?.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            rejectButton?.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            
            if (showActionButtons) {
                // Настраиваем текст кнопок в зависимости от типа
                when (notification.type) {
                    NotificationType.MEMORIAL_OWNERSHIP -> {
                        acceptButton?.text = "Разрешить"
                        rejectButton?.text = "Отклонить"
                    }
                    NotificationType.MEMORIAL_CHANGES -> {
                        acceptButton?.text = "Подтвердить"
                        rejectButton?.text = "Отклонить"
                    }
                    NotificationType.MEMORIAL_EDIT -> {
                        acceptButton?.text = "Принять"
                        rejectButton?.text = "Отклонить"
                    }
                    NotificationType.FAMILY_TREE_ACCESS_REQUEST -> {
                        acceptButton?.text = "Предоставить"
                        rejectButton?.text = "Отклонить"
                    }
                    else -> {
                        acceptButton?.text = "Принять"
                        rejectButton?.text = "Отклонить"
                    }
                }
                
                acceptButton?.setOnClickListener { onAcceptClick(notification) }
                rejectButton?.setOnClickListener { onRejectClick(notification) }
            }
        }
        
        private fun isActionableNotificationType(type: NotificationType?): Boolean {
            return when (type) {
                NotificationType.MEMORIAL_OWNERSHIP,
                NotificationType.MEMORIAL_CHANGES,
                NotificationType.MEMORIAL_EDIT,
                NotificationType.FAMILY_TREE_ACCESS_REQUEST -> true
                else -> false
            }
        }
    }

    // ViewHolder для уведомлений о черновиках
    class DraftNotificationViewHolder(
        itemView: View,
        private val isIncoming: Boolean,
        private val onItemClick: (DraftSubmission) -> Unit,
        private val onApproveClick: (DraftSubmission) -> Unit,
        private val onRejectClick: (DraftSubmission) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.text_message)
        private val treeNameTextView: TextView = itemView.findViewById(R.id.text_tree_name)
        private val editorNameTextView: TextView = itemView.findViewById(R.id.text_editor_name)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_date)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_status)
        private val memorialsCountTextView: TextView = itemView.findViewById(R.id.text_memorials_count)
        private val relationsCountTextView: TextView = itemView.findViewById(R.id.text_relations_count)
        private val approveButton: Button = itemView.findViewById(R.id.button_approve)
        private val rejectButton: Button = itemView.findViewById(R.id.button_reject)

        fun bind(submission: DraftSubmission) {
            val context = itemView.context
            
            android.util.Log.d("UnifiedAdapter", "Binding draft submission: id=${submission.id}, reviewStatus=${submission.reviewStatus}, isReviewed=${submission.isReviewed}")
            
            // Заголовок
            titleTextView.text = if (isIncoming) {
                "Предложены изменения в дереве"
            } else {
                "Отправлены изменения на рассмотрение"
            }
            
            // Сообщение от редактора
            messageTextView.text = submission.message ?: "Без сообщения"
            
            // Название дерева
            treeNameTextView.text = "Дерево: ${submission.draft.familyTree?.name ?: "Неизвестно"}"
            
            // Имя редактора (для входящих) или владельца (для исходящих)
            if (isIncoming) {
                editorNameTextView.text = "От: ${submission.draft.editor?.fio ?: submission.draft.editor?.login ?: "Неизвестно"}"
            } else {
                editorNameTextView.text = "Владелец: ${submission.draft.familyTree?.owner?.fio ?: submission.draft.familyTree?.owner?.login ?: "Неизвестно"}"
            }
            
            // Статистика изменений из черновика
            try {
                // Парсим JSON с мемориалами черновика
                val memorialsJson = submission.draft.draftMemorialsJson
                val relationsJson = submission.draft.draftRelationsJson
                
                android.util.Log.d("UnifiedAdapter", "=== Draft JSON Debug ===")
                android.util.Log.d("UnifiedAdapter", "Draft ID: ${submission.draft.id}")
                android.util.Log.d("UnifiedAdapter", "Memorials JSON: ${memorialsJson?.take(200) ?: "null"}...")
                android.util.Log.d("UnifiedAdapter", "Relations JSON: ${relationsJson?.take(200) ?: "null"}...")
                
                val memorialsCount = if (!memorialsJson.isNullOrEmpty()) {
                    try {
                        val jsonArray = JsonParser.parseString(memorialsJson).asJsonArray
                        jsonArray.size()
                    } catch (e: Exception) {
                        android.util.Log.e("UnifiedAdapter", "Error parsing memorials JSON", e)
                        0
                    }
                } else {
                    0
                }
                
                val relationsCount = if (!relationsJson.isNullOrEmpty()) {
                    try {
                        val jsonArray = JsonParser.parseString(relationsJson).asJsonArray
                        jsonArray.size()
                    } catch (e: Exception) {
                        android.util.Log.e("UnifiedAdapter", "Error parsing relations JSON", e)
                        0
                    }
                } else {
                    0
                }
                
                memorialsCountTextView.text = "Мемориалы: $memorialsCount"
                relationsCountTextView.text = "Связи: $relationsCount"
                
                android.util.Log.d("UnifiedAdapter", "Draft stats: memorials=$memorialsCount, relations=$relationsCount")
                android.util.Log.d("UnifiedAdapter", "Memorials JSON length: ${memorialsJson?.length ?: 0}")
                android.util.Log.d("UnifiedAdapter", "Relations JSON length: ${relationsJson?.length ?: 0}")
                
            } catch (e: Exception) {
                android.util.Log.e("UnifiedAdapter", "Error parsing draft stats", e)
                memorialsCountTextView.text = "Мемориалы: ?"
                relationsCountTextView.text = "Связи: ?"
            }
            
            // Дата отправки
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(submission.submittedAt)
                if (date != null) outputFormat.format(date) else submission.submittedAt
            } catch (e: Exception) {
                submission.submittedAt
            }
            dateTextView.text = "Отправлено: $formattedDate"
            
            // Статус
            val statusText = when (submission.reviewStatus) {
                DraftSubmission.ReviewStatus.PENDING -> "Ожидает рассмотрения"
                DraftSubmission.ReviewStatus.APPROVED -> "Одобрено"
                DraftSubmission.ReviewStatus.REJECTED -> "Отклонено"
                null -> "Ожидает рассмотрения"
            }
            statusTextView.text = statusText
            
            // Цвет статуса
            val statusColor = when (submission.reviewStatus) {
                DraftSubmission.ReviewStatus.PENDING -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                DraftSubmission.ReviewStatus.APPROVED -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                DraftSubmission.ReviewStatus.REJECTED -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                null -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            }
            statusTextView.setTextColor(statusColor)
            
            // Цвет обводки карточки
            cardView.strokeColor = statusColor
            cardView.strokeWidth = 4
            
            // Кнопки действий (только для входящих и не рассмотренных)
            val showActionButtons = isIncoming && !submission.isReviewed
            approveButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            rejectButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            
            // Обработчики кликов
            itemView.setOnClickListener { onItemClick(submission) }
            approveButton.setOnClickListener { onApproveClick(submission) }
            rejectButton.setOnClickListener { onRejectClick(submission) }
        }
    }

    private class NotificationItemDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id && oldItem::class == newItem::class
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
} 