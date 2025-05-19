package ru.sevostyanov.aiscemetery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial

class MemorialSpinnerAdapter(
    context: Context,
    private val memorials: List<Memorial>
) : ArrayAdapter<Memorial>(context, R.layout.item_spinner_memorial, memorials) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_memorial, parent, false)

        val memorial = getItem(position)
        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = memorial?.fio ?: ""

        return view
    }
} 