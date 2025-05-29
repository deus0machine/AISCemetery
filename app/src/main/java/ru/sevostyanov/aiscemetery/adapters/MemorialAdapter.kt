package ru.sevostyanov.aiscemetery.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.util.GlideHelper
import com.google.android.material.card.MaterialCardView

class MemorialAdapter(
    private var memorials: List<Memorial>,
    private val onItemClick: (Memorial) -> Unit,
    private val onEditClick: (Memorial) -> Unit,
    private val onDeleteClick: (Memorial) -> Unit,
    private val onPrivacyClick: (Memorial) -> Unit,
    private var showControls: Boolean = true // По умолчанию показываем кнопки управления
) : RecyclerView.Adapter<MemorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
        val photoImage: ImageView = view.findViewById(R.id.image_photo)
        val photoAwaitingApproval: ImageView = view.findViewById(R.id.photo_awaiting_approval)
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val datesTextView: TextView = view.findViewById(R.id.text_dates)
        val burialLocationTextView: TextView = view.findViewById(R.id.text_burial_location)
        val mainLocationTextView: TextView = view.findViewById(R.id.text_main_location)
        val editButton: ImageButton = view.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = view.findViewById(R.id.button_delete)
        val privacyButton: ImageButton = view.findViewById(R.id.button_privacy)
        val treeIndicator: TextView = view.findViewById(R.id.text_tree_indicator)
        val publicIndicator: TextView = view.findViewById(R.id.text_public_indicator)
        val editorIndicator: TextView = view.findViewById(R.id.text_editor_indicator)
        val pendingChangesIndicator: TextView = view.findViewById(R.id.text_pending_changes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memorial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memorial = memorials[position]
        val context = holder.itemView.context
        
        // Настройка основной информации
        holder.nameTextView.text = memorial.fio
        holder.datesTextView.text = buildString {
            append("${memorial.birthDate}")
            memorial.deathDate?.let { append(" - $it") }
        }

        // Отображаем местоположение
        memorial.mainLocation?.let { location ->
            holder.mainLocationTextView.visibility = View.VISIBLE
            holder.mainLocationTextView.text = if (!location.address.isNullOrBlank()) {
                "Местоположение: ${location.address}"
            } else {
                "Местоположение: ${location.latitude}, ${location.longitude}"
            }
        } ?: run {
            holder.mainLocationTextView.visibility = View.GONE
        }

        // Отображаем место захоронения
        memorial.burialLocation?.let { location ->
            holder.burialLocationTextView.visibility = View.VISIBLE
            holder.burialLocationTextView.text = if (!location.address.isNullOrBlank()) {
                "Место захоронения: ${location.address}"
            } else {
                "Место захоронения: ${location.latitude}, ${location.longitude}"
            }
        } ?: run {
            holder.burialLocationTextView.visibility = View.GONE
        }

        // Настройка видимости кнопок управления
        val controlsVisibility = if (showControls) View.VISIBLE else View.GONE
        
        // Проверяем, находится ли мемориал на модерации, чтобы отключить редактирование
        val isUnderModeration = memorial.publicationStatus == PublicationStatus.PENDING_MODERATION
        
        // Настраиваем кнопку редактирования
        holder.editButton.visibility = controlsVisibility
        if (isUnderModeration) {
            // Для мемориалов на модерации делаем кнопку редактирования неактивной и серой
            holder.editButton.isEnabled = false
            holder.editButton.alpha = 0.5f
            holder.editButton.setOnClickListener {
                Toast.makeText(holder.itemView.context, 
                    "Мемориал на модерации и недоступен для редактирования", 
                    Toast.LENGTH_SHORT).show()
            }
        } else {
            // Обычная настройка для редактируемых мемориалов
            holder.editButton.isEnabled = true
            holder.editButton.alpha = 1.0f
            holder.editButton.setOnClickListener { onEditClick(memorial) }
        }
        
        holder.deleteButton.visibility = controlsVisibility
        holder.deleteButton.setOnClickListener { onDeleteClick(memorial) }
        
        // Проверяем статус публикации, чтобы определить видимость кнопки конфиденциальности
        val privacyButtonVisibility = if (showControls && 
            (memorial.publicationStatus == null || memorial.publicationStatus == PublicationStatus.DRAFT)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        holder.privacyButton.visibility = privacyButtonVisibility
        
        // Настройка иконки приватности
        if (privacyButtonVisibility == View.VISIBLE) {
            holder.privacyButton.apply {
                setImageResource(if (memorial.isPublic) R.drawable.ic_public else R.drawable.ic_private)
                contentDescription = if (memorial.isPublic) "Сделать приватным" else "Сделать публичным"
            }
        }

        // Настройка индикатора публичности/статуса и стиля карточки
        holder.publicIndicator.visibility = View.VISIBLE
        
        // Определение текста и цвета на основе статуса публикации
        when (memorial.publicationStatus) {
            PublicationStatus.PUBLISHED -> {
                holder.publicIndicator.text = "🌐 Опубликован"
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, android.R.color.holo_green_dark)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                }
            }
            PublicationStatus.PENDING_MODERATION -> {
                holder.publicIndicator.text = "⏳ На модерации (редактирование недоступно)"
                holder.publicIndicator.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    // Добавляем затемнение для мемориалов на модерации
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.moderation_background))
                }
            }
            PublicationStatus.REJECTED -> {
                holder.publicIndicator.text = "❌ Отклонен"
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                }
            }
            PublicationStatus.DRAFT -> {
                holder.publicIndicator.text = "📝 Черновик"
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, android.R.color.darker_gray)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                }
            }
            null -> {
                // Используем legacy поведение для обратной совместимости
                if (memorial.isPublic) {
                    holder.publicIndicator.text = "🌐 Публичный"
                    holder.cardView.apply {
                        strokeColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
                        strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    }
                } else {
                    holder.publicIndicator.visibility = View.GONE
                    holder.cardView.apply {
                        strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
                        strokeWidth = 0
                    }
                }
            }
        }

        // Отображение индикатора принадлежности к древу
        holder.treeIndicator.visibility = if (memorial.treeId != null) View.VISIBLE else View.GONE
        memorial.treeId?.let {
            holder.treeIndicator.text = "🌳 Древо #$it"
        }
        
        // Отображение индикатора совместного владения
        if (memorial.isEditor) {
            // Если текущий пользователь - редактор
            holder.editorIndicator.visibility = View.VISIBLE
            holder.editorIndicator.text = "👥 Совместный (редактор)"
            // Устанавливаем желтый цвет для выделения
            holder.editorIndicator.setTextColor(ContextCompat.getColor(context, R.color.gold))
            holder.cardView.strokeColor = ContextCompat.getColor(context, R.color.gold)
            holder.cardView.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
        } else if (showControls && !memorial.editors.isNullOrEmpty()) {
            // Если пользователь - владелец (showControls = true означает, что это личный мемориал пользователя)
            // и мемориал имеет редакторов
            holder.editorIndicator.visibility = View.VISIBLE
            holder.editorIndicator.text = "👥 Совместный (владелец)"
            holder.editorIndicator.setTextColor(ContextCompat.getColor(context, R.color.teal_700))
        } else {
            holder.editorIndicator.visibility = View.GONE
        }
        
        // Отображение индикатора ожидающих изменений
        holder.pendingChangesIndicator.visibility = if (memorial.pendingChanges) View.VISIBLE else View.GONE
        if (memorial.pendingChanges) {
            holder.pendingChangesIndicator.text = "⚠️ Ожидает подтверждения изменений"
        }

        // Выбор URL для загрузки фото
        val photoUrl = when {
            // Если есть ожидающее фото и это наш мемориал (владелец) или редактор
            memorial.pendingPhotoUrl != null && (showControls || memorial.isEditor) -> {
                // Показываем индикатор ожидающего фото
                holder.photoAwaitingApproval.visibility = View.VISIBLE
                memorial.pendingPhotoUrl
            }
            // Иначе используем обычное фото
            else -> {
                // Скрываем индикатор ожидающего фото
                holder.photoAwaitingApproval.visibility = View.GONE
                memorial.photoUrl
            }
        }

        // Загрузка фото
        if (!photoUrl.isNullOrBlank()) {
            println("MemorialAdapter: Загрузка изображения из URL: $photoUrl")
            GlideHelper.loadImage(
                holder.itemView.context,
                photoUrl,
                holder.photoImage,
                R.drawable.placeholder_photo,
                R.drawable.placeholder_photo
            )
        } else {
            println("MemorialAdapter: URL изображения пустой или null, загружаем placeholder")
            holder.photoImage.setImageResource(R.drawable.placeholder_photo)
        }

        // Настройка обработчиков нажатий
        holder.itemView.setOnClickListener { onItemClick(memorial) }
        if (showControls) {
            holder.editButton.setOnClickListener { onEditClick(memorial) }
            holder.deleteButton.setOnClickListener { onDeleteClick(memorial) }
            holder.privacyButton.setOnClickListener { onPrivacyClick(memorial) }
        }
    }

    override fun getItemCount() = memorials.size

    fun updateData(newMemorials: List<Memorial>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = memorials.size
            override fun getNewListSize(): Int = newMemorials.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return memorials[oldItemPosition].id == newMemorials[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldMemorial = memorials[oldItemPosition]
                val newMemorial = newMemorials[newItemPosition]
                return oldMemorial == newMemorial
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        memorials = newMemorials
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateControlsVisibility(showControls: Boolean) {
        this.showControls = showControls
        notifyDataSetChanged()
    }

    fun updateMemorial(updatedMemorial: Memorial) {
        println("Обновление мемориала в адаптере: $updatedMemorial")
        val index = memorials.indexOfFirst { it.id == updatedMemorial.id }
        println("Индекс мемориала в списке: $index")
        if (index != -1) {
            val newList = memorials.toMutableList()
            newList[index] = updatedMemorial
            memorials = newList
            println("Список мемориалов после обновления: $memorials")
            notifyItemChanged(index)
        } else {
            println("Мемориал не найден в списке")
        }
    }

    fun getMemorials(): List<Memorial> = memorials

    fun clear() {
        memorials = emptyList()
        notifyDataSetChanged()
    }
} 