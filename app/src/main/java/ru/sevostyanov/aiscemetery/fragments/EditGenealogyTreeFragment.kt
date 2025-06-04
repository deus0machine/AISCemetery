package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.AvailableMemorialAdapter
import ru.sevostyanov.aiscemetery.adapters.MemorialRelationAdapter
import ru.sevostyanov.aiscemetery.adapters.TreeMemorialAdapter
import ru.sevostyanov.aiscemetery.databinding.FragmentEditGenealogyTreeBinding
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType
import ru.sevostyanov.aiscemetery.viewmodels.EditGenealogyTreeViewModel

@AndroidEntryPoint
class EditGenealogyTreeFragment : Fragment() {

    private var _binding: FragmentEditGenealogyTreeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditGenealogyTreeViewModel by viewModels()
    
    private lateinit var treeMemorialAdapter: TreeMemorialAdapter
    private lateinit var availableMemorialAdapter: AvailableMemorialAdapter
    private lateinit var relationAdapter: MemorialRelationAdapter
    
    private val treeId: Long by lazy { 
        arguments?.getLong("treeId") ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditGenealogyTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters()
        setupTabLayout()
        setupObservers()
        setupClickListeners()
        
        // Загружаем данные
        if (treeId != -1L) {
            viewModel.loadFamilyTree(treeId)
        } else {
            showError("Ошибка: ID дерева не найден")
        }
    }

    private fun setupTabLayout() {
        // Устанавливаем начальные заголовки с нулевыми счетчиками
        val tabTitles = arrayOf(
            "В дереве (0)",
            "Доступно (0)", 
            "Связи (0)"
        )
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun setupAdapters() {
        // Адаптер для мемориалов в дереве
        treeMemorialAdapter = TreeMemorialAdapter(
            onRemoveClick = { memorial ->
                showRemoveMemorialDialog(memorial)
            },
            onCreateRelationClick = { memorial ->
                showCreateRelationDialog(memorial)
            }
        )
        
        // Адаптер для доступных мемориалов
        availableMemorialAdapter = AvailableMemorialAdapter(
            onAddClick = { memorial ->
                viewModel.addMemorialToTree(memorial.id!!)
            }
        )
        
        // Адаптер для связей
        relationAdapter = MemorialRelationAdapter(
            onEditClick = { relation ->
                showEditRelationDialog(relation)
            },
            onDeleteClick = { relation ->
                showDeleteRelationDialog(relation)
            }
        )

        // Настраиваем ViewPager2 ПЕРЕД TabLayoutMediator
        binding.viewPager.adapter = EditTreePagerAdapter()
    }

    private fun setupObservers() {
        viewModel.familyTree.observe(viewLifecycleOwner) { tree ->
            binding.textTreeName.text = tree.name
        }

        viewModel.treeMemorials.observe(viewLifecycleOwner) { memorials ->
            treeMemorialAdapter.submitList(memorials)
            updateAvailableMemorialsList()
            updateMemorialCounts()
        }

        viewModel.availableMemorials.observe(viewLifecycleOwner) { memorials ->
            updateAvailableMemorialsList()
            updateMemorialCounts()
        }

        viewModel.memorialRelations.observe(viewLifecycleOwner) { relations ->
            relationAdapter.submitList(relations)
            treeMemorialAdapter.updateRelations(relations)
            updateRelationCount(relations.size)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(context, "Операция выполнена успешно", Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.fabAddRelation.setOnClickListener {
            val treeMemorials = viewModel.treeMemorials.value
            if (treeMemorials.isNullOrEmpty() || treeMemorials.size < 2) {
                showError("Добавьте минимум 2 мемориала в дерево для создания связей")
                return@setOnClickListener
            }
            showCreateRelationDialog()
        }
    }

    private fun showCreateRelationDialog(preselectedMemorial: Memorial? = null) {
        val treeMemorials = viewModel.treeMemorials.value ?: return
        
        CreateRelationDialogFragment.newInstance(
            memorials = treeMemorials,
            preselectedMemorial = preselectedMemorial,
            onRelationCreated = { source, target, type ->
                viewModel.createMemorialRelation(source, target, type)
            }
        ).show(parentFragmentManager, "create_relation")
    }

    private fun showEditRelationDialog(relation: MemorialRelation) {
        val treeMemorials = viewModel.treeMemorials.value ?: return
        
        CreateRelationDialogFragment.newInstance(
            memorials = treeMemorials,
            existingRelation = relation,
            onRelationCreated = { _, _, _ -> 
                // Не используется при редактировании
            },
            onRelationUpdated = { existingRelation, source, target, type ->
                viewModel.updateMemorialRelation(existingRelation.id, source, target, type)
            }
        ).show(parentFragmentManager, "edit_relation")
    }

    private fun showDeleteRelationDialog(relation: MemorialRelation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить связь")
            .setMessage("Вы уверены, что хотите удалить связь между ${relation.sourceMemorial.fio} и ${relation.targetMemorial.fio}?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteMemorialRelation(relation.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showRemoveMemorialDialog(memorial: Memorial) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить мемориал")
            .setMessage("Вы уверены, что хотите удалить ${memorial.fio} из дерева? Все связи с этим мемориалом также будут удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.removeMemorialFromTree(memorial.id!!)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateMemorialCounts() {
        val treeCount = viewModel.treeMemorials.value?.size ?: 0
        val treeMemorialIds = viewModel.treeMemorials.value?.map { it.id } ?: emptyList()
        val availableMemorials = viewModel.availableMemorials.value?.filter { it.id !in treeMemorialIds } ?: emptyList()
        val availableCount = availableMemorials.size
        
        // Обновляем заголовки табов
        binding.tabLayout.getTabAt(0)?.text = "В дереве ($treeCount)"
        binding.tabLayout.getTabAt(1)?.text = "Доступно ($availableCount)"
    }

    private fun updateRelationCount(count: Int) {
        binding.tabLayout.getTabAt(2)?.text = "Связи ($count)"
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(treeId: Long): EditGenealogyTreeFragment {
            return EditGenealogyTreeFragment().apply {
                arguments = Bundle().apply {
                    putLong("treeId", treeId)
                }
            }
        }
    }

    // Внутренний адаптер для ViewPager2
    private inner class EditTreePagerAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
        
        override fun getItemCount() = 3
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            val recyclerView = androidx.recyclerview.widget.RecyclerView(parent.context).apply {
                layoutManager = LinearLayoutManager(parent.context)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(recyclerView) {}
        }
        
        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            val recyclerView = holder.itemView as androidx.recyclerview.widget.RecyclerView
            when (position) {
                0 -> recyclerView.adapter = treeMemorialAdapter
                1 -> recyclerView.adapter = availableMemorialAdapter
                2 -> recyclerView.adapter = relationAdapter
            }
        }
    }

    private fun updateAvailableMemorialsList() {
        val allMemorials = viewModel.availableMemorials.value ?: emptyList()
        val treeMemorialIds = viewModel.treeMemorials.value?.map { it.id } ?: emptyList()
        val available = allMemorials.filter { it.id !in treeMemorialIds }
        availableMemorialAdapter.submitList(available)
    }
} 