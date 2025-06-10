package ru.sevostyanov.aiscemetery.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.sevostyanov.aiscemetery.fragments.NotificationPageFragment

class NotificationPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationPageFragment.newInstance(true)  // Входящие
            1 -> NotificationPageFragment.newInstance(false) // Исходящие
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 