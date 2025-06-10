package ru.sevostyanov.aiscemetery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.RelationType

class RelationTypeDropdownAdapter(
    context: Context,
    private val relationTypes: Array<RelationType>
) : ArrayAdapter<RelationType>(context, 0, relationTypes) {

    private val inflater = LayoutInflater.from(context)
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_relation_type_dropdown, parent, false)
        
        val relationType = getItem(position)
        val titleTextView = view.findViewById<TextView>(R.id.text_relation_title)
        val descriptionTextView = view.findViewById<TextView>(R.id.text_relation_description)
        val iconImageView = view.findViewById<android.widget.ImageView>(R.id.icon_relation_type)

        relationType?.let {
            val relationInfo = getRelationTypeInfo(it)
            titleTextView.text = relationInfo.title
            descriptionTextView.text = relationInfo.description
            
            // Устанавливаем специфичную иконку для типа связи
            val iconRes = getRelationTypeIcon(it)
            iconImageView.setImageResource(iconRes)
        }

        return view
    }

    private fun getRelationTypeInfo(type: RelationType): RelationTypeInfo {
        return when (type) {
            RelationType.PARENT -> RelationTypeInfo(
                "Родитель",
                "Первый человек является родителем второго"
            )
            RelationType.CHILD -> RelationTypeInfo(
                "Ребенок", 
                "Первый человек является ребенком второго"
            )
            RelationType.SPOUSE -> RelationTypeInfo(
                "Супруг/Супруга",
                "Супружеская пара"
            )
            RelationType.SIBLING -> RelationTypeInfo(
                "Брат/Сестра",
                "Братья или сестры"
            )
            RelationType.GRANDPARENT -> RelationTypeInfo(
                "Дедушка/Бабушка",
                "Устанавливает связь дедушка/бабушка → внук/внучка"
            )
            RelationType.GRANDCHILD -> RelationTypeInfo(
                "Внук/Внучка",
                "Устанавливает связь внук/внучка → дедушка/бабушка"
            )
            RelationType.UNCLE_AUNT -> RelationTypeInfo(
                "Дядя/Тетя",
                "Устанавливает связь дядя/тетя → племянник/племянница"
            )
            RelationType.NEPHEW_NIECE -> RelationTypeInfo(
                "Племянник/Племянница",
                "Устанавливает связь племянник/племянница → дядя/тетя"
            )
            RelationType.PLACEHOLDER -> RelationTypeInfo(
                "Без связи",
                "Мемориал добавлен в дерево без семейных связей"
            )
        }
    }

    private fun getRelationTypeIcon(type: RelationType): Int {
        return when (type) {
            RelationType.PARENT -> R.drawable.ic_family
            RelationType.CHILD -> R.drawable.ic_child_care
            RelationType.SPOUSE -> R.drawable.ic_relation
            RelationType.SIBLING -> R.drawable.ic_people
            RelationType.GRANDPARENT -> R.drawable.ic_elder
            RelationType.GRANDCHILD -> R.drawable.ic_child_care
            RelationType.UNCLE_AUNT -> R.drawable.ic_person
            RelationType.NEPHEW_NIECE -> R.drawable.ic_person
            RelationType.PLACEHOLDER -> R.drawable.ic_person
        }
    }

    private data class RelationTypeInfo(
        val title: String,
        val description: String
    )
} 