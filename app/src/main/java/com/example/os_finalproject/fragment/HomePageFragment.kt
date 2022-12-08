package com.example.os_finalproject.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.tool.Method
import com.example.os_finalproject.databinding.FragmentHomepageBinding
import com.example.os_finalproject.tool.PERMISSION_CAMERA
import com.example.os_finalproject.tool.PERMISSION_RECORD_AUDIO

class HomePageFragment: Fragment() {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentHomepageBinding? = null

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
        }
    }
}