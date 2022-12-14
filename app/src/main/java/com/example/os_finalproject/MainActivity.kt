package com.example.os_finalproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.os_finalproject.Data.RoomInfoRes
import com.example.os_finalproject.adapter.ViewPager2Adapter
import com.example.os_finalproject.databinding.ActivityMainBinding
import com.example.os_finalproject.fragment.ChatRoomListFragment
import com.example.os_finalproject.fragment.HomePageFragment
import com.example.os_finalproject.tool.*
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity(), Observer {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ViewPager2Adapter
    private val fragments = arrayOf<Fragment>(
        HomePageFragment(),
        ChatRoomListFragment()
    )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RequestCode_Permission -> {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        when {
                            permissions.any { it == PERMISSION_CAMERA } ->
                                showSnackBar("Failed to navigate to the camera since the camera permission is not granted")

                            permissions.any { it == PERMISSION_RECORD_AUDIO } ->
                                showSnackBar("Failed to call since the record audio permission is not granted")
                        }
                        return
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val permission = arrayOf(PERMISSION_RECORD_AUDIO, PERMISSION_CAMERA)
        Method.requestPermission(this, *permission)

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
        Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
    }

    private fun showSnackBar(msg: String) {
        val snackBar = Snackbar.make(binding.root, msg, 3000)

        val tvText = snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tvText.setOnClickListener { snackBar.dismiss() }
        tvText.setPadding(0, 0, 0, 0)
        tvText.textSize = 13f
        tvText.maxLines = 4

        snackBar.setActionTextColor(getColor(R.color.blue_8AD1E9))
        snackBar.setAction("go to") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        snackBar.view.alpha = 0.9f
        snackBar.show()
    }

    override fun update(p0: Observable?, arg: Any?) {
        when (arg) {
            is RoomInfoRes -> {

            }
        }
    }
}