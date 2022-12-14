package com.example.os_finalproject.tool

import android.util.Log
import io.socket.client.IO
import org.json.JSONObject


class SocketManager {
    companion object {
        val instance : SocketManager by lazy { SocketManager() }
    }

    private var mSocket: io.socket.client.Socket? = null

    fun connectUrl(url: String) {
        mSocket = IO.socket(url)
        mSocket?.connect()
    }

    fun disconnect() {
        mSocket?.disconnect()
        mSocket = null
    }

    fun emit(key: String, arg: JSONObject) {
        Log.e("socket.emit($key)", "$arg")
        mSocket?.emit(key, arg)
    }

    fun on(key: String, result: (JSONObject) -> Unit) {
        mSocket?.on(key) { args ->
            Log.e("socket.on($key)", "${args[0]}")
            val data = args[0] as JSONObject
            result(data)
        }
    }
}