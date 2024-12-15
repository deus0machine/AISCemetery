package ru.sevostyanov.aiscemetery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
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
import java.time.LocalDate

class BurialDetailsActivity : AppCompatActivity() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var burialDetailsTextView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private var burialId: Long = -1
    private lateinit var burialImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                if (burial.photo != null && burial.photo.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(burial.photo, 0, burial.photo.size)
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


            lifecycleScope.launch {
                try {
                    // Создание объекта с обновлёнными данными
                    val updatedBurial = Burial(
                        id = burialId,
                        guest = Guest(guestId, balance), // сохраняем гостя без изменений
                        fio = updatedFio,
                        birthDate = updatedBirthDate,
                        deathDate = updatedDeathDate,
                        biography = if (updatedBiography.isBlank()) null else updatedBiography
                    )

                    // Отправка данных на сервер
                    apiService.updateBurial(burialId, updatedBurial)
                    Toast.makeText(this@BurialDetailsActivity, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()

                    // Перезагрузка данных
                    loadBurialDetails()
                } catch (e: Exception) {
                    Log.e("BurialDetailsActivity", "Ошибка сохранения: ${e.message}", e)
                    Toast.makeText(this@BurialDetailsActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            }
        }

        bottomSheetDialog.show()
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
