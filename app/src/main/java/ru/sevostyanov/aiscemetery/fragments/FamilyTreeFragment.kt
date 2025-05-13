package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FamilyTreeFragment : Fragment() {

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private lateinit var nameTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var ownerTextView: TextView
    private lateinit var createdAtTextView: TextView
    private lateinit var memorialCountTextView: TextView
    private lateinit var addRelationFab: FloatingActionButton

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
        setupListeners()
        observeViewModel()
        
        // Загрузка данных
        arguments?.let { args ->
            val familyTreeId = FamilyTreeFragmentArgs.fromBundle(args).familyTreeId
            viewModel.loadFamilyTree(familyTreeId)
        }
    }

    private fun initializeViews(view: View) {
        nameTextView = view.findViewById(R.id.text_name)
        descriptionTextView = view.findViewById(R.id.text_description)
        ownerTextView = view.findViewById(R.id.text_owner)
        createdAtTextView = view.findViewById(R.id.text_created_at)
        memorialCountTextView = view.findViewById(R.id.text_memorial_count)
        addRelationFab = view.findViewById(R.id.fab_add_relation)
    }

    private fun setupListeners() {
        addRelationFab.setOnClickListener {
            // TODO: Реализовать навигацию к экрану добавления связи
            // Можно использовать viewModel.availableMemorials для выбора мемориала
        }
    }

    private fun observeViewModel() {
        viewModel.familyTree.observe(viewLifecycleOwner) { tree ->
            tree?.let { updateUI(it) }
        }

        viewModel.memorialRelations.observe(viewLifecycleOwner) { relations ->
            memorialCountTextView.text = getString(R.string.memorial_count, relations.size)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Показать/скрыть индикатор загрузки
        }

        viewModel.isAuthorized.observe(viewLifecycleOwner) { isAuthorized ->
            if (!isAuthorized) {
                // TODO: Перенаправить на экран авторизации
            }
        }
    }

    private fun updateUI(tree: FamilyTree) {
        nameTextView.text = tree.name
        descriptionTextView.text = tree.description
        ownerTextView.text = getString(R.string.tree_owner, tree.ownerId.toString())
        
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        createdAtTextView.text = getString(R.string.tree_created_at, 
            dateFormat.format(tree.createdAt))
    }
} 