package ru.sevostyanov.aiscemetery.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.util.ValidationUtils

/**
 * Кастомный компонент для ввода ФИО через отдельные поля:
 * - firstName (имя) - обязательное
 * - lastName (фамилия) - обязательное  
 * - middleName (отчество) - опциональное
 * 
 * Упрощенная версия: только отдельные поля, автоматическое формирование fio на сервере
 */
class NameInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI элементы для отдельных полей
    private lateinit var layoutLastName: TextInputLayout
    private lateinit var editLastName: TextInputEditText
    private lateinit var layoutFirstName: TextInputLayout
    private lateinit var editFirstName: TextInputEditText
    private lateinit var layoutMiddleName: TextInputLayout
    private lateinit var editMiddleName: TextInputEditText
    private lateinit var cardPreview: View
    private lateinit var textPreviewFull: android.widget.TextView
    private lateinit var textPreviewShort: android.widget.TextView

    // Состояние
    private var onNameChangedListener: ((firstName: String?, lastName: String?, middleName: String?) -> Unit)? = null

    init {
        initView()
        setupListeners()
    }

    private fun initView() {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_name_input_simplified, this, true)
        
        // Находим элементы (только для отдельных полей)
        layoutLastName = view.findViewById(R.id.layout_last_name)
        editLastName = view.findViewById(R.id.edit_last_name)
        layoutFirstName = view.findViewById(R.id.layout_first_name)
        editFirstName = view.findViewById(R.id.edit_first_name)
        layoutMiddleName = view.findViewById(R.id.layout_middle_name)
        editMiddleName = view.findViewById(R.id.edit_middle_name)
        cardPreview = view.findViewById(R.id.card_preview)
        textPreviewFull = view.findViewById(R.id.text_preview_full)
        textPreviewShort = view.findViewById(R.id.text_preview_short)
    }

    private fun setupListeners() {
        // Текстовые изменения для всех полей
        val nameFieldWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview()
                notifyNameChanged()
            }
        }

        editLastName.addTextChangedListener(nameFieldWatcher)
        editFirstName.addTextChangedListener(nameFieldWatcher)
        editMiddleName.addTextChangedListener(nameFieldWatcher)
    }

    private fun updatePreview() {
        clearErrors()
        validateFields()
        
        val firstName = editFirstName.text?.toString()?.trim()
        val lastName = editLastName.text?.toString()?.trim()
        val middleName = editMiddleName.text?.toString()?.trim()
        
        if (!firstName.isNullOrBlank() && !lastName.isNullOrBlank()) {
            val fullName = Memorial.buildFio(firstName, lastName, middleName)
            val shortName = buildShortName(firstName, lastName, middleName)
            
            textPreviewFull.text = "Полное: $fullName"
            textPreviewShort.text = "Краткое: $shortName"
            cardPreview.visibility = View.VISIBLE
        } else {
            cardPreview.visibility = View.GONE
        }
    }

    private fun buildShortName(firstName: String?, lastName: String?, middleName: String?): String {
        return buildString {
            if (!lastName.isNullOrBlank()) {
                append(lastName)
            }
            if (!firstName.isNullOrBlank()) {
                if (isNotEmpty()) append(" ")
                append(firstName.first().uppercase())
                append(".")
            }
            if (!middleName.isNullOrBlank()) {
                append(middleName.first().uppercase())
                append(".")
            }
        }
    }

    private fun validateFields() {
        val firstName = editFirstName.text?.toString()?.trim()
        val lastName = editLastName.text?.toString()?.trim()
        val middleName = editMiddleName.text?.toString()?.trim()

        // Валидация отдельных полей
        ValidationUtils.validateFirstName(firstName)?.let {
            layoutFirstName.error = it
        }

        ValidationUtils.validateLastName(lastName)?.let {
            layoutLastName.error = it
        }

        ValidationUtils.validateMiddleName(middleName)?.let {
            layoutMiddleName.error = it
        }
    }

    private fun clearErrors() {
        layoutFirstName.error = null
        layoutLastName.error = null
        layoutMiddleName.error = null
    }

    private fun notifyNameChanged() {
        val firstName = editFirstName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val lastName = editLastName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val middleName = editMiddleName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        onNameChangedListener?.invoke(firstName, lastName, middleName)
    }

    // ========== Публичные методы ==========

    /**
     * Устанавливает данные из объекта Memorial
     */
    fun setMemorial(memorial: Memorial) {
        // Используем отдельные поля если есть, иначе пытаемся разобрать fio
        if (memorial.hasSeparateNameFields()) {
            editFirstName.setText(memorial.firstName ?: "")
            editLastName.setText(memorial.lastName ?: "")
            editMiddleName.setText(memorial.middleName ?: "")
        } else if (memorial.fio.isNotBlank()) {
            // Разбираем старое fio на компоненты для обратной совместимости
            val (firstName, lastName, middleName) = Memorial.parseFio(memorial.fio)
            editFirstName.setText(firstName ?: "")
            editLastName.setText(lastName ?: "")
            editMiddleName.setText(middleName ?: "")
        }
        updatePreview()
    }

    /**
     * Возвращает текущие данные для отдельных полей
     */
    fun getCurrentData(): Triple<String?, String?, String?> {
        val firstName = editFirstName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val lastName = editLastName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val middleName = editMiddleName.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        return Triple(firstName, lastName, middleName)
    }

    /**
     * Получает текущее полное ФИО (для совместимости)
     */
    fun getFullName(): String {
        val (firstName, lastName, middleName) = getCurrentData()
        return Memorial.buildFio(firstName, lastName, middleName)
    }

    /**
     * Валидация текущих данных
     */
    fun validate(): String? {
        clearErrors()
        
        val firstName = editFirstName.text?.toString()?.trim()
        val lastName = editLastName.text?.toString()?.trim()
        val middleName = editMiddleName.text?.toString()?.trim()
        
        val error = ValidationUtils.validateNameFields(firstName, lastName, middleName)
        if (error != null) {
            // Показываем ошибку на соответствующем поле
            when {
                ValidationUtils.validateFirstName(firstName) != null -> layoutFirstName.error = error
                ValidationUtils.validateLastName(lastName) != null -> layoutLastName.error = error
                ValidationUtils.validateMiddleName(middleName) != null -> layoutMiddleName.error = error
            }
        }
        return error
    }

    /**
     * Устанавливает слушатель изменений (теперь только отдельные поля)
     */
    fun setOnNameChangedListener(listener: (firstName: String?, lastName: String?, middleName: String?) -> Unit) {
        onNameChangedListener = listener
    }

    /**
     * Устанавливает фокус на первое поле (фамилия)
     */
    fun requestFocusOnFirstField() {
        editLastName.requestFocus()
    }
} 