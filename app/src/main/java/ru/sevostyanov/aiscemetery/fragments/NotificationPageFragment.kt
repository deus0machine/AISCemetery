package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.NotificationsAdapter
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel

class NotificationPageFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: NotificationsAdapter
    
    private var isIncoming: Boolean = true

    companion object {
        private const val ARG_IS_INCOMING = "is_incoming"
        
        fun newInstance(isIncoming: Boolean): NotificationPageFragment {
            return NotificationPageFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_INCOMING, isIncoming)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isIncoming = it.getBoolean(ARG_IS_INCOMING, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        emptyTextView = view.findViewById(R.id.text_empty)
        
        setupRecyclerView()
        setupObservers()
        
        // Load notifications
        if (isIncoming) {
            viewModel.loadIncomingNotifications()
        } else {
            viewModel.loadSentNotifications()
        }
    }

    private fun setupRecyclerView() {
        // Добавляем обработчики принятия/отклонения только для входящих
        adapter = if (isIncoming) {
            NotificationsAdapter(
                isIncoming = true,
                onAcceptClick = { notification ->
                    handleAcceptClick(notification)
                },
                onRejectClick = { notification ->
                    handleRejectClick(notification)
                },
                onItemClick = { notification ->
                    handleItemClick(notification)
                }
            )
        } else {
            NotificationsAdapter(
                isIncoming = false,
                onItemClick = { notification ->
                    // Для исходящих просто логируем клик
                    // Можно добавить дополнительную логику если нужно
                }
            )
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationPageFragment.adapter
        }
    }

    private fun handleItemClick(notification: Notification) {
        // Если уведомление еще не прочитано, отмечаем его как прочитанное
        if (!notification.isRead) {
            viewModel.markAsRead(notification.id)
        }
    }

    private fun handleAcceptClick(notification: Notification) {
        viewModel.respondToNotification(notification.id, true)
        Toast.makeText(context, "Запрос принят", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleRejectClick(notification: Notification) {
        viewModel.respondToNotification(notification.id, false)
        Toast.makeText(context, "Запрос отклонен", Toast.LENGTH_SHORT).show()
    }

    private fun setupObservers() {
        if (isIncoming) {
            viewModel.incomingNotifications.observe(viewLifecycleOwner) { notifications ->
                adapter.submitList(notifications)
                updateEmptyView(notifications.isEmpty())
            }
        } else {
            viewModel.sentNotifications.observe(viewLifecycleOwner) { notifications ->
                adapter.submitList(notifications)
                updateEmptyView(notifications.isEmpty())
            }
        }
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        emptyTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
} 