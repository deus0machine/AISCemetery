package ru.sevostyanov.aiscemetery.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.MainActivity
import ru.sevostyanov.aiscemetery.activities.EditMemorialActivity
import ru.sevostyanov.aiscemetery.activities.PendingChangesActivity
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.util.GlideHelper
import ru.sevostyanov.aiscemetery.user.UserManager
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ViewMemorialActivity : AppCompatActivity() {

    private val TAG = "ViewMemorialActivity"

    private lateinit var photoImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var datesTextView: TextView
    private lateinit var biographyTextView: TextView
    private lateinit var mainLocationTextView: TextView
    private lateinit var burialLocationTextView: TextView
    private lateinit var treeInfoTextView: TextView
    private lateinit var createdByTextView: TextView
    private lateinit var requestAccessButton: Button
    private lateinit var editButton: Button
    private lateinit var ownershipRequestButton: Button
    private lateinit var pendingChangesButton: Button
    private lateinit var photoAwaitingApprovalIndicator: ImageView
    
    // Новые UI элементы для модерации
    private lateinit var publicationStatusTextView: TextView
    private lateinit var moderationCardView: androidx.cardview.widget.CardView
    private lateinit var moderationMessageTextView: TextView
    private lateinit var sendForModerationButton: Button
    private lateinit var adminModerationButtonsLayout: LinearLayout
    private lateinit var approveButton: Button
    private lateinit var rejectButton: Button
    
    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var repository: ru.sevostyanov.aiscemetery.repository.MemorialRepository

    private var memorial: Memorial? = null
    private var isLoadingMemorial = false
    private var isPreviewPendingChanges = false // Флаг режима предпросмотра изменений

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_memorial)

        // Настройка toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Проверяем режим предпросмотра изменений
        isPreviewPendingChanges = intent.getBooleanExtra(EXTRA_PREVIEW_PENDING_CHANGES, false)
        if (isPreviewPendingChanges) {
            supportActionBar?.title = "Предпросмотр изменений"
        }
        
        // Инициализация ViewModel и репозитория
        notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)
        repository = ru.sevostyanov.aiscemetery.repository.MemorialRepository()

        // Инициализация view
        photoImageView = findViewById(R.id.photoImageView)
        nameTextView = findViewById(R.id.nameTextView)
        datesTextView = findViewById(R.id.datesTextView)
        biographyTextView = findViewById(R.id.biographyTextView)
        mainLocationTextView = findViewById(R.id.mainLocationTextView)
        burialLocationTextView = findViewById(R.id.burialLocationTextView)
        treeInfoTextView = findViewById(R.id.treeInfoTextView)
        createdByTextView = findViewById(R.id.createdByTextView)
        requestAccessButton = findViewById(R.id.requestAccessButton)
        editButton = findViewById(R.id.editButton)
        ownershipRequestButton = findViewById(R.id.ownershipRequestButton)
        pendingChangesButton = findViewById(R.id.pendingChangesButton)
        photoAwaitingApprovalIndicator = findViewById(R.id.photoAwaitingApprovalIndicator)
        
        // Инициализация новых UI элементов
        publicationStatusTextView = findViewById(R.id.publicationStatusTextView)
        moderationCardView = findViewById(R.id.moderationCardView)
        moderationMessageTextView = findViewById(R.id.moderationMessageTextView)
        sendForModerationButton = findViewById(R.id.sendForModerationButton)
        adminModerationButtonsLayout = findViewById(R.id.adminModerationButtonsLayout)
        approveButton = findViewById(R.id.approveButton)
        rejectButton = findViewById(R.id.rejectButton)

        // Получаем мемориал из intent или загружаем по ID
        memorial = intent.getParcelableExtra(EXTRA_MEMORIAL)
        
        Log.d(TAG, "onCreate: получен мемориал из Intent? ${memorial != null}")
        if (memorial != null) {
            Log.d(TAG, "onCreate: ID=${memorial?.id}, название=${memorial?.fio}, isEditor=${memorial?.isEditor}, createdBy=${memorial?.createdBy}")
            loadMemorialData(memorial!!)
        } else {
            // Если мемориал не передан напрямую, пробуем загрузить по ID
            val memorialId = intent.getLongExtra(EXTRA_MEMORIAL_ID, -1)
            Log.d(TAG, "onCreate: memorialId из Intent = $memorialId")
            if (memorialId != -1L) {
                loadMemorialById(memorialId)
            } else {
                Toast.makeText(this, "Ошибка: Мемориал не найден", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Настройка кнопок
        setupRequestAccessButton()
        setupEditButton()
        setupOwnershipRequestButton()
        setupPendingChangesButton()
    }

    override fun onResume() {
        super.onResume()
        // Если мемориал уже был загружен (т.е. memorial != null),
        // и активность не находится в процессе загрузки данных (isLoadingMemorial == false),
        // перезагружаем его данные, чтобы отразить возможные изменения статуса модерации,
        // например, если администратор одобрил или отклонил публикацию, пока экран был неактивен.
        memorial?.id?.let { currentMemorialId ->
            if (!isLoadingMemorial) {
                Log.d(TAG, "onResume: Обнаружен активный мемориал ID=$currentMemorialId. Перезагрузка данных...")
                loadMemorialById(currentMemorialId)
            }
        } ?: Log.d(TAG, "onResume: Мемориал еще не загружен, перезагрузка не требуется.")
    }

    private fun loadMemorialData(memorial: Memorial) {
        Log.d(TAG, "loadMemorialData: начало загрузки данных мемориала ID=${memorial.id}, isEditor=${memorial.isEditor}, isUserEditor=${memorial.isUserEditor}")
        Log.d(TAG, "loadMemorialData: публикационный статус=${memorial.publicationStatus}, canEdit=${memorial.canEdit()}")
        Log.d(TAG, "loadMemorialData: режим предпросмотра изменений: $isPreviewPendingChanges")
        
        // Проверяем наличие ожидающего фото и режим отображения
        val photoUrl = when {
            // В режиме предпросмотра всегда показываем ожидающее фото, если оно есть
            isPreviewPendingChanges && memorial.pendingPhotoUrl != null -> {
                Log.d(TAG, "loadMemorialData: показываем ожидающее фото (режим предпросмотра)")
                photoAwaitingApprovalIndicator.visibility = View.VISIBLE
                memorial.pendingPhotoUrl
            }
            // Если это владелец и есть ожидающее фото - показываем его с индикатором
            memorial.isUserOwner && memorial.pendingPhotoUrl != null -> {
                // Показываем индикатор ожидающего фото
                photoAwaitingApprovalIndicator.visibility = View.VISIBLE
                // Возвращаем ожидающее фото для отображения
                memorial.pendingPhotoUrl
            }
            // Если это редактор, который загрузил фото - показываем его с индикатором
            memorial.isUserEditor && memorial.pendingPhotoUrl != null && memorial.pendingChanges -> {
                // Показываем индикатор ожидающего фото
                photoAwaitingApprovalIndicator.visibility = View.VISIBLE
                // Возвращаем ожидающее фото для отображения
                memorial.pendingPhotoUrl
            }
            // В остальных случаях показываем обычное фото без индикатора
            else -> {
                // Скрываем индикатор ожидающего фото
                photoAwaitingApprovalIndicator.visibility = View.GONE
                // Возвращаем обычное фото
                memorial.photoUrl
            }
        }
        
        // Загружаем фото
        GlideHelper.loadImage(
            this,
            photoUrl ?: "",
            photoImageView,
            R.drawable.placeholder_photo,
            R.drawable.placeholder_photo
        )

        // Всегда отображаем ФИО мемориала
        nameTextView.text = memorial.fio

        // Если мы в режиме предпросмотра изменений, добавляем индикатор вверху страницы
        if (isPreviewPendingChanges) {
            // Показываем полосу с текстом о предпросмотре изменений
            findViewById<View>(R.id.pendingChangesIndicator)?.visibility = View.VISIBLE
            
            // Показываем заголовок с информацией о изменениях
            supportActionBar?.title = "Предпросмотр изменений"
            
            // Скрываем все кнопки действий в режиме предпросмотра
            editButton.visibility = View.GONE
            ownershipRequestButton.visibility = View.GONE
            requestAccessButton.visibility = View.GONE
            pendingChangesButton.visibility = View.GONE
            
            // Отображаем все ожидающие изменения
            
            // Биография - используем pendingBiography если доступно
            val biographyText = if (memorial.pendingBiography != null) {
                memorial.pendingBiography
            } else {
                memorial.biography ?: "Биография отсутствует"
            }
            biographyTextView.text = biographyText
            
            // Обрабатываем даты
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            
            // Используем pendingBirthDate/pendingDeathDate если доступны
            var birthDateStr = ""
            try {
                val birthDateSource = if (memorial.pendingBirthDate != null) memorial.pendingBirthDate else memorial.birthDate
                if (!birthDateSource.isNullOrEmpty()) {
                    val birthDate = dateFormat.parse(birthDateSource)
                    birthDateStr = displayFormat.format(birthDate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка форматирования даты рождения", e)
            }
            
            var deathDateStr = ""
            try {
                val deathDateSource = if (memorial.pendingDeathDate != null) memorial.pendingDeathDate else memorial.deathDate
                if (!deathDateSource.isNullOrEmpty()) {
                    val deathDate = dateFormat.parse(deathDateSource)
                    deathDateStr = displayFormat.format(deathDate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка форматирования даты смерти", e)
            }
            
            // Формируем строку дат
            val datesText = if (birthDateStr.isNotEmpty() && deathDateStr.isNotEmpty()) {
                "$birthDateStr - $deathDateStr"
            } else if (birthDateStr.isNotEmpty()) {
                "Родился: $birthDateStr"
            } else if (deathDateStr.isNotEmpty()) {
                "Умер: $deathDateStr"
            } else {
                "Даты не указаны"
            }
            datesTextView.text = datesText
            
            // Обрабатываем местоположения
            if (memorial.pendingMainLocation != null) {
                mainLocationTextView.text = memorial.pendingMainLocation.address ?: "Не указано"
            } else if (memorial.mainLocation != null) {
                mainLocationTextView.text = memorial.mainLocation.address ?: "Не указано"
            } else {
                mainLocationTextView.text = "Не указано"
            }
            
            if (memorial.pendingBurialLocation != null) {
                burialLocationTextView.text = memorial.pendingBurialLocation.address ?: "Не указано"
            } else if (memorial.burialLocation != null) {
                burialLocationTextView.text = memorial.burialLocation.address ?: "Не указано"
            } else {
                burialLocationTextView.text = "Не указано"
            }
            
        } else {
            // Скрываем полосу
            findViewById<View>(R.id.pendingChangesIndicator)?.visibility = View.GONE
            
            // Для обычного режима просмотра:
            // Кнопка запроса доступа - скрываем, так как это будет в другом разделе
            requestAccessButton.visibility = View.GONE
            
            // Кнопка редактирования - используем новый метод canEdit()
            editButton.visibility = if (memorial.canEdit()) View.VISIBLE else View.GONE
            
            // Кнопка запроса совместного владения - показываем только если пользователь НЕ является владельцем И не может редактировать
            ownershipRequestButton.visibility = if (!memorial.isUserOwner && !memorial.canEdit()) View.VISIBLE else View.GONE
            
            // Проверяем есть ли у мемориала ожидающие изменения и является ли текущий пользователь владельцем
            // НЕ показываем кнопку если изменения находятся на модерации у админа
            if (memorial.pendingChanges && memorial.isUserOwner && !memorial.changesUnderModeration) {
                pendingChangesButton.visibility = View.VISIBLE
            } else {
                pendingChangesButton.visibility = View.GONE
            }
            
            // Заполняем основные данные для обычного режима
            
            // Биография
            biographyTextView.text = memorial.biography ?: "Биография отсутствует"
            
            // Форматируем даты
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            
            var birthDateStr = ""
            try {
                if (!memorial.birthDate.isNullOrEmpty()) {
                    val birthDate = dateFormat.parse(memorial.birthDate)
                    birthDateStr = displayFormat.format(birthDate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка форматирования даты рождения", e)
            }
            
            var deathDateStr = ""
            try {
                if (!memorial.deathDate.isNullOrEmpty()) {
                    val deathDate = dateFormat.parse(memorial.deathDate)
                    deathDateStr = displayFormat.format(deathDate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка форматирования даты смерти", e)
            }
            
            // Формируем строку дат
            val datesText = if (birthDateStr.isNotEmpty() && deathDateStr.isNotEmpty()) {
                "$birthDateStr - $deathDateStr"
            } else if (birthDateStr.isNotEmpty()) {
                "Родился: $birthDateStr"
            } else if (deathDateStr.isNotEmpty()) {
                "Умер: $deathDateStr"
            } else {
                "Даты не указаны"
            }
            
            datesTextView.text = datesText
            
            if (memorial.mainLocation != null) {
                mainLocationTextView.text = memorial.mainLocation.address ?: "Не указано"
            } else {
                mainLocationTextView.text = "Не указано"
            }
            
            if (memorial.burialLocation != null) {
                burialLocationTextView.text = memorial.burialLocation.address ?: "Не указано"
            } else {
                burialLocationTextView.text = "Не указано"
            }
        }

        // Логируем все детали для отладки
        val user = UserManager.getCurrentUser()
        Log.d(TAG, "loadMemorialData: детали доступа: " +
                "userId=${user?.id}, " +
                "creatorId=${memorial.createdBy}, " +
                "isEditor=${memorial.isEditor}, " +
                "isUserEditor=${memorial.isUserEditor}, " +
                "isUserOwner=${memorial.isUserOwner}, " + 
                "canEdit=${memorial.canEdit()}, " +
                "pendingChanges=${memorial.pendingChanges}, " +
                "editors=${memorial.editors}")
        
        // Устанавливаем информацию о создателе
        createdByTextView.text = "Владелец ID: " + (memorial.createdBy?.toString() ?: "Неизвестно")
        
        // Сохраняем мемориал для использования в других методах
        this.memorial = memorial

        // Обновляем информацию о статусе публикации
        updatePublicationStatus(memorial)
    }
    
    private fun setupRequestAccessButton() {
        requestAccessButton.setOnClickListener {
            showRequestAccessDialog()
        }
    }
    
    private fun setupEditButton() {
        editButton.setOnClickListener {
            memorial?.let {
                // Открываем активность редактирования мемориала
                EditMemorialActivity.start(this, it)
                // Закрываем текущую активность просмотра
                finish()
            }
        }
    }
    
    private fun setupOwnershipRequestButton() {
        ownershipRequestButton.setOnClickListener {
            showJointOwnershipRequestDialog()
        }
    }
    
    private fun setupPendingChangesButton() {
        pendingChangesButton.setOnClickListener {
            memorial?.let { memorial ->
                // Открываем активность для просмотра ожидающих изменений с ID мемориала
                PendingChangesActivity.startWithMemorialId(this, memorial.id ?: 0)
            }
        }
    }
    
    private fun showRequestAccessDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_ownership_request)
            .create()
        
        dialog.show()
        
        // Настраиваем диалог
        dialog.findViewById<TextView>(R.id.recipientTextView)?.text = 
            "Владелец мемориала"
        
        // Обработчики кнопок
        dialog.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<Button>(R.id.sendRequestButton)?.setOnClickListener {
            val messageEditText = dialog.findViewById<EditText>(R.id.messageEditText)
            val message = messageEditText?.text.toString()
            
            // Здесь должен быть код для отправки запроса
            // В качестве заглушки просто показываем сообщение
            Toast.makeText(
                this,
                "Запрос на доступ к дереву отправлен!",
                Toast.LENGTH_SHORT
            ).show()
            
            dialog.dismiss()
        }
    }

    private fun showJointOwnershipRequestDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_joint_ownership_request)
            .create()
        
        dialog.show()
        
        // Настраиваем диалог
        dialog.findViewById<TextView>(R.id.recipientTextView)?.text = 
            "Владелец мемориала"
        
        // Обработчики кнопок
        dialog.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<Button>(R.id.sendRequestButton)?.setOnClickListener {
            val messageEditText = dialog.findViewById<EditText>(R.id.messageEditText)
            val message = messageEditText?.text.toString().trim()
            
            if (message.isEmpty()) {
                Toast.makeText(this, "Необходимо добавить сообщение для запроса", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Получаем ID получателя и мемориала
            val receiverId = memorial?.createdBy
            val memorialId = memorial?.id
            
            Log.d(TAG, "Подготовка запроса: receiverId=$receiverId, memorialId=$memorialId")
            
            if (receiverId != null && memorialId != null) {
                // Отправляем запрос через ViewModel
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Log.d(TAG, "Начало отправки запроса на совместное владение")
                        
                        val token = RetrofitClient.getToken()
                        Log.d(TAG, "Текущий токен для запроса: ${token?.substring(0, 10)}...")
                        
                        withContext(Dispatchers.IO) {
                            notificationsViewModel.createMemorialOwnershipRequest(
                                receiverId = receiverId,
                                memorialId = memorialId,
                                message = message
                            )
                        }
                        
                        Log.d(TAG, "Запрос на совместное владение успешно отправлен")
                        
                        Toast.makeText(
                            this@ViewMemorialActivity,
                            "Запрос на совместное владение отправлен!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Переходим к списку уведомлений
                        redirectToNotifications()
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка отправки запроса: ${e.message}", e)
                        Toast.makeText(
                            this@ViewMemorialActivity,
                            "Ошибка при отправке запроса: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Log.e(TAG, "Ошибка: receiverId=$receiverId, memorialId=$memorialId")
                Toast.makeText(
                    this@ViewMemorialActivity,
                    "Ошибка: не удалось определить получателя запроса",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            dialog.dismiss()
        }
    }
    
    private fun loadMemorialById(memorialId: Long) {
        isLoadingMemorial = true
        Log.d(TAG, "loadMemorialById: начало загрузки мемориала ID=$memorialId")
        
        // Показываем индикатор загрузки
        val loadingView = findViewById<View>(R.id.loadingProgressBar)
        loadingView?.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Загружаем мемориал из репозитория
                val loadedMemorial = withContext(Dispatchers.IO) {
                    repository.getMemorialById(memorialId)
                }
                
                Log.d(TAG, "loadMemorialById: загружен мемориал с сервера: ID=${loadedMemorial.id}, " +
                        "ФИО=${loadedMemorial.fio}, isEditor=${loadedMemorial.isEditor}, " +
                        "createdBy=${loadedMemorial.createdBy}, " +
                        "биография=${loadedMemorial.biography?.take(50)}, " +
                        "pendingChanges=${loadedMemorial.pendingChanges}")
                
                // Сохраняем и отображаем загруженный мемориал
                memorial = loadedMemorial
                loadMemorialData(loadedMemorial)
                
                loadingView?.visibility = View.GONE
                isLoadingMemorial = false
            } catch (e: Exception) {
                Log.e(TAG, "loadMemorialById: ошибка загрузки мемориала: ${e.message}", e)
                Toast.makeText(
                    this@ViewMemorialActivity,
                    "Ошибка загрузки мемориала: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                loadingView?.visibility = View.GONE
                isLoadingMemorial = false
                finish()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        const val EXTRA_MEMORIAL = "extra_memorial"
        const val EXTRA_MEMORIAL_ID = "memorial_id"
        const val EXTRA_PREVIEW_PENDING_CHANGES = "preview_pending_changes"
        
        fun start(activity: Activity, memorial: Memorial) {
            val intent = Intent(activity, ViewMemorialActivity::class.java).apply {
                putExtra(EXTRA_MEMORIAL, memorial)
            }
            activity.startActivity(intent)
        }
        
        // Новый метод для запуска активности с режимом предпросмотра изменений
        fun startPreviewPendingChanges(activity: Activity, memorial: Memorial) {
            val intent = Intent(activity, ViewMemorialActivity::class.java).apply {
                putExtra(EXTRA_MEMORIAL, memorial)
                putExtra(EXTRA_PREVIEW_PENDING_CHANGES, true)
            }
            activity.startActivity(intent)
        }
        
        fun startWithId(activity: Activity, memorialId: Long) {
            val intent = Intent(activity, ViewMemorialActivity::class.java).apply {
                putExtra(EXTRA_MEMORIAL_ID, memorialId)
            }
            activity.startActivity(intent)
        }
    }

    // Метод для обновления информации о статусе публикации
    private fun updatePublicationStatus(memorial: Memorial) {
        // Обновляем TextView со статусом публикации
        val statusText = "Статус публикации: ${memorial.getPublicationStatusText()}"
        publicationStatusTextView.text = statusText
        
        // Проверяем роль пользователя (владелец, админ или обычный пользователь)
        val currentUser = UserManager.getCurrentUser()
        val isAdmin = currentUser?.role == "ADMIN" || currentUser?.role == "ROLE_ADMIN"
        val isOwner = memorial.isUserOwner
        
        // По умолчанию скрываем карточку модерации
        moderationCardView.visibility = View.GONE
        
        // Скрываем кнопку редактирования, если мемориал на модерации
        if (memorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
            editButton.visibility = View.GONE
        }
        
        // Скрываем кнопку редактирования, если изменения на модерации
        if (memorial.changesUnderModeration) {
            editButton.visibility = View.GONE
        }
        
        when (memorial.publicationStatus) {
            PublicationStatus.DRAFT -> {
                // Черновик - показываем кнопку отправки на модерацию только владельцу
                if (isOwner) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Мемориал сохранен как черновик. Отправьте его на публикацию для размещения на сайте."
                    sendForModerationButton.visibility = View.VISIBLE
                    adminModerationButtonsLayout.visibility = View.GONE
                    
                    // Настраиваем обработчик для отправки на модерацию
                    setupSendForModerationButton()
                }
            }
            
            PublicationStatus.PENDING_MODERATION -> {
                moderationCardView.visibility = View.VISIBLE
                
                if (isAdmin) {
                    // Для админа показываем информацию и кнопки одобрения/отклонения
                    moderationMessageTextView.text = "Мемориал ожидает вашего рассмотрения. Вы можете одобрить или отклонить публикацию."
                    sendForModerationButton.visibility = View.GONE
                    adminModerationButtonsLayout.visibility = View.VISIBLE
                    
                    // Настраиваем обработчики для кнопок админа
                    setupAdminModerationButtons()
                } else {
                    // Для владельца или других пользователей просто показываем информацию
                    moderationMessageTextView.text = "Мемориал ожидает публикации. Ожидайте решения администратора."
                    sendForModerationButton.visibility = View.GONE
                    adminModerationButtonsLayout.visibility = View.GONE
                }
            }
            
            PublicationStatus.REJECTED -> {
                if (isOwner) {
                    // Для владельца показываем информацию о причине отклонения и кнопку повторной отправки
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Публикация мемориала была отклонена. Проверьте уведомления для получения информации о причинах."
                    sendForModerationButton.visibility = View.VISIBLE
                    sendForModerationButton.text = "Отправить на повторную публикацию"
                    adminModerationButtonsLayout.visibility = View.GONE
                    
                    // Настраиваем обработчик для отправки на модерацию
                    setupSendForModerationButton()
                }
            }
            
            PublicationStatus.PUBLISHED -> {
                // Для опубликованного мемориала проверяем, есть ли изменения на модерации
                if (memorial.changesUnderModeration && isOwner) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Ваши изменения мемориала находятся на модерации. Дождитесь решения администратора."
                    sendForModerationButton.visibility = View.GONE
                    adminModerationButtonsLayout.visibility = View.GONE
                } else {
                    // Для обычного опубликованного мемориала не показываем карточку модерации
                moderationCardView.visibility = View.GONE
                }
            }
            
            null -> {
                // Для совместимости со старой версией API
                if (isOwner && !memorial.isPublic) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Мемориал не опубликован. Отправьте его на публикацию для размещения на сайте."
                    sendForModerationButton.visibility = View.VISIBLE
                    adminModerationButtonsLayout.visibility = View.GONE
                    
                    // Настраиваем обработчик для отправки на модерацию
                    setupSendForModerationButton()
                }
            }
        }
    }
    
    // Настройка кнопки отправки на модерацию
    private fun setupSendForModerationButton() {
        sendForModerationButton.setOnClickListener {
            // Показываем диалог подтверждения с расширенным предупреждением
            AlertDialog.Builder(this)
                .setTitle("Отправка на публикацию")
                .setMessage("Отправить мемориал на публикацию?\n\n" +
                        "Важно! После отправки на публикацию:\n" +
                        "• Мемориал станет недоступен для редактирования\n" +
                        "• Изменения будут возможны только после решения администратора\n" +
                        "• Вы получите уведомление о результате проверки")
                .setPositiveButton("Отправить") { _, _ ->
                    sendMemorialForModeration()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
    
    // Настройка кнопок одобрения и отклонения для администратора
    private fun setupAdminModerationButtons() {
        approveButton.setOnClickListener {
            // Показываем диалог подтверждения
            AlertDialog.Builder(this)
                .setTitle("Одобрение публикации")
                .setMessage("Одобрить публикацию этого мемориала?\n\n" +
                        "Владельцу мемориала будет отправлено уведомление об одобрении публикации.")
                .setPositiveButton("Одобрить") { _, _ ->
                    approveMemorial()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        
        rejectButton.setOnClickListener {
            // Показываем диалог с полем для ввода причины отклонения
            val dialogView = layoutInflater.inflate(R.layout.dialog_rejection_reason, null)
            val reasonEditText = dialogView.findViewById<EditText>(R.id.reasonEditText)
            
            AlertDialog.Builder(this)
                .setTitle("Отклонение публикации")
                .setMessage("Укажите причину отклонения публикации. Это сообщение будет отправлено владельцу мемориала.")
                .setView(dialogView)
                .setPositiveButton("Отклонить") { _, _ ->
                    val reason = reasonEditText.text.toString().trim()
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Необходимо указать причину отклонения", Toast.LENGTH_SHORT).show()
                    } else {
                        rejectMemorial(reason)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
    
    // Метод для отправки мемориала на модерацию
    private fun sendMemorialForModeration() {
        memorial?.id?.let { memorialId ->
            val progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Отправка на публикацию...")
                setCancelable(false)
                show()
            }

            // Проверка токена
            val token = RetrofitClient.getToken()
            if (token.isNullOrEmpty()) {
                progressDialog.dismiss()
                Log.e(TAG, "Ошибка аутентификации: токен пустой или null")
                Toast.makeText(this, "Ошибка аутентификации. Пожалуйста, перезайдите в приложение.", Toast.LENGTH_LONG).show()
                return@let
            }
            Log.d(TAG, "Используем токен: ${token.substring(0, Math.min(20, token.length))}...")

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d(TAG, "Начинаем отправку мемориала ID=$memorialId на модерацию")
                    val apiService = RetrofitClient.getApiService()
                    val updatedMemorial = withContext(Dispatchers.IO) {
                        apiService.sendMemorialForModeration(memorialId)
                    }
                    progressDialog.dismiss()
                    Log.d(TAG, "Мемориал успешно отправлен на модерацию, получен ответ от сервера")
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Мемориал отправлен на публикацию",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Обновляем мемориал
                    memorial = updatedMemorial
                    updateUI(updatedMemorial)
                    
                    // Переходим на экран уведомлений
                    redirectToNotifications()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Ошибка при отправке мемориала на модерацию: ${e.message}", e)
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Ошибка при отправке на публикацию: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Метод для одобрения мемориала (только для администраторов)
    private fun approveMemorial() {
        memorial?.id?.let { memorialId ->
            val progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Одобрение публикации...")
                setCancelable(false)
                show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val apiService = RetrofitClient.getApiService()
                    val updatedMemorial = withContext(Dispatchers.IO) {
                        apiService.approveMemorial(memorialId)
                    }
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Мемориал успешно опубликован",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Обновляем мемориал
                    memorial = updatedMemorial
                    updateUI(updatedMemorial)
                    
                    // Переходим на экран уведомлений
                    redirectToNotifications()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Ошибка при публикации: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Ошибка при публикации мемориала: ${e.message}", e)
                }
            }
        }
    }
    
    // Метод для отклонения мемориала с указанием причины (только для администраторов)
    private fun rejectMemorial(reason: String) {
        memorial?.id?.let { memorialId ->
            val progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Отклонение публикации...")
                setCancelable(false)
                show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val apiService = RetrofitClient.getApiService()
                    val updatedMemorial = withContext(Dispatchers.IO) {
                        apiService.rejectMemorial(memorialId, reason)
                    }
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Публикация мемориала отклонена",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Обновляем мемориал
                    memorial = updatedMemorial
                    updateUI(updatedMemorial)
                    
                    // Переходим на экран уведомлений
                    redirectToNotifications()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewMemorialActivity,
                        "Ошибка при отклонении: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Ошибка при отклонении мемориала: ${e.message}", e)
                }
            }
        }
    }
    
    // Метод для перехода к экрану уведомлений
    private fun redirectToNotifications() {
        Log.d(TAG, "Подготовка к перенаправлению на экран уведомлений (задержка 1.5 сек)")
        // Небольшая задержка, чтобы сервер успел создать уведомления
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            Log.d(TAG, "Выполняем перенаправление на экран уведомлений, вкладка исходящих")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "notifications")
                putExtra("tab_position", 1) // Переходим на вкладку "Исходящие"
                // Убираем FLAG_ACTIVITY_CLEAR_TOP для правильной навигации
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }, 1000) // Задержка 1 секунды
    }

    // Вспомогательный метод для обновления UI после изменения статуса мемориала
    private fun updateUI(memorial: Memorial) {
        // Сохраняем мемориал и обновляем статус публикации
        this.memorial = memorial
        updatePublicationStatus(memorial)
    }
} 