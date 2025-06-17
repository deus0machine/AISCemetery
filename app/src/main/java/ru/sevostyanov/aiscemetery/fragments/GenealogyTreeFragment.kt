package ru.sevostyanov.aiscemetery.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.activities.FamilyTreeDraftActivity
import ru.sevostyanov.aiscemetery.ui.genealogy.GenealogyTreeScreen
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel
import ru.sevostyanov.aiscemetery.models.RelationType
import ru.sevostyanov.aiscemetery.models.Memorial

@AndroidEntryPoint
class GenealogyTreeFragment : Fragment() {

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { 
        val id = arguments?.getLong("familyTreeId") ?: arguments?.getLong("treeId") ?: -1L
        android.util.Log.d("GenealogyTreeFragment", "Получен treeId из аргументов: $id")
        id
    }
    private val isDraft: Boolean by lazy {
        arguments?.getBoolean("isDraft", false) ?: false
    }
    private val hasAccess: Boolean by lazy {
        arguments?.getBoolean("hasAccess", false) ?: false
    }
    private val refreshData: Boolean by lazy {
        arguments?.getBoolean("refreshData", false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        loadTreeData()
        return ComposeView(requireContext()).apply {
            setContent {
                GenealogyTreeWithToolbar()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Если установлен флаг refreshData, принудительно обновляем данные
        if (refreshData) {
            loadTreeData()
        }
    }
    
    private fun loadTreeData() {
        if (isDraft) {
            viewModel.loadDraftGenealogyData(treeId)
        } else {
            viewModel.loadGenealogyData(treeId)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GenealogyTreeWithToolbar() {
        val tree by viewModel.familyTree.observeAsState()
        val relations by viewModel.memorialRelations.observeAsState(emptyList())
        val availableMemorials by viewModel.availableMemorials.observeAsState(emptyList())
        val isLoading by viewModel.isLoading.observeAsState(false)
        val error by viewModel.error.observeAsState()
        
        val memorials = if (isDraft) {
            // Для черновиков используем availableMemorials
            availableMemorials
        } else {
            // Для обычных деревьев извлекаем из связей
            val memorialSet = mutableSetOf<Memorial>()
            relations.forEach { relation ->
                if (relation.relationType == RelationType.PLACEHOLDER) {
                    // Для PLACEHOLDER связей добавляем только источник (он же и цель)
                    // чтобы избежать дублирования одного и того же мемориала
                    memorialSet.add(relation.sourceMemorial)
                } else {
                    // Для обычных связей добавляем оба мемориала
                    memorialSet.add(relation.sourceMemorial)
                    memorialSet.add(relation.targetMemorial)
                }
            }
            memorialSet.toList()
        }

        // Проверяем права доступа пользователя
        val canEdit = tree?.canUserEdit() == true
        val isOwner = tree?.isUserOwner == true

        var showAccessRequestDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Toolbar
            TopAppBar(
                title = { Text(tree?.name ?: "Генеалогическое дерево") },
                navigationIcon = {
                    IconButton(onClick = { 
                        findNavController().navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Основной контент
                Box(modifier = Modifier.weight(1f)) {
                    GenealogyTreeScreen(
                        memorials = memorials,
                        relations = relations
                    )
                }

                // Кнопки в зависимости от прав доступа
                when {
                    // Пользователи без доступа видят кнопку запроса доступа
                    // НО НЕ для черновиков - если пользователь видит черновик, значит у него уже есть доступ
                    // И НЕ для владельцев - владелец не может запросить доступ к своему дереву
                    !hasAccess && !isDraft && !isOwner -> {
                        Button(
                            onClick = { 
                                showAccessRequestDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Отправить запрос на совместное владение")
                        }
                    }
                    // Для всех остальных (включая редакторов в публичных деревьях и черновиков) - никаких кнопок
                }
            }
        }

        // Диалог запроса доступа
        if (showAccessRequestDialog) {
            AccessRequestDialog(
                onDismiss = { showAccessRequestDialog = false },
                onSendRequest = { message ->
                    sendAccessRequest(treeId, message)
                    showAccessRequestDialog = false
                }
            )
        }
    }

    private fun openDraftActivity() {
        val intent = Intent(requireContext(), FamilyTreeDraftActivity::class.java)
        intent.putExtra("FAMILY_TREE_ID", treeId)
        startActivity(intent)
    }

    private fun sendAccessRequest(treeId: Long, message: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = ru.sevostyanov.aiscemetery.models.FamilyTreeAccessRequest(
                    familyTreeId = treeId,
                    requestedAccessLevel = "EDITOR",
                    message = message
                )
                
                val response = RetrofitClient.getApiService().requestFamilyTreeAccess(treeId, request)
                
                Toast.makeText(
                    requireContext(),
                    "Запрос на доступ отправлен владельцу дерева",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 200) {
                    // Успешный ответ, но проблема с десериализацией
                    Toast.makeText(
                        requireContext(),
                        "Запрос на доступ отправлен владельцу дерева",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка при отправке запроса: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("JSON") == true -> "Запрос отправлен успешно"
                    e.message?.contains("You already have access") == true -> "У вас уже есть доступ к этому дереву"
                    else -> "Ошибка при отправке запроса: ${e.message}"
                }
                
                Toast.makeText(
                    requireContext(),
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Composable
    private fun AccessRequestDialog(
        onDismiss: () -> Unit,
        onSendRequest: (String) -> Unit
    ) {
        var message by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Запрос на совместное владение") },
            text = {
                Column {
                    Text(
                        "Отправить запрос владельцу дерева на предоставление прав редактирования?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Сообщение (необязательно)") },
                        placeholder = { Text("Укажите причину запроса...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSendRequest(message) }
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
} 