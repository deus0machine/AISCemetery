package ru.sevostyanov.aiscemetery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.RelationType

class RelationTypeSpinnerAdapter(
    context: Context,
    private val relationTypes: Array<RelationType>
) : ArrayAdapter<RelationType>(context, R.layout.item_spinner_relation_type, relationTypes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_relation_type, parent, false)

        val relationType = getItem(position)
        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = when (relationType) {
            RelationType.PARENT -> "Родитель"
            RelationType.CHILD -> "Ребенок"
            RelationType.SPOUSE -> "Супруг/супруга"
            RelationType.SIBLING -> "Брат/сестра"
            RelationType.GRANDPARENT -> "Дедушка/бабушка"
            RelationType.GRANDCHILD -> "Внук/внучка"
            RelationType.UNCLE_AUNT -> "Дядя/тетя"
            RelationType.NEPHEW_NIECE -> "Племянник/племянница"
            null -> ""
        }

        return view
    }
} 