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
import ru.sevostyanov.aiscemetery.adapters.MemorialAdapter
import ru.sevostyanov.aiscemetery.dialogs.MemorialFilterDialog
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.user.UserManager

class MemorialsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var addMemorialButton: FloatingActionButton
    private lateinit var filterButton: Button
    private lateinit var adapter: MemorialAdapter
    private val repository = MemorialRepository()
    private var currentMemorials = listOf<Memorial>()
    private var isFirstLoad = true

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
        // Перезагружаем список только если были изменения
        if (!isFirstLoad) {
            loadMemorials(tabLayout.selectedTabPosition == 0)
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
        filterButton = view.findViewById(R.id.btn_filter)

        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupAdapter() {
        adapter = MemorialAdapter(
            memorials = emptyList(),
            onItemClick = { memorial ->
                if (tabLayout.selectedTabPosition == 0) {
                    EditMemorialActivity.start(requireActivity(), memorial)
                } else {
                    showMemorialDetails(memorial)
                }
            },
            onEditClick = { memorial ->
                EditMemorialActivity.start(requireActivity(), memorial)
            },
            onDeleteClick = { memorial ->
                showDeleteConfirmationDialog(memorial)
            },
            onPrivacyClick = { memorial ->
                updateMemorialPrivacy(memorial)
            },
            showControls = true
        )
        recyclerView.adapter = adapter
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Мои"))
        tabLayout.addTab(tabLayout.newTab().setText("Публичные"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        adapter.updateControlsVisibility(true)
                        loadMemorials(showOnlyMine = true)
                    }
                    1 -> {
                        adapter.updateControlsVisibility(false)
                        loadMemorials(showOnlyMine = false)
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
                query?.let { searchMemorials(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadMemorials(tabLayout.selectedTabPosition == 0)
                }
                return true
            }
        })
    }

    private fun setupListeners() {
        addMemorialButton.setOnClickListener {
            EditMemorialActivity.start(requireActivity())
        }

        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialog = MemorialFilterDialog.newInstance()
        dialog.setOnFilterAppliedListener { filterOptions ->
            lifecycleScope.launch {
                try {
                    val memorials = repository.searchMemorials(
                        query = "",
                        location = filterOptions.location,
                        startDate = filterOptions.startDate,
                        endDate = filterOptions.endDate,
                        isPublic = filterOptions.isPublic
                    )
                    adapter.updateData(memorials)
                } catch (e: Exception) {
                    showError("Ошибка при применении фильтров: ${e.message}")
                }
            }
        }
        dialog.show(parentFragmentManager, "filter_dialog")
    }

    private fun loadMemorials(showOnlyMine: Boolean) {
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

        lifecycleScope.launch {
            try {
                currentMemorials = if (showOnlyMine) {
                    val myMemorials = repository.getMyMemorials()
                    println("Мои мемориалы: ${myMemorials.map { "${it.id}: isPublic=${it.isPublic}" }}")
                    myMemorials
                } else {
                    val publicMemorials = repository.getPublicMemorials()
                    println("Публичные мемориалы: ${publicMemorials.map { "${it.id}: isPublic=${it.isPublic}" }}")
                    publicMemorials
                }
                
                println("Текущий таб: ${if (showOnlyMine) "Мои" else "Публичные"}")
                println("Количество мемориалов в списке: ${currentMemorials.size}")
                
                // Обновляем данные в адаптере
                adapter.updateData(currentMemorials)
                
                if (currentMemorials.isEmpty()) {
                    showMessage(if (showOnlyMine) "У вас пока нет мемориалов" else "Нет доступных публичных мемориалов")
                }
            } catch (e: Exception) {
                Log.e("MemorialsFragment", "Error loading memorials", e)
                val errorMessage = when {
                    e.message?.contains("HTTP 401") == true -> "Необходима авторизация"
                    e.message?.contains("HTTP 403") == true -> "Нет доступа"
                    else -> "Ошибка при загрузке мемориалов: ${e.message}"
                }
                showError(errorMessage)
            }
        }
    }

    private fun searchMemorials(query: String) {
        lifecycleScope.launch {
            try {
                val memorials = repository.searchMemorials(query)
                adapter.updateData(memorials)
            } catch (e: Exception) {
                showError("Ошибка при поиске: ${e.message}")
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
                    loadMemorials(tabLayout.selectedTabPosition == 0)
                    showMessage("Мемориал успешно удален")
                }
            } catch (e: Exception) {
                showError("Ошибка при удалении: ${e.message}")
            }
        }
    }

    private fun updateMemorialPrivacy(memorial: Memorial) {
        lifecycleScope.launch {
            try {
                memorial.id?.let { id ->
                    val newIsPublic = !memorial.isPublic
                    
                    // Обновляем статус на сервере
                    repository.updateMemorialPrivacy(id, newIsPublic)
                    
                    // Перезагружаем список мемориалов
                    loadMemorials(tabLayout.selectedTabPosition == 0)
                    
                    showMessage(if (newIsPublic) "Мемориал теперь публичный" else "Мемориал теперь приватный")
                }
            } catch (e: Exception) {
                showError("Ошибка при обновлении статуса: ${e.message}")
                loadMemorials(tabLayout.selectedTabPosition == 0)
            }
        }
    }

    private fun getCurrentUserId(): Long? {
        val sharedPreferences = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1).takeIf { it != -1L }
    }

    private fun showMemorialDetails(memorial: Memorial) {
        val message = buildString {
            append("${memorial.fio}\n")
            append("Дата рождения: ${memorial.birthDate ?: "Не указана"}\n")
            if (memorial.deathDate != null) {
                append("Дата смерти: ${memorial.deathDate}\n")
            }
            if (!memorial.biography.isNullOrBlank()) {
                append("\nБиография:\n${memorial.biography}\n")
            }
            memorial.mainLocation?.let { location ->
                append("\nМестоположение: ${location.address ?: "Координаты: ${location.latitude}, ${location.longitude}"}")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Информация о мемориале")
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
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
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
} 
