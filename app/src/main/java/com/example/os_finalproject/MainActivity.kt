package com.example.os_finalproject

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.os_finalproject.adapter.ViewPager2Adapter
import com.example.os_finalproject.databinding.ActivityMainBinding
import com.example.os_finalproject.fragment.ChatRoomListFragment
import com.example.os_finalproject.fragment.HomePageFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ViewPager2Adapter
    private val fragments = arrayOf<Fragment>(
        HomePageFragment(),
        ChatRoomListFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        adapter = ViewPager2Adapter(fragments, this)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(position: Int) {}

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(position: Int) {
                if (position == 0) binding.viewPager2.isUserInputEnabled = false
            }
        })
    }

    fun setPage() {
        binding.viewPager2.currentItem = 1
        binding.viewPager2.isUserInputEnabled = true
    }
}