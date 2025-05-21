package ru.sevostyanov.aiscemetery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTree

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
        private val nameTextView: TextView = itemView.findViewById(R.id.text_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_description)
        private val ownerTextView: TextView = itemView.findViewById(R.id.text_owner)
        private val createdAtTextView: TextView = itemView.findViewById(R.id.text_created_at)
        private val memorialCountTextView: TextView = itemView.findViewById(R.id.text_memorial_count)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(tree: FamilyTree) {
            nameTextView.text = tree.name
            descriptionTextView.text = tree.description ?: "Нет описания"
            ownerTextView.text = "ID владельца: ${tree.userId ?: "Неизвестно"}"
            createdAtTextView.text = itemView.context.getString(R.string.created_at, tree.createdAt)
            memorialCountTextView.text = itemView.context.getString(R.string.memorial_count, tree.memorialCount ?: 0)

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