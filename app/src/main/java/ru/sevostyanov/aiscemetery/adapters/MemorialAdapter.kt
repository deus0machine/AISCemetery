package ru.sevostyanov.aiscemetery.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial
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
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val datesTextView: TextView = view.findViewById(R.id.text_dates)
        val burialLocationTextView: TextView = view.findViewById(R.id.text_burial_location)
        val mainLocationTextView: TextView = view.findViewById(R.id.text_main_location)
        val editButton: ImageButton = view.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = view.findViewById(R.id.button_delete)
        val privacyButton: ImageButton = view.findViewById(R.id.button_privacy)
        val treeIndicator: TextView = view.findViewById(R.id.text_tree_indicator)
        val publicIndicator: TextView = view.findViewById(R.id.text_public_indicator)
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
        holder.editButton.visibility = controlsVisibility
        holder.deleteButton.visibility = controlsVisibility
        holder.privacyButton.visibility = controlsVisibility

        // Настройка индикатора приватности и публичности
        if (showControls) {
            holder.privacyButton.apply {
                visibility = View.VISIBLE
                setImageResource(if (memorial.isPublic) R.drawable.ic_public else R.drawable.ic_private)
                contentDescription = if (memorial.isPublic) "Сделать приватным" else "Сделать публичным"
            }
        } else {
            holder.privacyButton.visibility = View.GONE
        }

        // Отображение индикатора публичности и стиля карточки
        if (memorial.isPublic) {
            holder.publicIndicator.visibility = View.VISIBLE
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

        // Отображение индикатора принадлежности к древу
        holder.treeIndicator.visibility = if (memorial.treeId != null) View.VISIBLE else View.GONE
        memorial.treeId?.let {
            holder.treeIndicator.text = "🌳 Древо #$it"
        }

        // Загрузка фото
        if (!memorial.photoUrl.isNullOrBlank()) {
            println("MemorialAdapter: Загрузка изображения из URL: ${memorial.photoUrl}")
            GlideHelper.loadImage(
                holder.itemView.context,
                memorial.photoUrl,
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