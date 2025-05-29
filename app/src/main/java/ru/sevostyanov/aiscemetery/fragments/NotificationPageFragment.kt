package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.NotificationsAdapter
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationType
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.models.NotificationStatus

class NotificationPageFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: NotificationsAdapter
    
    private var isIncoming: Boolean = true

    companion object {
        private const val ARG_IS_INCOMING = "is_incoming"
        
        fun newInstance(isIncoming: Boolean): NotificationPageFragment {
            return NotificationPageFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_INCOMING, isIncoming)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isIncoming = it.getBoolean(ARG_IS_INCOMING, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        emptyTextView = view.findViewById(R.id.text_empty)
        
        setupRecyclerView()
        setupObservers()
        
        // Load notifications
        if (isIncoming) {
            viewModel.loadIncomingNotifications()
        } else {
            viewModel.loadSentNotifications()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем уведомления при возвращении к фрагменту
        // чтобы правильно отображался статус после подтверждения/отклонения изменений
        if (isIncoming) {
            viewModel.loadIncomingNotifications()
        } else {
            viewModel.loadSentNotifications()
        }
    }

    private fun setupRecyclerView() {
        // Добавляем обработчики принятия/отклонения только для входящих
        adapter = if (isIncoming) {
            NotificationsAdapter(
                isIncoming = true,
                onAcceptClick = { notification ->
                    handleAcceptClick(notification)
                },
                onRejectClick = { notification ->
                    handleRejectClick(notification)
                },
                onItemClick = { notification ->
                    handleItemClick(notification)
                },
                onDeleteClick = { notification ->
                    handleDeleteClick(notification, true)
                }
            )
        } else {
            NotificationsAdapter(
                isIncoming = false,
                onItemClick = { notification ->
                    // Для исходящих просто логируем клик
                    handleOutgoingItemClick(notification)
                },
                onDeleteClick = { notification ->
                    handleDeleteClick(notification, false)
                }
            )
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationPageFragment.adapter
        }
    }

    private fun handleItemClick(notification: Notification) {
        // Если уведомление еще не прочитано, отмечаем его как прочитанное
        if (!notification.isRead) {
            viewModel.markAsRead(notification.id)
        }
        
        // Для уведомлений об изменениях мемориала - прямой переход к активности подтверждения изменений
        if (notification.type == NotificationType.MEMORIAL_EDIT && notification.relatedEntityId != null) {
            // Запускаем активность для просмотра ожидающих изменений с ID мемориала
            val memorialId = notification.relatedEntityId
            context?.let { ctx ->
                ru.sevostyanov.aiscemetery.activities.PendingChangesActivity.startWithMemorialId(ctx, memorialId)
            }
            return
        }
        
        // Для остальных типов уведомлений - показываем подробную информацию в диалоге
        when (notification.type) {
            NotificationType.MEMORIAL_OWNERSHIP -> showMemorialOwnershipDetails(notification)
            NotificationType.MEMORIAL_CHANGES -> showMemorialChangesDetails(notification)
            NotificationType.MEMORIAL_EDIT -> showMemorialEditDetails(notification)
            else -> showGenericNotificationDetails(notification)
        }
    }
    
    private fun handleOutgoingItemClick(notification: Notification) {
        // Показываем информацию о статусе запроса
        showOutgoingNotificationStatus(notification)
    }

    private fun handleAcceptClick(notification: Notification) {
        when (notification.type) {
            NotificationType.MEMORIAL_OWNERSHIP -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Подтверждение")
                    .setMessage("Вы действительно хотите разрешить совместное владение мемориалом ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, true)
                        Toast.makeText(context, "Запрос на совместное владение принят", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.MEMORIAL_CHANGES -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Подтверждение изменений")
                    .setMessage("Вы действительно хотите подтвердить изменения в мемориале ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, true)
                        Toast.makeText(context, "Изменения подтверждены", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.MEMORIAL_EDIT -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Подтверждение изменений")
                    .setMessage("Вы действительно хотите подтвердить изменения в мемориале ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, true)
                        Toast.makeText(context, "Изменения отправлены на модерацию", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            else -> {
                viewModel.respondToNotification(notification.id, true)
                Toast.makeText(context, "Запрос принят", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleRejectClick(notification: Notification) {
        when (notification.type) {
            NotificationType.MEMORIAL_OWNERSHIP -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Отклонение запроса")
                    .setMessage("Вы действительно хотите отклонить запрос на совместное владение мемориалом ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, false)
                        Toast.makeText(context, "Запрос на совместное владение отклонен", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.MEMORIAL_CHANGES -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Отклонение изменений")
                    .setMessage("Вы действительно хотите отклонить изменения в мемориале ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, false)
                        Toast.makeText(context, "Изменения отклонены", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.MEMORIAL_EDIT -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Отклонение изменений")
                    .setMessage("Вы действительно хотите отклонить изменения в мемориале ${notification.relatedEntityName ?: ""}?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToNotification(notification.id, false)
                        Toast.makeText(context, "Изменения отклонены", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            else -> {
                viewModel.respondToNotification(notification.id, false)
                Toast.makeText(context, "Запрос отклонен", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleDeleteClick(notification: Notification, isIncoming: Boolean) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление уведомления")
            .setMessage("Вы действительно хотите удалить это уведомление?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteNotification(notification.id, isIncoming)
                Toast.makeText(context, "Уведомление удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showMemorialOwnershipDetails(notification: Notification) {
        val title = "Запрос на совместное владение"
        val message = buildString {
            append(notification.message)
            append("\n\nМемориал: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nОтправитель: ${notification.senderName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showMemorialChangesDetails(notification: Notification) {
        val title = "Запрос на изменение мемориала"
        val message = buildString {
            append(notification.message)
            append("\n\nМемориал: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nОтправитель: ${notification.senderName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showMemorialEditDetails(notification: Notification) {
        val title = "Изменения в мемориале"
        val message = buildString {
            append(notification.message)
            append("\n\nМемориал: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nСтатус: ")
            when (notification.status) {
                NotificationStatus.PENDING -> append("Ожидает ответа")
                NotificationStatus.ACCEPTED -> append("Принято")
                NotificationStatus.REJECTED -> append("Отклонено")
                else -> append("Обработано")
            }
            if (notification.senderName != null) {
                append("\nОтправитель: ${notification.senderName}")
            }
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showGenericNotificationDetails(notification: Notification) {
        val title = "Уведомление"
        val message = buildString {
            append(notification.message)
            if (notification.relatedEntityName != null) {
                append("\n\nМемориал: ${notification.relatedEntityName}")
            }
            if (notification.senderName != null) {
                append("\nОтправитель: ${notification.senderName}")
            }
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showOutgoingNotificationStatus(notification: Notification) {
        val title = when(notification.type) {
            NotificationType.MEMORIAL_OWNERSHIP -> "Запрос на совместное владение"
            NotificationType.MEMORIAL_CHANGES -> "Запрос на изменение мемориала"
            NotificationType.MEMORIAL_EDIT -> "Изменения в мемориале"
            else -> "Уведомление"
        }
        
        val message = buildString {
            append(notification.message)
            append("\n\nСтатус: ")
            when (notification.status) {
                NotificationStatus.PENDING -> append("Ожидает ответа")
                NotificationStatus.ACCEPTED -> append("Принято")
                NotificationStatus.REJECTED -> append("Отклонено")
                NotificationStatus.PROCESSED -> append("Обработано")
            }
            if (notification.relatedEntityName != null) {
                append("\nМемориал: ${notification.relatedEntityName}")
            }
            append("\nПолучатель: ${notification.receiverName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun setupObservers() {
        if (isIncoming) {
            viewModel.incomingNotifications.observe(viewLifecycleOwner) { notifications ->
                Log.d("NotificationPage", "Получены входящие уведомления: ${notifications.size}")
                if (notifications.isNotEmpty()) {
                    Log.d("NotificationPage", "Первое уведомление: ID=${notifications[0].id}, тип=${notifications[0].type}")
                } else {
                    Log.d("NotificationPage", "Список входящих уведомлений пуст")
                }
                adapter.submitList(notifications)
                updateEmptyView(notifications.isEmpty())
            }
        } else {
            viewModel.sentNotifications.observe(viewLifecycleOwner) { notifications ->
                Log.d("NotificationPage", "Получены исходящие уведомления: ${notifications.size}")
                if (notifications.isNotEmpty()) {
                    Log.d("NotificationPage", "Первое уведомление: ID=${notifications[0].id}, тип=${notifications[0].type}")
                } else {
                    Log.d("NotificationPage", "Список исходящих уведомлений пуст")
                }
                adapter.submitList(notifications)
                updateEmptyView(notifications.isEmpty())
            }
        }
        
        // Наблюдаем за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("NotificationPage", "Состояние загрузки: $isLoading")
            view?.findViewById<View>(R.id.progress_bar)?.visibility = 
                if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Log.e("NotificationPage", "Ошибка: $errorMessage")
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        emptyTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        emptyTextView.text = if (isIncoming) {
            "У вас нет входящих уведомлений"
        } else {
            "У вас нет исходящих уведомлений"
        }
    }
} 