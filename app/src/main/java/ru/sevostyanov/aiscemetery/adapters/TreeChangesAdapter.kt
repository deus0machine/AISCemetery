package ru.sevostyanov.aiscemetery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R

data class TreeChange(
    val type: ChangeType,
    val description: String,
    val details: String? = null
)

enum class ChangeType {
    PERSON_ADDED,
    PERSON_REMOVED,
    PERSON_MODIFIED,
    RELATION_ADDED,
    RELATION_REMOVED,
    RELATION_MODIFIED
}

class TreeChangesAdapter : RecyclerView.Adapter<TreeChangesAdapter.ChangeViewHolder>() {

    private var changes = listOf<TreeChange>()

    fun updateChanges(newChanges: List<TreeChange>) {
        changes = newChanges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_change, parent, false)
        return ChangeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChangeViewHolder, position: Int) {
        holder.bind(changes[position])
    }

    override fun getItemCount(): Int = changes.size

    class ChangeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeTextView: TextView = itemView.findViewById(R.id.change_type_text_view)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.change_description_text_view)
        private val detailsTextView: TextView = itemView.findViewById(R.id.change_details_text_view)

        fun bind(change: TreeChange) {
            typeTextView.text = getChangeTypeText(change.type)
            descriptionTextView.text = change.description
            
            if (change.details != null) {
                detailsTextView.visibility = View.VISIBLE
                detailsTextView.text = change.details
            } else {
                detailsTextView.visibility = View.GONE
            }
            
            // Устанавливаем цвет в зависимости от типа изменения
            val colorRes = when (change.type) {
                ChangeType.PERSON_ADDED, ChangeType.RELATION_ADDED -> android.R.color.holo_green_dark
                ChangeType.PERSON_REMOVED, ChangeType.RELATION_REMOVED -> android.R.color.holo_red_dark
                ChangeType.PERSON_MODIFIED, ChangeType.RELATION_MODIFIED -> android.R.color.holo_orange_dark
            }
            
            typeTextView.setTextColor(itemView.context.getColor(colorRes))
        }

        private fun getChangeTypeText(type: ChangeType): String {
            return when (type) {
                ChangeType.PERSON_ADDED -> "➕ Добавлена персона"
                ChangeType.PERSON_REMOVED -> "➖ Удалена персона"
                ChangeType.PERSON_MODIFIED -> "✏️ Изменена персона"
                ChangeType.RELATION_ADDED -> "🔗 Добавлена связь"
                ChangeType.RELATION_REMOVED -> "❌ Удалена связь"
                ChangeType.RELATION_MODIFIED -> "🔄 Изменена связь"
            }
        }
    }
} 