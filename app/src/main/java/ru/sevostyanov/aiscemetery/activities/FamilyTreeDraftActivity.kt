package ru.sevostyanov.aiscemetery.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.models.FamilyTreeDraft
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.databinding.ActivityFamilyTreeDraftBinding
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDraftViewModel

class FamilyTreeDraftActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFamilyTreeDraftBinding
    private val viewModel: FamilyTreeDraftViewModel by viewModels()
    
    private var familyTreeId: Long = 0
    private var editorId: Long = 0
    private var currentDraft: FamilyTreeDraft? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyTreeDraftBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Получаем параметры из Intent
        familyTreeId = intent.getLongExtra("familyTreeId", 0)
        editorId = intent.getLongExtra("editorId", 0)
        
        if (familyTreeId == 0L || editorId == 0L) {
            Toast.makeText(this, "Неверные параметры", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        observeViewModel()
        loadDraft()
    }
    
    private fun setupUI() {
        binding.apply {
            // Кнопка сохранения изменений
            btnSaveDraft.setOnClickListener {
                saveDraft()
            }
            
            // Кнопка сброса к исходным данным
            btnResetDraft.setOnClickListener {
                resetDraft()
            }
            
            // Кнопка назад
            btnBack.setOnClickListener {
                finish()
            }
            
            // Кнопка для тестирования
            btnLoadPendingDrafts.setOnClickListener {
                viewModel.getSubmittedDraftsForOwner(1L) // Тестовый ID владельца
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.draft.observe(this) { draft ->
            currentDraft = draft
            updateUI(draft)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) 
                android.view.View.VISIBLE else android.view.View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.pendingDrafts.observe(this) { drafts ->
            // Здесь можно показать список черновиков для рассмотрения
            binding.tvPendingDraftsCount.text = "Черновиков на рассмотрении: ${drafts.size}"
        }
    }
    
    private fun updateUI(draft: FamilyTreeDraft?) {
        draft?.let {
            binding.apply {
                // Заполняем поля данными черновика
                etDraftName.setText(it.draftName)
                etDraftDescription.setText(it.draftDescription ?: "")
                switchDraftPublic.isChecked = it.draftIsPublic
                
                // Показываем исходные данные для сравнения
                tvOriginalName.text = "Исходное название: ${it.originalName}"
                tvOriginalDescription.text = "Исходное описание: ${it.originalDescription ?: "Не указано"}"
                tvOriginalPublic.text = "Исходная видимость: ${if (it.originalIsPublic) "Публичное" else "Приватное"}"
                
                // Показываем статус и изменения
                tvDraftStatus.text = "Статус: ${it.getStatusText()}"
                tvChangesDescription.text = "Изменения: ${it.getChangesDescription()}"
                
                // Управляем доступностью кнопок
                val canEdit = it.canEdit()
                etDraftName.isEnabled = canEdit
                etDraftDescription.isEnabled = canEdit
                switchDraftPublic.isEnabled = canEdit
                btnSaveDraft.isEnabled = canEdit
                btnResetDraft.isEnabled = canEdit
                
                // Показываем сообщения рецензента
                if (!it.reviewMessage.isNullOrEmpty()) {
                    tvReviewMessage.text = "Комментарий рецензента: ${it.reviewMessage}"
                    tvReviewMessage.visibility = android.view.View.VISIBLE
                } else {
                    tvReviewMessage.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun loadDraft() {
        lifecycleScope.launch {
            viewModel.getOrCreateActiveDraft(familyTreeId, editorId)
        }
    }
    
    private fun saveDraft() {
        currentDraft?.let { draft ->
            val name = binding.etDraftName.text.toString().trim()
            val description = binding.etDraftDescription.text.toString().trim()
            val isPublic = binding.switchDraftPublic.isChecked
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                return
            }
            
            lifecycleScope.launch {
                viewModel.updateDraft(draft.id, name, description.ifEmpty { null }, isPublic)
            }
        }
    }
    
    private fun resetDraft() {
        currentDraft?.let { draft ->
            binding.apply {
                etDraftName.setText(draft.originalName)
                etDraftDescription.setText(draft.originalDescription ?: "")
                switchDraftPublic.isChecked = draft.originalIsPublic
            }
            
            // Сохраняем сброшенные данные
            saveDraft()
        }
    }
} 