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
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial

class MemorialAdapter(
    private var memorials: List<Memorial>,
    private val onItemClick: (Memorial) -> Unit,
    private val onEditClick: (Memorial) -> Unit,
    private val onDeleteClick: (Memorial) -> Unit,
    private val onPrivacyClick: (Memorial) -> Unit,
    private val showControls: Boolean = true // По умолчанию показываем кнопки управления
) : RecyclerView.Adapter<MemorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
        val photoImage: ImageView = view.findViewById(R.id.image_photo)
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val datesTextView: TextView = view.findViewById(R.id.text_dates)
        val locationTextView: TextView = view.findViewById(R.id.text_location)
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
        holder.locationTextView.text = memorial.mainLocation?.address ?: "Место не указано"

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
        Glide.with(holder.itemView.context)
            .load(memorial.photoUrl)
            .placeholder(R.drawable.placeholder_photo)
            .error(R.drawable.placeholder_photo)
            .centerCrop()
            .into(holder.photoImage)

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
        println("Обновление данных в адаптере:")
        println("Количество мемориалов: ${newMemorials.size}")
        println("Статусы мемориалов: ${newMemorials.map { "${it.id}: isPublic=${it.isPublic}" }}")
        memorials = newMemorials
        notifyDataSetChanged()
    }

    fun getMemorials(): List<Memorial> = memorials
} 