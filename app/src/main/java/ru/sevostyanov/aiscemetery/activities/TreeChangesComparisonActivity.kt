package ru.sevostyanov.aiscemetery.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.TreeConnectionsAdapter
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import ru.sevostyanov.aiscemetery.models.FamilyTreeConnection
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.MemorialRelation

@AndroidEntryPoint
class TreeChangesComparisonActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ORIGINAL_TREE_ID = "original_tree_id"
        private const val EXTRA_DRAFT_TREE_ID = "draft_tree_id"
        private const val EXTRA_TREE_NAME = "tree_name"
        private const val EXTRA_IS_DRAFT = "is_draft"
        private const val EXTRA_RELATIONS_JSON = "relations_json"

        fun newIntent(
            context: Context,
            originalTreeId: Long,
            draftTreeId: Long,
            treeName: String,
            isDraft: Boolean = false,
            relationsJson: String? = null
        ): Intent {
            return Intent(context, TreeChangesComparisonActivity::class.java).apply {
                putExtra(EXTRA_ORIGINAL_TREE_ID, originalTreeId)
                putExtra(EXTRA_DRAFT_TREE_ID, draftTreeId)
                putExtra(EXTRA_TREE_NAME, treeName)
                putExtra(EXTRA_IS_DRAFT, isDraft)
                putExtra(EXTRA_RELATIONS_JSON, relationsJson)
            }
        }
    }

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    
    private lateinit var toolbar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var statusTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateTextView: TextView
    
    private lateinit var connectionsAdapter: TreeConnectionsAdapter
    
    private val originalTreeId: Long by lazy { intent.getLongExtra(EXTRA_ORIGINAL_TREE_ID, -1L) }
    private val draftTreeId: Long by lazy { intent.getLongExtra(EXTRA_DRAFT_TREE_ID, -1L) }
    private val treeName: String by lazy { intent.getStringExtra(EXTRA_TREE_NAME) ?: "Дерево" }
    private val isDraft: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_DRAFT, false) }
    private val relationsJson: String? by lazy { intent.getStringExtra(EXTRA_RELATIONS_JSON) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_changes_comparison)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        loadTreeConnections()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        appBarLayout = findViewById(R.id.app_bar_layout)
        statusTextView = findViewById(R.id.status_text_view)
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        emptyStateTextView = findViewById(R.id.empty_state_text_view)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Связи в дереве"
        }
        
        statusTextView.text = "Связи в дереве: $treeName"
    }

    private fun setupRecyclerView() {
        connectionsAdapter = TreeConnectionsAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TreeChangesComparisonActivity)
            adapter = connectionsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.familyTree.observe(this) { tree ->
            tree?.let {
                Log.d("TreeConnections", "Дерево загружено: ${it.name}, связей: ${it.memorialRelations?.size ?: 0}")
                
                // Преобразуем MemorialRelation в FamilyTreeConnection для отображения
                val connections = it.memorialRelations?.map { relation ->
                    FamilyTreeConnection(
                        id = relation.id,
                        relationType = relation.relationType,
                        sourcePersonName = relation.sourceMemorial.fio,
                        targetPersonName = relation.targetMemorial.fio,
                        details = null
                    )
                } ?: emptyList()
                
                if (connections.isNotEmpty()) {
                    connectionsAdapter.updateConnections(connections)
                    showContent()
                } else {
                    showEmptyState("В этом дереве пока нет связей между персонами")
                }
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Log.e("TreeConnections", "Ошибка: $it")
                showEmptyState("Ошибка загрузки связей: $it")
                Toast.makeText(this, "Ошибка: $it", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showProgress(isLoading)
        }
    }

    private fun loadTreeConnections() {
        Log.d("TreeConnections", "Загрузка связей для дерева: $originalTreeId, isDraft: $isDraft")
        
        // Проверяем валидность ID
        if (originalTreeId <= 0) {
            showEmptyState("Неверный ID дерева")
            return
        }
        
        lifecycleScope.launch {
            try {
                if (isDraft) {
                    // Загружаем связи черновика
                    loadDraftConnections()
                } else {
                    // Загружаем связи оригинального дерева
                    viewModel.loadFamilyTree(originalTreeId)
                }
            } catch (e: Exception) {
                Log.e("TreeConnections", "Ошибка загрузки связей: ${e.message}", e)
                showEmptyState("Ошибка загрузки связей: ${e.message}")
                Toast.makeText(this@TreeChangesComparisonActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadDraftConnections() {
        try {
            showProgress(true)
            Log.d("TreeConnections", "Загружаем связи черновика для дерева: $originalTreeId")
            
            val relations = RetrofitClient.getApiService().getDraftRelationsByDraftId(draftTreeId)
            Log.d("TreeConnections", "Получены связи черновика: ${relations.size}")
            
            // Преобразуем MemorialRelation в FamilyTreeConnection для отображения
            val connections = relations.map { relation ->
                FamilyTreeConnection(
                    id = relation.id,
                    relationType = relation.relationType,
                    sourcePersonName = relation.sourceMemorial.fio,
                    targetPersonName = relation.targetMemorial.fio,
                    details = null
                )
            }
            
            if (connections.isNotEmpty()) {
                connectionsAdapter.updateConnections(connections)
                showContent()
            } else {
                showEmptyState("В этом черновике пока нет связей между персонами")
            }
            
        } catch (e: Exception) {
            Log.e("TreeConnections", "Ошибка загрузки связей черновика: ${e.message}", e)
            showEmptyState("Ошибка загрузки связей черновика: ${e.message}")
            Toast.makeText(this@TreeChangesComparisonActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            showProgress(false)
        }
    }

    private fun parseRelationsFromJson(json: String): List<MemorialRelation> {
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<MemorialRelation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("TreeConnections", "Ошибка парсинга JSON связей: ${e.message}", e)
            emptyList()
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateTextView.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        emptyStateTextView.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyStateTextView.visibility = View.VISIBLE
        emptyStateTextView.text = message
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 