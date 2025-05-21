package ru.sevostyanov.aiscemetery.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import ru.sevostyanov.aiscemetery.models.Memorial
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
    
    private lateinit var notificationsViewModel: NotificationsViewModel

    private var memorial: Memorial? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_memorial)

        // Настройка toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Инициализация ViewModel
        notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

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

        // Получаем мемориал из intent
        memorial = intent.getParcelableExtra(EXTRA_MEMORIAL)
        memorial?.let {
            loadMemorialData(it)
        } ?: run {
            Toast.makeText(this, "Ошибка: Мемориал не найден", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Настройка кнопок
        setupRequestAccessButton()
        setupEditButton()
        setupOwnershipRequestButton()
    }

    private fun loadMemorialData(memorial: Memorial) {
        // Загружаем фото
        GlideHelper.loadImage(
            this,
            memorial.photoUrl ?: "",
            photoImageView,
            R.drawable.placeholder_photo,
            R.drawable.placeholder_photo
        )

        // Заполняем основные данные
        nameTextView.text = memorial.fio
        
        // Форматируем даты
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        
        val birthDate = memorial.birthDate?.let {
            try {
                val date = dateFormat.parse(it)
                date?.let { d -> displayFormat.format(d) } ?: "?"
            } catch (e: Exception) {
                it.substring(0, 4)
            }
        } ?: "?"
        
        val deathDate = memorial.deathDate?.let {
            try {
                val date = dateFormat.parse(it)
                date?.let { d -> displayFormat.format(d) } ?: "?"
            } catch (e: Exception) {
                it.substring(0, 4)
            }
        } ?: "?"
        
        datesTextView.text = "$birthDate - $deathDate"
        
        // Биография
        biographyTextView.text = memorial.biography ?: "Информация отсутствует"
        
        // Местоположения
        mainLocationTextView.text = "Основное: ${memorial.mainLocation?.address ?: "Не указано"}"
        burialLocationTextView.text = "Захоронение: ${memorial.burialLocation?.address ?: "Не указано"}"
        
        // Информация о дереве и создателе
        val hasTree = memorial.treeId != null
        treeInfoTextView.text = "Генеалогическое дерево: ${if (hasTree) "Да (ID: ${memorial.treeId})" else "Нет"}"
        createdByTextView.text = "Создатель: ${memorial.createdBy?.fio ?: "Неизвестно"}"
        
        // Получаем текущего пользователя
        val currentUser = UserManager.getCurrentUser()
        
        // Проверяем, является ли текущий пользователь создателем этого мемориала
        val isCreator = memorial.createdBy?.id == currentUser?.id
        
        // Настраиваем видимость кнопок
        requestAccessButton.visibility = if (hasTree && !isCreator) View.VISIBLE else View.GONE
        editButton.visibility = if (isCreator) View.VISIBLE else View.GONE
        ownershipRequestButton.visibility = if (!isCreator) View.VISIBLE else View.GONE
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
    
    private fun showRequestAccessDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_ownership_request)
            .create()
        
        dialog.show()
        
        // Настраиваем диалог
        dialog.findViewById<TextView>(R.id.recipientTextView)?.text = 
            memorial?.createdBy?.fio ?: "Владелец мемориала"
        
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
            memorial?.createdBy?.fio ?: "Владелец мемориала"
        
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
            val receiverId = memorial?.createdBy?.id
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
                        val intent = Intent(this@ViewMemorialActivity, MainActivity::class.java).apply {
                            putExtra("navigate_to", "notifications")
                            putExtra("tab_position", 1) // Открываем вкладку исходящих
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка отправки запроса: ${e.message}", e)
                        Toast.makeText(
                            this@ViewMemorialActivity,
                            "Ошибка отправки запроса: ${e.message}",
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
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        const val EXTRA_MEMORIAL = "extra_memorial"
        
        fun start(activity: Activity, memorial: Memorial) {
            val intent = Intent(activity, ViewMemorialActivity::class.java).apply {
                putExtra(EXTRA_MEMORIAL, memorial)
            }
            activity.startActivity(intent)
        }
    }
} 