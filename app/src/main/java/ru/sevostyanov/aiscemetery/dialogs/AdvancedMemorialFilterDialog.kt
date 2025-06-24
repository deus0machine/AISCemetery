package ru.sevostyanov.aiscemetery.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import ru.sevostyanov.aiscemetery.R
import java.text.SimpleDateFormat
import java.util.*

class AdvancedMemorialFilterDialog : BottomSheetDialogFragment() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var middleNameEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var generalQueryEditText: EditText
    
    private lateinit var birthDateFromButton: Button
    private lateinit var birthDateToButton: Button
    private lateinit var deathDateFromButton: Button
    private lateinit var deathDateToButton: Button
    
    private lateinit var onlyPublicCheckBox: CheckBox
    private lateinit var onlyPrivateCheckBox: CheckBox
    
    private lateinit var sortBySpinner: Spinner
    private lateinit var sortDirectionSpinner: Spinner
    
    private lateinit var applyButton: Button
    private lateinit var resetButton: Button
    private lateinit var quickSearchButton: Button

    private var birthDateFrom: Calendar? = null
    private var birthDateTo: Calendar? = null
    private var deathDateFrom: Calendar? = null
    private var deathDateTo: Calendar? = null
    
    private var onFilterAppliedListener: ((AdvancedFilterOptions) -> Unit)? = null
    private var onQuickSearchListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_advanced_memorial_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupSpinners()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        firstNameEditText = view.findViewById(R.id.edit_text_first_name)
        lastNameEditText = view.findViewById(R.id.edit_text_last_name)
        middleNameEditText = view.findViewById(R.id.edit_text_middle_name)
        locationEditText = view.findViewById(R.id.edit_text_location)
        generalQueryEditText = view.findViewById(R.id.edit_text_general_query)
        
        birthDateFromButton = view.findViewById(R.id.button_birth_date_from)
        birthDateToButton = view.findViewById(R.id.button_birth_date_to)
        deathDateFromButton = view.findViewById(R.id.button_death_date_from)
        deathDateToButton = view.findViewById(R.id.button_death_date_to)
        
        onlyPublicCheckBox = view.findViewById(R.id.checkbox_only_public)
        onlyPrivateCheckBox = view.findViewById(R.id.checkbox_only_private)
        
        sortBySpinner = view.findViewById(R.id.spinner_sort_by)
        sortDirectionSpinner = view.findViewById(R.id.spinner_sort_direction)
        
        applyButton = view.findViewById(R.id.button_apply)
        resetButton = view.findViewById(R.id.button_reset)
        quickSearchButton = view.findViewById(R.id.button_quick_search)
    }

    private fun setupSpinners() {
        // Настройка спиннера сортировки
        val sortOptions = arrayOf(
            "По умолчанию",
            "По имени",
            "По фамилии", 
            "По дате рождения",
            "По дате смерти",
            "По дате создания"
        )
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortBySpinner.adapter = sortAdapter

        // Настройка спиннера направления сортировки
        val directionOptions = arrayOf("По возрастанию", "По убыванию")
        val directionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, directionOptions)
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortDirectionSpinner.adapter = directionAdapter
    }

    private fun setupListeners() {
        birthDateFromButton.setOnClickListener {
            showDatePicker("Дата рождения с") { date ->
                birthDateFrom = Calendar.getInstance().apply { timeInMillis = date }
                birthDateFromButton.text = formatDateShort(date)
            }
        }

        birthDateToButton.setOnClickListener {
            showDatePicker("Дата рождения по") { date ->
                birthDateTo = Calendar.getInstance().apply { timeInMillis = date }
                birthDateToButton.text = formatDateShort(date)
            }
        }

        deathDateFromButton.setOnClickListener {
            showDatePicker("Дата смерти с") { date ->
                deathDateFrom = Calendar.getInstance().apply { timeInMillis = date }
                deathDateFromButton.text = formatDateShort(date)
            }
        }

        deathDateToButton.setOnClickListener {
            showDatePicker("Дата смерти по") { date ->
                deathDateTo = Calendar.getInstance().apply { timeInMillis = date }
                deathDateToButton.text = formatDateShort(date)
            }
        }

        onlyPublicCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onlyPrivateCheckBox.isChecked = false
        }

        onlyPrivateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onlyPublicCheckBox.isChecked = false
        }

        applyButton.setOnClickListener {
            val filterOptions = AdvancedFilterOptions(
                firstName = firstNameEditText.text.toString().takeIf { it.isNotBlank() },
                lastName = lastNameEditText.text.toString().takeIf { it.isNotBlank() },
                middleName = middleNameEditText.text.toString().takeIf { it.isNotBlank() },
                location = locationEditText.text.toString().takeIf { it.isNotBlank() },
                generalQuery = generalQueryEditText.text.toString().takeIf { it.isNotBlank() },
                birthDateFrom = birthDateFrom?.let { formatDate(it.timeInMillis) },
                birthDateTo = birthDateTo?.let { formatDate(it.timeInMillis) },
                deathDateFrom = deathDateFrom?.let { formatDate(it.timeInMillis) },
                deathDateTo = deathDateTo?.let { formatDate(it.timeInMillis) },
                isPublic = when {
                    onlyPublicCheckBox.isChecked -> true
                    onlyPrivateCheckBox.isChecked -> false
                    else -> null
                },
                sortBy = getSortByValue(),
                sortDirection = if (sortDirectionSpinner.selectedItemPosition == 0) "asc" else "desc"
            )
            onFilterAppliedListener?.invoke(filterOptions)
            dismiss()
        }

        resetButton.setOnClickListener {
            resetAllFields()
        }

        quickSearchButton.setOnClickListener {
            val query = generalQueryEditText.text.toString().trim()
            if (query.isNotBlank()) {
                onQuickSearchListener?.invoke(query)
                dismiss()
            } else {
                Toast.makeText(context, "Введите поисковый запрос", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSortByValue(): String? {
        return when (sortBySpinner.selectedItemPosition) {
            0 -> null // По умолчанию
            1 -> "firstName"
            2 -> "lastName"
            3 -> "birthDate"
            4 -> "deathDate"
            5 -> "createdAt"
            else -> null
        }
    }

    private fun showDatePicker(title: String, onDateSelected: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener { date ->
            onDateSelected(date)
        }

        picker.show(parentFragmentManager, null)
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDateShort(timestamp: Long): String {
        return SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun resetAllFields() {
        firstNameEditText.text.clear()
        lastNameEditText.text.clear()
        middleNameEditText.text.clear()
        locationEditText.text.clear()
        generalQueryEditText.text.clear()
        
        birthDateFrom = null
        birthDateTo = null
        deathDateFrom = null
        deathDateTo = null
        
        birthDateFromButton.text = "С"
        birthDateToButton.text = "По"
        deathDateFromButton.text = "С"
        deathDateToButton.text = "По"
        
        onlyPublicCheckBox.isChecked = false
        onlyPrivateCheckBox.isChecked = false
        
        sortBySpinner.setSelection(0)
        sortDirectionSpinner.setSelection(0)
    }

    fun setOnFilterAppliedListener(listener: (AdvancedFilterOptions) -> Unit) {
        onFilterAppliedListener = listener
    }

    fun setOnQuickSearchListener(listener: (String) -> Unit) {
        onQuickSearchListener = listener
    }

    data class AdvancedFilterOptions(
        val firstName: String?,
        val lastName: String?,
        val middleName: String?,
        val location: String?,
        val generalQuery: String?,
        val birthDateFrom: String?,
        val birthDateTo: String?,
        val deathDateFrom: String?,
        val deathDateTo: String?,
        val isPublic: Boolean?,
        val sortBy: String?,
        val sortDirection: String?
    ) {
        fun hasActiveFilters(): Boolean {
            return !firstName.isNullOrBlank() ||
                   !lastName.isNullOrBlank() ||
                   !middleName.isNullOrBlank() ||
                   !location.isNullOrBlank() ||
                   !generalQuery.isNullOrBlank() ||
                   !birthDateFrom.isNullOrBlank() ||
                   !birthDateTo.isNullOrBlank() ||
                   !deathDateFrom.isNullOrBlank() ||
                   !deathDateTo.isNullOrBlank() ||
                   isPublic != null ||
                   !sortBy.isNullOrBlank()
        }
    }

    companion object {
        fun newInstance() = AdvancedMemorialFilterDialog()
    }
} 