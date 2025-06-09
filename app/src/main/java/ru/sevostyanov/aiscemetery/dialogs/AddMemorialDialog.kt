package ru.sevostyanov.aiscemetery.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.MemorialAdapter
import ru.sevostyanov.aiscemetery.databinding.DialogAddMemorialBinding
import ru.sevostyanov.aiscemetery.models.Memorial

class AddMemorialDialog : DialogFragment() {

    private var _binding: DialogAddMemorialBinding? = null
    private val binding get() = _binding!!

    private var onMemorialSelected: ((Memorial) -> Unit)? = null
    private var availableMemorials: List<Memorial> = emptyList()
    private lateinit var adapter: MemorialAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddMemorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = MemorialAdapter(
            memorials = availableMemorials,
            onItemClick = { memorial ->
                onMemorialSelected?.invoke(memorial)
                dismiss()
            },
            onEditClick = { /* не используется */ },
            onDeleteClick = { /* не используется */ },
            showControls = false
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            memorials: List<Memorial>,
            onMemorialSelected: (Memorial) -> Unit
        ): AddMemorialDialog {
            return AddMemorialDialog().apply {
                this.availableMemorials = memorials
                this.onMemorialSelected = onMemorialSelected
            }
        }
    }
} 