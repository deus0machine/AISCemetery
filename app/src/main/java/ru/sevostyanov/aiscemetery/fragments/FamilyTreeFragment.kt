package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.setFragmentResult
import android.util.Log

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
            Log.d("FamilyTreeFragment", "Кнопка просмотра генеалогии нажата, treeId: $treeId")
            
            // Закрываем текущий диалог
            dismiss()
            
            try {
                // Простой способ - попробуем найти NavController через активность
                val activity = requireActivity()
                Log.d("FamilyTreeFragment", "Activity получена: ${activity.javaClass.simpleName}")
                
                val navHostFragment = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)
                
                Log.d("FamilyTreeFragment", "NavHostFragment: ${navHostFragment?.javaClass?.simpleName}")
                
                if (navHostFragment != null) {
                    val navController = navHostFragment.findNavController()
                    Log.d("FamilyTreeFragment", "NavController получен: $navController")
                    
                    // Проверяем текущий destination
                    val currentDestination = navController.currentDestination
                    Log.d("FamilyTreeFragment", "Текущий destination: ${currentDestination?.label}")
                    
                    val bundle = Bundle().apply {
                        putLong("treeId", treeId)
                    }
                    Log.d("FamilyTreeFragment", "Bundle создан с treeId: $treeId")
                    
                    // Пытаемся навигировать
                    navController.navigate(R.id.action_familyTreesListFragment_to_genealogyTreeFragment, bundle)
                    Log.d("FamilyTreeFragment", "Навигация выполнена успешно")
                } else {
                    Log.e("FamilyTreeFragment", "NavHostFragment не найден!")
                    Toast.makeText(context, "NavHostFragment не найден", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FamilyTreeFragment", "Ошибка навигации через action: ${e.message}")
                
                // Пробуем альтернативный способ - навигация напрямую к destination
                try {
                    val activity = requireActivity()
                    val navHostFragment = activity.supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment)
                    
                    if (navHostFragment != null) {
                        val navController = navHostFragment.findNavController()
                        val bundle = Bundle().apply {
                            putLong("treeId", treeId)
                        }
                        
                        // Навигация напрямую к destination
                        navController.navigate(R.id.genealogyTreeFragment, bundle)
                        Log.d("FamilyTreeFragment", "Прямая навигация выполнена успешно")
                    } else {
                        Toast.makeText(context, "NavHostFragment не найден", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    Log.e("FamilyTreeFragment", "Ошибка прямой навигации: ${e2.message}", e2)
                    Toast.makeText(context, "Ошибка навигации: ${e2.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        editGenealogyButton.setOnClickListener {
            // TODO: Реализовать редактирование генеалогии
            Toast.makeText(context, "Редактирование генеалогии", Toast.LENGTH_SHORT).show()
        }
    }
} 