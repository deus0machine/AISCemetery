package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationType

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val testNotifications = listOf(
        Notification(
            id = 1,
            userId = 1,
            type = NotificationType.TREE_ACCESS_REQUEST,
            title = "Новый запрос на доступ",
            message = "Пользователь Иван Иванов запрашивает доступ к вашему древу 'Род Ивановых'",
            relatedId = 1,
            isRead = false,
            createdAt = "2024-03-20"
        ),
        Notification(
            id = 2,
            userId = 1,
            type = NotificationType.MEMORIAL_COMMENT,
            title = "Новый комментарий",
            message = "Пользователь Петр Петров оставил комментарий к мемориалу",
            relatedId = 2,
            isRead = false,
            createdAt = "2024-03-20"
        ),
        Notification(
            id = 3,
            userId = 1,
            type = NotificationType.ANNIVERSARY,
            title = "Годовщина",
            message = "Завтра годовщина памяти Ивана Ивановича",
            relatedId = 1,
            isRead = true,
            createdAt = "2024-03-19"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_notifications)
        recyclerView.layoutManager = LinearLayoutManager(context)
        // TODO: Создать и установить адаптер для уведомлений

        // Временно отображаем тестовые данные
        val testDataView = view.findViewById<TextView>(R.id.text_test_data)
        testDataView.text = testNotifications.joinToString("\n\n") { notification ->
            "${if (!notification.isRead) "🔵 " else ""}${notification.title}\n" +
            "${notification.message}\n" +
            "Тип: ${notification.type.name}"
        }
    }
} 