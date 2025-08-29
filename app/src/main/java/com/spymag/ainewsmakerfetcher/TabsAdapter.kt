package com.spymag.ainewsmakerfetcher

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabsAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val reportsFragment = ReportsFragment()
    private val actionsFragment = ActionsFragment()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> reportsFragment
            1 -> actionsFragment
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun refreshCurrentData(position: Int) {
        when (position) {
            0 -> reportsFragment.refreshData()
            1 -> actionsFragment.refreshData()
        }
    }
}