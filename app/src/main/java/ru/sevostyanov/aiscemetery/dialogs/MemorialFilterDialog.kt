package ru.sevostyanov.aiscemetery.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import ru.sevostyanov.aiscemetery.R
import java.text.SimpleDateFormat
import java.util.*

class MemorialFilterDialog : BottomSheetDialogFragment() {

    private lateinit var locationEditText: EditText
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var onlyPublicCheckBox: CheckBox
    private lateinit var onlyPrivateCheckBox: CheckBox
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
        return inflater.inflate(R.layout.dialog_memorial_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        locationEditText = view.findViewById(R.id.edit_text_location)
        startDateButton = view.findViewById(R.id.button_start_date)
        endDateButton = view.findViewById(R.id.button_end_date)
        onlyPublicCheckBox = view.findViewById(R.id.checkbox_only_public)
        onlyPrivateCheckBox = view.findViewById(R.id.checkbox_only_private)
        applyButton = view.findViewById(R.id.button_apply)
        resetButton = view.findViewById(R.id.button_reset)
    }

    private fun setupListeners() {
        startDateButton.setOnClickListener {
            showDatePicker("Начальная дата") { date ->
                startDate = Calendar.getInstance().apply { timeInMillis = date }
                startDateButton.text = formatDate(date)
            }
        }

        endDateButton.setOnClickListener {
            showDatePicker("Конечная дата") { date ->
                endDate = Calendar.getInstance().apply { timeInMillis = date }
                endDateButton.text = formatDate(date)
            }
        }

        onlyPublicCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onlyPrivateCheckBox.isChecked = false
        }

        onlyPrivateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onlyPublicCheckBox.isChecked = false
        }

        applyButton.setOnClickListener {
            val filterOptions = FilterOptions(
                location = locationEditText.text.toString().takeIf { it.isNotBlank() },
                startDate = startDate?.let { formatDate(it.timeInMillis) },
                endDate = endDate?.let { formatDate(it.timeInMillis) },
                isPublic = when {
                    onlyPublicCheckBox.isChecked -> true
                    onlyPrivateCheckBox.isChecked -> false
                    else -> null
                }
            )
            onFilterAppliedListener?.invoke(filterOptions)
            dismiss()
        }

        resetButton.setOnClickListener {
            locationEditText.text.clear()
            startDate = null
            endDate = null
            startDateButton.text = "Выбрать начальную дату"
            endDateButton.text = "Выбрать конечную дату"
            onlyPublicCheckBox.isChecked = false
            onlyPrivateCheckBox.isChecked = false
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

    fun setOnFilterAppliedListener(listener: (FilterOptions) -> Unit) {
        onFilterAppliedListener = listener
    }

    data class FilterOptions(
        val location: String?,
        val startDate: String?,
        val endDate: String?,
        val isPublic: Boolean?
    )

    companion object {
        fun newInstance() = MemorialFilterDialog()
    }
} 