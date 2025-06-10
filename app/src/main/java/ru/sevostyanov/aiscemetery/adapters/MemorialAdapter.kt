package ru.sevostyanov.aiscemetery.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import android.widget.ProgressBar
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
    private var showControls: Boolean = true, // По умолчанию показываем кнопки управления
    private var showLoadMore: Boolean = false,
    private var isLoadingMore: Boolean = false,
    private val onLoadMoreClick: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MEMORIAL = 0
        private const val VIEW_TYPE_LOAD_MORE = 1
    }

    class MemorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
        val photoImage: ImageView = view.findViewById(R.id.image_photo)
        val photoAwaitingApproval: ImageView = view.findViewById(R.id.photo_awaiting_approval)
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val datesTextView: TextView = view.findViewById(R.id.text_dates)
        val burialLocationTextView: TextView = view.findViewById(R.id.text_burial_location)
        val mainLocationTextView: TextView = view.findViewById(R.id.text_main_location)
        val editButton: ImageButton = view.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = view.findViewById(R.id.button_delete)
        val treeIndicator: TextView = view.findViewById(R.id.text_tree_indicator)
        val publicIndicator: TextView = view.findViewById(R.id.text_public_indicator)
        val statusIndicator: TextView = view.findViewById(R.id.text_status_indicator)
        val editorIndicator: TextView = view.findViewById(R.id.text_editor_indicator)
        val pendingChangesIndicator: TextView = view.findViewById(R.id.text_pending_changes)
        val controlsContainer: View = view.findViewById(R.id.controls_container)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val loadMoreButton: Button = view.findViewById(R.id.btn_load_more)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_load_more)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == memorials.size && showLoadMore) {
            VIEW_TYPE_LOAD_MORE
        } else {
            VIEW_TYPE_MEMORIAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_memorial, parent, false)
                MemorialViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LoadMoreViewHolder -> {
                bindLoadMoreViewHolder(holder)
            }
            is MemorialViewHolder -> {
                bindMemorialViewHolder(holder, position)
            }
        }
    }

    private fun bindLoadMoreViewHolder(holder: LoadMoreViewHolder) {
        holder.loadMoreButton.visibility = if (isLoadingMore) View.GONE else View.VISIBLE
        holder.progressBar.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        
        holder.loadMoreButton.setOnClickListener {
            onLoadMoreClick?.invoke()
        }
    }

    private fun bindMemorialViewHolder(holder: MemorialViewHolder, position: Int) {
        Log.d("MemorialAdapter", "=== onBindViewHolder ===")
        Log.d("MemorialAdapter", "position: $position, memorials.size: ${memorials.size}")
        
        if (position >= memorials.size) {
            Log.e("MemorialAdapter", "ОШИБКА: position=$position >= memorials.size=${memorials.size}")
            return
        }
        
        val memorial = memorials[position]
        Log.d("MemorialAdapter", "Отображаем мемориал: id=${memorial.id}, fio=${memorial.fio}, showControls=$showControls")
        
        val context = holder.itemView.context
        
        // Настройка основной информации
        holder.nameTextView.text = memorial.fio
        holder.datesTextView.text = buildString {
            append("${memorial.birthDate}")
            memorial.deathDate?.let { append(" - $it") }
        }
        
        Log.d("MemorialAdapter", "Установили основную информацию: name=${memorial.fio}, dates=${holder.datesTextView.text}")

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
        holder.controlsContainer.visibility = if (showControls) View.VISIBLE else View.GONE
        Log.d("MemorialAdapter", "Видимость controlsContainer: ${if (showControls) "VISIBLE" else "GONE"}")
        
        // Проверяем, находится ли мемориал на модерации, чтобы отключить редактирование
        val isUnderModeration = memorial.publicationStatus == PublicationStatus.PENDING_MODERATION
        val changesUnderModeration = memorial.changesUnderModeration
        
        // Настраиваем кнопку редактирования
        if (isUnderModeration || changesUnderModeration) {
            // Для мемориалов на модерации или с изменениями на модерации делаем кнопку редактирования неактивной и серой
            holder.editButton.isEnabled = false
            holder.editButton.alpha = 0.5f
            val message = if (isUnderModeration) {
                "Мемориал на модерации и недоступен для редактирования"
            } else {
                "Изменения мемориала находятся на модерации и недоступны для редактирования"
            }
            holder.editButton.setOnClickListener {
                Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            // Обычная настройка для редактируемых мемориалов
            holder.editButton.isEnabled = true
            holder.editButton.alpha = 1.0f
            holder.editButton.setOnClickListener { onEditClick(memorial) }
        }
        
        holder.deleteButton.setOnClickListener { onDeleteClick(memorial) }
        
        // Применяем новую унифицированную систему стилизации
        applyMemorialStyling(holder, memorial, context)

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
            Log.d("MemorialAdapter", "Загрузка изображения из URL: $photoUrl")
            GlideHelper.loadImage(
                holder.itemView.context,
                photoUrl,
                holder.photoImage,
                R.drawable.placeholder_photo,
                R.drawable.placeholder_photo
            )
        } else {
            Log.d("MemorialAdapter", "URL изображения пустой или null, загружаем placeholder")
            holder.photoImage.setImageResource(R.drawable.placeholder_photo)
        }

        // Настройка обработчиков нажатий
        holder.itemView.setOnClickListener { onItemClick(memorial) }
        if (showControls) {
            holder.editButton.setOnClickListener { onEditClick(memorial) }
            holder.deleteButton.setOnClickListener { onDeleteClick(memorial) }
        }
        
        Log.d("MemorialAdapter", "onBindViewHolder завершен для position=$position")
    }

    /**
     * Применяет унифицированную систему стилизации к карточке мемориала
     */
    private fun applyMemorialStyling(holder: MemorialViewHolder, memorial: Memorial, context: android.content.Context) {
        // Сначала скрываем все индикаторы
        holder.publicIndicator.visibility = View.GONE
        holder.statusIndicator.visibility = View.GONE
        holder.treeIndicator.visibility = View.GONE
        holder.editorIndicator.visibility = View.GONE
        holder.pendingChangesIndicator.visibility = View.GONE

        // Определяем основной статус мемориала и применяем соответствующую стилизацию карточки
        when {
            memorial.publicationStatus == PublicationStatus.PENDING_MODERATION -> {
                // Мемориал на модерации
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.memorial_moderation)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.memorial_moderation_bg))
                }
                holder.pendingChangesIndicator.apply {
                    visibility = View.VISIBLE
                    text = "⚠️ На модерации"
                    setBackgroundResource(R.drawable.memorial_indicator_moderation)
                }
            }
            memorial.changesUnderModeration && memorial.isUserOwner -> {
                // Изменения на модерации
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.memorial_moderation)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.memorial_moderation_bg))
                }
                holder.pendingChangesIndicator.apply {
                    visibility = View.VISIBLE
                    text = "🔄 Изменения на модерации"
                    setBackgroundResource(R.drawable.memorial_indicator_moderation)
                }
            }
            memorial.pendingChanges -> {
                // Ожидает подтверждения изменений
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.memorial_moderation)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
                holder.pendingChangesIndicator.apply {
                    visibility = View.VISIBLE
                    text = "⚠️ Ожидает подтверждения"
                    setBackgroundResource(R.drawable.memorial_indicator_moderation)
                }
            }
            else -> {
                // Обычное состояние - применяем стандартную стилизацию
                holder.cardView.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.memorial_card_stroke)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
            }
        }

        // Настройка основного индикатора приватности/публичности (показывается всегда)
        if (memorial.publicationStatus == PublicationStatus.PUBLISHED) {
            // Опубликованный мемориал
            holder.publicIndicator.apply {
                visibility = View.VISIBLE
                text = "Опубликован"
                setBackgroundResource(R.drawable.memorial_indicator_published)
            }
        } else {
            // Все остальные мемориалы считаются приватными
            holder.publicIndicator.apply {
                visibility = View.VISIBLE
                text = "Приватный"
                setBackgroundResource(R.drawable.memorial_indicator_private)
            }
        }

        // Дополнительные статусы модерации (показываются рядом с основными индикаторами)
        when (memorial.publicationStatus) {
            PublicationStatus.REJECTED -> {
                holder.statusIndicator.apply {
                    visibility = View.VISIBLE
                    text = "Отклонён"
                    setBackgroundResource(R.drawable.memorial_indicator_rejected)
                }
            }
            else -> {
                // Для опубликованных мемориалов дополнительный индикатор не нужен (уже показан в основном)
                // Остальные статусы обработаны в pendingChangesIndicator
            }
        }

        // Отображение индикатора принадлежности к древу
        memorial.treeId?.let {
            holder.treeIndicator.apply {
                visibility = View.VISIBLE
                text = "🌳 Древо #$it"
                setBackgroundResource(R.drawable.memorial_indicator_background)
            }
        }
        
        // Отображение индикатора совместного владения
        when {
            memorial.isEditor -> {
                // Если текущий пользователь - редактор
                holder.editorIndicator.apply {
                    visibility = View.VISIBLE
                    text = "Редактор"
                    setBackgroundResource(R.drawable.memorial_indicator_collaborative)
                }
            }
            showControls && !memorial.editors.isNullOrEmpty() -> {
                // Если пользователь - владелец и мемориал имеет редакторов
                holder.editorIndicator.apply {
                    visibility = View.VISIBLE
                    text = "Совместный"
                    setBackgroundResource(R.drawable.memorial_indicator_collaborative)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        val count = memorials.size + if (showLoadMore) 1 else 0
        Log.d("MemorialAdapter", "getItemCount(): $count (memorials: ${memorials.size}, showLoadMore: $showLoadMore)")
        return count
    }

    fun updateData(newMemorials: List<Memorial>, forceUpdate: Boolean = false) {
        Log.d("MemorialAdapter", "=== updateData ===")
        Log.d("MemorialAdapter", "Текущее количество мемориалов: ${memorials.size}")
        Log.d("MemorialAdapter", "Новое количество мемориалов: ${newMemorials.size}")
        Log.d("MemorialAdapter", "forceUpdate: $forceUpdate")
        
        Log.d("MemorialAdapter", "Старые мемориалы:")
        memorials.forEachIndexed { index, memorial ->
            Log.d("MemorialAdapter", "[$index] Старый мемориал: id=${memorial.id}, fio=${memorial.fio}")
        }
        
        Log.d("MemorialAdapter", "Новые мемориалы:")
        newMemorials.forEachIndexed { index, memorial ->
            Log.d("MemorialAdapter", "[$index] Новый мемориал: id=${memorial.id}, fio=${memorial.fio}, isPublic=${memorial.isPublic}")
        }
        
        if (forceUpdate) {
            Log.d("MemorialAdapter", "Принудительное обновление - используем notifyDataSetChanged()")
            memorials = newMemorials
            notifyDataSetChanged()
            Log.d("MemorialAdapter", "updateData завершен (принудительно), итоговое количество: ${memorials.size}")
            return
        }
        
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                val size = memorials.size
                Log.d("MemorialAdapter", "DiffUtil.getOldListSize(): $size")
                return size
            }
            
            override fun getNewListSize(): Int {
                val size = newMemorials.size
                Log.d("MemorialAdapter", "DiffUtil.getNewListSize(): $size")
                return size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldId = memorials[oldItemPosition].id
                val newId = newMemorials[newItemPosition].id
                val same = oldId == newId
                Log.d("MemorialAdapter", "DiffUtil.areItemsTheSame($oldItemPosition, $newItemPosition): oldId=$oldId, newId=$newId, same=$same")
                return same
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldMemorial = memorials[oldItemPosition]
                val newMemorial = newMemorials[newItemPosition]
                val same = oldMemorial == newMemorial
                Log.d("MemorialAdapter", "DiffUtil.areContentsTheSame($oldItemPosition, $newItemPosition): same=$same")
                return same
            }
        }

        Log.d("MemorialAdapter", "Вычисляем DiffResult...")
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        Log.d("MemorialAdapter", "Обновляем список мемориалов...")
        memorials = newMemorials
        
        Log.d("MemorialAdapter", "Применяем изменения к адаптеру...")
        diffResult.dispatchUpdatesTo(this)
        
        Log.d("MemorialAdapter", "updateData завершен, итоговое количество: ${memorials.size}")
    }

    fun updateControlsVisibility(showControls: Boolean) {
        Log.d("MemorialAdapter", "=== updateControlsVisibility ===")
        Log.d("MemorialAdapter", "Изменяем видимость кнопок: ${this.showControls} -> $showControls")
        
        val wasChanged = this.showControls != showControls
        this.showControls = showControls
        
        if (wasChanged) {
            Log.d("MemorialAdapter", "Видимость кнопок изменилась - форсируем полное обновление")
            notifyDataSetChanged()
        } else {
            Log.d("MemorialAdapter", "Видимость кнопок не изменилась")
        }
        
        Log.d("MemorialAdapter", "updateControlsVisibility завершен")
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

    fun updateLoadMoreState(showLoadMore: Boolean, isLoading: Boolean = false) {
        val oldShowLoadMore = this.showLoadMore
        val oldIsLoading = this.isLoadingMore
        
        this.showLoadMore = showLoadMore
        this.isLoadingMore = isLoading
        
        when {
            !oldShowLoadMore && showLoadMore -> {
                // Добавляем footer
                notifyItemInserted(memorials.size)
            }
            oldShowLoadMore && !showLoadMore -> {
                // Убираем footer
                notifyItemRemoved(memorials.size)
            }
            oldShowLoadMore && showLoadMore && oldIsLoading != isLoading -> {
                // Обновляем состояние footer
                notifyItemChanged(memorials.size)
            }
        }
    }
} 