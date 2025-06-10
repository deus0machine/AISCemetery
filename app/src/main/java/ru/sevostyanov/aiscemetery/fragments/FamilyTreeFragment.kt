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

@AndroidEntryPoint
class FamilyTreeFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(treeId: Long): FamilyTreeFragment {
            val fragment = FamilyTreeFragment()
            val args = Bundle()
            args.putLong("familyTreeId", treeId)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { arguments?.getLong("familyTreeId") ?: -1L }

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
                nameEditText.setText(it.name)
                descriptionEditText.setText(it.description ?: "")
                updatePublicationStatus(it)
                
                // Блокируем редактирование если дерево на модерации
                val canEdit = viewModel.canEditTree()
                nameEditText.isEnabled = canEdit
                descriptionEditText.isEnabled = canEdit
                saveButton.isEnabled = canEdit
                editGenealogyButton.isEnabled = canEdit
                
                // Обновляем доступность кнопки отправки на модерацию
                sendForModerationButton.isEnabled = viewModel.canSendForModeration()
                
                // Обновляем видимость кнопки снятия с публикации
                unpublishButton.visibility = if (viewModel.canUnpublishTree()) View.VISIBLE else View.GONE
            }
            // dismiss только если было сохранение
            if (wasSaved && tree != null && tree.id == treeId) {
                setFragmentResult("family_tree_updated", Bundle())
                dismiss()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Для длинных сообщений об ошибках используем LONG
                val duration = if (it.length > 50) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                Toast.makeText(context, it, duration).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Блокируем все кнопки во время загрузки, но учитываем модерацию
            val tree = viewModel.familyTree.value
            val canEdit = tree?.let { viewModel.canEditTree() } ?: false
            val canSendForModeration = tree?.let { viewModel.canSendForModeration() } ?: false
            val canUnpublish = tree?.let { viewModel.canUnpublishTree() } ?: false
            
            saveButton.isEnabled = !isLoading && canEdit
            editGenealogyButton.isEnabled = !isLoading && canEdit
            sendForModerationButton.isEnabled = !isLoading && canSendForModeration
            unpublishButton.isEnabled = !isLoading && canUnpublish
            viewGenealogyButton.isEnabled = !isLoading // Просмотр всегда доступен
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
        lifecycleScope.launch {
            try {
                viewModel.sendFamilyTreeForModeration(treeId)
                Toast.makeText(requireContext(), "Дерево отправлено на публикацию", Toast.LENGTH_SHORT).show()
                
                // Сначала уведомляем о необходимости обновления
                setFragmentResult("family_tree_updated", Bundle())
                
                // Закрываем диалог после успешной отправки
                dismiss()
            } catch (e: Exception) {
                Log.e("FamilyTreeFragment", "Ошибка при отправке дерева на модерацию: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun unpublishTree() {
        lifecycleScope.launch {
            try {
                viewModel.unpublishFamilyTree(treeId)
                Toast.makeText(requireContext(), "Дерево снято с публикации", Toast.LENGTH_SHORT).show()
                
                // Сначала уведомляем о необходимости обновления
                setFragmentResult("family_tree_updated", Bundle())
                
                // Закрываем диалог после успешного снятия
                dismiss()
            } catch (e: Exception) {
                Log.e("FamilyTreeFragment", "Ошибка при снятии дерева с публикации: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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

            wasSaved = true
            viewModel.updateFamilyTree(
                id = treeId,
                name = name,
                description = description
            )
        }

        viewGenealogyButton.setOnClickListener {
            Log.d("FamilyTreeFragment", "Кнопка просмотра генеалогии нажата, treeId: $treeId")
            
            // Закрываем текущий диалог
            dismiss()
            
            // Переходим к фрагменту просмотра генеалогии
            try {
                val bundle = Bundle().apply {
                    putLong("treeId", treeId)
                }
                findNavController().navigate(R.id.action_familyTreesListFragment_to_genealogyTreeFragment, bundle)
            } catch (e: Exception) {
                Log.e("FamilyTreeFragment", "Ошибка навигации: ${e.message}", e)
                Toast.makeText(context, "Ошибка при переходе к дереву", Toast.LENGTH_SHORT).show()
            }
        }

        editGenealogyButton.setOnClickListener {
            Log.d("FamilyTreeFragment", "Кнопка редактирования генеалогии нажата, treeId: $treeId")
            
            // Закрываем текущий диалог
            dismiss()
            
            // Переходим к фрагменту редактирования генеалогии
            try {
                val bundle = Bundle().apply {
                    putLong("treeId", treeId)
                }
                findNavController().navigate(R.id.action_familyTreesListFragment_to_editGenealogyTreeFragment, bundle)
            } catch (e: Exception) {
                Log.e("FamilyTreeFragment", "Ошибка навигации: ${e.message}", e)
                Toast.makeText(context, "Ошибка при переходе к редактору", Toast.LENGTH_SHORT).show()
            }
        }
    }

} 