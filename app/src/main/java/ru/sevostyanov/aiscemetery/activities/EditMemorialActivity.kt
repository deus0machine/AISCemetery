package ru.sevostyanov.aiscemetery.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.databinding.ActivityEditMemorialBinding
import ru.sevostyanov.aiscemetery.fragments.LocationPickerFragment
import ru.sevostyanov.aiscemetery.models.Location
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.util.GlideHelper
import ru.sevostyanov.aiscemetery.util.NetworkUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditMemorialActivity : AppCompatActivity() {

    private lateinit var photoImageView: ImageView
    private lateinit var fioEdit: EditText
    private lateinit var birthDateButton: Button
    private lateinit var deathDateButton: Button
    private lateinit var biographyEdit: EditText
    private lateinit var mainLocationButton: Button
    private lateinit var burialLocationButton: Button

    private var selectedPhotoUri: Uri? = null
    private var birthDate: Long? = null
    private var deathDate: Long? = null
    private var mainLocation: Location? = null
    private var burialLocation: Location? = null
    private var shouldDeletePhoto: Boolean = false
    private var hasSubscription: Boolean = false

    private var memorial: Memorial? = null
    private lateinit var repository: MemorialRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var binding: ActivityEditMemorialBinding
    private lateinit var editFio: EditText
    private lateinit var buttonBirthDate: Button
    private lateinit var buttonDeathDate: Button
    private lateinit var editBiography: EditText
    private lateinit var buttonSave: Button
    private lateinit var dateFormatter: SimpleDateFormat
    private var memorialId: Long = -1L

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                // Проверяем размер файла
                contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val fileSize = inputStream.available()
                    if (fileSize > MAX_FILE_SIZE) {
                        showError("Размер файла не должен превышать 10MB")
                        return@let
                    }
                }
                // Используем изображение напрямую
                selectedPhotoUri = selectedUri
                shouldDeletePhoto = false
                GlideHelper.loadImageFromUri(
                    this,
                    selectedUri,
                    photoImageView,
                    R.drawable.placeholder_photo,
                    R.drawable.placeholder_photo
                )
            } catch (e: Exception) {
                showError("Ошибка при выборе фото: ${e.message}")
            }
        }
    }

    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                shouldDeletePhoto = false
                GlideHelper.loadImageFromUri(
                    this,
                    uri,
                    photoImageView,
                    R.drawable.placeholder_photo,
                    R.drawable.placeholder_photo
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация ViewBinding
        binding = ActivityEditMemorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем наличие подписки у пользователя
        val user = ru.sevostyanov.aiscemetery.user.UserManager.getCurrentUser() 
            ?: ru.sevostyanov.aiscemetery.user.UserManager.loadUserFromPreferences(this)
        hasSubscription = user?.hasSubscription ?: false

        // Инициализация форматтера даты
        dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Инициализация Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Инициализация репозитория
        repository = MemorialRepository()

        // Инициализация кнопок
        mainLocationButton = binding.mainLocationButton
        burialLocationButton = binding.burialLocationButton

        // Если нет подписки, настраиваем UI соответственно
        if (!hasSubscription) {
            mainLocationButton.isEnabled = false
            burialLocationButton.isEnabled = false
            mainLocationButton.text = "Нужна подписка"
            burialLocationButton.text = "Нужна подписка"
        }

        // Инициализация других view
        editFio = binding.editFio
        buttonBirthDate = binding.buttonBirthDate
        buttonDeathDate = binding.buttonDeathDate
        editBiography = binding.editBiography
        buttonSave = binding.buttonSave
        photoImageView = binding.photoImageView

        // Загрузка мемориала, если он есть
        val memorial = intent.getParcelableExtra<Memorial>(EXTRA_MEMORIAL)
        if (memorial != null) {
            this.memorial = memorial
            memorialId = memorial.id ?: -1L
            
            // Проверяем, находится ли мемориал на модерации
            if (memorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
                // Показываем диалог и логгируем
                Log.e("EditMemorialActivity", "Попытка открыть на редактирование мемориал на модерации! id=${memorial.id}, статус=${memorial.publicationStatus}")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Мемориал на модерации")
                    .setMessage("Этот мемориал находится на модерации и не может быть отредактирован до принятия решения администратором.")
                    .setPositiveButton("ОК") { _, _ ->
                        // Закрываем активность редактирования и возвращаемся к просмотру
                        ViewMemorialActivity.start(this, memorial)
                        finish()
                    }
                    .setCancelable(false)
                    .show()
                return
            }
            
            // Проверяем, находятся ли изменения мемориала на модерации
            if (memorial.changesUnderModeration) {
                // Показываем диалог и логгируем
                Log.e("EditMemorialActivity", "Попытка открыть на редактирование мемориал с изменениями на модерации! id=${memorial.id}")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Изменения на модерации")
                    .setMessage("Изменения этого мемориала находятся на модерации и мемориал не может быть отредактирован до принятия решения администратором.")
                    .setPositiveButton("ОК") { _, _ ->
                        // Закрываем активность редактирования и возвращаемся к просмотру
                        ViewMemorialActivity.start(this, memorial)
                        finish()
                    }
                    .setCancelable(false)
                    .show()
                return
            }
            
            loadMemorial(memorialId)
        } else {
            // Если это новый мемориал, сбрасываем все поля
            editFio.setText("")
            editBiography.setText("")
            buttonBirthDate.text = "Выбрать дату рождения"
            buttonDeathDate.text = "Выбрать дату смерти"
            
            // Настройка текста кнопок локации в зависимости от наличия подписки
            if (hasSubscription) {
                mainLocationButton.text = "Выбрать основное местоположение"
                burialLocationButton.text = "Выбрать место захоронения"
            }
            
            photoImageView.setImageResource(R.drawable.placeholder_photo)
        }

        // Настройка слушателей
        setupListeners()
    }

    private fun setupListeners() {
        photoImageView.setOnClickListener {
            showPhotoOptions()
        }

        buttonBirthDate.setOnClickListener {
            showDatePicker("Дата рождения") { timestamp ->
                birthDate = timestamp
                buttonBirthDate.text = dateFormatter.format(Date(timestamp))
            }
        }

        buttonDeathDate.setOnClickListener {
            showDatePicker("Дата смерти") { timestamp ->
                deathDate = timestamp
                buttonDeathDate.text = dateFormatter.format(Date(timestamp))
            }
        }

        buttonSave.setOnClickListener {
            saveMemorial()
        }

        // Проверка прав доступа к редактированию местоположения
        // Разрешаем редакторам мемориала редактировать местоположение даже без подписки
        val isEditor = memorial?.isEditor == true || memorial?.isUserEditor == true
        val canEditLocation = hasSubscription || isEditor

        mainLocationButton.setOnClickListener {
            if (canEditLocation) {
                showLocationPicker(true)
            } else {
                showSubscriptionRequiredDialog()
            }
        }

        burialLocationButton.setOnClickListener {
            if (canEditLocation) {
                showLocationPicker(false)
            } else {
                showSubscriptionRequiredDialog()
            }
        }

        // Обновляем текст кнопок в зависимости от прав доступа
        if (!hasSubscription && !isEditor) {
            mainLocationButton.isEnabled = false
            burialLocationButton.isEnabled = false
            mainLocationButton.text = "Нужна подписка"
            burialLocationButton.text = "Нужна подписка"
        } else {
            mainLocationButton.isEnabled = true
            burialLocationButton.isEnabled = true
            
            // Обновляем текст только если он не был установлен ранее
            if (mainLocationButton.text == "Нужна подписка") {
                mainLocationButton.text = "Выбрать основное местоположение"
            }
            if (burialLocationButton.text == "Нужна подписка") {
                burialLocationButton.text = "Выбрать место захоронения"
            }
        }

        // Подписываемся на результат выбора местоположения
        supportFragmentManager.setFragmentResultListener("location_picker_result", this) { _, bundle ->
            val location = bundle.getParcelable<Location>("location")
            if (location != null) {
                if (bundle.getBoolean("is_main_location", true)) {
                    mainLocation = location
                    mainLocationButton.text = location.address ?: "Основное местоположение выбрано"
                } else {
                    burialLocation = location
                    burialLocationButton.text = location.address ?: "Место захоронения выбрано"
                }
            }
        }
    }

    private fun showDatePicker(title: String, onDateSelected: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            onDateSelected(timestamp)
        }

        picker.show(supportFragmentManager, null)
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Выбрать фото", "Удалить фото")
        MaterialAlertDialogBuilder(this)
            .setTitle("Фото мемориала")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> {
                        selectedPhotoUri = null
                        shouldDeletePhoto = true
                        photoImageView.setImageResource(R.drawable.placeholder_photo)
                    }
                }
            }
            .show()
    }

    private fun startImageCrop(uri: Uri) {
        try {
            val intent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(uri, "image/*")
                putExtra("crop", "true")
                putExtra("aspectX", 1)
                putExtra("aspectY", 1)
                putExtra("return-data", false)
                putExtra("outputFormat", "JPEG")
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }
            cropImage.launch(intent)
        } catch (e: Exception) {
            // Если устройство не поддерживает обрезку, просто используем оригинальное изображение
            selectedPhotoUri = uri
            shouldDeletePhoto = false
            GlideHelper.loadImageFromUri(
                this,
                uri,
                photoImageView,
                R.drawable.placeholder_photo,
                R.drawable.placeholder_photo
            )
        }
    }

    private fun isInternetAvailable(): Boolean {
        return NetworkUtil.isInternetAvailable(this)
    }

    private fun loadImage(url: String?) {
        if (url == null) {
            photoImageView.setImageResource(R.drawable.placeholder_photo)
            return
        }

        println("EditMemorialActivity: Loading image from URL: $url")

        if (!NetworkUtil.checkInternetAndShowMessage(this, false)) {
            showMessage("Нет подключения к интернету. Изображение не может быть загружено")
            photoImageView.setImageResource(R.drawable.placeholder_photo)
            return
        }

        try {
            GlideHelper.loadImage(
                this,
                url,
                photoImageView,
                R.drawable.placeholder_photo, 
                R.drawable.placeholder_photo
            )
        } catch (e: Exception) {
            println("EditMemorialActivity: Ошибка при загрузке изображения: ${e.message}")
            photoImageView.setImageResource(R.drawable.placeholder_photo)
        }
    }

    private fun loadMemorial(id: Long) {
        lifecycleScope.launch {
            try {
                val memorial = repository.getMemorialById(id)
                println("Загруженный мемориал: $memorial")
                memorial?.let {
                    editFio.setText(it.fio)
                    editBiography.setText(it.biography)
                    
                    // Установка дат
                    it.birthDate?.let { date ->
                        try {
                            // Пробуем разные форматы даты
                            val formats = listOf(
                                "yyyy-MM-dd",
                                "dd.MM.yyyy",
                                "yyyy/MM/dd"
                            )
                            
                            var parsedDate: Date? = null
                            for (format in formats) {
                                try {
                                    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                                    val tempDate = dateFormat.parse(date)
                                    if (tempDate != null) {
                                        parsedDate = tempDate
                                        break
                                    }
                                } catch (e: Exception) {
                                    continue
                                }
                            }
                            
                            parsedDate?.let { finalDate ->
                                birthDate = finalDate.time
                                buttonBirthDate.text = dateFormatter.format(finalDate)
                            } ?: run {
                                println("Не удалось распарсить дату рождения: $date")
                            }
                        } catch (e: Exception) {
                            println("Ошибка при обработке даты рождения: ${e.message}")
                        }
                    }
                    
                    it.deathDate?.let { date ->
                        try {
                            // Пробуем разные форматы даты
                            val formats = listOf(
                                "yyyy-MM-dd",
                                "dd.MM.yyyy",
                                "yyyy/MM/dd"
                            )
                            
                            var parsedDate: Date? = null
                            for (format in formats) {
                                try {
                                    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                                    val tempDate = dateFormat.parse(date)
                                    if (tempDate != null) {
                                        parsedDate = tempDate
                                        break
                                    }
                                } catch (e: Exception) {
                                    continue
                                }
                            }
                            
                            parsedDate?.let { finalDate ->
                                deathDate = finalDate.time
                                buttonDeathDate.text = dateFormatter.format(finalDate)
                            } ?: run {
                                println("Не удалось распарсить дату смерти: $date")
                            }
                        } catch (e: Exception) {
                            println("Ошибка при обработке даты смерти: ${e.message}")
                        }
                    }

                    // Установка местоположений
                    it.mainLocation?.let { location ->
                        mainLocation = location
                        mainLocationButton.text = location.address ?: "Основное местоположение выбрано"
                    }
                    it.burialLocation?.let { location ->
                        burialLocation = location
                        burialLocationButton.text = location.address ?: "Место захоронения выбрано"
                    }

                    // Загрузка изображения
                    it.photoUrl?.let { url ->
                        loadImage(url)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка загрузки мемориала: ${e.message}")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_memorial, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveMemorial()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveMemorial() {
        if (!NetworkUtil.checkInternetAndShowMessage(this)) {
            return
        }

        val name = editFio.text.toString().trim()
        val description = editBiography.text.toString().trim()

        // Валидация полей согласно серверным ограничениям
        if (name.isEmpty()) {
            showMessage("Пожалуйста, укажите ФИО")
            editFio.requestFocus()
            return
        }
        
        if (name.length < 2) {
            showMessage("ФИО должно содержать не менее 2 символов")
            editFio.requestFocus()
            return
        }
        
        if (name.length > 255) {
            showMessage("ФИО не должно превышать 255 символов")
            editFio.requestFocus()
            return
        }
        
        // Проверка даты рождения - обязательное поле согласно серверной модели
        if (birthDate == null) {
            showMessage("Пожалуйста, укажите дату рождения")
            buttonBirthDate.requestFocus()
            return
        }
        
        // Проверка логической корректности дат
        val currentTimeMillis = System.currentTimeMillis()
        if (birthDate!! > currentTimeMillis) {
            showMessage("Дата рождения не может быть в будущем")
            buttonBirthDate.requestFocus()
            return
        }
        
        // Если указана дата смерти, проверяем что она не раньше даты рождения
        deathDate?.let { death ->
            if (death < birthDate!!) {
                showMessage("Дата смерти не может быть раньше даты рождения")
                buttonDeathDate.requestFocus()
                return
            }
            
            if (death > currentTimeMillis) {
                showMessage("Дата смерти не может быть в будущем")
                buttonDeathDate.requestFocus()
                return
            }
        }
        
        // Проверка биографии на разумную длину
        if (description.length > 5000) {
            showMessage("Биография не должна превышать 5000 символов")
            editBiography.requestFocus()
            return
        }

        val currentMemorial = memorial
        
        // Дополнительная проверка перед сохранением - убедимся, что мемориал не на модерации
        if (currentMemorial?.publicationStatus == PublicationStatus.PENDING_MODERATION) {
            Log.e("EditMemorialActivity", "Попытка сохранить мемориал, находящийся на модерации! id=${currentMemorial.id}")
            MaterialAlertDialogBuilder(this)
                .setTitle("Невозможно сохранить")
                .setMessage("Этот мемориал находится на модерации и не может быть отредактирован до принятия решения администратором.")
                .setPositiveButton("ОК", null)
                .show()
            return
        }
        
        // Дополнительная проверка - изменения на модерации
        if (currentMemorial?.changesUnderModeration == true) {
            Log.e("EditMemorialActivity", "Попытка сохранить мемориал с изменениями на модерации! id=${currentMemorial.id}")
            MaterialAlertDialogBuilder(this)
                .setTitle("Невозможно сохранить")
                .setMessage("Изменения этого мемориала находятся на модерации и мемориал не может быть отредактирован до принятия решения администратором.")
                .setPositiveButton("ОК", null)
                .show()
            return
        }
        
        // Log locations before creating memorial object
        println("LOCATION DEBUG - Before creating memorial:")
        println("Main Location: $mainLocation")
        println("Burial Location: $burialLocation")
        
        // Определяем статус пользователя как редактора
        // Используем isEditor из текущего мемориала или проверяем isUserEditor
        val isEditor = currentMemorial?.isEditor == true || currentMemorial?.isUserEditor == true
        
        // Если у пользователя нет подписки и он не редактор, не разрешаем ему устанавливать местоположение
        // Иначе разрешаем редактировать местоположение всем редакторам и пользователям с подпиской
        val hasLocationEditAccess = hasSubscription || isEditor
        val finalMainLocation = if (hasLocationEditAccess) mainLocation else currentMemorial?.mainLocation
        val finalBurialLocation = if (hasLocationEditAccess) burialLocation else currentMemorial?.burialLocation
        
        val newMemorial = Memorial(
            id = currentMemorial?.id,
            fio = name,
            biography = description,
            birthDate = birthDate?.let { formatDateForServer(it) },
            deathDate = deathDate?.let { formatDateForServer(it) },
            mainLocation = finalMainLocation,
            burialLocation = finalBurialLocation,
            photoUrl = currentMemorial?.photoUrl,
            isPublic = currentMemorial?.isPublic ?: false, // Сохраняем текущее значение или по умолчанию false
            treeId = null,
            createdBy = null,
            createdAt = null,
            updatedAt = null,
            editors = currentMemorial?.editors,
            isEditor = isEditor,
            // Если пользователь редактор и это существующий мемориал, устанавливаем флаг pendingChanges
            pendingChanges = isEditor && currentMemorial?.id != null
        )
        
        // Log the memorial object being sent
        println("LOCATION DEBUG - Memorial being sent to server:")
        println("Memorial: $newMemorial")
        println("mainLocation: ${newMemorial.mainLocation}")
        println("burialLocation: ${newMemorial.burialLocation}")
        println("isEditor: ${newMemorial.isEditor}")
        println("pendingChanges: ${newMemorial.pendingChanges}")
        println("isUserOwner: ${currentMemorial?.isUserOwner}")
        println("isUserEditor: ${currentMemorial?.isUserEditor}")

        // Сохраняем ссылку на контекст активности за пределами корутины
        val activityContext = this

        // Проверяем, является ли пользователь редактором
        Log.d("EditMemorialActivity", "Проверка прав доступа: isEditor=$isEditor, isUserEditor=${currentMemorial?.isUserEditor}, isUserOwner=${currentMemorial?.isUserOwner}")
        
        // Проверяем, является ли это опубликованный мемориал, который изменяет владелец
        val isPublishedMemorial = currentMemorial?.publicationStatus == PublicationStatus.PUBLISHED
        val isOwner = currentMemorial?.isUserOwner == true
        
        if (isOwner && isPublishedMemorial && currentMemorial?.id != null) {
            // Владелец редактирует опубликованный мемориал - предлагаем отправить изменения на модерацию
            MaterialAlertDialogBuilder(this)
                .setTitle("Отправить изменения на публикацию?")
                .setMessage("Поскольку мемориал уже опубликован, ваши изменения должны быть проверены администратором перед публикацией.\n\n" +
                        "• Изменения будут отправлены на публикацию\n" +
                        "• Мемориал станет недоступен для редактирования до решения администратора\n" +
                        "• Вы получите уведомление о результате публикации")
                .setPositiveButton("Отправить на публикацию") { _, _ ->
                    // Сохраняем изменения и сразу отправляем на публикацию
                    performSaveAndSendForModeration(newMemorial, activityContext)
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else if (isEditor && currentMemorial?.isUserOwner != true && currentMemorial?.id != null) {
            // Показываем диалог подтверждения для редакторов в обоих случаях - и для публичных, и для личных мемориалов
            MaterialAlertDialogBuilder(this)
                .setTitle("Запрос на изменение")
                .setMessage("Поскольку вы являетесь редактором этого мемориала, ваши изменения должны быть одобрены владельцем. После сохранения владельцу будет отправлен запрос на подтверждение изменений.")
                .setPositiveButton("Отправить на рассмотрение") { _, _ ->
                    // Продолжаем сохранение с флагом pendingChanges
                    performSave(newMemorial, activityContext)
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            // Обычное сохранение для владельца непубличного мемориала или нового мемориала
            performSave(newMemorial, activityContext)
        }
    }
    
    // Метод для сохранения мемориала и отправки изменений на модерацию
    private fun performSaveAndSendForModeration(newMemorial: Memorial, activityContext: Context) {
        lifecycleScope.launch {
            try {
                // Сначала сохраняем изменения
                val savedMemorial = if (newMemorial.id == null) {
                    repository.createMemorial(newMemorial)
                } else {
                    repository.updateMemorial(newMemorial.id!!, newMemorial)
                }

                // Обработка фото как в обычном методе сохранения
                var finalMemorial = savedMemorial
                
                // Если нужно удалить фото
                if (shouldDeletePhoto && newMemorial.photoUrl != null) {
                    try {
                        savedMemorial.id?.let { id ->
                            repository.deletePhoto(id)
                            runOnUiThread {
                                loadMemorial(memorialId)
                            }
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при удалении фото: ${e.message}")
                        return@launch
                    }
                }

                // Если есть новое фото для загрузки
                val currentPhotoUri = selectedPhotoUri
                if (currentPhotoUri != null) {
                    try {
                        savedMemorial.id?.let { id ->
                            val photoUrl = repository.uploadPhoto(id, currentPhotoUri, activityContext)
                            // Обновляем мемориал с новым URL фото
                            val updatedMemorial = Memorial(
                                id = savedMemorial.id,
                                fio = savedMemorial.fio,
                                birthDate = savedMemorial.birthDate,
                                deathDate = savedMemorial.deathDate,
                                biography = savedMemorial.biography,
                                mainLocation = savedMemorial.mainLocation,
                                burialLocation = savedMemorial.burialLocation,
                                photoUrl = photoUrl,
                                isPublic = savedMemorial.isPublic,
                                publicationStatus = savedMemorial.publicationStatus,
                                treeId = savedMemorial.treeId,
                                createdBy = null,
                                createdAt = null,
                                updatedAt = null,
                                editors = savedMemorial.editors,
                                isEditor = savedMemorial.isEditor,
                                pendingChanges = savedMemorial.pendingChanges,
                                changesUnderModeration = savedMemorial.changesUnderModeration
                            )
                            finalMemorial = repository.updateMemorial(id, updatedMemorial)
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при загрузке фото: ${e.message}")
                        return@launch
                    }
                }

                // Теперь отправляем изменения на модерацию
                try {
                    finalMemorial.id?.let { id ->
                        val moderatedMemorial = repository.sendChangesForModeration(id)
                        
                        runOnUiThread {
                            showMessage("Изменения сохранены и отправлены на модерацию")
                        }
                        
                        // Возвращаем результат
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_MEMORIAL, moderatedMemorial)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } catch (e: Exception) {
                    showError("Ошибка при отправке на модерацию: ${e.message}")
                    return@launch
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка при сохранении: ${e.message}")
            }
        }
    }

    // Выносим логику сохранения в отдельный метод
    private fun performSave(newMemorial: Memorial, activityContext: Context) {
        lifecycleScope.launch {
            try {
                var savedMemorial = if (newMemorial.id == null) {
                    println("Создание нового мемориала")
                    repository.createMemorial(newMemorial)
                } else {
                    println("Обновление существующего мемориала с id: ${newMemorial.id}")
                    repository.updateMemorial(newMemorial.id!!, newMemorial)
                }

                println("LOCATION DEBUG - Saved memorial response from server:")
                println("savedMemorial: $savedMemorial")
                println("mainLocation: ${savedMemorial.mainLocation}")
                println("burialLocation: ${savedMemorial.burialLocation}")
                println("pendingChanges: ${savedMemorial.pendingChanges}")

                // Если нужно удалить фото
                if (shouldDeletePhoto && newMemorial.photoUrl != null) {
                    try {
                        savedMemorial.id?.let { id ->
                            repository.deletePhoto(id)
                            runOnUiThread {
                                loadMemorial(memorialId)
                            }
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при удалении фото: ${e.message}")
                        return@launch
                    }
                }

                // Если есть новое фото для загрузки
                val currentPhotoUri = selectedPhotoUri
                if (currentPhotoUri != null) {
                    if (!NetworkUtil.isInternetAvailable(activityContext)) {
                        showError("Нет подключения к интернету. Фото будет загружено позже")
                        setResult(Activity.RESULT_OK)
                        finish()
                        return@launch
                    }

                    try {
                        contentResolver.openInputStream(currentPhotoUri)?.use { inputStream ->
                            val fileSize = inputStream.available()
                            if (fileSize > MAX_FILE_SIZE) {
                                showError("Размер файла не должен превышать 10MB")
                                return@launch
                            }
                        }
                        try {
                            savedMemorial.id?.let { id ->
                                val photoUrl = repository.uploadPhoto(id, currentPhotoUri, activityContext)
                                try {
                                    // Обновляем мемориал с новым URL фото, сохраняя все остальные поля
                                    val updatedMemorial = Memorial(
                                        id = savedMemorial.id,
                                        fio = savedMemorial.fio,
                                        birthDate = savedMemorial.birthDate,
                                        deathDate = savedMemorial.deathDate,
                                        biography = savedMemorial.biography,
                                        mainLocation = savedMemorial.mainLocation,
                                        burialLocation = savedMemorial.burialLocation,
                                        photoUrl = photoUrl,
                                        isPublic = savedMemorial.isPublic,
                                        publicationStatus = savedMemorial.publicationStatus,
                                        treeId = savedMemorial.treeId,
                                        createdBy = null,
                                        createdAt = null,
                                        updatedAt = null,
                                        editors = savedMemorial.editors,
                                        isEditor = savedMemorial.isEditor,
                                        pendingChanges = savedMemorial.pendingChanges,
                                        changesUnderModeration = savedMemorial.changesUnderModeration
                                    )
                                    savedMemorial = repository.updateMemorial(id, updatedMemorial)
                                    println("Мемориал обновлен с новым фото: $savedMemorial")
                                    
                                    // Обновляем локальную переменную и UI
                                    memorial = savedMemorial
                                    runOnUiThread {
                                        loadMemorial(memorialId)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    showError("Ошибка при обновлении фото: ${e.message}")
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showError("Ошибка при загрузке фото: ${e.message}")
                            return@launch
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при загрузке фото: ${e.message}")
                        return@launch
                    }
                }

                // Показываем сообщение в зависимости от статуса (редактор или владелец)
                runOnUiThread {
                    // Если пользователь редактор (но не владелец) и это существующий мемориал
                    if (newMemorial.isEditor && savedMemorial.isUserOwner != true && newMemorial.id != null) {
                        showMessage("Ваши изменения отправлены на рассмотрение владельцу мемориала")
                    } else {
                        showMessage("Мемориал успешно сохранен")
                    }
                }

                // Возвращаем обновленный мемориал
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_MEMORIAL, savedMemorial)
                }
                println("Возвращаем результат: $savedMemorial")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка при сохранении: ${e.message}")
            }
        }
    }

    private fun formatDateForServer(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLocationPicker(isMainLocation: Boolean) {
        val fragment = LocationPickerFragment()
        val bundle = Bundle().apply {
            putBoolean("is_main_location", isMainLocation)
        }
        fragment.arguments = bundle
        
        fragment.show(supportFragmentManager, "location_picker")
    }

    private fun showSubscriptionRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Требуется подписка")
            .setMessage("Для использования возможности указания местоположения требуется подписка")
            .setPositiveButton("Информация о подписке") { _, _ ->
                // Здесь можно открыть экран с информацией о подписке
                Toast.makeText(this, "Информация о подписке", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        const val EXTRA_MEMORIAL = "extra_memorial"
        const val REQUEST_CREATE = 100
        const val REQUEST_EDIT = 101

        fun start(activity: Activity, memorial: Memorial? = null) {
            val intent = Intent(activity, EditMemorialActivity::class.java).apply {
                putExtra(EXTRA_MEMORIAL, memorial)
            }
            activity.startActivityForResult(
                intent,
                if (memorial == null) REQUEST_CREATE else REQUEST_EDIT
            )
        }
    }
} 