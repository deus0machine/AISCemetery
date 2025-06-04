package ru.sevostyanov.aiscemetery.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.MemorialDropdownAdapter
import ru.sevostyanov.aiscemetery.adapters.RelationTypeDropdownAdapter
import ru.sevostyanov.aiscemetery.databinding.DialogCreateRelationBinding
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType
import ru.sevostyanov.aiscemetery.util.ValidationUtils

class CreateRelationDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(
            memorials: List<Memorial>,
            preselectedMemorial: Memorial? = null,
            existingRelation: MemorialRelation? = null,
            onRelationCreated: (Memorial, Memorial, RelationType) -> Unit,
            onRelationUpdated: ((MemorialRelation, Memorial, Memorial, RelationType) -> Unit)? = null
        ): CreateRelationDialogFragment {
            return CreateRelationDialogFragment().apply {
                this.memorials = memorials
                this.preselectedMemorial = preselectedMemorial
                this.existingRelation = existingRelation
                this.onRelationCreated = onRelationCreated
                this.onRelationUpdated = onRelationUpdated
            }
        }
    }

    private var memorials: List<Memorial> = emptyList()
    private var preselectedMemorial: Memorial? = null
    private var existingRelation: MemorialRelation? = null
    private var onRelationCreated: ((Memorial, Memorial, RelationType) -> Unit)? = null
    private var onRelationUpdated: ((MemorialRelation, Memorial, Memorial, RelationType) -> Unit)? = null

    private lateinit var binding: DialogCreateRelationBinding
    private lateinit var memorialAdapter: MemorialDropdownAdapter
    private lateinit var relationTypeAdapter: RelationTypeDropdownAdapter

    private var selectedSourceMemorial: Memorial? = null
    private var selectedTargetMemorial: Memorial? = null
    private var selectedRelationType: RelationType? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreateRelationBinding.inflate(LayoutInflater.from(requireContext()))
        
        setupUI()
        setupAdapters()
        setupListeners()
        
        // Если редактируем существующую связь
        existingRelation?.let { relation ->
            binding.textDialogTitle.text = "Редактировать связь"
            binding.buttonSave.text = "Сохранить изменения"
            
            // Заполняем поля данными существующей связи
            selectedSourceMemorial = relation.sourceMemorial
            selectedTargetMemorial = relation.targetMemorial
            selectedRelationType = relation.relationType
            
            binding.autocompleteSourceMemorial.setText(relation.sourceMemorial.fio, false)
            binding.autocompleteTargetMemorial.setText(relation.targetMemorial.fio, false)
            binding.autocompleteRelationType.setText(getRelationTypeTitle(relation.relationType), false)
            
            updatePreview()
            updateSaveButtonState()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupUI() {
        // Оставляем только основные типы связей для генеалогического дерева
        val availableRelationTypes = arrayOf(
            RelationType.PARENT,
            RelationType.CHILD, 
            RelationType.SPOUSE,
            RelationType.SIBLING
        )
        relationTypeAdapter = RelationTypeDropdownAdapter(requireContext(), availableRelationTypes)
        
        binding.autocompleteRelationType.setAdapter(relationTypeAdapter)
    }

    private fun setupAdapters() {
        // Адаптер для мемориалов (исключаем уже выбранные)
        updateMemorialAdapters()
    }

    private fun updateMemorialAdapters() {
        // Для первого dropdown - все мемориалы
        val sourceMemorials = if (selectedTargetMemorial != null) {
            memorials.filter { it.id != selectedTargetMemorial?.id }
        } else {
            memorials
        }
        
        // Для второго dropdown - все мемориалы кроме выбранного в первом
        val targetMemorials = if (selectedSourceMemorial != null) {
            memorials.filter { it.id != selectedSourceMemorial?.id }
        } else {
            memorials
        }

        memorialAdapter = MemorialDropdownAdapter(requireContext(), sourceMemorials)
        binding.autocompleteSourceMemorial.setAdapter(memorialAdapter)

        val targetAdapter = MemorialDropdownAdapter(requireContext(), targetMemorials)
        binding.autocompleteTargetMemorial.setAdapter(targetAdapter)
    }

    private fun setupListeners() {
        // Выбор первого мемориала
        binding.autocompleteSourceMemorial.setOnItemClickListener { _, _, position, _ ->
            selectedSourceMemorial = memorialAdapter.getItem(position)
            selectedSourceMemorial?.let { memorial ->
                binding.autocompleteSourceMemorial.setText(memorial.fio, false)
            }
            updateMemorialAdapters()
            updatePreview()
            updateSaveButtonState()
        }

        // Выбор второго мемориала  
        binding.autocompleteTargetMemorial.setOnItemClickListener { _, _, position, _ ->
            val targetAdapter = binding.autocompleteTargetMemorial.adapter as MemorialDropdownAdapter
            selectedTargetMemorial = targetAdapter.getItem(position)
            selectedTargetMemorial?.let { memorial ->
                binding.autocompleteTargetMemorial.setText(memorial.fio, false)
            }
            updateMemorialAdapters()
            updatePreview()
            updateSaveButtonState()
        }

        // Выбор типа связи
        binding.autocompleteRelationType.setOnItemClickListener { _, _, position, _ ->
            selectedRelationType = relationTypeAdapter.getItem(position)
            selectedRelationType?.let { type ->
                binding.autocompleteRelationType.setText(getRelationTypeTitle(type), false)
            }
            updatePreview()
            updateSaveButtonState()
        }

        // Очистка полей при изменении текста
        binding.autocompleteSourceMemorial.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != selectedSourceMemorial?.fio) {
                    selectedSourceMemorial = null
                    updatePreview()
                    updateSaveButtonState()
                }
            }
        })

        binding.autocompleteTargetMemorial.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != selectedTargetMemorial?.fio) {
                    selectedTargetMemorial = null
                    updatePreview()
                    updateSaveButtonState()
                }
            }
        })

        binding.autocompleteRelationType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentText = s.toString()
                // Проверяем только среди доступных типов связей
                val availableTypes = arrayOf(
                    RelationType.PARENT,
                    RelationType.CHILD, 
                    RelationType.SPOUSE,
                    RelationType.SIBLING
                )
                val matchingType = availableTypes.find { 
                    getRelationTypeTitle(it) == currentText 
                }
                if (matchingType != selectedRelationType) {
                    selectedRelationType = null
                    updatePreview()
                    updateSaveButtonState()
                }
            }
        })

        // Кнопки
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            val source = selectedSourceMemorial
            val target = selectedTargetMemorial
            val type = selectedRelationType

            if (source != null && target != null && type != null) {
                // Дополнительная валидация бизнес-логики
                val validationError = validateRelation(source, target, type)
                if (validationError != null) {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Некорректная связь")
                        .setMessage(validationError)
                        .setPositiveButton("ОК", null)
                        .show()
                    return@setOnClickListener
                }
                
                if (existingRelation != null) {
                    onRelationUpdated?.invoke(existingRelation!!, source, target, type)
                } else {
                    onRelationCreated?.invoke(source, target, type)
                }
                dismiss()
            }
        }

        // Предварительно выбрать мемориал, если указан
        preselectedMemorial?.let { memorial ->
            selectedSourceMemorial = memorial
            binding.autocompleteSourceMemorial.setText(memorial.fio, false)
            updateMemorialAdapters()
            updatePreview()
            updateSaveButtonState()
        }
    }

    private fun updatePreview() {
        val source = selectedSourceMemorial
        val target = selectedTargetMemorial
        val type = selectedRelationType

        if (source != null && target != null && type != null) {
            val previewText = generatePreviewText(source, target, type)
            binding.textPreview.text = previewText
            binding.cardPreview.visibility = View.VISIBLE
        } else {
            binding.cardPreview.visibility = View.GONE
        }
    }

    private fun generatePreviewText(source: Memorial, target: Memorial, type: RelationType): String {
        return when (type) {
            RelationType.PARENT -> "${source.fio} — родитель → ${target.fio}"
            RelationType.CHILD -> "${source.fio} — ребенок → ${target.fio}"
            RelationType.SPOUSE -> "${source.fio} ♥ ${target.fio} (супруги)"
            RelationType.SIBLING -> "${source.fio} ↔ ${target.fio} (брат/сестра)"
            RelationType.GRANDPARENT -> "${source.fio} является дедушкой/бабушкой для ${target.fio}"
            RelationType.GRANDCHILD -> "${source.fio} является внуком/внучкой для ${target.fio}"
            RelationType.UNCLE_AUNT -> "${source.fio} является дядей/тетей для ${target.fio}"
            RelationType.NEPHEW_NIECE -> "${source.fio} является племянником/племянницей для ${target.fio}"
            RelationType.PLACEHOLDER -> "${source.fio} добавлен в дерево без связи с ${target.fio}"
        }
    }

    private fun updateSaveButtonState() {
        val canSave = selectedSourceMemorial != null && 
                      selectedTargetMemorial != null && 
                      selectedRelationType != null &&
                      selectedSourceMemorial?.id != selectedTargetMemorial?.id
        
        binding.buttonSave.isEnabled = canSave
    }

    private fun getRelationTypeTitle(type: RelationType): String {
        return when (type) {
            RelationType.PARENT -> "Родитель"
            RelationType.CHILD -> "Ребенок"
            RelationType.SPOUSE -> "Супруг/Супруга"
            RelationType.SIBLING -> "Брат/Сестра"
            RelationType.GRANDPARENT -> "Дедушка/Бабушка"
            RelationType.GRANDCHILD -> "Внук/Внучка"
            RelationType.UNCLE_AUNT -> "Дядя/Тетя"
            RelationType.NEPHEW_NIECE -> "Племянник/Племянница"
            RelationType.PLACEHOLDER -> "Без связи"
        }
    }

    private fun validateRelation(source: Memorial, target: Memorial, type: RelationType): String? {
        // Проверка одинаковых мемориалов (уже есть в updateSaveButtonState, но дублируем для надежности)
        if (source.id == target.id) {
            return "Нельзя создать связь мемориала с самим собой"
        }
        
        // Используем новый утилитный класс для валидации
        return ValidationUtils.validateRelationAgeCompatibility(
            source.birthDate,
            target.birthDate,
            type
        )
    }
} 