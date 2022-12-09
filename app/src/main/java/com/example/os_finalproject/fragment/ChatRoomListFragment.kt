package com.example.os_finalproject.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.MainActivity
import com.example.os_finalproject.RTCActivity
import com.example.os_finalproject.adapter.ListAdapter
import com.example.os_finalproject.databinding.FragmentChatroomListBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import java.util.*

class ChatRoomListFragment: Fragment() {
    private lateinit var mActivity: MainActivity
    private var binding: FragmentChatroomListBinding? = null

    private lateinit var adapter: ListAdapter
    private lateinit var viewModel: DataViewModel
    private var userName = ""
    private var isJoin  = false

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

        viewModel = ViewModelProvider(mActivity)[DataViewModel::class.java]
        viewModel.userName.observe(viewLifecycleOwner) {name ->
            userName = name
            binding?.tvName?.text = "Name: $name"
        }

        setRecyclerView()

        binding?.tvName?.setOnClickListener {
            isJoin = !isJoin
            viewModel.updateIsJoin(isJoin)
            Toast.makeText(mActivity, "IsJoin: $isJoin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(mActivity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        val list = arrayListOf("ChatRoom 1", "ChatRoom 2", "ChatRoom 3")

        adapter = ListAdapter(list)
        adapter.setListener(object : ListAdapter.ClickListener {
            override fun onEnterChatRoom(item: String) {
                val uuid = UUID.randomUUID().toString()
                //if (!isJoin) {
                db.collection("calls")
                    .document(item)
                    .get()
                    .addOnSuccessListener {
                        when (it["state"]) {
                            "callee" -> { //已經在通話了
                                Toast.makeText(mActivity, "ChatRoom is full", Toast.LENGTH_SHORT).show()
                            }
                            "caller" -> { //目前只有一人
                                val b = Bundle()
                                b.putString("RoomID", item)
                                b.putBoolean("IsJoin", true)
                                b.putString("Uuid", uuid)
                                startActivity(Intent(mActivity, RTCActivity::class.java).putExtras(b))
                            }
                            "END_CAll" -> { //已結束通話
                                val b = Bundle()
                                b.putString("RoomID", item)
                                b.putBoolean("IsJoin", false)
                                b.putString("Uuid", uuid)
                                startActivity(Intent(mActivity, RTCActivity::class.java).putExtras(b))
                            }
                            else -> { //初始房間
                                val b = Bundle()
                                b.putString("RoomID", item)
                                b.putBoolean("IsJoin", false)
                                b.putString("Uuid", uuid)
                                startActivity(Intent(mActivity, RTCActivity::class.java).putExtras(b))
                            }
                        }
                    }
                //}
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