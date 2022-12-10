package com.example.os_finalproject.tool

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class SocketManager {
    val Tag = "SocketManager"

    companion object {
        val instance : SocketManager by lazy { SocketManager() }
    }

    private val url = "https://d423-140-124-73-7.jp.ngrok.io"

    private var mSocket: Socket? = null

    fun connectUrl() {
        try {
            mSocket = IO.socket(url)
            mSocket?.connect()
        } catch (e: URISyntaxException) {
            Log.e(Tag, "error　$e")
        }
    }

    fun disconnect() {
        mSocket?.disconnect()
        mSocket = null
    }
}