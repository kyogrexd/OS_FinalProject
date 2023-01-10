package com.example.os_finalproject.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.tool.Method
import com.example.os_finalproject.databinding.FragmentHomepageBinding
import com.example.os_finalproject.tool.DataManager
import com.example.os_finalproject.tool.PERMISSION_CAMERA
import com.example.os_finalproject.tool.PERMISSION_RECORD_AUDIO

class HomePageFragment: Fragment() {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentHomepageBinding? = null

    private var isDevelop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomepageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListener()
        changeVersion()
    }

    private fun setListener() {
        binding?.run {
            val permission = arrayOf(PERMISSION_RECORD_AUDIO, PERMISSION_CAMERA)

            tvSend.setOnClickListener {
                if (edName.text.isEmpty() || edName.text.isBlank()) {
                    Toast.makeText(mActivity, "Please enter your name !", Toast.LENGTH_SHORT).show()
                } else if (!Method.requestPermission(mActivity, *permission)) {

                } else {
                    val viewModel = ViewModelProvider(mActivity).get(DataViewModel::class.java)
                    viewModel.updateUserName(binding?.edName?.text.toString())

                    Method.hideKeyBoard(mActivity)
                    mActivity.setPage()
                }
            }

            val gd = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    isDevelop = !isDevelop
                    changeVersion()
                    return super.onDoubleTap(e)
                }

                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }
            })

            imageView2.setOnTouchListener { view, event -> gd.onTouchEvent(event) }
        }
    }

    fun changeVersion() {
        if (isDevelop) {
            binding?.tvVersion?.text = "develop"
            DataManager.instance.ServerUrl = "http://140.124.73.7:8500/"
            DataManager.instance.APIUrl = "http://140.124.73.7:8000/"
        } else {
            binding?.tvVersion?.text = "production"
            DataManager.instance.ServerUrl = "https://webrtc.haowei.space"
            DataManager.instance.APIUrl = "https://api.haowei.space/"
        }
    }
}