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
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType

class TreeMemorialAdapter(
    private val onRemoveClick: (Memorial) -> Unit,
    private val onCreateRelationClick: (Memorial) -> Unit
) : ListAdapter<Memorial, TreeMemorialAdapter.TreeMemorialViewHolder>(TreeMemorialDiffCallback()) {

    private var relations: List<MemorialRelation> = emptyList()

    fun updateRelations(newRelations: List<MemorialRelation>) {
        android.util.Log.d("TreeMemorialAdapter", "=== updateRelations called ===")
        android.util.Log.d("TreeMemorialAdapter", "Previous relations count: ${relations.size}")
        android.util.Log.d("TreeMemorialAdapter", "New relations count: ${newRelations.size}")
        
        relations = newRelations
        
        newRelations.forEachIndexed { index, relation ->
            android.util.Log.d("TreeMemorialAdapter", "New relation $index: source=${relation.sourceMemorial.id}(${relation.sourceMemorial.fio}), target=${relation.targetMemorial.id}(${relation.targetMemorial.fio}), type=${relation.relationType}")
        }
        
        android.util.Log.d("TreeMemorialAdapter", "=== updateRelations notifyDataSetChanged ===")
        notifyDataSetChanged() // Обновляем все элементы чтобы показать новые статусы связей
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeMemorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_memorial, parent, false)
        return TreeMemorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeMemorialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TreeMemorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_name)
        private val birthYearTextView: TextView = itemView.findViewById(R.id.text_birth_year)
        private val deathYearTextView: TextView = itemView.findViewById(R.id.text_death_year)
        private val relationStatusTextView: TextView = itemView.findViewById(R.id.text_relation_status)
        private val removeButton: ImageButton = itemView.findViewById(R.id.button_remove)
        private val relationButton: ImageButton = itemView.findViewById(R.id.button_create_relation)

        fun bind(memorial: Memorial) {
            nameTextView.text = memorial.fio
            birthYearTextView.text = memorial.birthDate?.substring(0, 4) ?: "?"
            deathYearTextView.text = memorial.deathDate?.substring(0, 4) ?: "?"

            // Определяем статус связи для этого мемориала
            val relationStatus = getRelationStatus(memorial)
            relationStatusTextView.text = relationStatus

            removeButton.setOnClickListener { onRemoveClick(memorial) }
            relationButton.setOnClickListener { onCreateRelationClick(memorial) }
        }

        private fun getRelationStatus(memorial: Memorial): String {
            val memorialId = memorial.id ?: return "нет связи"
            
            android.util.Log.d("TreeMemorialAdapter", "=== getRelationStatus for memorial ${memorial.fio} (ID: $memorialId) ===")
            android.util.Log.d("TreeMemorialAdapter", "Total relations available: ${relations.size}")
            
            relations.forEachIndexed { index, relation ->
                android.util.Log.d("TreeMemorialAdapter", "Relation $index: source=${relation.sourceMemorial.id}(${relation.sourceMemorial.fio}), target=${relation.targetMemorial.id}(${relation.targetMemorial.fio}), type=${relation.relationType}")
            }
            
            // Ищем все связи для этого мемориала (кроме PLACEHOLDER)
            val memorialRelations = relations.filter { relation ->
                val sourceMatches = relation.sourceMemorial.id == memorialId
                val targetMatches = relation.targetMemorial.id == memorialId
                val notPlaceholder = relation.relationType != RelationType.PLACEHOLDER
                
                android.util.Log.d("TreeMemorialAdapter", "Checking relation: sourceMatches=$sourceMatches, targetMatches=$targetMatches, notPlaceholder=$notPlaceholder")
                
                (sourceMatches || targetMatches) && notPlaceholder
            }

            val result = if (memorialRelations.isEmpty()) {
                "нет связи"
            } else {
                "связан (${memorialRelations.size})"
            }
            
            android.util.Log.d("TreeMemorialAdapter", "Result for memorial $memorialId: $result")
            android.util.Log.d("TreeMemorialAdapter", "=== END getRelationStatus ===")
            
            return result
        }
    }

    private class TreeMemorialDiffCallback : DiffUtil.ItemCallback<Memorial>() {
        override fun areItemsTheSame(oldItem: Memorial, newItem: Memorial): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Memorial, newItem: Memorial): Boolean {
            return oldItem == newItem
        }
    }
} 