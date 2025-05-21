package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.setFragmentResult

@AndroidEntryPoint
class FamilyTreeFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(treeId: Long): FamilyTreeFragment {
            val fragment = FamilyTreeFragment()
            val args = Bundle()
            args.putLong("familyTreeId", treeId)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { arguments?.getLong("familyTreeId") ?: -1L }

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var isPublicSwitch: Switch
    private lateinit var viewGenealogyButton: Button
    private lateinit var editGenealogyButton: Button
    private lateinit var saveButton: Button
    private var wasSaved = false

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupObservers()
        setupClickListeners()
        viewModel.loadFamilyTree(treeId)
    }

    private fun initializeViews(view: View) {
        nameEditText = view.findViewById(R.id.text_name)
        descriptionEditText = view.findViewById(R.id.text_description)
        isPublicSwitch = view.findViewById(R.id.switch_is_public)
        viewGenealogyButton = view.findViewById(R.id.button_view_genealogy)
        editGenealogyButton = view.findViewById(R.id.button_edit_genealogy)
        saveButton = view.findViewById(R.id.button_save)
    }

    private fun setupObservers() {
        viewModel.familyTree.observe(viewLifecycleOwner) { tree ->
            tree?.let {
                nameEditText.setText(it.name)
                descriptionEditText.setText(it.description ?: "")
                isPublicSwitch.isChecked = it.isPublic
            }
            // dismiss только если было сохранение
            if (wasSaved && tree != null && tree.id == treeId) {
                setFragmentResult("family_tree_updated", Bundle())
                dismiss()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            saveButton.isEnabled = !isLoading
        }
    }

    private fun setupClickListeners() {

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val isPublic = isPublicSwitch.isChecked

            if (name.isBlank()) {
                Toast.makeText(context, "Введите название дерева", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            wasSaved = true
            viewModel.updateFamilyTree(
                id = treeId,
                name = name,
                description = description,
                isPublic = isPublic
            )
        }

        viewGenealogyButton.setOnClickListener {
            val fragment = GenealogyTreeFragment.newInstance(treeId)
            fragment.show(parentFragmentManager, "genealogy_tree")
        }

        editGenealogyButton.setOnClickListener {
            // TODO: Реализовать редактирование генеалогии
            Toast.makeText(context, "Редактирование генеалогии", Toast.LENGTH_SHORT).show()
        }
    }
} 