package com.example.os_finalproject.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.adapter.ListAdapter
import com.example.os_finalproject.databinding.FragmentChatroomListBinding

class ChatRoomListFragment: Fragment() {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentChatroomListBinding? = null

    private lateinit var adapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatroomListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(mActivity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        val list = arrayListOf<String>("ChatRoom 1", "ChatRoom 2", "ChatRoom 3")

        adapter = ListAdapter(list)
        adapter.setListener(object : ListAdapter.ClickListener {
            override fun onEnterChatRoom(item: String) {
                Toast.makeText(mActivity, item, Toast.LENGTH_SHORT).show()
            }
        })
        binding?.rcList?.adapter = adapter
        binding?.rcList?.layoutManager = layoutManager
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}