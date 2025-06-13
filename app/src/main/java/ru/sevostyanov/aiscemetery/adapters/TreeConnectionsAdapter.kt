package ru.sevostyanov.aiscemetery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.FamilyTreeConnection

class TreeConnectionsAdapter : RecyclerView.Adapter<TreeConnectionsAdapter.ConnectionViewHolder>() {

    private var connections = listOf<FamilyTreeConnection>()

    fun updateConnections(newConnections: List<FamilyTreeConnection>) {
        connections = newConnections
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_connection, parent, false)
        return ConnectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(connections[position])
    }

    override fun getItemCount(): Int = connections.size

    class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val relationTypeTextView: TextView = itemView.findViewById(R.id.relation_type_text_view)
        private val fromPersonTextView: TextView = itemView.findViewById(R.id.from_person_text_view)
        private val toPersonTextView: TextView = itemView.findViewById(R.id.to_person_text_view)
        private val detailsTextView: TextView = itemView.findViewById(R.id.details_text_view)

        fun bind(connection: FamilyTreeConnection) {
            relationTypeTextView.text = connection.getRelationEmoji() + " " + connection.getRelationTypeText()
            fromPersonTextView.text = connection.sourcePersonName
            toPersonTextView.text = connection.targetPersonName
            
            // Показываем дополнительную информацию если есть
            if (!connection.details.isNullOrBlank()) {
                detailsTextView.visibility = View.VISIBLE
                detailsTextView.text = connection.details
            } else {
                detailsTextView.visibility = View.GONE
            }
        }

        private fun getRelationTypeText(relationType: String?): String {
            return when (relationType?.uppercase()) {
                "PARENT" -> "👨‍👩‍👧‍👦 Родитель"
                "CHILD" -> "👶 Ребенок"
                "SPOUSE" -> "💑 Супруг(а)"
                "SIBLING" -> "👫 Брат/Сестра"
                "GRANDPARENT" -> "👴👵 Дедушка/Бабушка"
                "GRANDCHILD" -> "👶 Внук/Внучка"
                "UNCLE_AUNT" -> "👨‍👩 Дядя/Тетя"
                "NEPHEW_NIECE" -> "👦👧 Племянник/Племянница"
                "COUSIN" -> "👥 Двоюродный брат/сестра"
                "FRIEND" -> "🤝 Друг"
                "COLLEAGUE" -> "💼 Коллега"
                "OTHER" -> "🔗 Другая связь"
                else -> "❓ ${relationType ?: "Неизвестная связь"}"
            }
        }
    }
} 