package ru.sevostyanov.aiscemetery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial

class MemorialDropdownAdapter(
    context: Context,
    private val memorials: List<Memorial>
) : ArrayAdapter<Memorial>(context, 0, memorials) {

    private val inflater = LayoutInflater.from(context)
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_memorial_dropdown, parent, false)
        
        val memorial = getItem(position)
        val nameTextView = view.findViewById<TextView>(R.id.text_memorial_name)
        val datesTextView = view.findViewById<TextView>(R.id.text_memorial_dates)

        memorial?.let {
            nameTextView.text = it.fio
            
            val dates = buildString {
                it.birthDate?.let { birthDate ->
                    append(birthDate)
                }
                it.deathDate?.let { deathDate ->
                    if (it.birthDate != null) {
                        append(" - ")
                    }
                    append(deathDate)
                }
            }
            
            datesTextView.text = if (dates.isNotEmpty()) dates else "Даты неизвестны"
            datesTextView.visibility = View.VISIBLE
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                
                if (constraint.isNullOrEmpty()) {
                    results.values = memorials
                    results.count = memorials.size
                } else {
                    val filteredMemorials = memorials.filter { memorial ->
                        memorial.fio.contains(constraint.toString(), ignoreCase = true)
                    }
                    results.values = filteredMemorials
                    results.count = filteredMemorials.size
                }
                
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                results?.values?.let { values ->
                    addAll(values as List<Memorial>)
                }
                notifyDataSetChanged()
            }
        }
    }
} 