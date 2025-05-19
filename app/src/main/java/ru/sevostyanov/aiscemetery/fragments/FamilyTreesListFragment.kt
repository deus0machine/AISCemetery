package ru.sevostyanov.aiscemetery.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.LoginActivity
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.adapters.FamilyTreeAdapter
import ru.sevostyanov.aiscemetery.databinding.FragmentFamilyTreesListBinding
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeViewModel
import androidx.fragment.app.setFragmentResultListener

@AndroidEntryPoint
class FamilyTreesListFragment : Fragment() {

    private var _binding: FragmentFamilyTreesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FamilyTreeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var addTreeButton: FloatingActionButton
    private lateinit var filterButton: Button
    private lateinit var adapter: FamilyTreeAdapter
    private lateinit var progressBar: View
    private lateinit var emptyTextView: TextView
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFamilyTreesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupAdapter()
        setupTabLayout()
        setupSearchView()
        setupListeners()
        setupObservers()
        
        // Слушаем результат редактирования дерева
        parentFragmentManager.setFragmentResultListener("family_tree_updated", this) { _, _ ->
            loadTrees(tabLayout.selectedTabPosition == 0)
        }
        
        // Загружаем данные только при первом создании фрагмента
        if (isFirstLoad) {
            loadTrees(showOnlyMine = true)
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем список только если были изменения
        if (!isFirstLoad) {
            loadTrees(tabLayout.selectedTabPosition == 0)
        }
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.search_view)
        tabLayout = view.findViewById(R.id.tab_layout)
        addTreeButton = view.findViewById(R.id.createButton)
        filterButton = view.findViewById(R.id.btn_filter)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyTextView = view.findViewById(R.id.text_empty)

        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupAdapter() {
        adapter = FamilyTreeAdapter(
            onItemClick = { tree ->
                Toast.makeText(context, "Выбрано дерево: ${tree.name}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { tree ->
                if (tree.userId == viewModel.getCurrentUserId()) {
                    tree.id?.let { id ->
                        val dialog = FamilyTreeFragment.newInstance(id)
                        dialog.show(parentFragmentManager, "edit_tree")
                    }
                } else {
                    Toast.makeText(context, "У вас нет прав на редактирование этого дерева", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { tree ->
                if (tree.userId == viewModel.getCurrentUserId()) {
                    viewModel.deleteFamilyTree(tree.id!!)
                } else {
                    Toast.makeText(context, "У вас нет прав на удаление этого дерева", Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.familyTrees.observe(viewLifecycleOwner) { trees ->
            adapter.submitList(trees)
            if (trees.isEmpty()) {
                showEmptyView(true)
                showMessage(if (tabLayout.selectedTabPosition == 0) "У вас пока нет деревьев" else "Нет доступных публичных деревьев")
            } else {
                showEmptyView(false)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                if (it.contains("Ошибка авторизации") || it.contains("401")) {
                    // Очищаем токен
                    RetrofitClient.clearToken()
                    // Перенаправление на экран входа
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Мои"))
        tabLayout.addTab(tabLayout.newTab().setText("Публичные"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    viewModel.setCurrentTabPosition(position)
                    when (position) {
                        0 -> loadTrees(showOnlyMine = true)
                        1 -> loadTrees(showOnlyMine = false)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchTrees(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadTrees(tabLayout.selectedTabPosition == 0)
                }
                return true
            }
        })
    }

    private fun setupListeners() {
        addTreeButton.setOnClickListener {
            findNavController().navigate(R.id.action_familyTreesListFragment_to_createFamilyTreeFragment)
        }

        filterButton.setOnClickListener {
            // TODO: Реализовать фильтрацию
        }
    }

    private fun loadTrees(showOnlyMine: Boolean) {
        if (showOnlyMine) {
            viewModel.loadMyFamilyTrees()
        } else {
            viewModel.loadPublicFamilyTrees()
        }
    }

    private fun createNewFamilyTree() {
        lifecycleScope.launch {
            try {
                val newTree = FamilyTree(
                    name = "Новое дерево",
                    description = "Описание нового дерева",
                    isPublic = false
                )
                val createdTree = RetrofitClient.getApiService().createFamilyTree(newTree)
                // Обновляем список после создания
                loadTrees(showOnlyMine = true)
                Toast.makeText(context, "Дерево создано", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка создания дерева: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(show: Boolean) {
        emptyTextView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 