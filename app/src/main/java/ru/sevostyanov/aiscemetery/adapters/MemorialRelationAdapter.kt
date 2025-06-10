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
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType

class MemorialRelationAdapter(
    private val onEditClick: (MemorialRelation) -> Unit,
    private val onDeleteClick: (MemorialRelation) -> Unit
) : ListAdapter<MemorialRelation, MemorialRelationAdapter.RelationViewHolder>(RelationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memorial_relation, parent, false)
        return RelationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RelationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RelationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sourceNameTextView: TextView = itemView.findViewById(R.id.text_source_name)
        private val targetNameTextView: TextView = itemView.findViewById(R.id.text_target_name)
        private val relationTypeTextView: TextView = itemView.findViewById(R.id.text_relation_type)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(relation: MemorialRelation) {
            sourceNameTextView.text = relation.sourceMemorial.fio
            targetNameTextView.text = relation.targetMemorial.fio
            relationTypeTextView.text = getRelationTypeText(relation.relationType)

            editButton.setOnClickListener { onEditClick(relation) }
            deleteButton.setOnClickListener { onDeleteClick(relation) }
        }

        private fun getRelationTypeText(type: RelationType): String {
            return when (type) {
                RelationType.PARENT -> "родитель"
                RelationType.CHILD -> "ребенок"
                RelationType.SPOUSE -> "супруг(а)"
                RelationType.SIBLING -> "брат/сестра"
                RelationType.GRANDPARENT -> "дедушка/бабушка"
                RelationType.GRANDCHILD -> "внук/внучка"
                RelationType.UNCLE_AUNT -> "дядя/тетя"
                RelationType.NEPHEW_NIECE -> "племянник/племянница"
                RelationType.PLACEHOLDER -> "нет связи"
            }
        }
    }

    private class RelationDiffCallback : DiffUtil.ItemCallback<MemorialRelation>() {
        override fun areItemsTheSame(oldItem: MemorialRelation, newItem: MemorialRelation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MemorialRelation, newItem: MemorialRelation): Boolean {
            return oldItem == newItem
        }
    }
} 