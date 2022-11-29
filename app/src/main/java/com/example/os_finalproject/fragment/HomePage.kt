package com.example.os_finalproject.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.adapter.ListAdapter
import com.example.os_finalproject.databinding.FragmentHomepageBinding

class HomePage: Fragment() {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentHomepageBinding? = null

    private lateinit var adapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomepageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = arrayListOf<String>("ChatRoom 1", "ChatRoom 2", "ChatRoom 3")

        adapter = ListAdapter(list)
        binding?.rcList?.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}