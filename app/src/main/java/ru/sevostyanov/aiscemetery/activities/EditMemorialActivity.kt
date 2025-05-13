package ru.sevostyanov.aiscemetery.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.databinding.ActivityEditMemorialBinding
import ru.sevostyanov.aiscemetery.fragments.LocationPickerFragment
import ru.sevostyanov.aiscemetery.models.Location
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
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
                Glide.with(this)
                    .load(selectedUri)
                    .placeholder(R.drawable.placeholder_photo)
                    .error(R.drawable.placeholder_photo)
                    .centerCrop()
                    .into(photoImageView)
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
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_photo)
                    .error(R.drawable.placeholder_photo)
                    .centerCrop()
                    .into(photoImageView)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация ViewBinding
        binding = ActivityEditMemorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            loadMemorial(memorialId)
        } else {
            // Если это новый мемориал, сбрасываем все поля
            editFio.setText("")
            editBiography.setText("")
            buttonBirthDate.text = "Выбрать дату рождения"
            buttonDeathDate.text = "Выбрать дату смерти"
            mainLocationButton.text = "Выбрать основное местоположение"
            burialLocationButton.text = "Выбрать место захоронения"
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

        mainLocationButton.setOnClickListener {
            showLocationPicker(true)
        }

        burialLocationButton.setOnClickListener {
            showLocationPicker(false)
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
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.placeholder_photo)
                .error(R.drawable.placeholder_photo)
                .centerCrop()
                .into(photoImageView)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadImage(url: String?) {
        if (url == null) {
            photoImageView.setImageResource(R.drawable.placeholder_photo)
            return
        }

        println("EditMemorialActivity: Loading image from URL: $url")

        if (!isInternetAvailable()) {
            showMessage("Нет подключения к интернету. Изображение не может быть загружено")
            photoImageView.setImageResource(R.drawable.placeholder_photo)
            return
        }

        try {
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.placeholder_photo)
                .error(R.drawable.placeholder_photo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(photoImageView)
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
        if (!isInternetAvailable()) {
            showMessage("Нет подключения к интернету")
            return
        }

        val name = editFio.text.toString().trim()
        val description = editBiography.text.toString().trim()

        if (name.isEmpty()) {
            showMessage("Пожалуйста, укажите ФИО")
            return
        }

        val currentMemorial = memorial
        val newMemorial = Memorial(
            id = currentMemorial?.id,
            fio = name,
            biography = description,
            birthDate = birthDate?.let { formatDateForServer(it) },
            deathDate = deathDate?.let { formatDateForServer(it) },
            mainLocation = mainLocation,
            burialLocation = burialLocation,
            photoUrl = currentMemorial?.photoUrl,
            isPublic = true,
            treeId = null,
            createdBy = null,
            createdAt = null,
            updatedAt = null
        )

        lifecycleScope.launch {
            try {
                var savedMemorial = if (newMemorial.id == null) {
                    println("Создание нового мемориала")
                    repository.createMemorial(newMemorial)
                } else {
                    println("Обновление существующего мемориала с id: ${newMemorial.id}")
                    repository.updateMemorial(newMemorial.id!!, newMemorial)
                }

                println("Сохраненный мемориал: $savedMemorial")

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
                    if (!isInternetAvailable()) {
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
                                val photoUrl = repository.uploadPhoto(id, currentPhotoUri, this@EditMemorialActivity)
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
                                        treeId = savedMemorial.treeId,
                                        createdBy = null,
                                        createdAt = null,
                                        updatedAt = null
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