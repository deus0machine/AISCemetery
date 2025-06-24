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
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.UnifiedNotificationAdapter
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationType
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.models.NotificationStatus

class NotificationPageFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: ru.sevostyanov.aiscemetery.adapters.UnifiedNotificationAdapter
    
    private var isIncoming: Boolean = true
    
    // Храним списки уведомлений
    private var regularNotifications: List<Notification> = emptyList()
    private var draftSubmissions: List<ru.sevostyanov.aiscemetery.models.DraftSubmission> = emptyList()

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
        
        // Очищаем предыдущие ошибки
        viewModel.clearError()
        
        // Load notifications
        if (isIncoming) {
            viewModel.loadIncomingNotifications()
            viewModel.loadIncomingDraftSubmissions()
        } else {
            viewModel.loadSentNotifications()
            viewModel.loadOutgoingDraftSubmissions()
        }
    }

    override fun onResume() {
        super.onResume()
        // Очищаем предыдущие ошибки
        viewModel.clearError()
        
        // Обновляем уведомления при возвращении к фрагменту
        // чтобы правильно отображался статус после подтверждения/отклонения изменений
        if (isIncoming) {
            viewModel.loadIncomingNotifications()
            viewModel.loadIncomingDraftSubmissions()
        } else {
            viewModel.loadSentNotifications()
            viewModel.loadOutgoingDraftSubmissions()
        }
    }

    private fun setupRecyclerView() {
        // Создаем объединенный адаптер для обычных уведомлений и уведомлений о черновиках
        adapter = ru.sevostyanov.aiscemetery.adapters.UnifiedNotificationAdapter(
            isIncoming = isIncoming,
            onRegularNotificationClick = { notification ->
                handleItemClick(notification)
            },
            onRegularAcceptClick = { notification ->
                handleAcceptClick(notification)
            },
            onRegularRejectClick = { notification ->
                handleRejectClick(notification)
            },
            onRegularDeleteClick = { notification ->
                handleDeleteClick(notification, isIncoming)
            },
            onDraftNotificationClick = { draftSubmission ->
                handleDraftItemClick(draftSubmission)
            },
            onDraftApproveClick = { draftSubmission ->
                handleDraftApproveClick(draftSubmission)
            },
            onDraftRejectClick = { draftSubmission ->
                handleDraftRejectClick(draftSubmission)
            },
            onDraftDeleteClick = { draftSubmission ->
                handleDraftDeleteClick(draftSubmission)
            }
        )
        
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
            NotificationType.FAMILY_TREE_ACCESS_REQUEST -> showFamilyTreeAccessRequestDetails(notification)
            NotificationType.FAMILY_TREE_APPROVED -> showFamilyTreeApprovedDetails(notification)
            NotificationType.FAMILY_TREE_REJECTED -> showFamilyTreeRejectedDetails(notification)
            NotificationType.FAMILY_TREE_ACCESS_GRANTED -> showFamilyTreeAccessGrantedDetails(notification)
            NotificationType.FAMILY_TREE_ACCESS_REVOKED -> showFamilyTreeAccessRevokedDetails(notification)
            NotificationType.MEMORIAL_EDITOR_REMOVED -> showMemorialEditorRemovedDetails(notification)
            NotificationType.MEMORIAL_EDITOR_RESIGNED -> showMemorialEditorResignedDetails(notification)
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
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.ACCEPTED)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.FAMILY_TREE_ACCESS_REQUEST -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Подтверждение доступа к дереву")
                    .setMessage("Вы действительно хотите предоставить доступ к генеалогическому дереву \"${notification.relatedEntityName ?: ""}\"?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToFamilyTreeAccessRequest(notification.id, true)
                        Toast.makeText(context, "Доступ к дереву предоставлен", Toast.LENGTH_SHORT).show()
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.ACCEPTED)
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
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.ACCEPTED)
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
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.ACCEPTED)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            else -> {
                viewModel.respondToNotification(notification.id, true)
                Toast.makeText(context, "Запрос принят", Toast.LENGTH_SHORT).show()
                // Мгновенно обновляем статус в адаптере
                updateNotificationStatus(notification.id, NotificationStatus.ACCEPTED)
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
                        Toast.makeText(context, "Запрос на совместное владение отклонён", Toast.LENGTH_SHORT).show()
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            NotificationType.FAMILY_TREE_ACCESS_REQUEST -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Отклонение доступа к дереву")
                    .setMessage("Вы действительно хотите отклонить запрос на доступ к генеалогическому дереву \"${notification.relatedEntityName ?: ""}\"?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.respondToFamilyTreeAccessRequest(notification.id, false)
                        Toast.makeText(context, "Запрос на доступ к дереву отклонён", Toast.LENGTH_SHORT).show()
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
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
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
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
                        // Мгновенно обновляем статус в адаптере
                        updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            else -> {
                viewModel.respondToNotification(notification.id, false)
                Toast.makeText(context, "Запрос отклонен", Toast.LENGTH_SHORT).show()
                // Мгновенно обновляем статус в адаптере
                updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
            }
        }
    }
    
    private fun handleDeleteClick(notification: Notification, isIncoming: Boolean) {
        android.util.Log.d("NotificationPageFragment", "handleDeleteClick вызван для уведомления: ${notification.id}, isIncoming: $isIncoming")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление уведомления")
            .setMessage("Вы действительно хотите удалить это уведомление?")
            .setPositiveButton("Да") { _, _ ->
                android.util.Log.d("NotificationPageFragment", "Пользователь подтвердил удаление уведомления: ${notification.id}")
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
                NotificationStatus.PROCESSED -> append("Обработано")
                null -> append("Неизвестно")
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
    
    private fun showFamilyTreeAccessRequestDetails(notification: Notification) {
        val title = "Запрос на доступ к дереву"
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
    
    private fun showFamilyTreeApprovedDetails(notification: Notification) {
        val title = "Генеалогическое дерево одобрено"
        val message = buildString {
            append(notification.message)
            append("\n\nДерево: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Перейти к дереву") { _, _ ->
                // Переходим к дереву и обновляем его данные
                notification.relatedEntityId?.let { treeId ->
                    navigateToFamilyTree(treeId)
                }
                // Уведомляем о необходимости обновления списка деревьев
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .setNegativeButton("Закрыть") { _, _ ->
                // Уведомляем о необходимости обновления списка деревьев даже при закрытии
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .show()
    }
    
    private fun showFamilyTreeRejectedDetails(notification: Notification) {
        val title = "Генеалогическое дерево отклонено"
        val message = buildString {
            append(notification.message)
            append("\n\nДерево: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Перейти к дереву") { _, _ ->
                // Переходим к дереву и обновляем его данные
                notification.relatedEntityId?.let { treeId ->
                    navigateToFamilyTree(treeId)
                }
                // Уведомляем о необходимости обновления списка деревьев
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .setNegativeButton("Закрыть") { _, _ ->
                // Уведомляем о необходимости обновления списка деревьев даже при закрытии
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .show()
    }
    
    private fun showFamilyTreeAccessGrantedDetails(notification: Notification) {
        val title = "Доступ к дереву предоставлен"
        val message = buildString {
            append(notification.message)
            append("\n\nДерево: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Перейти к дереву") { _, _ ->
                // Переходим к дереву
                notification.relatedEntityId?.let { treeId ->
                    navigateToFamilyTree(treeId)
                }
                // Уведомляем о необходимости обновления списка деревьев
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .setNegativeButton("Закрыть") { _, _ ->
                // Уведомляем о необходимости обновления списка деревьев даже при закрытии
                setFragmentResult("family_tree_status_updated", Bundle())
            }
            .show()
    }
    
    private fun showFamilyTreeAccessRevokedDetails(notification: Notification) {
        val title = "Доступ к дереву отозван"
        val message = buildString {
            append(notification.message)
            append("\n\nДерево: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showMemorialEditorRemovedDetails(notification: Notification) {
        val title = "Исключение из редакторов"
        val message = buildString {
            append(notification.message)
            append("\n\nМемориал: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nВладелец: ${notification.senderName ?: "Неизвестно"}")
            append("\nДата: ${notification.createdAt}")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showMemorialEditorResignedDetails(notification: Notification) {
        val title = "Отказ от редактирования"
        val message = buildString {
            append(notification.message)
            append("\n\nМемориал: ${notification.relatedEntityName ?: "Неизвестно"}")
            append("\nРедактор: ${notification.senderName ?: "Неизвестно"}")
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
            NotificationType.FAMILY_TREE_ACCESS_REQUEST -> "Запрос на доступ к дереву"
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
                null -> append("Неизвестно")
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
                regularNotifications = notifications
                updateCombinedNotificationsList()
            }
            
            // Добавляем наблюдение за уведомлениями о черновиках
            viewModel.incomingDraftSubmissions.observe(viewLifecycleOwner) { draftSubmissions ->
                this.draftSubmissions = draftSubmissions
                updateCombinedNotificationsList()
            }
        } else {
            viewModel.sentNotifications.observe(viewLifecycleOwner) { notifications ->
                regularNotifications = notifications
                updateCombinedNotificationsList()
            }
            
            // Добавляем наблюдение за уведомлениями о черновиках
            viewModel.outgoingDraftSubmissions.observe(viewLifecycleOwner) { draftSubmissions ->
                this.draftSubmissions = draftSubmissions
                updateCombinedNotificationsList()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("NotificationPageFragment", "Error: $error")
                // Показываем ошибку пользователю
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateCombinedNotificationsList() {
        val combinedList = mutableListOf<ru.sevostyanov.aiscemetery.models.NotificationItem>()
        
        Log.d("NotificationPageFragment", "=== Обновление списка уведомлений ===")
        Log.d("NotificationPageFragment", "Обычных уведомлений: ${regularNotifications.size}")
        Log.d("NotificationPageFragment", "Уведомлений о черновиках: ${draftSubmissions.size}")
        
        // Добавляем обычные уведомления
        regularNotifications.forEachIndexed { index, notification ->
            Log.d("NotificationPageFragment", "Обычное уведомление #$index: id=${notification.id}, status=${notification.status}, title=${notification.title}, createdAt=${notification.createdAt}")
            combinedList.add(ru.sevostyanov.aiscemetery.models.RegularNotificationItem(notification))
        }
        
        // Добавляем уведомления о черновиках
        draftSubmissions.forEachIndexed { index, draftSubmission ->
            Log.d("NotificationPageFragment", "Уведомление о черновике #$index: id=${draftSubmission.id}, status=${draftSubmission.reviewStatus}, submittedAt=${draftSubmission.submittedAt}")
            combinedList.add(ru.sevostyanov.aiscemetery.models.DraftNotificationItem(draftSubmission, isIncoming))
        }
        
        // Логируем даты перед сортировкой
        Log.d("NotificationPageFragment", "=== Даты перед сортировкой ===")
        combinedList.forEachIndexed { index, item ->
            val itemType = when (item) {
                is ru.sevostyanov.aiscemetery.models.RegularNotificationItem -> "Regular"
                is ru.sevostyanov.aiscemetery.models.DraftNotificationItem -> "Draft"
                else -> "Unknown"
            }
            Log.d("NotificationPageFragment", "Item #$index ($itemType): id=${item.id}, createdAt='${item.createdAt}', title='${item.getDisplayTitle()}'")
        }
        
        // Сортируем по дате (новые сверху)
        combinedList.sortWith(compareByDescending<ru.sevostyanov.aiscemetery.models.NotificationItem> { item ->
            try {
                // Проверяем, что дата не null
                val dateStr = item.createdAt
                if (dateStr.isNullOrBlank() || dateStr == "null") {
                    Log.w("NotificationPageFragment", "Дата null для item ${item.id}, используем минимальную дату")
                    return@compareByDescending java.time.LocalDateTime.MIN
                }
                
                // Пытаемся преобразовать строку даты в LocalDateTime для корректной сортировки
                val cleanDateStr = dateStr.replace("Z", "").replace("+00:00", "")
                java.time.LocalDateTime.parse(cleanDateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                Log.w("NotificationPageFragment", "Не удалось распарсить дату: '${item.createdAt}' для item ${item.id}, ошибка: ${e.message}")
                // Если не получилось распарсить дату, используем минимальную дату
                java.time.LocalDateTime.MIN
            }
        }.thenByDescending { item ->
            // Дополнительная сортировка по ID (новые записи обычно имеют больший ID)
            item.id
        })
        
        // Логируем даты после сортировки
        Log.d("NotificationPageFragment", "=== Даты после сортировки ===")
        combinedList.forEachIndexed { index, item ->
            val itemType = when (item) {
                is ru.sevostyanov.aiscemetery.models.RegularNotificationItem -> "Regular"
                is ru.sevostyanov.aiscemetery.models.DraftNotificationItem -> "Draft"
                else -> "Unknown"
            }
            Log.d("NotificationPageFragment", "Item #$index ($itemType): id=${item.id}, createdAt='${item.createdAt}', title='${item.getDisplayTitle()}'")
        }
        
        Log.d("NotificationPageFragment", "Объединенный список: ${combinedList.size} уведомлений (${regularNotifications.size} обычных + ${draftSubmissions.size} черновиков)")
        
        if (combinedList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter.submitList(combinedList)
        }
    }
    
    private fun showEmptyState() {
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = if (isIncoming) {
            "У вас нет входящих уведомлений"
        } else {
            "У вас нет исходящих уведомлений"
        }
    }
    
    private fun hideEmptyState() {
        emptyTextView.visibility = View.GONE
    }

    // Обработчики для уведомлений о черновиках
    private fun handleDraftItemClick(draftSubmission: ru.sevostyanov.aiscemetery.models.DraftSubmission) {
        // Открываем экран сравнения изменений
        Log.d("NotificationPageFragment", "Клик по уведомлению о черновике: ${draftSubmission.id}")
        ru.sevostyanov.aiscemetery.activities.DraftComparisonActivity.start(requireContext(), draftSubmission.id)
    }
    
    private fun handleDraftApproveClick(draftSubmission: ru.sevostyanov.aiscemetery.models.DraftSubmission) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Одобрение изменений")
            .setMessage("Вы действительно хотите одобрить изменения в дереве \"${draftSubmission.draft.familyTree?.name ?: "Неизвестно"}\"?")
            .setPositiveButton("Одобрить") { _, _ ->
                viewModel.respondToDraftSubmission(draftSubmission.id, true, null)
                android.widget.Toast.makeText(context, "Изменения одобрены", android.widget.Toast.LENGTH_SHORT).show()
                
                // Мгновенно обновляем статус в адаптере
                updateDraftSubmissionStatus(draftSubmission.id, ru.sevostyanov.aiscemetery.models.DraftSubmission.ReviewStatus.APPROVED)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun handleDraftRejectClick(draftSubmission: ru.sevostyanov.aiscemetery.models.DraftSubmission) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Отклонение изменений")
            .setMessage("Вы действительно хотите отклонить изменения в дереве \"${draftSubmission.draft.familyTree?.name ?: "Неизвестно"}\"?")
            .setPositiveButton("Отклонить") { _, _ ->
                viewModel.respondToDraftSubmission(draftSubmission.id, false, "Изменения отклонены")
                android.widget.Toast.makeText(context, "Изменения отклонены", android.widget.Toast.LENGTH_SHORT).show()
                
                // Мгновенно обновляем статус в адаптере
                updateDraftSubmissionStatus(draftSubmission.id, ru.sevostyanov.aiscemetery.models.DraftSubmission.ReviewStatus.REJECTED)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun handleDraftDeleteClick(draftSubmission: ru.sevostyanov.aiscemetery.models.DraftSubmission) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление уведомления")
            .setMessage("Вы действительно хотите удалить это уведомление о черновике дерева \"${draftSubmission.draft.familyTree?.name ?: "Неизвестно"}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                // Удаляем уведомление из списка мгновенно
                removeDraftSubmissionFromList(draftSubmission.id)
                android.widget.Toast.makeText(context, "Уведомление удалено", android.widget.Toast.LENGTH_SHORT).show()
                
                // Удаляем на сервере
                viewModel.deleteDraftSubmission(draftSubmission.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun navigateToFamilyTree(treeId: Long) {
        try {
            // Переходим к фрагменту просмотра генеалогического дерева
            val bundle = Bundle().apply {
                putLong("familyTreeId", treeId)  // Изменено с "treeId" на "familyTreeId"
                putBoolean("isDraft", false)
                putBoolean("refreshData", true) // Флаг для принудительного обновления данных
            }
            
            // Используем NavController для навигации
            val navController = findNavController()
            navController.navigate(R.id.action_notificationsFragment_to_genealogyTreeFragment, bundle)
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationPageFragment", "Error navigating to family tree: ${e.message}", e)
            
            // Если навигация не удалась, показываем Toast с информацией
            android.widget.Toast.makeText(
                context, 
                "Перейдите в раздел 'Деревья' для просмотра обновленного статуса дерева", 
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Мгновенно обновляет статус уведомления о черновике в адаптере
     */
    private fun updateDraftSubmissionStatus(submissionId: Long, newStatus: ru.sevostyanov.aiscemetery.models.DraftSubmission.ReviewStatus) {
        try {
            Log.d("NotificationPageFragment", "=== ОБНОВЛЕНИЕ СТАТУСА ЧЕРНОВИКА ===")
            Log.d("NotificationPageFragment", "Обновляем статус черновика ID=$submissionId на $newStatus")
            
            // Получаем текущий список уведомлений
            val currentNotifications = adapter.currentList.toMutableList()
            Log.d("NotificationPageFragment", "Текущий список содержит ${currentNotifications.size} уведомлений")
            
            var updated = false
            
            // Ищем и обновляем соответствующее уведомление
            for (i in currentNotifications.indices) {
                val item = currentNotifications[i]
                Log.d("NotificationPageFragment", "Проверяем элемент #$i: ${item::class.simpleName}, ID=${item.id}")
                
                if (item is ru.sevostyanov.aiscemetery.models.DraftNotificationItem && 
                    item.draftSubmission.id == submissionId) {
                    
                    Log.d("NotificationPageFragment", "Найден черновик для обновления!")
                    Log.d("NotificationPageFragment", "Старый статус: reviewStatus=${item.draftSubmission.reviewStatus}, isReviewed=${item.draftSubmission.isReviewed}")
                    
                    // Создаем обновленную копию DraftSubmission
                    val updatedSubmission = item.draftSubmission.copy(
                        reviewStatus = newStatus,
                        isReviewed = true,
                        reviewedAt = java.time.LocalDateTime.now().toString()
                    )
                    
                    Log.d("NotificationPageFragment", "Новый статус: reviewStatus=${updatedSubmission.reviewStatus}, isReviewed=${updatedSubmission.isReviewed}")
                    
                    // Создаем обновленный NotificationItem
                    val updatedItem = ru.sevostyanov.aiscemetery.models.DraftNotificationItem(updatedSubmission, isIncoming)
                    currentNotifications[i] = updatedItem
                    updated = true
                    break
                }
            }
            
            // Если нашли и обновили, применяем изменения к адаптеру
            if (updated) {
                Log.d("NotificationPageFragment", "Применяем обновленный список к адаптеру")
                adapter.submitList(currentNotifications)
                Log.d("NotificationPageFragment", "Мгновенно обновлен статус уведомления $submissionId на $newStatus")
            } else {
                Log.w("NotificationPageFragment", "Не найдено уведомление с ID $submissionId для обновления статуса")
                // Логируем все элементы для отладки
                currentNotifications.forEachIndexed { index, item ->
                    Log.w("NotificationPageFragment", "Элемент #$index: ${item::class.simpleName}, ID=${item.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationPageFragment", "Ошибка при мгновенном обновлении статуса уведомления: ${e.message}", e)
        }
    }
    
    /**
     * Мгновенно обновляет статус обычного уведомления в адаптере
     */
    private fun updateNotificationStatus(notificationId: Long, newStatus: NotificationStatus) {
        try {
            // Получаем текущий список уведомлений
            val currentNotifications = adapter.currentList.toMutableList()
            var updated = false
            
            // Ищем и обновляем соответствующее уведомление
            for (i in currentNotifications.indices) {
                val item = currentNotifications[i]
                if (item is ru.sevostyanov.aiscemetery.models.RegularNotificationItem && 
                    item.notification.id == notificationId) {
                    
                    // Создаем обновленную копию Notification
                    val updatedNotification = item.notification.copy(
                        status = newStatus
                    )
                    
                    // Создаем обновленный NotificationItem
                    val updatedItem = ru.sevostyanov.aiscemetery.models.RegularNotificationItem(updatedNotification)
                    currentNotifications[i] = updatedItem
                    updated = true
                    break
                }
            }
            
            // Если нашли и обновили, применяем изменения к адаптеру
            if (updated) {
                adapter.submitList(currentNotifications)
                Log.d("NotificationPageFragment", "Мгновенно обновлен статус уведомления $notificationId на $newStatus")
            } else {
                Log.w("NotificationPageFragment", "Не найдено уведомление с ID $notificationId для обновления статуса")
            }
        } catch (e: Exception) {
            Log.e("NotificationPageFragment", "Ошибка при мгновенном обновлении статуса уведомления: ${e.message}", e)
        }
    }
    
    /**
     * Удаляет уведомление о черновике из списка
     */
    private fun removeDraftSubmissionFromList(submissionId: Long) {
        try {
            Log.d("NotificationPageFragment", "=== УДАЛЕНИЕ УВЕДОМЛЕНИЯ О ЧЕРНОВИКЕ ===")
            Log.d("NotificationPageFragment", "Удаляем уведомление о черновике ID=$submissionId")
            
            // Получаем текущий список уведомлений
            val currentNotifications = adapter.currentList.toMutableList()
            Log.d("NotificationPageFragment", "Текущий список содержит ${currentNotifications.size} уведомлений")
            
            var removed = false
            
            // Ищем и удаляем соответствующее уведомление
            val iterator = currentNotifications.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item is ru.sevostyanov.aiscemetery.models.DraftNotificationItem && 
                    item.draftSubmission.id == submissionId) {
                    
                    Log.d("NotificationPageFragment", "Найдено уведомление о черновике для удаления!")
                    iterator.remove()
                    removed = true
                    break
                }
            }
            
            // Если нашли и удалили, применяем изменения к адаптеру
            if (removed) {
                Log.d("NotificationPageFragment", "Применяем обновленный список к адаптеру (${currentNotifications.size} элементов)")
                adapter.submitList(currentNotifications)
                Log.d("NotificationPageFragment", "Уведомление о черновике $submissionId удалено из списка")
                
                // Проверяем, нужно ли показать пустое состояние
                if (currentNotifications.isEmpty()) {
                    showEmptyState()
                }
            } else {
                Log.w("NotificationPageFragment", "Не найдено уведомление о черновике с ID $submissionId для удаления")
            }
        } catch (e: Exception) {
            Log.e("NotificationPageFragment", "Ошибка при удалении уведомления о черновике: ${e.message}", e)
        }
    }
} 