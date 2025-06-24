package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class DraftSubmission(
    val id: Long,
    val draft: FamilyTreeDraft,
    val message: String?,
    @SerializedName("submitted_at")
    val submittedAt: String,
    @SerializedName("is_reviewed")
    val isReviewed: Boolean = false,
    @SerializedName("reviewed_at")
    val reviewedAt: String?,
    @SerializedName("review_message")
    val reviewMessage: String?,
    @SerializedName("review_status")
    val reviewStatus: ReviewStatus? = ReviewStatus.PENDING
) {
    enum class ReviewStatus {
        PENDING,    // Ожидает рассмотрения
        APPROVED,   // Одобрено
        REJECTED    // Отклонено
    }
}

data class RespondToSubmissionRequest(
    val approved: Boolean,
    val reviewMessage: String?
) 