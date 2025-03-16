package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.FamilyTree

class FamilyTreeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createTreeButton: FloatingActionButton
    private lateinit var searchTreeButton: Button
    private val testTrees = listOf(
        FamilyTree(1, "Род Ивановых", 1, true, "История рода с 1800 года", "2024-03-20", "2024-03-20"),
        FamilyTree(2, "Род Петровых", 1, false, "Семейная история", "2024-03-20", "2024-03-20"),
        FamilyTree(3, "Род Сидоровых", 2, true, "Наша семья", "2024-03-20", "2024-03-20")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация UI компонентов
        recyclerView = view.findViewById(R.id.recycler_view_trees)
        createTreeButton = view.findViewById(R.id.fab_create_tree)
        searchTreeButton = view.findViewById(R.id.btn_search_tree)

        // Настройка RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // TODO: Создать и установить адаптер для деревьев

        // Обработчики нажатий
        createTreeButton.setOnClickListener {
            // TODO: Открыть экран создания нового дерева
        }

        searchTreeButton.setOnClickListener {
            // TODO: Открыть экран поиска деревьев
        }

        // Временно отображаем тестовые данные
        val testDataView = view.findViewById<TextView>(R.id.text_test_data)
        testDataView.text = testTrees.joinToString("\n\n") { tree ->
            "Название: ${tree.name}\n" +
            "Статус: ${if (tree.isPublic) "Публичное" else "Приватное"}\n" +
            "Описание: ${tree.description}"
        }
    }
} 