package com.example.os_finalproject.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.os_finalproject.Data.RoomInfoRes
import com.example.os_finalproject.R
import com.example.os_finalproject.databinding.ItemListBinding

class ListAdapter(val context: Context, private val roomInfoList: ArrayList<RoomInfoRes.RoomInfoList>)
    : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private lateinit var listener: ClickListener

    interface ClickListener {
        fun onEnterChatRoom(item: RoomInfoRes.RoomInfoList)
    }

    fun setListener(l: ClickListener) { listener = l }

    inner class ViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int = roomInfoList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = roomInfoList[position]

        holder.binding.tvContent.text = item.roomID
        holder.binding.clItem.setOnClickListener { listener.onEnterChatRoom(item) }

        holder.binding.tvCount.text = "Number: 0"

        holder.binding.tvCount.text =
            if (item.roomUser.size < 2) "Number: ${item.roomUser.size}"
            else "Full"
        holder.binding.tvCount.background = context.getDrawable(
            if (item.roomUser.size < 2) R.drawable.rp_rectangle_green
            else R.drawable.rp_rectangle_red
        )
    }
}