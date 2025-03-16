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
import ru.sevostyanov.aiscemetery.models.Location
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val repository = MemorialRepository()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setContentView(R.layout.activity_edit_memorial)

        memorial = intent.getParcelableExtra(EXTRA_MEMORIAL)

        setupToolbar()
        initializeViews()
        setupListeners()
        loadMemorial()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (memorial == null) "Новый мемориал" else "Редактирование"
    }

    private fun initializeViews() {
        photoImageView = findViewById(R.id.image_photo)
        fioEdit = findViewById(R.id.edit_fio)
        birthDateButton = findViewById(R.id.button_birth_date)
        deathDateButton = findViewById(R.id.button_death_date)
        biographyEdit = findViewById(R.id.edit_biography)
        mainLocationButton = findViewById(R.id.button_main_location)
        burialLocationButton = findViewById(R.id.button_burial_location)
    }

    private fun setupListeners() {
        photoImageView.setOnClickListener {
            showPhotoOptions()
        }

        birthDateButton.setOnClickListener {
            showDatePicker("Дата рождения") { date ->
                birthDate = date
                birthDateButton.text = formatDate(date)
            }
        }

        deathDateButton.setOnClickListener {
            showDatePicker("Дата смерти") { date ->
                deathDate = date
                deathDateButton.text = formatDate(date)
            }
        }

        mainLocationButton.setOnClickListener {
            // TODO: Открыть карту для выбора местоположения
            showMessage("Функция выбора местоположения будет добавлена позже")
        }

        burialLocationButton.setOnClickListener {
            // TODO: Открыть карту для выбора местоположения
            showMessage("Функция выбора местоположения будет добавлена позже")
        }
    }

    private fun showDatePicker(title: String, onDateSelected: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener { date ->
            onDateSelected(date)
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

        println("Loading image from URL: $url")

        if (!isInternetAvailable()) {
            showMessage("Нет подключения к интернету. Изображение не может быть загружено")
            photoImageView.setImageResource(R.drawable.placeholder_photo)
            return
        }

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.placeholder_photo)
            .error(R.drawable.placeholder_photo)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Кэшируем изображения
            .centerCrop()
            .into(photoImageView)
    }

    private fun loadMemorial() {
        memorial?.let { memorial ->
            fioEdit.setText(memorial.fio)
            biographyEdit.setText(memorial.biography)

            loadImage(memorial.photoUrl)

            memorial.birthDate?.let {
                birthDateButton.text = it
                birthDate = dateFormat.parse(it)?.time
            }

            memorial.deathDate?.let {
                deathDateButton.text = it
                deathDate = dateFormat.parse(it)?.time
            }

            mainLocation = memorial.mainLocation
            mainLocation?.let {
                mainLocationButton.text = it.address ?: "Местоположение выбрано"
            }

            burialLocation = memorial.burialLocation
            burialLocation?.let {
                burialLocationButton.text = it.address ?: "Место захоронения выбрано"
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
        val fio = fioEdit.text.toString()
        if (fio.isBlank()) {
            showError("Введите ФИО")
            return
        }

        if (birthDate == null) {
            showError("Выберите дату рождения")
            return
        }

        if (!isInternetAvailable()) {
            showError("Нет подключения к интернету. Пожалуйста, проверьте подключение и попробуйте снова")
            return
        }

        lifecycleScope.launch {
            try {
                val newMemorial = Memorial(
                    id = memorial?.id,
                    fio = fio,
                    birthDate = birthDate?.let { formatDate(it) },
                    deathDate = deathDate?.let { formatDate(it) },
                    biography = biographyEdit.text.toString().takeIf { it.isNotBlank() },
                    mainLocation = mainLocation,
                    burialLocation = burialLocation,
                    isPublic = memorial?.isPublic ?: false,
                    treeId = memorial?.treeId,
                    photoUrl = if (shouldDeletePhoto) null else memorial?.photoUrl
                )

                val savedMemorial = if (newMemorial.id == null) {
                    repository.createMemorial(newMemorial)
                } else {
                    repository.updateMemorial(newMemorial.id, newMemorial)
                }

                // Если нужно удалить фото
                if (shouldDeletePhoto && memorial?.photoUrl != null) {
                    try {
                        repository.deletePhoto(savedMemorial.id!!)
                        runOnUiThread {
                            loadImage(null)
                        }
                    } catch (e: Exception) {
                        showError("Ошибка при удалении фото: ${e.message}")
                        return@launch
                    }
                }

                // Если есть новое фото для загрузки
                selectedPhotoUri?.let { uri ->
                    if (!isInternetAvailable()) {
                        showError("Нет подключения к интернету. Фото будет загружено позже")
                        setResult(Activity.RESULT_OK)
                        finish()
                        return@launch
                    }

                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val fileSize = inputStream.available()
                            if (fileSize > MAX_FILE_SIZE) {
                                showError("Размер файла не должен превышать 10MB")
                                return@launch
                            }
                        }
                        try {
                            val photoUrl = repository.uploadPhoto(savedMemorial.id!!, uri, this@EditMemorialActivity)
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
                                val finalMemorial = repository.updateMemorial(savedMemorial.id, updatedMemorial)
                                
                                // Обновляем UI с новым URL фото
                                runOnUiThread {
                                    loadImage(finalMemorial.photoUrl)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showError("Ошибка при обновлении фото: ${e.message}")
                                return@launch
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

                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка при сохранении: ${e.message}")
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val EXTRA_MEMORIAL = "extra_memorial"
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