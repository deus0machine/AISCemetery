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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.dialogs.FamilyTreeFilterDialog

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
    
    // Поиск и фильтрация
    private var searchJob: Job? = null
    private var isSearchMode = false
    private var currentSearchQuery: String? = null

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
            // Добавляем небольшую задержку для обновления данных на сервере
            lifecycleScope.launch {
                kotlinx.coroutines.delay(500) // 500ms задержка
                loadTrees(tabLayout.selectedTabPosition == 0)
            }
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
                // При клике на дерево открываем диалог редактирования дерева
                if (tree.userId == viewModel.getCurrentUserId()) {
                    tree.id?.let { id ->
                        val dialog = FamilyTreeFragment.newInstance(id)
                        dialog.show(parentFragmentManager, "edit_tree")
                    }
                } else {
                    Toast.makeText(context, "У вас нет прав на редактирование этого дерева", Toast.LENGTH_SHORT).show()
                }
            },
            onEditClick = { tree ->
                // При клике на карандаш открываем экран редактирования связей дерева
                if (tree.userId == viewModel.getCurrentUserId()) {
                    tree.id?.let { id ->
                        val bundle = Bundle().apply {
                            putLong("treeId", id)
                        }
                        findNavController().navigate(R.id.action_familyTreesListFragment_to_editGenealogyTreeFragment, bundle)
                    }
                } else {
                    Toast.makeText(context, "У вас нет прав на редактирование этого дерева", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { tree ->
                if (tree.userId == viewModel.getCurrentUserId()) {
                    showDeleteConfirmationDialog(tree)
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
                val message = if (tabLayout.selectedTabPosition == 0) {
                    "У вас пока нет деревьев"
                } else {
                    "Нет доступных публичных деревьев"
                }
                showEmptyState(message)
            } else {
                hideEmptyState()
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
                } else {
                    // При ошибке показываем пустой список
                    showEmptyState("Ошибка загрузки данных")
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
            if (!isLoading) {
                // После завершения загрузки восстанавливаем видимость списка
                // (если заглушка не активна)
                if (emptyTextView.visibility != View.VISIBLE) {
                    recyclerView.visibility = View.VISIBLE
                }
            }
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
                query?.let { 
                    performSearch(it.trim())
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Отменяем предыдущий поиск
                searchJob?.cancel()
                
                val query = newText?.trim()
                
                if (query.isNullOrBlank()) {
                    // Если поисковая строка пустая, возвращаемся к обычному режиму
                    exitSearchMode()
                } else {
                    // Запускаем поиск с задержкой 500ms
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        performSearch(query)
                    }
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
            showFilterDialog()
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
        if (show) {
            // Скрываем список и заглушку во время загрузки
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        emptyTextView.text = message
        emptyTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyTextView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog(tree: FamilyTree) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление дерева")
            .setMessage("Вы действительно хотите удалить дерево \"${tree.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteFamilyTree(tree.id!!)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performSearch(query: String) {
        currentSearchQuery = query
        isSearchMode = true
        
        // Простой поиск только по названию дерева
        viewModel.searchTrees(
            query = query,
            ownerName = null,
            startDate = null,
            endDate = null,
            myOnly = tabLayout.selectedTabPosition == 0
        )
    }
    
    private fun exitSearchMode() {
        isSearchMode = false
        currentSearchQuery = null
        searchJob?.cancel()
        
        // Возвращаемся к обычному режиму загрузки
        loadTrees(tabLayout.selectedTabPosition == 0)
    }
    
    private fun showFilterDialog() {
        val dialog = FamilyTreeFilterDialog.newInstance()
        dialog.setOnFilterAppliedListener { filterOptions ->
            // Расширенный поиск с фильтрами - независимо от простого поиска
            performAdvancedSearch(filterOptions)
        }
        dialog.show(parentFragmentManager, "filter_dialog")
    }
    
    private fun performAdvancedSearch(filterOptions: FamilyTreeFilterDialog.FilterOptions) {
        // Очищаем простой поиск при использовании расширенного
        searchView.setQuery("", false)
        currentSearchQuery = null
        isSearchMode = true
        
        // Расширенный поиск со всеми параметрами
        viewModel.searchTrees(
            query = filterOptions.treeName,
            ownerName = filterOptions.ownerName,
            startDate = filterOptions.startDate,
            endDate = filterOptions.endDate,
            myOnly = tabLayout.selectedTabPosition == 0
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
} 