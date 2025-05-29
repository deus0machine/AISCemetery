package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.NotificationPagerAdapter
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: NotificationPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.view_pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        
        setupViewPager()
        setupObservers()
        
        // Загружаем уведомления при создании фрагмента
        refreshNotifications()
        
        // Проверяем, нужно ли переключиться на конкретную вкладку
        activity?.intent?.getIntExtra("tab_position", -1)?.let { position ->
            if (position in 0..1) {
                viewPager.currentItem = position
                
                // Сбрасываем аргумент, чтобы не переключаться повторно при пересоздании
                activity?.intent?.removeExtra("tab_position")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем списки уведомлений при возвращении к экрану
        refreshNotifications()
    }
    
    private fun refreshNotifications() {
        // Загружаем оба типа уведомлений
        viewModel.loadIncomingNotifications()
        viewModel.loadSentNotifications()
    }

    private fun setupViewPager() {
        pagerAdapter = NotificationPagerAdapter(requireActivity())
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_incoming)
                1 -> getString(R.string.tab_outgoing)
                else -> null
            }
        }.attach()
    }
    
    private fun setupObservers() {
        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                
                // Если ошибка связана с сервером, предлагаем пользователю повторить загрузку
                if (errorMessage.contains("500") || 
                    errorMessage.contains("сервера") || 
                    errorMessage.contains("Ошибка загрузки")) {
                    
                    // Отображаем кнопку повторной загрузки
                    showRetryButton()
                }
            }
        }
        
        // Наблюдаем за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view?.findViewById<View>(R.id.loading_indicator)?.visibility = 
                if (isLoading) View.VISIBLE else View.GONE
            
            // Скрываем кнопку повторной загрузки при начале загрузки
            if (isLoading) {
                view?.findViewById<View>(R.id.btn_retry)?.visibility = View.GONE
            }
        }
    }
    
    private fun showRetryButton() {
        val retryButton = view?.findViewById<View>(R.id.btn_retry)
        if (retryButton == null) {
            // Если кнопки нет в макете, добавляем её программно
            val button = android.widget.Button(requireContext())
            button.id = R.id.btn_retry
            button.text = "Повторить загрузку"
            button.setOnClickListener {
                retryLoadNotifications()
            }
            
            // Находим родительский контейнер и добавляем кнопку
            val container = view?.findViewById<ViewGroup>(R.id.container)
            container?.addView(button)
        } else {
            retryButton.visibility = View.VISIBLE
            retryButton.setOnClickListener {
                retryLoadNotifications()
            }
        }
    }
    
    private fun retryLoadNotifications() {
        // Очищаем прошлую ошибку
        (viewModel.error as? MutableLiveData)?.value = null
        
        // Повторно загружаем данные
        viewModel.loadIncomingNotifications()
        viewModel.loadSentNotifications()
    }
} 