package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import android.widget.EditText
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.setFragmentResult
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.activities.DraftComparisonActivity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class FamilyTreeFragment : BottomSheetDialogFragment() {

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { arguments?.getLong("treeId") ?: -1L }
    private val isDraftTree: Boolean by lazy { arguments?.getBoolean("isDraft") ?: false }

    // Сохраняем ссылку на Activity для использования в навигации
    private var savedActivity: FragmentActivity? = null

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var publicationStatusTextView: TextView
    private lateinit var moderationCardView: CardView
    private lateinit var moderationMessageTextView: TextView
    private lateinit var sendForModerationButton: Button
    private lateinit var unpublishButton: Button
    private lateinit var viewGenealogyButton: Button
    private lateinit var editGenealogyButton: Button
    private lateinit var saveButton: Button
    private var wasSaved = false
    private var loadingDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("FamilyTreeFragment", "onAttach вызван, context: $context")
        Log.d("FamilyTreeFragment", "context is FragmentActivity: ${context is FragmentActivity}")
        Log.d("FamilyTreeFragment", "context is AppCompatActivity: ${context is AppCompatActivity}")
        
        savedActivity = when {
            context is FragmentActivity -> {
                Log.d("FamilyTreeFragment", "Сохраняем как FragmentActivity")
                context
            }
            else -> {
                Log.w("FamilyTreeFragment", "Context не является FragmentActivity: ${context::class.java}")
                null
            }
        }
        Log.d("FamilyTreeFragment", "onAttach: savedActivity = $savedActivity")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("FamilyTreeFragment", "onDetach: очищаем savedActivity")
        savedActivity = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FamilyTreeFragment", "onViewCreated: activity = $activity")
        Log.d("FamilyTreeFragment", "onViewCreated: savedActivity = $savedActivity")
        
        // Если savedActivity все еще null, пробуем получить через activity
        if (savedActivity == null && activity != null) {
            savedActivity = activity
            Log.d("FamilyTreeFragment", "Обновили savedActivity в onViewCreated: $savedActivity")
        }
        
        initializeViews(view)
        setupObservers()
        setupClickListeners()
        viewModel.loadFamilyTree(treeId)
    }

    private fun initializeViews(view: View) {
        nameEditText = view.findViewById(R.id.text_name)
        descriptionEditText = view.findViewById(R.id.text_description)
        publicationStatusTextView = view.findViewById(R.id.text_publication_status)
        moderationCardView = view.findViewById(R.id.moderation_card)
        moderationMessageTextView = view.findViewById(R.id.moderation_message)
        sendForModerationButton = view.findViewById(R.id.button_send_for_moderation)
        unpublishButton = view.findViewById(R.id.button_unpublish)
        viewGenealogyButton = view.findViewById(R.id.button_view_genealogy)
        editGenealogyButton = view.findViewById(R.id.button_edit_genealogy)
        saveButton = view.findViewById(R.id.button_save)
    }

    private fun setupObservers() {
        viewModel.familyTree.observe(viewLifecycleOwner) { tree ->
            tree?.let {
                // Проверяем, есть ли изменения на модерации
                if (it.pendingChanges) {
                    // Показываем данные на модерации и блокируем поля
                    nameEditText.setText(it.pendingName ?: it.name)
                    descriptionEditText.setText(it.pendingDescription ?: it.description ?: "")
                    nameEditText.isEnabled = false
                    descriptionEditText.isEnabled = false
                    saveButton.visibility = View.GONE
                    saveButton.isEnabled = false
                    
                    // Визуально показываем, что поля заблокированы
                    nameEditText.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    descriptionEditText.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    nameEditText.alpha = 0.6f
                    descriptionEditText.alpha = 0.6f
                    
                    // Обновляем статус для отображения информации о модерации
                    publicationStatusTextView.text = "Статус: ${it.getPublicationStatusText()}\nИзменения отправлены на модерацию, ожидайте решения администратора"
                    publicationStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                } else {
                    // Обычное отображение данных
                    nameEditText.setText(it.name)
                    descriptionEditText.setText(it.description ?: "")
                    
                    // Восстанавливаем доступность полей если нет модерации
                    nameEditText.isEnabled = true
                    descriptionEditText.isEnabled = true
                    saveButton.visibility = View.VISIBLE
                    saveButton.isEnabled = true
                    
                    // Восстанавливаем обычный вид полей
                    nameEditText.setBackgroundResource(android.R.drawable.edit_text)
                    descriptionEditText.setBackgroundResource(android.R.drawable.edit_text)
                    nameEditText.alpha = 1.0f
                    descriptionEditText.alpha = 1.0f
                }
                updatePublicationStatus(it)
                
                // Специальная логика для черновиков редакторов
                val isDraftEditor = isDraftTree && !it.isUserOwner
                
                if (isDraftEditor) {
                    // Для редакторов черновиков: только просмотр информации
                    nameEditText.isEnabled = false
                    descriptionEditText.isEnabled = false
                    saveButton.visibility = View.GONE
                    editGenealogyButton.isEnabled = true // Редактирование генеалогии доступно
                    viewGenealogyButton.isEnabled = true // Просмотр всегда доступен
                } else {
                    // Обычная логика для владельцев и обычных деревьев
                    val canEdit = viewModel.canEditTree() && !it.pendingChanges // Нельзя редактировать если изменения на модерации
                    nameEditText.isEnabled = canEdit
                    descriptionEditText.isEnabled = canEdit
                    saveButton.visibility = if (canEdit) View.VISIBLE else View.GONE
                    editGenealogyButton.isEnabled = canEdit
                    viewGenealogyButton.isEnabled = true // Просмотр всегда доступен
                }
                
                // Обновляем доступность кнопки отправки на модерацию
                sendForModerationButton.isEnabled = viewModel.canSendForModeration()
                
                // Обновляем видимость кнопки снятия с публикации
                unpublishButton.visibility = if (viewModel.canUnpublishTree()) View.VISIBLE else View.GONE
                
                // Проверяем, была ли успешно выполнена операция модерации
                checkForModerationSuccess(it)
            }
            // dismiss только если было сохранение
            if (wasSaved && tree != null && tree.id == treeId) {
                setFragmentResult("family_tree_updated", Bundle())
                dismiss()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Скрываем индикатор загрузки при ошибке
                hideLoadingDialog()
                
                // Показываем диалог ошибки вместо Toast для лучшего UX
                showErrorDialog("Ошибка", it)
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Скрываем индикатор загрузки при успехе
                hideLoadingDialog()
                
                // Проверяем, что фрагмент все еще активен
                if (isAdded && !isRemoving && !isDetached) {
                    // Показываем диалог успеха
                    showSuccessDialog("Успешно", it)
                    // Очищаем сообщение после показа диалога
                    viewModel.clearSuccessMessage()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Показываем/скрываем индикатор загрузки
            if (isLoading) {
                showLoadingDialog("Выполняется операция...")
            } else {
                hideLoadingDialog()
            }
            
            // Блокируем все кнопки во время загрузки, но учитываем модерацию и черновики
            val tree = viewModel.familyTree.value
            val isDraftEditor = isDraftTree && tree?.isUserOwner == false
            
            if (isDraftEditor) {
                // Для редакторов черновиков: только кнопки просмотра и редактирования генеалогии
                saveButton.visibility = View.GONE
                editGenealogyButton.isEnabled = !isLoading
                viewGenealogyButton.isEnabled = !isLoading
                sendForModerationButton.isEnabled = false
                unpublishButton.visibility = View.GONE
            } else {
                // Обычная логика для владельцев и обычных деревьев
                val canEdit = tree?.let { viewModel.canEditTree() && !it.pendingChanges } ?: false // Нельзя редактировать если изменения на модерации
                val canSendForModeration = tree?.let { viewModel.canSendForModeration() && !it.pendingChanges } ?: false // Нельзя отправлять на модерацию если уже есть изменения на модерации
                val canUnpublish = tree?.let { viewModel.canUnpublishTree() && !it.pendingChanges } ?: false // Нельзя снимать с публикации если есть изменения на модерации
                
                // Блокируем поля ввода если есть изменения на модерации
                nameEditText.isEnabled = !isLoading && canEdit && !(tree?.pendingChanges ?: false)
                descriptionEditText.isEnabled = !isLoading && canEdit && !(tree?.pendingChanges ?: false)
                
                saveButton.visibility = if (canEdit && !(tree?.pendingChanges ?: false)) View.VISIBLE else View.GONE
                saveButton.isEnabled = !isLoading && canEdit && !(tree?.pendingChanges ?: false)
                editGenealogyButton.isEnabled = !isLoading && canEdit
                sendForModerationButton.isEnabled = !isLoading && canSendForModeration
                unpublishButton.isEnabled = !isLoading && canUnpublish
                viewGenealogyButton.isEnabled = !isLoading // Просмотр всегда доступен
            }
        }
    }

    private fun updatePublicationStatus(tree: ru.sevostyanov.aiscemetery.models.FamilyTree) {
        publicationStatusTextView.text = "Статус: ${tree.getPublicationStatusText()}"
        
        when (tree.publicationStatus) {
            PublicationStatus.DRAFT -> {
                if (tree.isUserOwner) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Дерево приватное. Отправьте его на публикацию для размещения на сайте."
                    sendForModerationButton.text = "Отправить на публикацию"
                    setupSendForModerationButton()
                } else {
                    moderationCardView.visibility = View.GONE
                }
            }
            
            PublicationStatus.PENDING_MODERATION -> {
                moderationCardView.visibility = View.VISIBLE
                moderationMessageTextView.text = "Дерево ожидает публикации. Редактирование заблокировано до принятия решения администратором."
                sendForModerationButton.visibility = View.GONE
            }
            
            PublicationStatus.REJECTED -> {
                if (tree.isUserOwner) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Публикация дерева была отклонена. Проверьте уведомления для получения информации о причинах."
                    sendForModerationButton.text = "Отправить на повторную публикацию"
                    sendForModerationButton.visibility = View.VISIBLE
                    setupSendForModerationButton()
                } else {
                    moderationCardView.visibility = View.GONE
                }
            }
            
            PublicationStatus.PUBLISHED -> {
                moderationCardView.visibility = View.GONE
            }
            
            null -> {
                // Для совместимости со старой версией API
                if (tree.isUserOwner && !tree.isPublic) {
                    moderationCardView.visibility = View.VISIBLE
                    moderationMessageTextView.text = "Дерево приватное. Отправьте его на публикацию для размещения на сайте."
                    sendForModerationButton.text = "Отправить на публикацию"
                    setupSendForModerationButton()
                } else {
                    moderationCardView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSendForModerationButton() {
        sendForModerationButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Отправка на публикацию")
                .setMessage("Перед отправкой дерева на публикацию все мемориалы в дереве должны быть опубликованы.\n\n" +
                        "Отправить дерево на публикацию?\n\n" +
                        "Важно! После отправки на публикацию:\n" +
                        "• Дерево станет недоступно для редактирования\n" +
                        "• Изменения будут возможны только после решения администратора\n" +
                        "• Вы получите уведомление о результате проверки")
                .setPositiveButton("Отправить") { _, _ ->
                    sendTreeForModeration()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun sendTreeForModeration() {
        // Запускаем операцию через ViewModel
        viewModel.sendFamilyTreeForModeration(treeId)
    }

    private fun unpublishTree() {
        // Запускаем операцию через ViewModel
        viewModel.unpublishFamilyTree(treeId)
    }

    private fun setupClickListeners() {

        unpublishButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Снятие с публикации")
                .setMessage("Вы уверены, что хотите снять дерево с публикации?\n\n" +
                        "После снятия с публикации:\n" +
                        "• Дерево станет приватным\n" +
                        "• Оно не будет отображаться в публичном каталоге\n" +
                        "• Доступ к нему будут иметь только вы и пользователи с предоставленными правами")
                .setPositiveButton("Снять с публикации") { _, _ ->
                    unpublishTree()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()

            if (name.isBlank()) {
                Toast.makeText(context, "Введите название дерева", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTree = viewModel.familyTree.value
            if (currentTree != null && currentTree.publicationStatus == ru.sevostyanov.aiscemetery.models.PublicationStatus.PUBLISHED) {
                // Для публичных деревьев показываем диалог подтверждения
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Изменения будут отправлены на модерацию")
                    .setMessage("Дерево является публичным. Ваши изменения будут отправлены на рассмотрение администраторам.\n\n" +
                            "После одобрения изменения будут применены к дереву.")
                    .setPositiveButton("Отправить на модерацию") { _, _ ->
                        wasSaved = true
                        viewModel.submitTreeChangesForModeration(
                            id = treeId,
                            name = name,
                            description = description,
                            message = "Изменения в названии и/или описании дерева"
                        )
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } else {
                // Для приватных деревьев сохраняем изменения напрямую
                wasSaved = true
                viewModel.updateFamilyTree(
                    id = treeId,
                    name = name,
                    description = description
                )
            }
        }

        viewGenealogyButton.setOnClickListener {
            Log.d("FamilyTreeFragment", "Кнопка просмотра генеалогии нажата, treeId: $treeId")
            
            // Проверяем, находимся ли мы в DraftComparisonActivity
            val isDraftComparison = activity is DraftComparisonActivity
            
            if (isDraftComparison) {
                // В режиме сравнения черновиков открываем окно просмотра связей
                openTreeConnectionsView()
            } else {
                // В обычном режиме переходим к фрагменту просмотра генеалогии
                try {
                    val bundle = Bundle().apply {
                        putLong("familyTreeId", treeId)  // Изменено с "treeId" на "familyTreeId"
                        putBoolean("isDraft", isDraftTree)
                    }
                    findNavController().navigate(R.id.action_familyTreesListFragment_to_genealogyTreeFragment, bundle)
                    dismiss()
                } catch (e: Exception) {
                    Log.e("FamilyTreeFragment", "Ошибка навигации: ${e.message}", e)
                    Toast.makeText(context, "Ошибка навигации", Toast.LENGTH_SHORT).show()
                }
            }
        }

        editGenealogyButton.setOnClickListener {
            Log.d("FamilyTreeFragment", "Кнопка редактирования генеалогии нажата, treeId: $treeId")
            
            // Проверяем, находимся ли мы в DraftComparisonActivity
            val isDraftComparison = activity is DraftComparisonActivity
            
            if (isDraftComparison) {
                // В режиме сравнения черновиков редактирование недоступно
                Toast.makeText(context, "Редактирование недоступно в режиме сравнения", Toast.LENGTH_SHORT).show()
            } else {
                // В обычном режиме переходим к фрагменту редактирования генеалогии
                try {
                    val bundle = Bundle().apply {
                        putLong("treeId", treeId)
                    }
                    findNavController().navigate(R.id.action_familyTreesListFragment_to_editGenealogyTreeFragment, bundle)
                    dismiss()
                } catch (e: Exception) {
                    Log.e("FamilyTreeFragment", "Ошибка навигации: ${e.message}", e)
                    Toast.makeText(context, "Ошибка навигации", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openTreeConnectionsView() {
        // Открываем Activity для просмотра связей дерева
        val tree = viewModel.familyTree.value
        val treeName = tree?.name ?: "Дерево"
        
        val intent = ru.sevostyanov.aiscemetery.activities.TreeChangesComparisonActivity.newIntent(
            requireContext(),
            treeId, // originalTreeId
            treeId, // draftTreeId (пока одинаковые, потом доработаем)
            treeName
        )
        startActivity(intent)
    }

    private fun showLoadingDialog(message: String) {
        hideLoadingDialog() // Скрываем предыдущий диалог, если есть
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.loading_message)
        messageTextView.text = message
        
        loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        loadingDialog?.show()
    }
    
    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    
    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showSuccessDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Проверяем, что фрагмент все еще добавлен к менеджеру
                if (isAdded && !isRemoving && !isDetached) {
                    try {
                        // Закрываем диалог после успешной отправки на модерацию
                        setFragmentResult("family_tree_updated", Bundle())
                        dismiss()
                    } catch (e: Exception) {
                        Log.e("FamilyTreeFragment", "Error closing dialog: ${e.message}", e)
                    }
                }
            }
            .show()
    }
    
    private fun restoreButtonStates() {
        val tree = viewModel.familyTree.value
        if (tree != null) {
            val canEdit = viewModel.canEditTree()
            val canSendForModeration = viewModel.canSendForModeration()
            val canUnpublish = viewModel.canUnpublishTree()
            
            saveButton.isEnabled = canEdit
            sendForModerationButton.isEnabled = canSendForModeration
            unpublishButton.isEnabled = canUnpublish
        }
    }
    
    private var lastTreeStatus: ru.sevostyanov.aiscemetery.models.PublicationStatus? = null
    
    private fun checkForModerationSuccess(tree: ru.sevostyanov.aiscemetery.models.FamilyTree) {
        val currentStatus = tree.publicationStatus
        
        // Инициализируем при первой загрузке
        if (lastTreeStatus == null) {
            lastTreeStatus = currentStatus
            return
        }
        
        // Проверяем, изменился ли статус дерева
        if (lastTreeStatus != currentStatus) {
            when (currentStatus) {
                ru.sevostyanov.aiscemetery.models.PublicationStatus.PENDING_MODERATION -> {
                    Toast.makeText(requireContext(), "Дерево отправлено на публикацию", Toast.LENGTH_SHORT).show()
                    setFragmentResult("family_tree_updated", Bundle())
                    dismiss()
                }
                ru.sevostyanov.aiscemetery.models.PublicationStatus.DRAFT -> {
                    if (lastTreeStatus == ru.sevostyanov.aiscemetery.models.PublicationStatus.PUBLISHED) {
                        Toast.makeText(requireContext(), "Дерево снято с публикации", Toast.LENGTH_SHORT).show()
                        setFragmentResult("family_tree_updated", Bundle())
                        dismiss()
                    }
                }
                else -> {
                    // Другие изменения статуса
                }
            }
            lastTreeStatus = currentStatus
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        hideLoadingDialog()
    }

    companion object {
        fun newInstance(treeId: Long, isDraft: Boolean = false): FamilyTreeFragment {
            val fragment = FamilyTreeFragment()
            val args = Bundle()
            args.putLong("treeId", treeId)
            args.putBoolean("isDraft", isDraft)
            fragment.arguments = args
            return fragment
        }
    }
} 