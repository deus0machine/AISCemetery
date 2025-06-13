package ru.sevostyanov.aiscemetery.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.activities.EditMemorialActivity
import ru.sevostyanov.aiscemetery.activities.ViewMemorialActivity
import ru.sevostyanov.aiscemetery.adapters.MemorialAdapter
import ru.sevostyanov.aiscemetery.dialogs.AdvancedMemorialFilterDialog
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PagedResponse
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.user.UserManager
import android.app.AlertDialog
import retrofit2.HttpException
import com.google.gson.Gson
import com.google.gson.JsonObject
import android.widget.TextView

class MemorialsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var addMemorialButton: FloatingActionButton
    private lateinit var advancedFilterButton: Button
    private lateinit var searchModeIndicator: TextView
    private lateinit var emptyMemorialsText: TextView
    private lateinit var adapter: MemorialAdapter
    private val repository = MemorialRepository()
    private var currentMemorials = listOf<Memorial>()
    private var isFirstLoad = true
    
    // Переменные для пагинации
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private var hasMoreData = true
    private var allMemorials = mutableListOf<Memorial>()
    
    // Переменные для поиска
    private var currentSearchQuery: String? = null
    private var currentAdvancedFilterOptions: AdvancedMemorialFilterDialog.AdvancedFilterOptions? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private var isSearchMode = false
    private var isAdvancedSearchMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memorials, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Проверяем авторизацию
        val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(requireContext())
        if (user == null) {
            Log.d("MemorialsFragment", "User not authenticated in onViewCreated, returning to LoginActivity")
            UserManager.clearUserData(requireContext())
            startActivity(Intent(requireActivity(), ru.sevostyanov.aiscemetery.LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        initializeViews(view)
        setupAdapter()
        setupTabLayout()
        setupSearchView()
        setupListeners()
        
        // Загружаем данные только при первом создании фрагмента
        if (isFirstLoad) {
            loadMemorials(showOnlyMine = true)
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем только первую страницу если были изменения
        if (!isFirstLoad) {
            currentPage = 0
            hasMoreData = true
            allMemorials.clear()
            loadMemorialsPage(tabLayout.selectedTabPosition == 0, isFirstPage = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ресурсы
        adapter.clear()
        currentMemorials = emptyList()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_memorials)
        searchView = view.findViewById(R.id.search_view)
        tabLayout = view.findViewById(R.id.tab_layout)
        addMemorialButton = view.findViewById(R.id.fab_add_memorial)
        advancedFilterButton = view.findViewById(R.id.btn_advanced_filter)
        searchModeIndicator = view.findViewById(R.id.search_mode_indicator)
        emptyMemorialsText = view.findViewById(R.id.empty_memorials_text)

        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupAdapter() {
        adapter = MemorialAdapter(
            memorials = emptyList(),
            onItemClick = { memorial ->
                // Всегда открываем для просмотра при клике по мемориалу
                Log.d("MemorialsFragment", "Открываем мемориал для просмотра: ID=${memorial.id}, isEditor=${memorial.isEditor}")
                ViewMemorialActivity.start(requireActivity(), memorial)
            },
            onEditClick = { memorial ->
                onEditClick(memorial)
            },
            onDeleteClick = { memorial ->
                showDeleteConfirmationDialog(memorial)
            },
            showControls = true, // По умолчанию показываем контролы (первая вкладка "Мои")
            onLoadMoreClick = {
                loadMoreMemorials()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Мои"))
        tabLayout.addTab(tabLayout.newTab().setText("Публичные"))
        
        // Устанавливаем первую вкладку как выбранную по умолчанию
        tabLayout.selectTab(tabLayout.getTabAt(0))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d("MemorialsFragment", "=== СМЕНА ВКЛАДКИ ===")
                Log.d("MemorialsFragment", "Выбрана вкладка: position=${tab?.position}, text=${tab?.text}")
                
                when (tab?.position) {
                    0 -> {
                        Log.d("MemorialsFragment", "Переключаемся на 'Мои' мемориалы")
                        adapter.updateControlsVisibility(true)
                        loadMemorials(showOnlyMine = true)
                    }
                    1 -> {
                        Log.d("MemorialsFragment", "Переключаемся на 'Публичные' мемориалы")
                        adapter.updateControlsVisibility(false)
                        loadMemorials(showOnlyMine = false)
                    }
                }
                Log.d("MemorialsFragment", "=== КОНЕЦ СМЕНЫ ВКЛАДКИ ===")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Log.d("MemorialsFragment", "Отменена вкладка: position=${tab?.position}, text=${tab?.text}")
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d("MemorialsFragment", "Повторно выбрана вкладка: position=${tab?.position}, text=${tab?.text}")
            }
        })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { 
                    // При нажатии кнопки поиска на клавиатуре выполняем быстрый поиск
                    performQuickSearchWithQuery(it.trim())
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
                        kotlinx.coroutines.delay(500)
                        performSearch(query)
                    }
                }
                return true
            }
        })
    }

    private fun setupListeners() {
        addMemorialButton.setOnClickListener {
            EditMemorialActivity.start(requireActivity())
        }

        advancedFilterButton.setOnClickListener {
            showAdvancedFilterDialog()
        }
    }



    private fun showAdvancedFilterDialog() {
        val dialog = AdvancedMemorialFilterDialog.newInstance()
        dialog.setOnFilterAppliedListener { filterOptions ->
            Log.d("MemorialsFragment", "Применены расширенные фильтры: $filterOptions")
            
            currentAdvancedFilterOptions = filterOptions
            isAdvancedSearchMode = true
            
            performAdvancedSearch(filterOptions)
            updateSearchModeIndicator()
        }
        
        dialog.setOnQuickSearchListener { query ->
            performQuickSearchWithQuery(query)
        }
        
        dialog.show(parentFragmentManager, "advanced_filter_dialog")
    }



    private fun performQuickSearchWithQuery(query: String) {
        Log.d("MemorialsFragment", "=== БЫСТРЫЙ ПОИСК ===")
        Log.d("MemorialsFragment", "Поисковый запрос: '$query'")
        
        lifecycleScope.launch {
            try {
                val results = repository.quickSearchMemorials(query, 20)
                Log.d("MemorialsFragment", "Результаты быстрого поиска: ${results.size}")
                
                allMemorials.clear()
                allMemorials.addAll(results)
                adapter.updateData(allMemorials, forceUpdate = true)
                
                isSearchMode = true
                currentSearchQuery = query
                hasMoreData = false // Быстрый поиск не поддерживает пагинацию
                updateSearchModeIndicator()
                updateLoadMoreVisibility()
                
                if (results.isEmpty()) {
                    showEmptyState("По запросу '$query' ничего не найдено")
                } else {
                    hideEmptyState()
                }
                
            } catch (e: Exception) {
                Log.e("MemorialsFragment", "Ошибка быстрого поиска: ${e.message}", e)
                showMessage("Ошибка быстрого поиска: ${e.message}")
            }
        }
    }

    private fun performAdvancedSearch(filterOptions: AdvancedMemorialFilterDialog.AdvancedFilterOptions) {
        Log.d("MemorialsFragment", "=== РАСШИРЕННЫЙ ПОИСК ===")
        Log.d("MemorialsFragment", "Фильтры: $filterOptions")
        
        // Сбрасываем пагинацию для расширенного поиска
        currentPage = 0
        hasMoreData = true
        allMemorials.clear()
        
        performAdvancedSearchPage(filterOptions, isFirstPage = true)
    }
    
    private fun performAdvancedSearchPage(
        filterOptions: AdvancedMemorialFilterDialog.AdvancedFilterOptions, 
        isFirstPage: Boolean = false
    ) {
        if (isLoading) return
        
        isLoading = true
        
        Log.d("MemorialsFragment", "=== СТРАНИЦА РАСШИРЕННОГО ПОИСКА ===")
        Log.d("MemorialsFragment", "isFirstPage: $isFirstPage, currentPage: $currentPage")
        
        if (!isFirstPage) {
            adapter.updateLoadMoreState(hasMoreData, isLoading = true)
        }

        lifecycleScope.launch {
            try {
                val pagedResponse = repository.advancedSearchMemorials(
                    firstName = filterOptions.firstName,
                    lastName = filterOptions.lastName,
                    middleName = filterOptions.middleName,
                    birthDateFrom = filterOptions.birthDateFrom,
                    birthDateTo = filterOptions.birthDateTo,
                    deathDateFrom = filterOptions.deathDateFrom,
                    deathDateTo = filterOptions.deathDateTo,
                    location = filterOptions.location,
                    query = filterOptions.generalQuery,
                    isPublic = when {
                        tabLayout.selectedTabPosition == 0 -> null // Мои мемориалы
                        else -> true // Публичные мемориалы
                    },
                    sortBy = filterOptions.sortBy,
                    sortDirection = filterOptions.sortDirection,
                    page = currentPage,
                    size = pageSize
                )
                
                Log.d("MemorialsFragment", "Результаты расширенного поиска:")
                Log.d("MemorialsFragment", "- content.size: ${pagedResponse.content.size}")
                Log.d("MemorialsFragment", "- totalElements: ${pagedResponse.totalElements}")
                Log.d("MemorialsFragment", "- hasNext: ${pagedResponse.hasNext}")
                
                val newMemorials = pagedResponse.content
                
                if (isFirstPage) {
                    Log.d("MemorialsFragment", "Первая страница расширенного поиска - очищаем и добавляем ${newMemorials.size}")
                    allMemorials.clear()
                    allMemorials.addAll(newMemorials)
                    adapter.updateData(allMemorials, forceUpdate = true)
                    if (allMemorials.isNotEmpty()) {
                        hideEmptyState()
                    }
                } else {
                    Log.d("MemorialsFragment", "Дополнительная страница расширенного поиска - добавляем ${newMemorials.size}")
                    allMemorials.addAll(newMemorials)
                    adapter.updateData(allMemorials)
                }
                
                hasMoreData = pagedResponse.hasNext
                currentPage++
                
                updateLoadMoreVisibility()
                
                if (isFirstPage && allMemorials.isEmpty()) {
                    showEmptyState("По заданным критериям ничего не найдено")
                }
                
            } catch (e: Exception) {
                Log.e("MemorialsFragment", "Ошибка расширенного поиска: ${e.message}", e)
                showMessage("Ошибка расширенного поиска: ${e.message}")
            } finally {
                isLoading = false
                adapter.updateLoadMoreState(hasMoreData && allMemorials.isNotEmpty(), isLoading = false)
            }
        }
    }
    

    private fun loadMemorials(showOnlyMine: Boolean) {
        Log.d("MemorialsFragment", "=== НАЧАЛО loadMemorials ===")
        Log.d("MemorialsFragment", "showOnlyMine: $showOnlyMine")
        
        // Проверяем авторизацию перед загрузкой
        val user = UserManager.getCurrentUser()
            ?: UserManager.loadUserFromPreferences(requireContext())
        
        if (user == null) {
            Log.d("MemorialsFragment", "User not authenticated, returning to LoginActivity")
            UserManager.clearUserData(requireContext())
            startActivity(Intent(requireActivity(), ru.sevostyanov.aiscemetery.LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        Log.d("MemorialsFragment", "Пользователь авторизован: ${user.login}")
        
        // Сбрасываем пагинацию при смене вкладки
        Log.d("MemorialsFragment", "Сбрасываем пагинацию - currentPage: $currentPage -> 0, allMemorials.size: ${allMemorials.size} -> 0")
        currentPage = 0
        hasMoreData = true
        allMemorials.clear()
        
        loadMemorialsPage(showOnlyMine, isFirstPage = true)
        Log.d("MemorialsFragment", "=== КОНЕЦ loadMemorials ===")
    }
    
    private fun loadMemorialsPage(showOnlyMine: Boolean, isFirstPage: Boolean = false) {
        if (isLoading) return
        
        isLoading = true
        
        Log.d("MemorialsFragment", "=== НАЧАЛО ЗАГРУЗКИ СТРАНИЦЫ ===")
        Log.d("MemorialsFragment", "showOnlyMine: $showOnlyMine, isFirstPage: $isFirstPage, currentPage: $currentPage, pageSize: $pageSize")
        
        if (!isFirstPage) {
            adapter.updateLoadMoreState(hasMoreData, isLoading = true)
        }

        lifecycleScope.launch {
            try {
                Log.d("MemorialsFragment", "Вызываем API...")
                val pagedResponse = if (showOnlyMine) {
                    Log.d("MemorialsFragment", "Загружаем МОИ мемориалы - страница $currentPage, размер $pageSize")
                    repository.getMyMemorials(currentPage, pageSize)
                } else {
                    Log.d("MemorialsFragment", "Загружаем ПУБЛИЧНЫЕ мемориалы - страница $currentPage, размер $pageSize")
                    repository.getPublicMemorials(currentPage, pageSize)
                }
                
                Log.d("MemorialsFragment", "Получен ответ от API:")
                Log.d("MemorialsFragment", "- content.size: ${pagedResponse.content.size}")
                Log.d("MemorialsFragment", "- page: ${pagedResponse.page}")
                Log.d("MemorialsFragment", "- totalElements: ${pagedResponse.totalElements}")
                Log.d("MemorialsFragment", "- totalPages: ${pagedResponse.totalPages}")
                Log.d("MemorialsFragment", "- hasNext: ${pagedResponse.hasNext}")
                
                val newMemorials = pagedResponse.content
                
                Log.d("MemorialsFragment", "Обработка полученных данных:")
                newMemorials.forEachIndexed { index, memorial ->
                    Log.d("MemorialsFragment", "[$index] Мемориал: id=${memorial.id}, fio=${memorial.fio}, isPublic=${memorial.isPublic}, status=${memorial.publicationStatus}")
                }
                
                if (isFirstPage) {
                    Log.d("MemorialsFragment", "Первая страница - очищаем список и добавляем ${newMemorials.size} мемориалов")
                    allMemorials.clear()
                    allMemorials.addAll(newMemorials)
                    // Принудительное обновление при смене вкладки, чтобы гарантировать перерисовку элементов
                    adapter.updateData(allMemorials, forceUpdate = true)
                    if (allMemorials.isNotEmpty()) {
                        hideEmptyState()
                    }
                } else {
                    Log.d("MemorialsFragment", "Дополнительная страница - добавляем ${newMemorials.size} мемориалов к существующим ${allMemorials.size}")
                    allMemorials.addAll(newMemorials)
                    // Передаем ПОЛНЫЙ список в адаптер, а не добавляем элементы
                    adapter.updateData(allMemorials)
                }
                
                hasMoreData = pagedResponse.hasNext
                currentPage++
                
                // Обновляем состояние footer
                updateLoadMoreVisibility()
                
                Log.d("MemorialsFragment", "Итоговое состояние:")
                Log.d("MemorialsFragment", "- allMemorials.size: ${allMemorials.size}")
                Log.d("MemorialsFragment", "- hasMoreData: $hasMoreData")
                Log.d("MemorialsFragment", "- currentPage: $currentPage")
                
                if (isFirstPage && allMemorials.isEmpty()) {
                    val message = if (showOnlyMine) "У вас пока нет мемориалов" else "Нет доступных публичных мемориалов"
                    Log.d("MemorialsFragment", "Показываем сообщение: $message")
                    showEmptyState(message)
                }
                
                Log.d("MemorialsFragment", "Загружено ${newMemorials.size} мемориалов, всего: ${allMemorials.size}, hasMore: $hasMoreData")
                
            } catch (e: Exception) {
                Log.e("MemorialsFragment", "ОШИБКА при загрузке мемориалов: ${e.message}", e)
                e.printStackTrace()
                showMessage("Ошибка загрузки мемориалов: ${e.message}")
            } finally {
                Log.d("MemorialsFragment", "=== ЗАВЕРШЕНИЕ ЗАГРУЗКИ СТРАНИЦЫ ===")
                isLoading = false
                // Обновляем состояние footer (убираем loading)
                adapter.updateLoadMoreState(hasMoreData && allMemorials.isNotEmpty(), isLoading = false)
            }
        }
    }
    
    private fun loadMoreMemorials() {
        when {
            isAdvancedSearchMode && currentAdvancedFilterOptions != null -> {
                // В режиме расширенного поиска загружаем следующую страницу
                performAdvancedSearchPage(currentAdvancedFilterOptions!!)
            }
            isSearchMode && currentSearchQuery != null -> {
                // В режиме обычного поиска загружаем следующую страницу поиска
                performSearchPage(currentSearchQuery!!)
            }
            else -> {
                // В обычном режиме загружаем следующую страницу мемориалов
                loadMemorialsPage(tabLayout.selectedTabPosition == 0)
            }
        }
    }
    
    private fun updateLoadMoreVisibility() {
        // Обновляем состояние footer в адаптере
        adapter.updateLoadMoreState(hasMoreData && allMemorials.isNotEmpty())
    }

    private fun performSearch(query: String) {
        Log.d("MemorialsFragment", "=== НАЧАЛО ПОИСКА ===")
        Log.d("MemorialsFragment", "Поисковый запрос: '$query'")
        
        currentSearchQuery = query
        isSearchMode = true
        
        // Обновляем индикатор режима поиска
        updateSearchModeIndicator()
        
        // Сбрасываем пагинацию для поиска
        currentPage = 0
        hasMoreData = true
        allMemorials.clear()
        
        performSearchPage(query, isFirstPage = true)
    }
    
    private fun performSearchPage(query: String, isFirstPage: Boolean = false) {
        if (isLoading) return
        
        isLoading = true
        
        Log.d("MemorialsFragment", "=== НАЧАЛО СТРАНИЦЫ ПОИСКА ===")
        Log.d("MemorialsFragment", "query: '$query', isFirstPage: $isFirstPage, currentPage: $currentPage")
        
        if (!isFirstPage) {
            adapter.updateLoadMoreState(hasMoreData, isLoading = true)
        }

        lifecycleScope.launch {
            try {
                val pagedResponse = repository.searchMemorials(
                    query = query,
                    location = null,
                    startDate = null,
                    endDate = null,
                    isPublic = when {
                        // В режиме поиска учитываем текущую вкладку
                        tabLayout.selectedTabPosition == 0 -> null // Мои мемориалы - ищем все (публичные и приватные пользователя)
                        else -> true // Публичные мемориалы - ищем только опубликованные
                    },
                    page = currentPage,
                    size = pageSize
                )
                
                Log.d("MemorialsFragment", "Результаты поиска:")
                Log.d("MemorialsFragment", "- content.size: ${pagedResponse.content.size}")
                Log.d("MemorialsFragment", "- totalElements: ${pagedResponse.totalElements}")
                Log.d("MemorialsFragment", "- hasNext: ${pagedResponse.hasNext}")
                
                val newMemorials = pagedResponse.content
                
                if (isFirstPage) {
                    Log.d("MemorialsFragment", "Первая страница поиска - очищаем и добавляем ${newMemorials.size}")
                    allMemorials.clear()
                    allMemorials.addAll(newMemorials)
                    adapter.updateData(allMemorials, forceUpdate = true)
                    if (allMemorials.isNotEmpty()) {
                        hideEmptyState()
                    }
                } else {
                    Log.d("MemorialsFragment", "Дополнительная страница поиска - добавляем ${newMemorials.size}")
                    allMemorials.addAll(newMemorials)
                    adapter.updateData(allMemorials)
                }
                
                hasMoreData = pagedResponse.hasNext
                currentPage++
                
                updateLoadMoreVisibility()
                
                if (isFirstPage && allMemorials.isEmpty()) {
                    showEmptyState("По запросу '$query' ничего не найдено")
                }
                
            } catch (e: Exception) {
                Log.e("MemorialsFragment", "Ошибка поиска: ${e.message}", e)
                showMessage("Ошибка поиска: ${e.message}")
            } finally {
                isLoading = false
                adapter.updateLoadMoreState(hasMoreData && allMemorials.isNotEmpty(), isLoading = false)
            }
        }
    }
    
    private fun exitSearchMode() {
        Log.d("MemorialsFragment", "Выход из режима поиска")
        
        isSearchMode = false
        isAdvancedSearchMode = false
        currentSearchQuery = null
        currentAdvancedFilterOptions = null
        searchJob?.cancel()
        
        // Обновляем индикатор режима поиска
        updateSearchModeIndicator()
        
        // Возвращаемся к обычному режиму загрузки
        loadMemorials(tabLayout.selectedTabPosition == 0)
    }

    private fun updateSearchModeIndicator() {
        val hasQuery = !currentSearchQuery.isNullOrBlank()
        val hasAdvancedFilters = currentAdvancedFilterOptions?.hasActiveFilters() == true
        
        when {
            hasAdvancedFilters -> {
                searchModeIndicator.text = "Активен расширенный поиск"
                searchModeIndicator.visibility = View.VISIBLE
            }
            hasQuery -> {
                searchModeIndicator.text = "Поиск: \"$currentSearchQuery\""
                searchModeIndicator.visibility = View.VISIBLE
            }
            else -> {
                searchModeIndicator.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(memorial: Memorial) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление мемориала")
            .setMessage("Вы действительно хотите удалить мемориал ${memorial.fio}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteMemorial(memorial)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteMemorial(memorial: Memorial) {
        lifecycleScope.launch {
            try {
                memorial.id?.let { id ->
                    repository.deleteMemorial(id)
                    // Удаляем из локального списка
                    allMemorials.removeAll { it.id == id }
                    adapter.updateData(allMemorials)
                    updateLoadMoreVisibility()
                    
                    // Проверяем, не стал ли список пустым
                    if (allMemorials.isEmpty()) {
                        val message = if (tabLayout.selectedTabPosition == 0) {
                            "У вас пока нет мемориалов"
                        } else {
                            "Нет доступных публичных мемориалов"
                        }
                        showEmptyState(message)
                    }
                    
                    showMessage("Мемориал успешно удален")
                }
            } catch (e: Exception) {
                var errorMessage = e.message ?: ""
                if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (e.code() == 409 && errorBody != null) {
                        try {
                            val jsonObject = Gson().fromJson(errorBody, JsonObject::class.java)
                            if (jsonObject.has("error") && jsonObject.get("error").asString == "MEMORIAL_IN_RELATIONS") {
                                errorMessage = jsonObject.get("message").asString
                            }
                        } catch (ex: Exception) {
                            // Если не удалось распарсить JSON, используем стандартное сообщение
                        }
                    }
                }
                if (errorMessage.contains("MEMORIAL_IN_RELATIONS") || errorMessage.contains("Невозможно удалить мемориал, так как он связан в генеалогии")) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Ошибка")
                        .setMessage("Невозможно удалить мемориал, так как он связан в генеалогии. Сначала удалите его из связей генеалогии.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    showError("Ошибка при удалении: $errorMessage")
                }
            }
        }
    }

    private fun updateMemorialPrivacy(memorial: Memorial) {
        val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(requireContext())
        
        // Если мемориал в процессе модерации, запрещаем изменение статуса
        if (memorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Невозможно изменить статус")
                .setMessage("Мемориал находится на модерации. Дождитесь решения администратора.")
                .setPositiveButton("ОК", null)
                .show()
            return
        }
        
        // Для опубликованных или отклоненных мемориалов также запрещаем прямое изменение
        if (memorial.publicationStatus == PublicationStatus.PUBLISHED || 
            memorial.publicationStatus == PublicationStatus.REJECTED) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Невозможно изменить статус")
                .setMessage("Статус публикации мемориала управляется через систему модерации. Используйте кнопку 'Отправить на публикацию' для публикации.")
                .setPositiveButton("ОК") { _, _ ->
                    // Открываем экран мемориала, где можно отправить на модерацию
                    ViewMemorialActivity.start(requireActivity(), memorial)
                }
                .setNegativeButton("Отмена", null)
                .show()
            return
        }
        
        val newIsPublic = !memorial.isPublic
        
        // Если пытаемся сделать мемориал публичным и нет подписки
        if (newIsPublic && user?.hasSubscription == false) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Требуется подписка")
                .setMessage("Купите подписку, чтобы сделать ваш мемориал публичным")
                .setPositiveButton("Информация о подписке") { _, _ ->
                    // Здесь можно открыть экран с информацией о подписке
                    Toast.makeText(requireContext(), "Информация о подписке", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
            return
        }
        
        // Если меморил непубличный, показываем диалог о необходимости модерации
        if (newIsPublic) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Отправка на публикацию")
                .setMessage("Чтобы опубликовать мемориал, его необходимо отправить на публикацию. Перейти к экрану мемориала для отправки на публикацию?")
                .setPositiveButton("Да") { _, _ ->
                    ViewMemorialActivity.start(requireActivity(), memorial)
                }
                .setNegativeButton("Отмена", null)
                .show()
            return
        }
        
        // Если делаем мемориал приватным (это можно делать напрямую)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сделать мемориал приватным")
            .setMessage("Вы действительно хотите сделать мемориал приватным?")
            .setPositiveButton("Да") { _, _ ->
                lifecycleScope.launch {
                    try {
                        memorial.id?.let { id ->
                            // Обновляем статус на сервере
                            repository.updateMemorialPrivacy(id, false)
                            
                            // Перезагружаем список мемориалов
                            currentPage = 0
                            hasMoreData = true
                            allMemorials.clear()
                            loadMemorialsPage(tabLayout.selectedTabPosition == 0, isFirstPage = true)
                            
                            showMessage("Мемориал теперь приватный")
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при обновлении статуса: ${e.message}")
                        currentPage = 0
                        hasMoreData = true
                        allMemorials.clear()
                        loadMemorialsPage(tabLayout.selectedTabPosition == 0, isFirstPage = true)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getCurrentUserId(): Long? {
        val sharedPreferences = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1).takeIf { it != -1L }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                EditMemorialActivity.REQUEST_CREATE,
                EditMemorialActivity.REQUEST_EDIT -> {
                    val updatedMemorial = data?.getParcelableExtra<Memorial>(EditMemorialActivity.EXTRA_MEMORIAL)
                    if (updatedMemorial != null) {
                        // Обновляем список мемориалов
                        loadMemorials(tabLayout.selectedTabPosition == 0)
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showMessage(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyState(message: String) {
        emptyMemorialsText.text = message
        emptyMemorialsText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyMemorialsText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun onEditClick(memorial: Memorial) {
        println("onEditClick: нажатие на кнопку редактирования для мемориала ${memorial.id}")
        
        // Проверяем, находится ли мемориал на модерации
        if (memorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
            // Показываем сообщение о невозможности редактирования и логируем
            Log.e("MemorialsFragment", "Попытка редактировать мемориал на модерации! id=${memorial.id}, статус=${memorial.publicationStatus}")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Редактирование недоступно")
                .setMessage("Этот мемориал находится на модерации и не может быть отредактирован до принятия решения администратором.")
                .setPositiveButton("Понятно", null)
                .show()
            return
        }
        
        // Дополнительно проверяем, может ли мемориал быть отредактирован
        if (!memorial.canEdit()) {
            Log.e("MemorialsFragment", "Мемориал не может быть отредактирован! id=${memorial.id}, canEdit=false")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Редактирование недоступно")
                .setMessage("У вас нет прав на редактирование этого мемориала.")
                .setPositiveButton("Понятно", null)
                .show()
            return
        }
        
        // Открываем активность редактирования
        val intent = Intent(activity, EditMemorialActivity::class.java)
        intent.putExtra(EditMemorialActivity.EXTRA_MEMORIAL, memorial)
        startActivity(intent)
    }
} 


