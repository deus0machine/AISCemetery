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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.FamilyTreeAdapter
import ru.sevostyanov.aiscemetery.databinding.FragmentFamilyTreeBinding
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeViewModel

@AndroidEntryPoint
class FamilyTreeFragment : Fragment() {

    private var _binding: FragmentFamilyTreeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FamilyTreeViewModel by viewModels()
    private lateinit var adapter: FamilyTreeAdapter
    private lateinit var nameTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var ownerTextView: TextView
    private lateinit var createdAtTextView: TextView
    private lateinit var memorialCountTextView: TextView
    private lateinit var addRelationFab: FloatingActionButton
    private val args: FamilyTreeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFamilyTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Получаем ID дерева из аргументов
        val familyTreeId = args.familyTreeId
        
        // Загрузка данных
        viewModel.loadFamilyTree(familyTreeId)
    }

    private fun initializeViews(view: View) {
        nameTextView = view.findViewById(R.id.text_name)
        descriptionTextView = view.findViewById(R.id.text_description)
        ownerTextView = view.findViewById(R.id.text_owner)
        createdAtTextView = view.findViewById(R.id.text_created_at)
        memorialCountTextView = view.findViewById(R.id.text_memorial_count)
        addRelationFab = view.findViewById(R.id.fab_add_relation)
    }

    private fun setupRecyclerView() {
        adapter = FamilyTreeAdapter(
            onItemClick = { tree ->
                // TODO: Навигация к деталям дерева
            },
            onEditClick = { tree ->
                if (tree.ownerId == viewModel.getCurrentUserId()) {
                    // TODO: Навигация к редактированию дерева
                } else {
                    Toast.makeText(
                        requireContext(),
                        "У вас нет прав на редактирование этого дерева",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDeleteClick = { tree ->
                if (tree.ownerId == viewModel.getCurrentUserId()) {
                    // TODO: Показать диалог подтверждения удаления
                    viewModel.deleteFamilyTree(tree.id!!)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "У вас нет прав на удаление этого дерева",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@FamilyTreeFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.familyTrees.observe(viewLifecycleOwner) { trees ->
            adapter.submitList(trees)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTree.setOnClickListener {
            findNavController().navigate(R.id.action_familyTreeFragment_to_createFamilyTreeFragment)
        }

        addRelationFab.setOnClickListener {
            // TODO: Реализовать навигацию к экрану добавления связи
            // Можно использовать viewModel.availableMemorials для выбора мемориала
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 