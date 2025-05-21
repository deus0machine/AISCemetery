package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.adapters.NotificationPagerAdapter
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private val viewModel: NotificationsViewModel by viewModels()
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
} 