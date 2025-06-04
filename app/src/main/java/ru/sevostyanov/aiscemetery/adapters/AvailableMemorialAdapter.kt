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
import ru.sevostyanov.aiscemetery.models.Memorial

class AvailableMemorialAdapter(
    private val onAddClick: (Memorial) -> Unit
) : ListAdapter<Memorial, AvailableMemorialAdapter.AvailableMemorialViewHolder>(AvailableMemorialDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableMemorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_available_memorial, parent, false)
        return AvailableMemorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvailableMemorialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AvailableMemorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_name)
        private val birthYearTextView: TextView = itemView.findViewById(R.id.text_birth_year)
        private val deathYearTextView: TextView = itemView.findViewById(R.id.text_death_year)
        private val biographyTextView: TextView = itemView.findViewById(R.id.text_biography)
        private val addButton: ImageButton = itemView.findViewById(R.id.button_add)

        fun bind(memorial: Memorial) {
            nameTextView.text = memorial.fio
            birthYearTextView.text = memorial.birthDate?.substring(0, 4) ?: "?"
            deathYearTextView.text = memorial.deathDate?.substring(0, 4) ?: "?"
            
            // Показываем краткую биографию если есть
            if (!memorial.biography.isNullOrBlank()) {
                biographyTextView.text = memorial.biography.take(100) + 
                    if (memorial.biography.length > 100) "..." else ""
                biographyTextView.visibility = View.VISIBLE
            } else {
                biographyTextView.visibility = View.GONE
            }

            addButton.setOnClickListener { onAddClick(memorial) }
        }
    }

    private class AvailableMemorialDiffCallback : DiffUtil.ItemCallback<Memorial>() {
        override fun areItemsTheSame(oldItem: Memorial, newItem: Memorial): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Memorial, newItem: Memorial): Boolean {
            return oldItem == newItem
        }
    }
} 