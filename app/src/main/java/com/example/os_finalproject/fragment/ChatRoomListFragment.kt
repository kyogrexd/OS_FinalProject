package com.example.os_finalproject.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.Data.RoomInfoRes
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.R
import com.example.os_finalproject.RTCActivity
import com.example.os_finalproject.adapter.ListAdapter
import com.example.os_finalproject.databinding.DialogRoomInfoBinding
import com.example.os_finalproject.databinding.FragmentChatroomListBinding
import com.example.os_finalproject.tool.DataManager
import com.example.os_finalproject.tool.DialogManager
import com.example.os_finalproject.tool.ServerUrl
import com.example.os_finalproject.tool.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

            override fun onClickInfo(item: RoomInfoRes.RoomInfoList) {
                DialogManager.instance.showCustom(mActivity, DialogRoomInfoBinding.inflate(LayoutInflater.from(mActivity)).root)?.let {
                    val tvCount = it.findViewById<TextView>(R.id.tvCount)
                    val tvUser1 = it.findViewById<TextView>(R.id.tvUser1)
                    val tvUser2 = it.findViewById<TextView>(R.id.tvUser2)

                    val userName1 = if (item.roomUser.size > 0) item.roomUser[0].userName else ""
                    val userName2 = if (item.roomUser.size > 1) item.roomUser[1].userName else ""

                    tvCount.text = "Number :  ${item.roomUser.size}"
                    tvUser1.text = "Name : $userName1"
                    tvUser2.visibility = if (item.roomUser.size > 1) View.VISIBLE else View.GONE
                    tvUser2.text = "Name : $userName2"
                }
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
                CoroutineScope(Dispatchers.Main).launch {
                    roomInfoList.clear()
                    roomInfoList.addAll(arg.result.roomInfoList)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}