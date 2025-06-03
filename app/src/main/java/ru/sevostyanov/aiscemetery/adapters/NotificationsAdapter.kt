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
    private val onItemClick: ((Notification) -> Unit)? = null,
    private val onDeleteClick: ((Notification) -> Unit)? = null
) : ListAdapter<Notification, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val TYPE_STANDARD = 0
        private const val TYPE_INFO = 1
        
        // Вспомогательная функция для получения текста и цвета бэйджа типа уведомления
        fun getNotificationTypeBadge(notification: Notification): Pair<String, Int> {
            return when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> Pair("ЗАПРОС НА ДОСТУП", R.color.gold)
                NotificationType.MEMORIAL_CHANGES -> Pair("ИЗМЕНЕНИЯ МЕМОРИАЛА", R.color.green)
                NotificationType.MEMORIAL_EDIT -> Pair("РЕДАКТИРОВАНИЕ", R.color.orange)
                NotificationType.INFO -> Pair("ИНФОРМАЦИЯ", R.color.teal_700)
                NotificationType.SYSTEM -> {
                    // Проверяем, является ли это ответом на техническое обращение
                    val isTechnicalResponse = notification.title?.contains("Ответ на техническое обращение") == true ||
                            notification.relatedEntityName == "Техническая поддержка"
                    
                    if (isTechnicalResponse) {
                        Pair("ОТВЕТ ПОДДЕРЖКИ", R.color.green)
                    } else {
                        // Проверяем тип модерации
                        val titleContainsPublished = notification.title?.contains("опубликован") == true
                        val titleContainsNotPublished = notification.title?.contains("не опубликован") == true
                        val titleContainsRejected = notification.title?.contains("отклонен") == true
                        
                        when {
                            titleContainsPublished && !titleContainsNotPublished -> Pair("ОДОБРЕНО", R.color.green)
                            titleContainsNotPublished || titleContainsRejected -> Pair("ОТКЛОНЕНО", R.color.red)
                            else -> Pair("СИСТЕМА", R.color.teal_700)
                        }
                    }
                }
                NotificationType.MODERATION -> Pair("МОДЕРАЦИЯ", R.color.orange)
                NotificationType.TECHNICAL -> Pair("ТЕХПОДДЕРЖКА", R.color.teal_700)
                NotificationType.ADMIN_INFO -> Pair("АДМИНИСТРАЦИЯ", R.color.teal_700)
                NotificationType.MASS_ANNOUNCEMENT -> Pair("ВАЖНОЕ ОБЪЯВЛЕНИЕ", R.color.purple_500)
                NotificationType.ADMIN_SYSTEM -> Pair("АДМИН СИСТЕМА", R.color.grey)
                NotificationType.ADMIN_WARNING -> Pair("ПРЕДУПРЕЖДЕНИЕ", R.color.red)
                else -> Pair("УВЕДОМЛЕНИЕ", R.color.grey)
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        val notification = getItem(position)
        return when {
            // Информационные уведомления без кнопок или уже обработанные
            notification.type == NotificationType.SYSTEM ||
            notification.type == NotificationType.INFO ||
            notification.status == NotificationStatus.ACCEPTED ||
            notification.status == NotificationStatus.REJECTED ||
            notification.status == NotificationStatus.PROCESSED ||
            // Входящие технические уведомления (ответы от админов) - информационные
            (notification.type == NotificationType.TECHNICAL && isIncoming) ||
            // Все административные уведомления - информационные
            notification.type == NotificationType.ADMIN_INFO ||
            notification.type == NotificationType.MASS_ANNOUNCEMENT ||
            notification.type == NotificationType.ADMIN_SYSTEM ||
            notification.type == NotificationType.ADMIN_WARNING -> TYPE_INFO
            
            // Стандартные уведомления с возможными действиями (включая исходящие технические)
            else -> TYPE_STANDARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_INFO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_info, parent, false)
                InfoNotificationViewHolder(view, isIncoming, onItemClick, onDeleteClick)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification, parent, false)
                StandardNotificationViewHolder(view, isIncoming, onAcceptClick, onRejectClick, onItemClick, onDeleteClick)
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
        private val isIncoming: Boolean,
        private val onItemClick: ((Notification) -> Unit)?,
        private val onDeleteClick: ((Notification) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.text_message)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_date)
        private val memorialInfoTextView: TextView = itemView.findViewById(R.id.text_memorial_info)
        private val iconView: ImageView = itemView.findViewById(R.id.icon_notification)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete)
        private val typeBadgeTextView: TextView = itemView.findViewById(R.id.text_type_badge)
        
        private val context: Context = itemView.context
        
        fun bind(notification: Notification) {
            // Устанавливаем слушатель кликов
            itemView.setOnClickListener {
                onItemClick?.invoke(notification)
            }
            
            // Настраиваем кнопку удаления
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(notification)
            }
            
            // Устанавливаем заголовок в зависимости от типа
            val title = when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> {
                    // Для запросов на совместное владение учитываем статус
                    when (notification.status) {
                        NotificationStatus.ACCEPTED -> notification.title ?: "Запрос на совместное владение принят"
                        NotificationStatus.REJECTED -> notification.title ?: "Запрос на совместное владение отклонён"
                        else -> "Запрос на совместное владение"
                    }
                }
                NotificationType.MEMORIAL_CHANGES -> "Запрос на изменение мемориала"
                NotificationType.MEMORIAL_EDIT -> {
                    // Для уведомлений об изменениях тоже учитываем статус
                    when (notification.status) {
                        NotificationStatus.ACCEPTED -> notification.title ?: "Изменения в мемориале приняты"
                        NotificationStatus.REJECTED -> notification.title ?: "Изменения в мемориале отклонены"
                        else -> "Изменения в мемориале"
                    }
                }
                NotificationType.INFO -> notification.title ?: "Информация"
                NotificationType.SYSTEM -> {
                    // Проверяем, является ли это ответом на техническое обращение
                    if (notification.title?.contains("Ответ на техническое обращение") == true ||
                        notification.relatedEntityName == "Техническая поддержка") {
                        notification.title ?: "Ответ технической поддержки"
                    } else {
                    // Проверяем текст заголовка для определения типа системного уведомления
                    val titleContainsPublished = notification.title?.contains("опубликован") == true
                    val titleContainsNotPublished = notification.title?.contains("не опубликован") == true
                    val titleContainsRejected = notification.title?.contains("отклонен") == true
                    
                    if (titleContainsPublished && !titleContainsNotPublished) {
                        // Уведомление об одобрении публикации
                        notification.title ?: "Системное уведомление"
                    } else if (titleContainsNotPublished || titleContainsRejected) {
                        // Уведомление об отклонении публикации
                        notification.title ?: "Системное уведомление"
                    } else {
                        // Обычное системное уведомление
                        notification.title ?: "Системное уведомление"
                        }
                    }
                }
                NotificationType.MODERATION -> "Запрос на модерацию мемориала"
                NotificationType.TECHNICAL -> {
                    // Для технических уведомлений разные заголовки в зависимости от направления
                    if (isIncoming) {
                        notification.title ?: "Технические вопросы"
                    } else {
                        "Обращение в техподдержку"
                    }
                }
                NotificationType.ADMIN_INFO -> notification.title ?: "Информация от администратора"
                NotificationType.MASS_ANNOUNCEMENT -> notification.title ?: "Важное объявление"
                NotificationType.ADMIN_SYSTEM -> notification.title ?: "Системное уведомление"
                NotificationType.ADMIN_WARNING -> notification.title ?: "Предупреждение"
                else -> "Уведомление"
            }
            titleTextView.text = title
            
            messageTextView.text = notification.message
            
            try {
                // Parse the ISO date format from the server
                val dateTime = LocalDateTime.parse(notification.createdAt, DateTimeFormatter.ISO_DATE_TIME)
                val formatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                dateTextView.text = formatted
            } catch (e: Exception) {
                // Fallback if date parsing fails
                dateTextView.text = notification.createdAt
            }
            
            // Настройка информации о мемориале
            if (notification.relatedEntityName != null && 
                notification.relatedEntityName != "Техническая поддержка") {
                memorialInfoTextView.text = "Мемориал: ${notification.relatedEntityName}"
                memorialInfoTextView.visibility = View.VISIBLE
            } else {
                memorialInfoTextView.visibility = View.GONE
            }
            
            // Устанавливаем иконку и цвет
            val (iconRes, colorRes) = when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> Pair(android.R.drawable.ic_menu_share, R.color.gold)
                NotificationType.MEMORIAL_CHANGES -> Pair(android.R.drawable.ic_menu_edit, R.color.green)
                NotificationType.MEMORIAL_EDIT -> Pair(android.R.drawable.ic_menu_edit, R.color.orange)
                NotificationType.SYSTEM -> {
                    // Проверяем, является ли это ответом на техническое обращение
                    val isTechnicalResponse = notification.title?.contains("Ответ на техническое обращение") == true ||
                            notification.relatedEntityName == "Техническая поддержка"
                    
                    if (isTechnicalResponse) {
                        // Ответ технической поддержки
                        Pair(android.R.drawable.ic_dialog_email, R.color.green)
                    } else {
                    // Проверяем текст заголовка для определения типа системного уведомления
                    val titleContainsPublished = notification.title?.contains("опубликован") == true
                    val titleContainsNotPublished = notification.title?.contains("не опубликован") == true
                    val titleContainsRejected = notification.title?.contains("отклонен") == true
                    
                    if (titleContainsPublished && !titleContainsNotPublished) {
                        // Уведомление об одобрении публикации
                        Pair(android.R.drawable.ic_menu_upload, R.color.green)
                    } else if (titleContainsNotPublished || titleContainsRejected) {
                        // Уведомление об отклонении публикации
                        Pair(android.R.drawable.ic_menu_close_clear_cancel, R.color.red)
                    } else {
                        // Обычное системное уведомление
                        Pair(android.R.drawable.ic_dialog_info, R.color.teal_700)
                        }
                    }
                }
                NotificationType.MODERATION -> Pair(android.R.drawable.ic_menu_view, R.color.orange)
                NotificationType.TECHNICAL -> Pair(android.R.drawable.ic_dialog_info, R.color.teal_700)
                NotificationType.ADMIN_INFO -> Pair(android.R.drawable.ic_dialog_info, R.color.teal_700)
                NotificationType.MASS_ANNOUNCEMENT -> Pair(android.R.drawable.ic_dialog_alert, R.color.purple_500)
                NotificationType.ADMIN_SYSTEM -> Pair(android.R.drawable.ic_menu_manage, R.color.grey)
                NotificationType.ADMIN_WARNING -> Pair(android.R.drawable.ic_dialog_dialer, R.color.red)
                NotificationType.INFO -> Pair(android.R.drawable.ic_dialog_info, R.color.teal_700)
                else -> Pair(android.R.drawable.ic_dialog_email, R.color.grey)
            }
            
            // Устанавливаем иконку и цвет
            iconView.setImageResource(iconRes)
            iconView.setColorFilter(ContextCompat.getColor(context, colorRes))
            
            // Настраиваем бэйдж типа уведомления
            val (badgeText, badgeColor) = NotificationsAdapter.getNotificationTypeBadge(notification)
            typeBadgeTextView.text = badgeText
            val badgeDrawable = ContextCompat.getDrawable(context, R.drawable.badge_background)?.mutate()
            badgeDrawable?.setTint(ContextCompat.getColor(context, badgeColor))
            typeBadgeTextView.background = badgeDrawable
            
            // Настройка внешнего вида в зависимости от типа уведомления
            val backgroundColorHex: String
            val borderColor: Int
            
            when (notification.type) {
                NotificationType.MEMORIAL_OWNERSHIP -> {
                    backgroundColorHex = "#FFF8E1" // Light amber
                    borderColor = ContextCompat.getColor(context, R.color.gold)
                }
                NotificationType.MEMORIAL_CHANGES -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(context, R.color.green)
                }
                NotificationType.MEMORIAL_EDIT -> {
                    backgroundColorHex = "#FFECB3" // Light orange
                    borderColor = ContextCompat.getColor(context, R.color.orange)
                }
                NotificationType.SYSTEM -> {
                    // Для системных уведомлений проверяем заголовок для определения, связано ли с модерацией
                    val titleContainsPublished = notification.title?.contains("опубликован") == true
                    val titleContainsNotPublished = notification.title?.contains("не опубликован") == true
                    val titleContainsRejected = notification.title?.contains("отклонен") == true || notification.title?.contains("отклонена") == true
                    val messageContainsModeration = notification.message?.contains("публикац") == true || notification.message?.contains("модерац") == true
                    val isTechnicalResponse = notification.title?.contains("Ответ на техническое обращение") == true ||
                            notification.relatedEntityName == "Техническая поддержка"
                    
                    if (isTechnicalResponse) {
                        // Ответ технической поддержки
                        backgroundColorHex = "#E8F5E9" // Light green
                        borderColor = ContextCompat.getColor(context, R.color.green)
                    } else if (titleContainsPublished || titleContainsNotPublished || titleContainsRejected || messageContainsModeration) {
                        // Это уведомление о результате модерации
                        backgroundColorHex = if (titleContainsPublished && !titleContainsNotPublished) {
                            "#E8F5E9" // Light green для одобренных
                        } else {
                            "#FFEBEE" // Light red для отклоненных
                        }
                        borderColor = if (titleContainsPublished && !titleContainsNotPublished) {
                            ContextCompat.getColor(context, R.color.green)
                        } else {
                            ContextCompat.getColor(context, R.color.red)
                        }
                    } else {
                        // Обычное системное уведомление
                        backgroundColorHex = "#E3F2FD" // Light blue
                        borderColor = ContextCompat.getColor(context, R.color.teal_700)
                    }
                }
                NotificationType.MODERATION -> {
                    // Уведомления о модерации
                    backgroundColorHex = "#FFF3E0" // Light orange 
                    borderColor = ContextCompat.getColor(context, R.color.orange)
                }
                NotificationType.TECHNICAL -> {
                    backgroundColorHex = "#E3F2FD" // Light blue
                    borderColor = ContextCompat.getColor(context, R.color.teal_700)
                }
                NotificationType.ADMIN_INFO -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(context, R.color.teal_700)
                }
                NotificationType.MASS_ANNOUNCEMENT -> {
                    backgroundColorHex = "#FFF3E0" // Light orange (важное)
                    borderColor = ContextCompat.getColor(context, R.color.purple_500)
                }
                NotificationType.ADMIN_SYSTEM -> {
                    backgroundColorHex = "#F5F5F5" // Light grey
                    borderColor = ContextCompat.getColor(context, R.color.grey)
                }
                NotificationType.ADMIN_WARNING -> {
                    backgroundColorHex = "#FFEBEE" // Light red (предупреждение)
                    borderColor = ContextCompat.getColor(context, R.color.red)
                }
                NotificationType.INFO -> {
                    backgroundColorHex = "#E8F5E9" // Light green
                    borderColor = ContextCompat.getColor(context, R.color.teal_700)
                }
                else -> {
                    backgroundColorHex = "#F5F5F5" // Light grey
                    borderColor = ContextCompat.getColor(context, R.color.grey)
                }
            }
            
            cardView.setCardBackgroundColor(Color.parseColor(backgroundColorHex))
            
            // Устанавливаем рамку для карточки
            val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            
            if (cardView.background is GradientDrawable) {
                val drawable = cardView.background as GradientDrawable
                drawable.setStroke(strokeWidth, borderColor)
            }
        }
    }

    // ViewHolder для стандартных уведомлений с кнопками действий
    class StandardNotificationViewHolder(
        itemView: View,
        private val isIncoming: Boolean,
        private val onAcceptClick: ((Notification) -> Unit)?,
        private val onRejectClick: ((Notification) -> Unit)?,
        private val onItemClick: ((Notification) -> Unit)?,
        private val onDeleteClick: ((Notification) -> Unit)?
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
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete)
        private val typeBadgeTextView: TextView = itemView.findViewById(R.id.text_type_badge)

        private val context: Context = itemView.context

        fun bind(notification: Notification) {
            // Устанавливаем слушатель кликов
            itemView.setOnClickListener {
                onItemClick?.invoke(notification)
            }
            
            // Настраиваем заголовок в зависимости от типа
            val title = when (notification.type) {
                    NotificationType.MEMORIAL_OWNERSHIP -> {
                        // Для запросов на совместное владение учитываем статус
                        when (notification.status) {
                            NotificationStatus.ACCEPTED -> notification.title ?: "Запрос на совместное владение принят"
                            NotificationStatus.REJECTED -> notification.title ?: "Запрос на совместное владение отклонён"
                            else -> "Запрос на совместное владение"
                        }
                    }
                    NotificationType.MEMORIAL_CHANGES -> "Запрос на изменение мемориала"
                    NotificationType.MEMORIAL_EDIT -> {
                        // Для уведомлений об изменениях тоже учитываем статус
                        when (notification.status) {
                            NotificationStatus.ACCEPTED -> notification.title ?: "Изменения в мемориале приняты"
                            NotificationStatus.REJECTED -> notification.title ?: "Изменения в мемориале отклонены"
                            else -> "Изменения в мемориале"
                        }
                    }
                NotificationType.INFO -> notification.title ?: "Информация"
                    NotificationType.SYSTEM -> {
                    // Проверяем, является ли это ответом на техническое обращение
                    if (notification.title?.contains("Ответ на техническое обращение") == true ||
                        notification.relatedEntityName == "Техническая поддержка") {
                        notification.title ?: "Ответ технической поддержки"
                    } else {
                        // Проверяем текст заголовка для определения типа системного уведомления
                        val titleContainsPublished = notification.title?.contains("опубликован") == true
                        val titleContainsNotPublished = notification.title?.contains("не опубликован") == true
                        val titleContainsRejected = notification.title?.contains("отклонен") == true
                        
                        if (titleContainsPublished && !titleContainsNotPublished) {
                            // Уведомление об одобрении публикации
                            notification.title ?: "Системное уведомление"
                        } else if (titleContainsNotPublished || titleContainsRejected) {
                            // Уведомление об отклонении публикации
                            notification.title ?: "Системное уведомление"
                        } else {
                            // Обычное системное уведомление
                        notification.title ?: "Системное уведомление"
                        }
                    }
                    }
                    NotificationType.MODERATION -> "Запрос на модерацию мемориала"
                NotificationType.TECHNICAL -> {
                    // Для технических уведомлений разные заголовки в зависимости от направления
                    if (isIncoming) {
                        notification.title ?: "Технические вопросы"
                    } else {
                        "Обращение в техподдержку"
                    }
                }
                NotificationType.ADMIN_INFO -> notification.title ?: "Информация от администратора"
                NotificationType.MASS_ANNOUNCEMENT -> notification.title ?: "Важное объявление"
                NotificationType.ADMIN_SYSTEM -> notification.title ?: "Системное уведомление"
                NotificationType.ADMIN_WARNING -> notification.title ?: "Предупреждение"
                else -> "Уведомление"
            }
            titleTextView.text = title
            
            // Настраиваем бэйдж типа уведомления
            val (badgeText, badgeColor) = NotificationsAdapter.getNotificationTypeBadge(notification)
            typeBadgeTextView.text = badgeText
            val badgeDrawable = ContextCompat.getDrawable(context, R.drawable.badge_background)?.mutate()
            badgeDrawable?.setTint(ContextCompat.getColor(context, badgeColor))
            typeBadgeTextView.background = badgeDrawable
            
            messageTextView.text = notification.message
            
            try {
                // Parse the ISO date format from the server
                val dateTime = LocalDateTime.parse(notification.createdAt, DateTimeFormatter.ISO_DATE_TIME)
                val formatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                dateTextView.text = formatted
            } catch (e: Exception) {
                // Fallback if date parsing fails
                dateTextView.text = notification.createdAt
            }
            
            // Настройка индикатора непрочитанности
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Настройка информации о пользователе
            configureUserInfo(notification)
            
            // Настройка информации о мемориале
            if (notification.relatedEntityName != null && 
                notification.relatedEntityName != "Техническая поддержка") {
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
            
            // Настраиваем кнопку удаления
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(notification)
            }
        }
        
        private fun configureUserInfo(notification: Notification) {
            if (isIncoming) {
                // Для входящих обычных запросов показываем отправителя
                if (notification.status == null || notification.status == NotificationStatus.PENDING) {
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
                userInfoTextView.text = if (notification.receiverName != null) {
                    "Кому: ${notification.receiverName}"
                } else {
                    "Отправлено"
                }
                userInfoTextView.visibility = View.VISIBLE
            }
        }
        
        private fun configureStatus(notification: Notification) {
            // Защита от null в статусе
            if (notification.status == null) {
                statusTextView.text = "Неизвестно"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.grey))
                return
            }
            
            // Защита от null в типе уведомления
            if (notification.type == null) {
                statusTextView.text = "Обработано"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_neutral))
                return
            }
            
            // Особая обработка для уведомлений о модерации
            if (notification.isRelatedToModeration()) {
                // Для исходящих уведомлений типа MODERATION (запросы на модерацию)
                if (!isIncoming && notification.type == NotificationType.MODERATION) {
                    statusTextView.text = "Запрос отправлен"
                    statusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_neutral))
                    return
                }
                
                // Для системных уведомлений
                if (notification.type == NotificationType.SYSTEM) {
                    statusTextView.text = "Информация"
                    statusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_neutral))
                    return
                }
                
                // Проверяем статус уведомления о модерации для обычных случаев (входящие)
                if (notification.isMemorialApproved()) {
                    statusTextView.text = "Одобрено"
                    statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else if (notification.status == NotificationStatus.REJECTED) {
                    statusTextView.text = "Отклонено"
                    statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else {
                    statusTextView.text = "На рассмотрении"
                    statusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
                return
            }
            
            if (notification.status == NotificationStatus.PENDING) {
                statusTextView.text = "В ожидании"
                statusTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
            } else {
                when (notification.status) {
                    NotificationStatus.ACCEPTED -> {
                        statusTextView.text = "Принято"
                        statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
                    }
                    NotificationStatus.REJECTED -> {
                        statusTextView.text = "Отклонено"
                        statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
                    }
                    NotificationStatus.PROCESSED -> {
                        statusTextView.text = "Обработано"
                        statusTextView.setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                    }
                    else -> {
                        statusTextView.text = ""
                    }
                }
            }
        }
        
        private fun styleNotificationByType(notification: Notification) {
            var backgroundColorHex: String
            var borderColor: Int
            
            // Защита от null в типе уведомления
            if (notification.type == null) {
                backgroundColorHex = "#F5F5F5" // Light grey
                borderColor = ContextCompat.getColor(context, R.color.grey)
            } else {
                when {
                    // Для уведомлений о модерации из системных
                    notification.isRelatedToModeration() -> {
                        if (notification.isMemorialApproved()) {
                            // Одобренная модерация
                            backgroundColorHex = "#E8F5E9" // Light green
                            borderColor = ContextCompat.getColor(context, R.color.green)
                        } else {
                            // Отклоненная модерация
                            backgroundColorHex = "#FFEBEE" // Light red
                            borderColor = ContextCompat.getColor(context, R.color.red)
                        }
                    }
                    
                    notification.type == NotificationType.MEMORIAL_OWNERSHIP -> {
                        backgroundColorHex = "#FFF8E1" // Light amber
                        borderColor = ContextCompat.getColor(context, R.color.gold)
                    }
                    notification.type == NotificationType.MEMORIAL_CHANGES -> {
                        backgroundColorHex = "#E8F5E9" // Light green
                        borderColor = ContextCompat.getColor(context, R.color.green)
                    }
                    notification.type == NotificationType.MEMORIAL_EDIT -> {
                        backgroundColorHex = "#FFECB3" // Light orange
                        borderColor = ContextCompat.getColor(context, R.color.orange)
                    }
                    notification.type == NotificationType.INFO -> {
                        backgroundColorHex = "#E8F5E9" // Light green
                        borderColor = ContextCompat.getColor(context, R.color.teal_700)
                    }
                    notification.type == NotificationType.SYSTEM -> {
                        // Обычное системное уведомление
                        backgroundColorHex = "#E3F2FD" // Light blue
                        borderColor = ContextCompat.getColor(context, R.color.teal_700)
                    }
                    notification.type == NotificationType.MODERATION -> {
                        // Уведомления о модерации
                        backgroundColorHex = "#FFF3E0" // Light orange 
                        borderColor = ContextCompat.getColor(context, R.color.orange)
                    }
                    notification.type == NotificationType.TECHNICAL -> {
                        backgroundColorHex = "#E3F2FD" // Light blue
                        borderColor = ContextCompat.getColor(context, R.color.teal_700)
                    }
                    notification.type == NotificationType.ADMIN_INFO -> {
                        backgroundColorHex = "#E8F5E9" // Light green
                        borderColor = ContextCompat.getColor(context, R.color.teal_700)
                    }
                    notification.type == NotificationType.MASS_ANNOUNCEMENT -> {
                        backgroundColorHex = "#FFF3E0" // Light orange (важное)
                        borderColor = ContextCompat.getColor(context, R.color.purple_500)
                    }
                    notification.type == NotificationType.ADMIN_SYSTEM -> {
                        backgroundColorHex = "#F5F5F5" // Light grey
                        borderColor = ContextCompat.getColor(context, R.color.grey)
                    }
                    notification.type == NotificationType.ADMIN_WARNING -> {
                        backgroundColorHex = "#FFEBEE" // Light red (предупреждение)
                        borderColor = ContextCompat.getColor(context, R.color.red)
                    }
                    else -> {
                        backgroundColorHex = "#F5F5F5" // Light grey
                        borderColor = ContextCompat.getColor(context, R.color.grey)
                    }
                }
            }
            
            cardView.setCardBackgroundColor(Color.parseColor(backgroundColorHex))
            
            // Устанавливаем рамку для карточки
            val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            
            if (cardView.background is GradientDrawable) {
                val drawable = cardView.background as GradientDrawable
                drawable.setStroke(strokeWidth, borderColor)
            } else {
                // Создаем новый GradientDrawable, если необходимо
                val drawable = GradientDrawable()
                drawable.setColor(Color.parseColor(backgroundColorHex))
                drawable.setStroke(strokeWidth, borderColor)
                cardView.background = drawable
            }
        }
        
        private fun configureActionButtons(notification: Notification) {
            // Показываем или скрываем кнопки действий
            val showActionButtons = isIncoming && 
                                   (notification.status == NotificationStatus.PENDING) &&
                                   // Не показываем кнопки для исходящих технических уведомлений
                                   !(notification.type == NotificationType.TECHNICAL && !isIncoming)
            
            acceptButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            rejectButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            
            if (showActionButtons) {
                acceptButton.setOnClickListener { onAcceptClick?.invoke(notification) }
                rejectButton.setOnClickListener { onRejectClick?.invoke(notification) }
                
                // Настраиваем текст кнопок в зависимости от типа уведомления
                if (notification.type == null) {
                    // Защита от null в типе уведомления
                    acceptButton.text = "Принять"
                    rejectButton.text = "Отклонить"
                } else {
                    when (notification.type) {
                        NotificationType.MEMORIAL_OWNERSHIP -> {
                            acceptButton.text = "Разрешить доступ"
                            rejectButton.text = "Отклонить"
                        }
                        NotificationType.MEMORIAL_CHANGES -> {
                            acceptButton.text = "Подтвердить изменения"
                            rejectButton.text = "Отклонить изменения"
                        }
                        NotificationType.MEMORIAL_EDIT -> {
                            acceptButton.text = "Принять изменения"
                            rejectButton.text = "Отклонить изменения"
                        }
                        NotificationType.TECHNICAL -> {
                            acceptButton.text = "Ответить"
                            rejectButton.text = "Закрыть"
                        }
                        else -> {
                            acceptButton.text = "Принять"
                            rejectButton.text = "Отклонить"
                        }
                    }
                }
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }

    /* Unused methods - commented out to avoid compilation errors
    // Метод для преобразования статуса уведомления в текст
    private fun getStatusText(notification: Notification): String? {
        // Для уведомлений о модерации с типом SYSTEM, не показываем статус
        if (notification.type == NotificationType.SYSTEM) {
            return null
        }
        
        return when (notification.status) {
            NotificationStatus.PENDING -> "На рассмотрении"
            NotificationStatus.ACCEPTED -> "Принято"
            NotificationStatus.REJECTED -> "Отклонено"
            NotificationStatus.PROCESSED -> "Обработано"
            else -> null
        }
    }

    // Метод для определения цвета статуса
    private fun getStatusColor(context: Context, notification: Notification): Int {
        // Для системных уведомлений используем нейтральный цвет
        if (notification.type == NotificationType.SYSTEM) {
            return ContextCompat.getColor(context, R.color.status_neutral)
        }
        
        return when (notification.status) {
            NotificationStatus.PENDING -> ContextCompat.getColor(context, R.color.status_pending)
            NotificationStatus.ACCEPTED -> ContextCompat.getColor(context, R.color.status_accepted)
            NotificationStatus.REJECTED -> ContextCompat.getColor(context, R.color.status_rejected)
            NotificationStatus.PROCESSED -> ContextCompat.getColor(context, R.color.status_neutral)
            else -> ContextCompat.getColor(context, R.color.status_neutral)
        }
    }
    */
} 