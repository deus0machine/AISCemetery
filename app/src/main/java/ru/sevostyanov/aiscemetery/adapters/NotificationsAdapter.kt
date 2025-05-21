package ru.sevostyanov.aiscemetery.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationStatus
import ru.sevostyanov.aiscemetery.models.NotificationType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationsAdapter(
    private val isIncoming: Boolean = true,
    private val onAcceptClick: ((Notification) -> Unit)? = null,
    private val onRejectClick: ((Notification) -> Unit)? = null,
    private val onItemClick: ((Notification) -> Unit)? = null
) : ListAdapter<Notification, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val TYPE_STANDARD = 0
        private const val TYPE_INFO = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        val notification = getItem(position)
        return when {
            // Информационные уведомления без кнопок
            notification.type == NotificationType.INFO || 
            notification.type == NotificationType.SYSTEM ||
            notification.status == NotificationStatus.ACCEPTED ||
            notification.status == NotificationStatus.REJECTED -> TYPE_INFO
            
            // Стандартные уведомления с возможными действиями
            else -> TYPE_STANDARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_INFO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_info, parent, false)
                InfoNotificationViewHolder(view, onItemClick)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification, parent, false)
                StandardNotificationViewHolder(view, isIncoming, onAcceptClick, onRejectClick, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = getItem(position)
        when (holder) {
            is StandardNotificationViewHolder -> holder.bind(notification)
            is InfoNotificationViewHolder -> holder.bind(notification)
        }
    }

    // Отдельный ViewHolder для информационных уведомлений
    class InfoNotificationViewHolder(
        itemView: View,
        private val onItemClick: ((Notification) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.text_message)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_date)
        private val memorialInfoTextView: TextView = itemView.findViewById(R.id.text_memorial_info)
        private val iconView: ImageView = itemView.findViewById(R.id.icon_notification)
        
        private val context: Context = itemView.context
        
        fun bind(notification: Notification) {
            // Устанавливаем слушатель кликов
            itemView.setOnClickListener {
                onItemClick?.invoke(notification)
            }
            
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            
            try {
                // Parse the ISO date format from the server
                val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                
                val datetime = LocalDateTime.parse(notification.createdAt, inputFormatter)
                dateTextView.text = outputFormatter.format(datetime)
            } catch (e: Exception) {
                // Fallback if date parsing fails
                dateTextView.text = notification.createdAt
            }
            
            // Настройка информации о мемориале
            if (notification.relatedEntityName != null) {
                memorialInfoTextView.text = "Мемориал: ${notification.relatedEntityName}"
                memorialInfoTextView.visibility = View.VISIBLE
            } else {
                memorialInfoTextView.visibility = View.GONE
            }
            
            // Настройка иконки и цвета в зависимости от типа и статуса
            configureIconAndColor(notification)
        }
        
        private fun configureIconAndColor(notification: Notification) {
            // Определяем иконку и цвет
            val iconRes: Int
            val colorRes: Int
            
            when (notification.status) {
                NotificationStatus.ACCEPTED -> {
                    iconRes = android.R.drawable.ic_dialog_info
                    colorRes = R.color.green
                    cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // светло-зеленый
                }
                NotificationStatus.REJECTED -> {
                    iconRes = android.R.drawable.ic_dialog_alert
                    colorRes = R.color.red
                    cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // светло-красный
                }
                else -> {
                    when (notification.type) {
                        NotificationType.INFO -> {
                            iconRes = android.R.drawable.ic_dialog_info
                            colorRes = R.color.purple_300
                        }
                        NotificationType.SYSTEM -> {
                            iconRes = android.R.drawable.ic_dialog_info
                            colorRes = R.color.teal_700
                        }
                        NotificationType.ANNIVERSARY -> {
                            iconRes = android.R.drawable.ic_menu_my_calendar
                            colorRes = R.color.purple_200
                        }
                        else -> {
                            iconRes = android.R.drawable.ic_dialog_info
                            colorRes = R.color.grey
                        }
                    }
                    cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5")) // светло-серый
                }
            }
            
            // Устанавливаем иконку и цвет
            iconView.setImageResource(iconRes)
            iconView.setColorFilter(ContextCompat.getColor(context, colorRes))
        }
    }

    // ViewHolder для стандартных уведомлений с кнопками действий
    class StandardNotificationViewHolder(
        itemView: View,
        private val isIncoming: Boolean,
        private val onAcceptClick: ((Notification) -> Unit)?,
        private val onRejectClick: ((Notification) -> Unit)?,
        private val onItemClick: ((Notification) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.text_message)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_date)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_status)
        private val userInfoTextView: TextView = itemView.findViewById(R.id.text_user_info)
        private val memorialInfoTextView: TextView = itemView.findViewById(R.id.text_memorial_info)
        private val unreadIndicator: View = itemView.findViewById(R.id.view_unread)
        private val acceptButton: Button = itemView.findViewById(R.id.btn_accept)
        private val rejectButton: Button = itemView.findViewById(R.id.btn_reject)

        private val context: Context = itemView.context

        fun bind(notification: Notification) {
            // Устанавливаем слушатель кликов
            itemView.setOnClickListener {
                onItemClick?.invoke(notification)
            }
            
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            
            try {
                // Parse the ISO date format from the server
                val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                
                val datetime = LocalDateTime.parse(notification.createdAt, inputFormatter)
                dateTextView.text = outputFormatter.format(datetime)
            } catch (e: Exception) {
                // Fallback if date parsing fails
                dateTextView.text = notification.createdAt
            }
            
            // Настройка индикатора непрочитанности
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Настройка информации о пользователе
            configureUserInfo(notification)
            
            // Настройка информации о мемориале
            if (notification.relatedEntityName != null) {
                memorialInfoTextView.text = "Мемориал: ${notification.relatedEntityName}"
                memorialInfoTextView.visibility = View.VISIBLE
            } else {
                memorialInfoTextView.visibility = View.GONE
            }
            
            // Настройка статуса уведомления
            configureStatus(notification)
            
            // Настройка внешнего вида в зависимости от типа уведомления
            styleNotificationByType(notification)
            
            // Настройка кнопок действий
            configureActionButtons(notification)
        }
        
        private fun configureUserInfo(notification: Notification) {
            if (isIncoming) {
                // Для входящих обычных запросов показываем отправителя
                if (notification.status == NotificationStatus.PENDING || 
                    (notification.type != NotificationType.INFO && notification.type != NotificationType.SYSTEM)) {
                    
                    userInfoTextView.text = if (notification.senderName != null) {
                        "От: ${notification.senderName}"
                    } else {
                        "От: Система"
                    }
                    userInfoTextView.visibility = View.VISIBLE
                } else {
                    // Для информационных уведомлений (ответов) не показываем отправителя
                    userInfoTextView.visibility = View.GONE
                }
            } else {
                // Для исходящих показываем информацию о том, кому отправлено
                userInfoTextView.text = "Отправлено: " + when (notification.status) {
                    NotificationStatus.PENDING -> "ожидает ответа"
                    NotificationStatus.ACCEPTED -> "запрос принят"
                    NotificationStatus.REJECTED -> "запрос отклонён"
                    NotificationStatus.INFO -> ""
                }
                userInfoTextView.visibility = View.VISIBLE
            }
        }
        
        private fun configureStatus(notification: Notification) {
            val statusText = when (notification.status) {
                NotificationStatus.PENDING -> "Ожидает ответа"
                NotificationStatus.ACCEPTED -> "Принято"
                NotificationStatus.REJECTED -> "Отклонено"
                NotificationStatus.INFO -> ""
            }
            statusTextView.text = statusText
            statusTextView.visibility = if (statusText.isEmpty()) View.GONE else View.VISIBLE
            
            // Устанавливаем цвет текста статуса
            when (notification.status) {
                NotificationStatus.ACCEPTED -> statusTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.green)
                )
                NotificationStatus.REJECTED -> statusTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.red)
                )
                else -> statusTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.secondary_text)
                )
            }
        }
        
        private fun styleNotificationByType(notification: Notification) {
            // Создаем drawable для цветной полоски слева
            val strokeDrawable = cardView.background as? GradientDrawable ?: GradientDrawable()
            
            // Определяем цвет в зависимости от типа
            val colorRes = when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> R.color.purple_500
                NotificationType.TREE_ACCESS_REQUEST -> R.color.teal_700
                NotificationType.MEMORIAL_COMMENT -> R.color.purple_300
                NotificationType.ANNIVERSARY -> R.color.purple_200
                NotificationType.INFO, NotificationType.SYSTEM -> {
                    // Для информационных уведомлений разные цвета в зависимости от статуса
                    when (notification.status) {
                        NotificationStatus.ACCEPTED -> R.color.green
                        NotificationStatus.REJECTED -> R.color.red
                        else -> R.color.grey
                    }
                }
            }
            
            // Применяем стили
            val color = ContextCompat.getColor(context, colorRes)
            
            // Применяем цвет к индикатору непрочитанных
            unreadIndicator.setBackgroundColor(color)
        }
        
        private fun configureActionButtons(notification: Notification) {
            // Показываем кнопки только для входящих запросов со статусом PENDING
            // И только для определенных типов
            val isActionable = isIncoming && 
                notification.status == NotificationStatus.PENDING &&
                (notification.type == NotificationType.MEMORIAL_OWNERSHIP || 
                notification.type == NotificationType.TREE_ACCESS_REQUEST)
            
            acceptButton.visibility = if (isActionable && onAcceptClick != null) View.VISIBLE else View.GONE
            rejectButton.visibility = if (isActionable && onRejectClick != null) View.VISIBLE else View.GONE
            
            // Установка обработчиков нажатий
            acceptButton.setOnClickListener {
                onAcceptClick?.invoke(notification)
            }
            
            rejectButton.setOnClickListener {
                onRejectClick?.invoke(notification)
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
} 