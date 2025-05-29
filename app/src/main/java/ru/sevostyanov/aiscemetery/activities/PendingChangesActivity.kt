package ru.sevostyanov.aiscemetery.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.MemorialAdapter
import ru.sevostyanov.aiscemetery.databinding.ActivityPendingChangesBinding
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.util.NetworkUtil

class PendingChangesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingChangesBinding
    private lateinit var adapter: MemorialAdapter
    private lateinit var repository: MemorialRepository
    private lateinit var emptyView: TextView
    private var memorialId: Long? = null

    companion object {
        const val EXTRA_MEMORIAL_ID = "memorial_id"
        private const val TAG = "PendingChangesActivity"
        
        fun start(context: Context) {
            val intent = Intent(context, PendingChangesActivity::class.java)
            context.startActivity(intent)
        }
        
        fun startWithMemorialId(context: Context, memorialId: Long) {
            val intent = Intent(context, PendingChangesActivity::class.java)
            intent.putExtra(EXTRA_MEMORIAL_ID, memorialId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingChangesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ожидающие изменения"

        repository = MemorialRepository()
        emptyView = binding.textEmpty
        
        // Получаем ID мемориала, если он передан
        memorialId = intent.getLongExtra(EXTRA_MEMORIAL_ID, -1).takeIf { it != -1L }
        
        Log.d("PendingChangesActivity", "onCreate: memorialId=$memorialId")

        setupRecyclerView()
        
        // Если передан ID конкретного мемориала, загружаем только его
        if (memorialId != null) {
            loadSpecificMemorial(memorialId!!)
        } else {
            // Иначе загружаем все мемориалы с ожидающими изменениями
            loadPendingMemorials()
        }
    }

    private fun setupRecyclerView() {
        adapter = MemorialAdapter(
            memorials = emptyList(),
            onItemClick = { memorial ->
                showApprovalDialog(memorial)
            },
            onEditClick = { memorial ->
                // Не используется в этом контексте
            },
            onDeleteClick = { memorial ->
                // Не используется в этом контексте
            },
            onPrivacyClick = { memorial ->
                // Не используется в этом контексте
            },
            showControls = false // Скрываем кнопки управления, используем только клик по карточке
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadPendingMemorials() {
        if (!NetworkUtil.checkInternetAndShowMessage(this)) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val memorials = repository.getEditedMemorials()
                
                // Если есть ID конкретного мемориала, фильтруем список
                val filteredMemorials = if (memorialId != null) {
                    memorials.filter { it.id == memorialId }
                } else {
                    memorials
                }
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    
                    if (filteredMemorials.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = if (memorialId != null) {
                            "Нет ожидающих изменений для выбранного мемориала"
                        } else {
                            "Нет мемориалов, ожидающих подтверждения изменений"
                        }
                    } else {
                        emptyView.visibility = View.GONE
                        adapter.updateData(filteredMemorials)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Ошибка при загрузке данных: ${e.message}"
                    showMessage("Ошибка: ${e.message}")
                }
            }
        }
    }
    
    private fun loadSpecificMemorial(id: Long) {
        if (!NetworkUtil.checkInternetAndShowMessage(this)) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val memorial = repository.getMemorialById(id)
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    
                    if (memorial.pendingChanges) {
                        emptyView.visibility = View.GONE
                        adapter.updateData(listOf(memorial))
                    } else {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "Нет ожидающих изменений для выбранного мемориала"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Ошибка при загрузке данных: ${e.message}"
                    showMessage("Ошибка: ${e.message}")
                }
            }
        }
    }

    private fun showApprovalDialog(memorial: Memorial) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Подтверждение изменений")
            .setMessage("Мемориал \"${memorial.fio}\" был изменен редактором. Вы хотите принять или отклонить эти изменения?")
            .setPositiveButton("Принять") { _, _ ->
                approveChanges(memorial.id!!, true)
            }
            .setNegativeButton("Отклонить") { _, _ ->
                approveChanges(memorial.id!!, false)
            }
            .setNeutralButton("Просмотреть") { _, _ ->
                // Показываем индикатор загрузки при запросе деталей изменений
                binding.progressBar.visibility = View.VISIBLE
                
                lifecycleScope.launch {
                    try {
                        // Загружаем детали изменений для предпросмотра
                        Log.d("PendingChangesActivity", "Загружаем детали изменений для мемориала ID=${memorial.id}")
                        
                        // Пытаемся получить все детали изменений с сервера
                        val pendingMemorial = try {
                            repository.getMemorialPendingChanges(memorial.id!!)
                        } catch (e: Exception) {
                            Log.w("PendingChangesActivity", "Не удалось получить детали изменений с сервера: ${e.message}")
                            // Если не удалось получить детали с сервера, используем существующий объект
                            memorial
                        }
                        
                        binding.progressBar.visibility = View.GONE
                        
                        // Открываем экран просмотра мемориала в режиме предпросмотра изменений
                        Log.d("PendingChangesActivity", "Открываем мемориал для предпросмотра изменений: ID=${pendingMemorial.id}, фио=${pendingMemorial.fio}")
                        Log.d("PendingChangesActivity", "Детали изменений: pendingPhotoUrl=${pendingMemorial.pendingPhotoUrl != null}, " +
                                "pendingBiography=${pendingMemorial.pendingBiography != null}, " +
                                "pendingMainLocation=${pendingMemorial.pendingMainLocation != null}, " +
                                "pendingBurialLocation=${pendingMemorial.pendingBurialLocation != null}")
                        
                        ViewMemorialActivity.startPreviewPendingChanges(this@PendingChangesActivity, pendingMemorial)
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        Log.e("PendingChangesActivity", "Ошибка при загрузке деталей изменений: ${e.message}", e)
                        
                        // В случае ошибки, просто показываем текущий мемориал в режиме предпросмотра
                        ViewMemorialActivity.startPreviewPendingChanges(this@PendingChangesActivity, memorial)
                        
                        // Показываем сообщение об ошибке
                        Toast.makeText(
                            this@PendingChangesActivity,
                            "Не удалось загрузить детали изменений: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun approveChanges(memorialId: Long, approve: Boolean) {
        if (!NetworkUtil.checkInternetAndShowMessage(this)) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Получаем обновленный мемориал
                val updatedMemorial = repository.approveChanges(memorialId, approve)
                
                // НОВОЕ: Также обновляем статус соответствующего уведомления через API
                try {
                    val apiService = ru.sevostyanov.aiscemetery.RetrofitClient.getApiService()
                    
                    // Находим уведомление с типом MEMORIAL_EDIT и relatedEntityId = memorialId, статус PENDING
                    val incomingNotifications = withContext(Dispatchers.IO) {
                        apiService.getMyNotifications()
                    }
                    
                    val notificationToUpdate = incomingNotifications.find { notification ->
                        notification.type == ru.sevostyanov.aiscemetery.models.NotificationType.MEMORIAL_EDIT &&
                        notification.relatedEntityId == memorialId &&
                        notification.status == ru.sevostyanov.aiscemetery.models.NotificationStatus.PENDING
                    }
                    
                    if (notificationToUpdate != null) {
                        Log.d(TAG, "Найдено соответствующее уведомление ID=${notificationToUpdate.id} для обновления")
                        val requestData = mapOf("accept" to approve)
                        val response = withContext(Dispatchers.IO) {
                            apiService.respondToNotification(notificationToUpdate.id, requestData)
                        }
                        // Извлекаем данные из обёртки ResponseWrapper
                        val updatedNotification = response.data
                        Log.d(TAG, "Статус уведомления обновлен: ${updatedNotification.status}")
                    } else {
                        Log.w(TAG, "Не найдено уведомление для обновления статуса")
                    }
                } catch (e: Exception) {
                    Log.e("PendingChangesActivity", "Ошибка при обновлении статуса уведомления: ${e.message}", e)
                    // Не прерываем выполнение, так как основная операция уже выполнена
                }
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    showMessage(if (approve) "Изменения отправлены на модерацию" else "Изменения отклонены")
                    
                    // Перезагружаем список
                    loadPendingMemorials()
                    
                    // Обновляем уведомления в глобальной ViewModel, чтобы статус отразился в NotificationsFragment
                    try {
                        val notificationsViewModel = androidx.lifecycle.ViewModelProvider(
                            this@PendingChangesActivity as androidx.lifecycle.ViewModelStoreOwner
                        ).get(ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel::class.java)
                        
                        // Перезагружаем входящие уведомления для обновления статуса
                        notificationsViewModel.loadIncomingNotifications()
                        
                        Log.d("PendingChangesActivity", "Обновили список уведомлений после ${if (approve) "принятия" else "отклонения"} изменений")
                    } catch (e: Exception) {
                        Log.w("PendingChangesActivity", "Не удалось обновить уведомления: ${e.message}")
                    }
                    
                    // Если это подтверждение изменений, открываем обновленный мемориал
                    if (approve) {
                        // Делаем небольшую задержку перед открытием мемориала, чтобы дать серверу время на обработку
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Перезагружаем мемориал из базы данных перед открытием
                            lifecycleScope.launch {
                                try {
                                    // Явно запрашиваем мемориал по ID с сервера для получения свежих данных
                                    val refreshedMemorial = withContext(Dispatchers.IO) {
                                        repository.getMemorialById(memorialId)
                                    }
                                    
                                    Log.d("PendingChangesActivity", "Получен обновленный мемориал после подтверждения: " +
                                            "ID=${refreshedMemorial.id}, фио=${refreshedMemorial.fio}, " +
                                            "биография=${refreshedMemorial.biography?.take(50)}, " +
                                            "pendingChanges=${refreshedMemorial.pendingChanges}")
                                    
                                    // Открываем экран просмотра с обновленным мемориалом
                                    ViewMemorialActivity.start(this@PendingChangesActivity, refreshedMemorial)
                                } catch (e: Exception) {
                                    Log.e("PendingChangesActivity", "Ошибка при загрузке обновленного мемориала: ${e.message}", e)
                                    // Если не удалось загрузить обновленный мемориал, открываем по ID
                                    ViewMemorialActivity.startWithId(this@PendingChangesActivity, memorialId)
                                }
                            }
                        }, 500) // 500 мс задержка для обновления данных на сервере
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    showMessage("Ошибка: ${e.message}")
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 