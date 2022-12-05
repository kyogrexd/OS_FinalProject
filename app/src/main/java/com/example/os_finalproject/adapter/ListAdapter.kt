package com.example.os_finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.os_finalproject.R
import com.example.os_finalproject.databinding.ItemListBinding

class ListAdapter(private val list: ArrayList<String>): RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private lateinit var listener: ClickListener

    interface ClickListener {
        fun onEnterChatRoom(item: String)
    }

    fun setListener(l: ClickListener) { listener = l }

    inner class ViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvContent.text = item
        holder.binding.tvContent.setOnClickListener { listener.onEnterChatRoom(item) }
    }
}