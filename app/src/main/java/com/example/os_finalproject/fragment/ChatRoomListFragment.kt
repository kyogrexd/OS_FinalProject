package com.example.os_finalproject.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.Data.RoomInfoRes
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.RTCActivity
import com.example.os_finalproject.adapter.ListAdapter
import com.example.os_finalproject.databinding.FragmentChatroomListBinding
import com.example.os_finalproject.tool.DataManager
import com.example.os_finalproject.tool.ServerUrl
import com.example.os_finalproject.tool.SocketManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class ChatRoomListFragment: Fragment(), Observer {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentChatroomListBinding? = null

    private lateinit var adapter: ListAdapter
    private lateinit var viewModel: DataViewModel
    private var userName = ""
    private var socketID = ""
    private var roomInfoList = ArrayList<RoomInfoRes.RoomInfoList>()
    private var timer = Timer()
    private val TAG = "ChatRoomListFragment"

    val db = Firebase.firestore

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
        DataManager.instance.addObserver(this)
        DataManager.instance.doRoomInfo()

        viewModel = ViewModelProvider(mActivity)[DataViewModel::class.java]
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            userName = name
            binding?.tvName?.text = "Name: $name"

            startSocket()
        }
        setListener()
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        DataManager.instance.addObserver(this)
    }

    override fun onStop() {
        super.onStop()
        DataManager.instance.deleteObserver(this)
    }

    fun startSocket() {
        SocketManager.instance.run {
//            connectUrl(""https://webrtc.haowei.space"")
            connectUrl("$ServerUrl:8500/")

            on("connected") {
                mActivity.runOnUiThread {
                    socketID = it.getString("socketID")
                }
            }
        }
    }

    private fun setListener() {
        binding?.clCreate?.setOnClickListener {
            val uuid = UUID.randomUUID().toString()
            if (socketID.isNotEmpty()) {
                val b = Bundle()
                b.putBoolean("IsCreate", true)
                b.putString("Uuid", uuid)
                b.putString("SocketID", socketID)
                b.putString("UserName", userName)
                val intent = Intent(mActivity, RTCActivity::class.java).putExtras(b)
                mActivity.resultLauncher.launch(intent)
            }
        }
    }

    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(mActivity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        val list = arrayListOf("ChatRoom 1", "ChatRoom 2", "ChatRoom 3")

        adapter = ListAdapter(mActivity, roomInfoList)
        adapter.setListener(object : ListAdapter.ClickListener {
            override fun onEnterChatRoom(item: RoomInfoRes.RoomInfoList) {
                val uuid = UUID.randomUUID().toString()
                val b = Bundle()
                b.putString("RoomID", item.roomID)
                b.putString("SocketID", socketID)
                b.putString("Uuid", uuid)
                b.putString("UserName", userName)
                val intent = Intent(mActivity, RTCActivity::class.java).putExtras(b)
                mActivity.resultLauncher.launch(intent)
            }
        })
        binding?.rcList?.adapter = adapter
        binding?.rcList?.layoutManager = layoutManager
    }

    fun setUpdateTimer() {
        var count = 5
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                count --
                mActivity.runOnUiThread {
                    binding?.tvTimer?.text = "Next Update : ${count}s"
                }
                if (count == 0) {
                    count = 5
                    DataManager.instance.doRoomInfo()
                }
            }
        }, 0, 1000)
    }

    fun cancelTimer() {
        timer.cancel()
        timer.purge()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        timer.cancel()
        timer.purge()
    }

    override fun update(p0: Observable?, arg: Any?) {
        when (arg) {
            is RoomInfoRes -> {
                mActivity.runOnUiThread {
                    roomInfoList.clear()
                    roomInfoList.addAll(arg.result.roomInfoList)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}