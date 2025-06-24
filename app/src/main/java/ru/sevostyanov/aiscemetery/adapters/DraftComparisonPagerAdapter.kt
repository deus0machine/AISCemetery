package ru.sevostyanov.aiscemetery.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.sevostyanov.aiscemetery.fragments.FamilyTreeFragment

class DraftComparisonPagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val draftSubmissionId: Long
) : FragmentStateAdapter(fragmentActivity) {

    private var originalTreeId: Long = -1
    private var draftTreeId: Long = -1
    
    fun updateTreeIds(originalTreeId: Long, draftTreeId: Long) {
        this.originalTreeId = originalTreeId
        this.draftTreeId = draftTreeId
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                // Оригинальное дерево
                FamilyTreeFragment.newInstance(
                    treeId = originalTreeId,
                    isDraft = false
                )
            }
            1 -> {
                // Черновик
                FamilyTreeFragment.newInstance(
                    treeId = draftTreeId,
                    isDraft = true
                )
            }
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 