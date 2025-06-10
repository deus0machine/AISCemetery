package ru.sevostyanov.aiscemetery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import java.text.SimpleDateFormat
import java.util.*

class FamilyTreeAdapter(
    private val onItemClick: (FamilyTree) -> Unit,
    private val onEditClick: (FamilyTree) -> Unit,
    private val onDeleteClick: (FamilyTree) -> Unit
) : ListAdapter<FamilyTree, FamilyTreeAdapter.FamilyTreeViewHolder>(FamilyTreeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyTreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_tree, parent, false)
        return FamilyTreeViewHolder(view, onItemClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: FamilyTreeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FamilyTreeViewHolder(
        itemView: View,
        private val onItemClick: (FamilyTree) -> Unit,
        private val onEditClick: (FamilyTree) -> Unit,
        private val onDeleteClick: (FamilyTree) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val nameTextView: TextView = itemView.findViewById(R.id.text_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_description)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_status)
        private val ownerTextView: TextView = itemView.findViewById(R.id.text_owner)
        private val createdAtTextView: TextView = itemView.findViewById(R.id.text_created_at)
        private val memorialCountTextView: TextView = itemView.findViewById(R.id.text_memorial_count)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(tree: FamilyTree) {
            nameTextView.text = tree.name
            descriptionTextView.text = tree.description ?: "Нет описания"
            
            // Отображаем ФИО владельца вместо ID
            val ownerDisplayText = when {
                tree.owner?.fio?.isNotBlank() == true -> "Владелец: ${tree.owner.fio}"
                tree.owner?.login?.isNotBlank() == true -> "Владелец: ${tree.owner.login}"
                tree.userId != null -> "Владелец ID: ${tree.userId}"
                else -> "Владелец: Неизвестно"
            }
            ownerTextView.text = ownerDisplayText
            
            // Форматируем дату создания
            val formattedDate = tree.createdAt?.let { dateString ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateString)
                    if (date != null) "Создано: ${outputFormat.format(date)}" else "Дата создания неизвестна"
                } catch (e: Exception) {
                    "Дата создания неизвестна"
                }
            } ?: "Дата создания неизвестна"
            
            createdAtTextView.text = formattedDate
            memorialCountTextView.text = "Мемориалов: ${tree.memorialCount ?: 0}"

            // Получаем контекст для работы с цветами
            val context = itemView.context

            // Отображаем статус публикации
            val statusText = when (tree.publicationStatus) {
                PublicationStatus.PUBLISHED -> "Опубликовано"
                PublicationStatus.PENDING_MODERATION -> "На модерации"
                PublicationStatus.REJECTED -> "Отклонено"
                PublicationStatus.DRAFT -> "Приватное"
                null -> if (tree.isPublic) "Опубликовано" else "Приватное"
            }
            statusTextView.text = statusText

            // Устанавливаем цвет статуса
            val statusColor = when (tree.publicationStatus) {
                PublicationStatus.PUBLISHED -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                PublicationStatus.PENDING_MODERATION -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                PublicationStatus.REJECTED -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                PublicationStatus.DRAFT -> ContextCompat.getColor(context, android.R.color.darker_gray)
                null -> if (tree.isPublic) ContextCompat.getColor(context, android.R.color.holo_green_dark) 
                       else ContextCompat.getColor(context, android.R.color.darker_gray)
            }
            statusTextView.setTextColor(statusColor)

            // Устанавливаем цвет обводки в зависимости от статуса публикации
            val strokeColor = when (tree.publicationStatus) {
                PublicationStatus.PUBLISHED -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                PublicationStatus.PENDING_MODERATION -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                PublicationStatus.REJECTED -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                PublicationStatus.DRAFT -> ContextCompat.getColor(context, android.R.color.darker_gray)
                null -> if (tree.isPublic) ContextCompat.getColor(context, android.R.color.holo_green_dark) 
                       else ContextCompat.getColor(context, android.R.color.darker_gray)
            }
            
            cardView.strokeColor = strokeColor
            cardView.strokeWidth = 4 // Ширина обводки в dp

            itemView.setOnClickListener { onItemClick(tree) }
            editButton.setOnClickListener { onEditClick(tree) }
            deleteButton.setOnClickListener { onDeleteClick(tree) }

            // Проверяем, является ли текущий пользователь владельцем дерева
            val currentUserId = RetrofitClient.getCurrentUserId()
            val isOwner = currentUserId != -1L && currentUserId == tree.userId
            
            // Показываем кнопки редактирования и удаления только владельцу
            editButton.visibility = if (isOwner) View.VISIBLE else View.GONE
            deleteButton.visibility = if (isOwner) View.VISIBLE else View.GONE
        }
    }

    private class FamilyTreeDiffCallback : DiffUtil.ItemCallback<FamilyTree>() {
        override fun areItemsTheSame(oldItem: FamilyTree, newItem: FamilyTree): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FamilyTree, newItem: FamilyTree): Boolean {
            return oldItem == newItem
        }
    }
} 