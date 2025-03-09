package ru.sevostyanov.aiscemetery.memorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.sevostyanov.aiscemetery.R

class BurialSelectionBottomSheet(
    private val burials: List<Burial>,
    private val onBurialSelected: (Burial) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_burial_selection, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_burials)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = BurialAdapter(
            burials,
            onItemClick = { selectedBurial ->
                onBurialSelected(selectedBurial)
                dismiss()
            },
            isSelectable = true
        )

        return view
    }
}

