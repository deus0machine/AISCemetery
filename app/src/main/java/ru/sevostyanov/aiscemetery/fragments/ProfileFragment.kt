package ru.sevostyanov.aiscemetery.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.LoginActivity
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.user.UserManager
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var logoutButton: Button
    private lateinit var supportButton: Button
    private lateinit var topupButton: Button
    private lateinit var profileName: TextView
    private lateinit var profileContacts: TextView
    private lateinit var profileRegDate: TextView
    private lateinit var profileRole: TextView
    private lateinit var profileSubs: TextView
    
    private val notificationsViewModel: NotificationsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        // Инициализация UI элементов
        initializeViews(view)
        
        // Настройка кнопок
        setupButtons()
        
        // Загрузка данных пользователя
        loadUserData()
        
        return view
    }

    private fun initializeViews(view: View) {
        logoutButton = view.findViewById(R.id.btn_logout)
        supportButton = view.findViewById(R.id.btn_support)
        topupButton = view.findViewById(R.id.btn_topup)
        
        profileName = view.findViewById(R.id.profile_name)
        profileContacts = view.findViewById(R.id.profile_contacts)
        profileRegDate = view.findViewById(R.id.profile_reg_date)
        profileRole = view.findViewById(R.id.profile_role)
        profileSubs = view.findViewById(R.id.subscription)
    }

    private fun setupButtons() {
        logoutButton.setOnClickListener {
            try {
                UserManager.clearUserData(requireContext())
                startActivity(Intent(activity, LoginActivity::class.java))
                activity?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка при выходе: ${e.message}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(activity, LoginActivity::class.java))
                activity?.finish()
            }
        }

        supportButton.setOnClickListener {
            showSupportDialog()
        }

        topupButton.setOnClickListener {
            Toast.makeText(requireContext(), "TODO: Информация о подписке", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSupportDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_support, null)
        val messageEditText = dialogView.findViewById<EditText>(R.id.edit_message)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Поддержка")
            .setMessage("Опишите вашу проблему или вопрос. Сообщение будет отправлено администраторам.")
            .setView(dialogView)
            .setPositiveButton("Отправить") { _, _ ->
                val message = messageEditText.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendSupportMessage(message)
                } else {
                    Toast.makeText(requireContext(), "Введите сообщение", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun sendSupportMessage(message: String) {
        notificationsViewModel.createTechnicalSupport(message) {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Сообщение отправлено администраторам", Toast.LENGTH_LONG).show()
            }
        }
        
        // Наблюдаем за ошибками
        notificationsViewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Ошибка: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            // Предполагаем, что входная дата в формате "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            // Если не удалось распарсить дату, возвращаем исходную строку
            dateStr
        }
    }

    private fun loadUserData() {
        try {
            val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(requireContext())
            if (user != null) {
                profileName.text = user.fio
                profileContacts.text = user.contacts
                profileRegDate.text = formatDate(user.dateOfRegistration)
                profileRole.text = when (user.role) {
                    "ADMIN" -> "Администратор"
                    "USER" -> "Пользователь"
                    else -> user.role // на случай, если появятся другие роли
                }
                profileSubs.text = if (user.hasSubscription) "Есть подписка" else "Нет подписки"
            } else {
                // Если пользователь не найден, очищаем данные и переходим на экран входа
                UserManager.clearUserData(requireContext())
                startActivity(Intent(activity, LoginActivity::class.java))
                activity?.finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            // При ошибке очищаем данные и переходим на экран входа
            UserManager.clearUserData(requireContext())
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }
    }
}




