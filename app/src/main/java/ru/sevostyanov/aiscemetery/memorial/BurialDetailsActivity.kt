package ru.sevostyanov.aiscemetery.memorial

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.user.Guest
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class BurialDetailsActivity : AppCompatActivity() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var burialDetailsTextView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private var burialId: Long = -1
    private lateinit var burialImageView: ImageView
    private var burialPhoto: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        setContentView(R.layout.activity_burial_details)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        burialImageView = findViewById(R.id.image_burial)
        // Включаем кнопку "Назад"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        burialDetailsTextView = findViewById(R.id.text_burial_details)
        editButton = findViewById(R.id.button_edit_burial)
        deleteButton = findViewById(R.id.button_delete_burial)

        burialId = intent.getLongExtra("burial_id", -1)
        burialPhoto = intent.getStringExtra("burialPhotoPath").toString()
        if (!burialPhoto.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(burialPhoto)
            burialImageView.setImageBitmap(bitmap)
        } else {
            burialImageView.setImageResource(R.drawable.amogus) // Плейсхолдер
        }
        apiService = RetrofitClient.getApiService()

        loadBurialDetails()

        editButton.setOnClickListener {
            showEditBurialBottomSheet()
        }

        deleteButton.setOnClickListener {
            deleteBurial()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()  // Переход назад
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadBurialDetails() {
        lifecycleScope.launch {
            try {
                val burial = apiService.getBurialById(burialId)
                val isMine = intent.getBooleanExtra("isMine", false)

                // Проверяем, совпадает ли Guest_Id захоронения с текущим пользователем
                if (isMine) {
                    // Показываем кнопки для редактирования и удаления
                    editButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                } else {
                    // Скрываем кнопки, если Guest_Id не совпадает
                    editButton.visibility = View.GONE
                    deleteButton.visibility = View.GONE
                }

                // Установка текста деталей
                burialDetailsTextView.text = "Название: ${burial.fio}\n" +
                        "Дата рождения: ${burial.birthDate}" +
                        "\nДата смерти: ${burial.deathDate}" +
                        "\nОписание: ${burial.biography}"

                // Установка изображения
                if (!burial.photo.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(burial.photo, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    burialImageView.setImageBitmap(bitmap)
                } else {
                    burialImageView.setImageResource(R.drawable.amogus)
                }
            } catch (e: Exception) {
                Log.e("BurialDetailsActivity", "Ошибка загрузки: ${e.message}", e)
                Toast.makeText(this@BurialDetailsActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showEditBurialBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_burial, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        var guestId = 0L
        var balance = 0L
        // Получение ссылок на элементы внутри BottomSheet
        val fioEditText = bottomSheetView.findViewById<EditText>(R.id.edit_fio)
        val birthDateEditText = bottomSheetView.findViewById<EditText>(R.id.edit_birth_date)
        val deathDateEditText = bottomSheetView.findViewById<EditText>(R.id.edit_death_date)
        val biographyEditText = bottomSheetView.findViewById<EditText>(R.id.edit_biography)
        val saveButton = bottomSheetView.findViewById<Button>(R.id.button_save)
        // Установка текущих данных захоронения
        lifecycleScope.launch {
            try {
                val burial = apiService.getBurialById(burialId)
                guestId = intent.getLongExtra("guestId", -1)
                balance = apiService.getGuest(guestId = guestId).balance
                fioEditText.setText(burial.fio)
                birthDateEditText.setText(burial.birthDate)
                deathDateEditText.setText(burial.deathDate)
                biographyEditText.setText(burial.biography ?: "")
            } catch (e: Exception) {
                Log.e("BurialDetailsActivity", "Ошибка загрузки данных: ${e.message}", e)
                Toast.makeText(this@BurialDetailsActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        }

        // Логика сохранения изменений
        saveButton.setOnClickListener {
            val updatedFio = fioEditText.text.toString()
            val updatedBirthDate = birthDateEditText.text.toString()
            val updatedDeathDate = deathDateEditText.text.toString()
            val updatedBiography = biographyEditText.text.toString()

            if (isDateValid(updatedBirthDate, updatedDeathDate)) {
                lifecycleScope.launch {
                    try {
                        // Создание объекта с обновлёнными данными
                        val updatedBurial = Burial(
                            id = burialId,
                            guest = Guest(-1,-1), // сохраняем гостя без изменений
                            fio = updatedFio,
                            birthDate = updatedBirthDate,
                            deathDate = updatedDeathDate,
                            biography = updatedBiography,
                            photo = null
                        )

                        // Отправка данных на сервер
                        apiService.updatePartBurial(burialId, updatedBurial)
                        Toast.makeText(this@BurialDetailsActivity, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()

                        // Перезагрузка данных
                        loadBurialDetails()
                    } catch (e: Exception) {
                        Log.e("BurialDetailsActivity", "Ошибка сохранения: ${e.message}", e)
                        Toast.makeText(this@BurialDetailsActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Показать ошибку, если даты некорректны
                Toast.makeText(this@BurialDetailsActivity, "Дата смерти не может быть раньше даты рождения", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }
    private fun isDateValid(birthDateString: String, deathDateString: String): Boolean {
        try {
            val birthDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(birthDateString)
            val deathDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(deathDateString)

            // Если хотя бы одна из дат некорректна, вернуть false
            if (birthDate == null || deathDate == null) {
                return false
            }

            // Проверка, что дата смерти не раньше даты рождения
            if (deathDate.before(birthDate)) {
                return false
            }

            return true
        } catch (e: ParseException) {
            // Если произошла ошибка при парсинге даты, возвращаем false
            return false
        }
    }
    private fun deleteBurial() {
        lifecycleScope.launch {
            try {
                apiService.deleteBurialById(burialId)
                Toast.makeText(this@BurialDetailsActivity, "Захоронение удалено", Toast.LENGTH_SHORT).show()

                // Устанавливаем результат и закрываем активность
                val resultIntent = Intent()
                resultIntent.putExtra("isDeleted", true)  // Указываем, что захоронение было удалено
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                Log.e("BurialDetailsActivity", "Ошибка удаления захоронения: ${e.message}", e)
                Toast.makeText(this@BurialDetailsActivity, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
