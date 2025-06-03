package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.ui.genealogy.GenealogyTreeScreen
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel

@AndroidEntryPoint
class GenealogyTreeFragment : Fragment() {

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { 
        arguments?.getLong("treeId") ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.loadGenealogyData(treeId)
        return ComposeView(requireContext()).apply {
            setContent {
                GenealogyTreeWithToolbar()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GenealogyTreeWithToolbar() {
                val tree by viewModel.familyTree.observeAsState()
                val relations by viewModel.memorialRelations.observeAsState(emptyList())
        val isLoading by viewModel.isLoading.observeAsState(false)
        val error by viewModel.error.observeAsState()
        
                val memorials = relations.flatMap { listOf(it.sourceMemorial, it.targetMemorial) }.distinctBy { it.id }

        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель с кнопкой назад
            TopAppBar(
                title = {
                    Text(text = tree?.name ?: "Генеалогическое дерево")
                },
                navigationIcon = {
                    IconButton(onClick = { findNavController().navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )

            // Контент
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        // Показываем загрузку
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "Загрузка генеалогического дерева...")
                            }
                        }
                    }
                    error != null -> {
                        // Показываем ошибку
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Ошибка загрузки",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error ?: "Неизвестная ошибка",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadGenealogyData(treeId) }
                                ) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                    else -> {
                        // Показываем дерево
                GenealogyTreeScreen(
                    memorials = memorials,
                    relations = relations
                )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
} 