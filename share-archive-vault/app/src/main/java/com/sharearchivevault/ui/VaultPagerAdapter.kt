package com.sharearchivevault.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for the Photos / Videos tabs.
 */
class VaultPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaFragment.newInstance(isVideo = false)  // Photos tab
            1 -> MediaFragment.newInstance(isVideo = true)   // Videos tab
            else -> throw IllegalArgumentException("Unknown tab position: $position")
        }
    }
}
