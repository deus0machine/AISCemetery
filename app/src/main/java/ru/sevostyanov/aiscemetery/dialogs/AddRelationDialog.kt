package ru.sevostyanov.aiscemetery.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.MemorialSpinnerAdapter
import ru.sevostyanov.aiscemetery.adapters.RelationTypeSpinnerAdapter
import ru.sevostyanov.aiscemetery.databinding.DialogAddRelationBinding
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.RelationType

class AddRelationDialog : DialogFragment() {

    private var _binding: DialogAddRelationBinding? = null
    private val binding get() = _binding!!

    private var onRelationCreated: ((Memorial, Memorial, RelationType) -> Unit)? = null
    private var availableMemorials: List<Memorial> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddRelationBinding.inflate(layoutInflater)

        // Настройка спиннеров для выбора мемориалов
        val memorialAdapter = MemorialSpinnerAdapter(requireContext(), availableMemorials)
        binding.spinnerSourceMemorial.adapter = memorialAdapter
        binding.spinnerTargetMemorial.adapter = memorialAdapter

        // Настройка спиннера для выбора типа связи
        val relationTypes = RelationType.values()
        val relationAdapter = RelationTypeSpinnerAdapter(requireContext(), relationTypes)
        binding.spinnerRelationType.adapter = relationAdapter

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_relation_title)
            .setView(binding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val sourceMemorial = binding.spinnerSourceMemorial.selectedItem as Memorial
                val targetMemorial = binding.spinnerTargetMemorial.selectedItem as Memorial
                val relationType = binding.spinnerRelationType.selectedItem as RelationType
                onRelationCreated?.invoke(sourceMemorial, targetMemorial, relationType)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            memorials: List<Memorial>,
            onRelationCreated: (Memorial, Memorial, RelationType) -> Unit
        ): AddRelationDialog {
            return AddRelationDialog().apply {
                this.availableMemorials = memorials
                this.onRelationCreated = onRelationCreated
            }
        }
    }
} 