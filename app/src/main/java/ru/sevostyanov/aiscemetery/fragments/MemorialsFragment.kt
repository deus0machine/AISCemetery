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
import ru.sevostyanov.aiscemetery.dialogs.MemorialFilterDialog
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.user.UserManager
import android.app.AlertDialog
import retrofit2.HttpException
import com.google.gson.Gson
import com.google.gson.JsonObject
import ru.sevostyanov.aiscemetery.models.PublicationStatus

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
            onPrivacyClick = { memorial ->
                updateMemorialPrivacy(memorial)
            },
            showControls = tabLayout.selectedTabPosition == 0 // Показываем контролы только для вкладки "Мои"
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
                    
                    // Добавляем подробное логирование для каждого публичного мемориала
                    Log.d("MemorialsFragment", "Загружено ${publicMemorials.size} публичных мемориалов")
                    publicMemorials.forEachIndexed { index, memorial ->
                        Log.d("MemorialsFragment", "Публичный мемориал #$index: ID=${memorial.id}, " +
                                "Название=${memorial.fio}, isEditor=${memorial.isEditor}, " +
                                "createdBy=${memorial.createdBy?.id}")
                    }
                    
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
                e.printStackTrace()
                showMessage("Ошибка загрузки мемориалов: ${e.message}")
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
                            loadMemorials(tabLayout.selectedTabPosition == 0)
                            
                            showMessage("Мемориал теперь приватный")
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при обновлении статуса: ${e.message}")
                        loadMemorials(tabLayout.selectedTabPosition == 0)
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
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
