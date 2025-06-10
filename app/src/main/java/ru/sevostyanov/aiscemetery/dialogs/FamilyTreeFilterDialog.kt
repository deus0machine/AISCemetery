package ru.sevostyanov.aiscemetery.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import ru.sevostyanov.aiscemetery.R
import java.text.SimpleDateFormat
import java.util.*

class FamilyTreeFilterDialog : BottomSheetDialogFragment() {

    private lateinit var treeNameEditText: EditText
    private lateinit var ownerNameEditText: EditText
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var applyButton: Button
    private lateinit var resetButton: Button

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var onFilterAppliedListener: ((FilterOptions) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_family_tree_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        treeNameEditText = view.findViewById(R.id.edit_text_tree_name)
        ownerNameEditText = view.findViewById(R.id.edit_text_owner_name)
        startDateButton = view.findViewById(R.id.button_start_date)
        endDateButton = view.findViewById(R.id.button_end_date)
        applyButton = view.findViewById(R.id.button_apply)
        resetButton = view.findViewById(R.id.button_reset)
    }

    private fun setupListeners() {
        startDateButton.setOnClickListener {
            showDatePicker("Начальная дата создания") { date ->
                startDate = Calendar.getInstance().apply { timeInMillis = date }
                startDateButton.text = formatDate(date)
            }
        }

        endDateButton.setOnClickListener {
            showDatePicker("Конечная дата создания") { date ->
                endDate = Calendar.getInstance().apply { timeInMillis = date }
                endDateButton.text = formatDate(date)
            }
        }

        applyButton.setOnClickListener {
            val filterOptions = FilterOptions(
                treeName = treeNameEditText.text.toString().takeIf { it.isNotBlank() },
                ownerName = ownerNameEditText.text.toString().takeIf { it.isNotBlank() },
                startDate = startDate?.let { formatDateForServer(it.timeInMillis) },
                endDate = endDate?.let { formatDateForServer(it.timeInMillis) }
            )
            onFilterAppliedListener?.invoke(filterOptions)
            // Автоматически сбрасываем фильтры после применения
            resetAllFields()
            dismiss()
        }

        resetButton.setOnClickListener {
            resetAllFields()
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
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDateForServer(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun resetAllFields() {
        treeNameEditText.text.clear()
        ownerNameEditText.text.clear()
        startDate = null
        endDate = null
        startDateButton.text = "С (начальная дата)"
        endDateButton.text = "По (конечная дата)"
    }

    fun setOnFilterAppliedListener(listener: (FilterOptions) -> Unit) {
        onFilterAppliedListener = listener
    }

    data class FilterOptions(
        val treeName: String?,
        val ownerName: String?,
        val startDate: String?,
        val endDate: String?
    ) {
        fun hasActiveFilters(): Boolean {
            return !treeName.isNullOrBlank() ||
                   !ownerName.isNullOrBlank() || 
                   !startDate.isNullOrBlank() || 
                   !endDate.isNullOrBlank()
        }
    }

    companion object {
        fun newInstance() = FamilyTreeFilterDialog()
    }
} 