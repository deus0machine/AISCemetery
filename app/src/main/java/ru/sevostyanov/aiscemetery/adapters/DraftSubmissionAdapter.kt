package ru.sevostyanov.aiscemetery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.DraftSubmission
import java.text.SimpleDateFormat
import java.util.*

class DraftSubmissionAdapter(
    private val isIncoming: Boolean,
    private val onApproveClick: (DraftSubmission) -> Unit = {},
    private val onRejectClick: (DraftSubmission) -> Unit = {},
    private val onItemClick: (DraftSubmission) -> Unit = {}
) : ListAdapter<DraftSubmission, DraftSubmissionAdapter.DraftSubmissionViewHolder>(DraftSubmissionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftSubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft_submission, parent, false)
        return DraftSubmissionViewHolder(view, isIncoming, onApproveClick, onRejectClick, onItemClick)
    }

    override fun onBindViewHolder(holder: DraftSubmissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DraftSubmissionViewHolder(
        itemView: View,
        private val isIncoming: Boolean,
        private val onApproveClick: (DraftSubmission) -> Unit,
        private val onRejectClick: (DraftSubmission) -> Unit,
        private val onItemClick: (DraftSubmission) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.text_message)
        private val treeNameTextView: TextView = itemView.findViewById(R.id.text_tree_name)
        private val editorNameTextView: TextView = itemView.findViewById(R.id.text_editor_name)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_date)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_status)
        private val approveButton: Button = itemView.findViewById(R.id.button_approve)
        private val rejectButton: Button = itemView.findViewById(R.id.button_reject)

        fun bind(submission: DraftSubmission) {
            val context = itemView.context
            
            // Заголовок
            titleTextView.text = if (isIncoming) {
                "Предложены изменения в дереве"
            } else {
                "Отправлены изменения на рассмотрение"
            }
            
            // Сообщение от редактора
            messageTextView.text = submission.message ?: "Без сообщения"
            
            // Название дерева
            treeNameTextView.text = "Дерево: ${submission.draft.familyTree?.name ?: "Неизвестно"}"
            
            // Имя редактора (для входящих) или владельца (для исходящих)
            if (isIncoming) {
                editorNameTextView.text = "От: ${submission.draft.editor?.fio ?: submission.draft.editor?.login ?: "Неизвестно"}"
            } else {
                editorNameTextView.text = "Владелец: ${submission.draft.familyTree?.owner?.fio ?: submission.draft.familyTree?.owner?.login ?: "Неизвестно"}"
            }
            
            // Дата отправки
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(submission.submittedAt)
                if (date != null) outputFormat.format(date) else submission.submittedAt
            } catch (e: Exception) {
                submission.submittedAt
            }
            dateTextView.text = "Отправлено: $formattedDate"
            
            // Статус
            val statusText = when (submission.reviewStatus) {
                DraftSubmission.ReviewStatus.PENDING -> "Ожидает рассмотрения"
                DraftSubmission.ReviewStatus.APPROVED -> "Одобрено"
                DraftSubmission.ReviewStatus.REJECTED -> "Отклонено"
                null -> "Ожидает рассмотрения"
            }
            statusTextView.text = statusText
            
            // Цвет статуса
            val statusColor = when (submission.reviewStatus) {
                DraftSubmission.ReviewStatus.PENDING -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                DraftSubmission.ReviewStatus.APPROVED -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                DraftSubmission.ReviewStatus.REJECTED -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                null -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            }
            statusTextView.setTextColor(statusColor)
            
            // Цвет обводки карточки
            cardView.strokeColor = statusColor
            cardView.strokeWidth = 4
            
            // Кнопки действий (только для входящих и не рассмотренных)
            val showActionButtons = isIncoming && !submission.isReviewed
            approveButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            rejectButton.visibility = if (showActionButtons) View.VISIBLE else View.GONE
            
            // Обработчики кликов
            itemView.setOnClickListener { onItemClick(submission) }
            approveButton.setOnClickListener { onApproveClick(submission) }
            rejectButton.setOnClickListener { onRejectClick(submission) }
        }
    }

    private class DraftSubmissionDiffCallback : DiffUtil.ItemCallback<DraftSubmission>() {
        override fun areItemsTheSame(oldItem: DraftSubmission, newItem: DraftSubmission): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DraftSubmission, newItem: DraftSubmission): Boolean {
            return oldItem == newItem
        }
    }
} 