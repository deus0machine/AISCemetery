package ru.sevostyanov.aiscemetery.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.DraftSubmission
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel

@AndroidEntryPoint
class DraftComparisonActivity : AppCompatActivity() {
    
    private val viewModel: NotificationsViewModel by viewModels()
    private var draftSubmissionId: Long = -1
    private var draftSubmission: DraftSubmission? = null
    
    // UI элементы
    private lateinit var toolbar: Toolbar
    private lateinit var contentLayout: View
    private lateinit var progressBar: ProgressBar
    private lateinit var textStatus: TextView
    private lateinit var textMessage: TextView
    private lateinit var textOriginalTreeName: TextView
    private lateinit var textOriginalTreeDescription: TextView
    private lateinit var textDraftTreeName: TextView
    private lateinit var textDraftTreeDescription: TextView
    private lateinit var buttonViewOriginalConnections: Button
    private lateinit var buttonViewDraftConnections: Button
    private lateinit var buttonApprove: Button
    private lateinit var buttonReject: Button
    
    companion object {
        private const val EXTRA_DRAFT_SUBMISSION_ID = "draft_submission_id"
        private const val TAG = "DraftComparisonActivity"
        
        fun start(context: Context, draftSubmissionId: Long) {
            val intent = Intent(context, DraftComparisonActivity::class.java).apply {
                putExtra(EXTRA_DRAFT_SUBMISSION_ID, draftSubmissionId)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft_comparison)
        
        draftSubmissionId = intent.getLongExtra(EXTRA_DRAFT_SUBMISSION_ID, -1)
        if (draftSubmissionId == -1L) {
            Log.e(TAG, "Invalid draft submission ID")
            finish()
            return
        }
        
        initViews()
        setupToolbar()
        setupObservers()
        loadDraftSubmission()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        contentLayout = findViewById(R.id.content_layout)
        progressBar = findViewById(R.id.progress_bar)
        textStatus = findViewById(R.id.text_status)
        textMessage = findViewById(R.id.text_message)
        textOriginalTreeName = findViewById(R.id.text_original_tree_name)
        textOriginalTreeDescription = findViewById(R.id.text_original_tree_description)
        textDraftTreeName = findViewById(R.id.text_draft_tree_name)
        textDraftTreeDescription = findViewById(R.id.text_draft_tree_description)
        buttonViewOriginalConnections = findViewById(R.id.button_view_original_connections)
        buttonViewDraftConnections = findViewById(R.id.button_view_draft_connections)
        buttonApprove = findViewById(R.id.button_approve)
        buttonReject = findViewById(R.id.button_reject)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Просмотр изменений"
        }
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupObservers() {
        viewModel.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e(TAG, "Error: $error")
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun loadDraftSubmission() {
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading draft submission with ID: $draftSubmissionId")
                
                // Загружаем данные о черновике
                draftSubmission = viewModel.getDraftSubmissionById(draftSubmissionId)
                
                if (draftSubmission == null) {
                    Toast.makeText(this@DraftComparisonActivity, "Не удалось загрузить данные о черновике", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                
                setupUI()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading draft submission", e)
                Toast.makeText(this@DraftComparisonActivity, "Ошибка загрузки данных", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun setupUI() {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        
        val submission = draftSubmission ?: return
        
        // Заполняем информацию о деревьях
        textOriginalTreeName.text = submission.draft.familyTree?.name ?: "Неизвестно"
        textOriginalTreeDescription.text = submission.draft.familyTree?.description ?: "Без описания"
        
        textDraftTreeName.text = submission.draft.draftName
        textDraftTreeDescription.text = submission.draft.draftDescription ?: "Без описания"
        
        // Настраиваем кнопки просмотра связей
        buttonViewOriginalConnections.setOnClickListener {
            val originalTreeId = submission.draft.familyTree?.id
            if (originalTreeId != null) {
                val intent = TreeChangesComparisonActivity.newIntent(
                    this,
                    originalTreeId = originalTreeId,
                    draftTreeId = submission.draft.id,
                    treeName = submission.draft.familyTree?.name ?: "Неизвестно",
                    isDraft = false // Оригинальное дерево
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Не удалось получить ID оригинального дерева", Toast.LENGTH_SHORT).show()
            }
        }
        
        buttonViewDraftConnections.setOnClickListener {
            val intent = TreeChangesComparisonActivity.newIntent(
                this,
                originalTreeId = submission.draft.familyTree?.id ?: 0L, // ID оригинального дерева для отображения
                draftTreeId = submission.draft.id,
                treeName = "${submission.draft.familyTree?.name ?: "Неизвестно"} (черновик)",
                isDraft = true // Черновик дерева
            )
            startActivity(intent)
        }
        
        setupActionButtons()
    }
    
    private fun setupActionButtons() {
        val showButtons = draftSubmission?.isReviewed != true
        
        buttonApprove.visibility = if (showButtons) View.VISIBLE else View.GONE
        buttonReject.visibility = if (showButtons) View.VISIBLE else View.GONE
        
        if (showButtons) {
            buttonApprove.setOnClickListener {
                showApproveDialog()
            }
            
            buttonReject.setOnClickListener {
                showRejectDialog()
            }
        }
        
        updateStatusInfo()
    }
    
    private fun updateStatusInfo() {
        val submission = draftSubmission
        if (submission != null) {
            val statusText = when (submission.reviewStatus) {
                DraftSubmission.ReviewStatus.PENDING -> "Ожидает рассмотрения"
                DraftSubmission.ReviewStatus.APPROVED -> "Одобрено"
                DraftSubmission.ReviewStatus.REJECTED -> "Отклонено"
                null -> "Ожидает рассмотрения"
            }
            
            textStatus.text = "Статус: $statusText"
            
            if (submission.message?.isNotEmpty() == true) {
                textMessage.text = "Сообщение: ${submission.message}"
                textMessage.visibility = View.VISIBLE
            } else {
                textMessage.visibility = View.GONE
            }
        }
    }

    private fun showApproveDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Одобрить изменения")
            .setMessage("Вы уверены, что хотите одобрить предложенные изменения? Они будут применены к оригинальному дереву.")
            .setPositiveButton("Одобрить") { _, _ ->
                approveDraft()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showRejectDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Отклонить изменения")
            .setMessage("Вы уверены, что хотите отклонить предложенные изменения?")
            .setPositiveButton("Отклонить") { _, _ ->
                rejectDraft()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun approveDraft() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Approving draft submission: $draftSubmissionId")
                viewModel.respondToDraftSubmission(draftSubmissionId, true, "Изменения одобрены")
                
                Toast.makeText(this@DraftComparisonActivity, "Изменения одобрены", Toast.LENGTH_SHORT).show()
                
                // Перезагружаем данные с сервера
                loadDraftSubmission()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error approving draft", e)
                Toast.makeText(this@DraftComparisonActivity, "Ошибка при одобрении: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun rejectDraft() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Rejecting draft submission: $draftSubmissionId")
                viewModel.respondToDraftSubmission(draftSubmissionId, false, "Изменения отклонены")
                
                Toast.makeText(this@DraftComparisonActivity, "Изменения отклонены", Toast.LENGTH_SHORT).show()
                
                // Перезагружаем данные с сервера
                loadDraftSubmission()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting draft", e)
                Toast.makeText(this@DraftComparisonActivity, "Ошибка при отклонении: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }
        }
    }
} 