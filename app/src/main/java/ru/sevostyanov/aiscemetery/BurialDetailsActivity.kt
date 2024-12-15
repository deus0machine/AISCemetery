package ru.sevostyanov.aiscemetery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BurialDetailsActivity : AppCompatActivity() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var burialDetailsTextView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private var burialId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_burial_details)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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
            // Логика редактирования
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

                burialDetailsTextView.text = "Название: ${burial.fio}\nОписание: ${burial.biography}"
            } catch (e: Exception) {
                Log.e("BurialDetailsActivity", "Ошибка загрузки: ${e.message}", e)
                Toast.makeText(this@BurialDetailsActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
