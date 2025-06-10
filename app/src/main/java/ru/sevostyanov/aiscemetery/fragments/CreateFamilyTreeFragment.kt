package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.viewmodels.CreateFamilyTreeViewModel

@AndroidEntryPoint
class CreateFamilyTreeFragment : Fragment() {

    private val viewModel: CreateFamilyTreeViewModel by viewModels()
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var createButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_family_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        nameEditText = view.findViewById(R.id.edit_name)
        descriptionEditText = view.findViewById(R.id.edit_description)
        createButton = view.findViewById(R.id.button_create)
        cancelButton = view.findViewById(R.id.button_cancel)
    }

    private fun setupListeners() {
        createButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (name.isBlank()) {
                nameEditText.error = "Введите название дерева"
                nameEditText.requestFocus()
                return@setOnClickListener
            }
            
            if (name.length < 2) {
                nameEditText.error = "Название должно содержать не менее 2 символов"
                nameEditText.requestFocus()
                return@setOnClickListener
            }
            
            if (name.length > 100) {
                nameEditText.error = "Название не должно превышать 100 символов"
                nameEditText.requestFocus()
                return@setOnClickListener
            }
            
            if (description.length > 1000) {
                descriptionEditText.error = "Описание не должно превышать 1000 символов"
                descriptionEditText.requestFocus()
                return@setOnClickListener
            }
            
            nameEditText.error = null
            descriptionEditText.error = null

            viewModel.createFamilyTree(name, description)
        }

        cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            createButton.isEnabled = !isLoading
            cancelButton.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                findNavController().navigateUp()
            }
        }
    }
} 