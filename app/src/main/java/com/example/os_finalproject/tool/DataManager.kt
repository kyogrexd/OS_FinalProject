package com.example.os_finalproject.tool

import android.util.Log
import com.example.os_finalproject.Data.RoomInfoRes
import com.google.gson.Gson
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

const val ServerUrl = "http://140.124.73.7"

class DataManager private constructor() : Observable(){

    companion object {
        val instance : DataManager = DataManager()
    }

    private var client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor()).build()

    private class LoggingInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val t1 = System.nanoTime()
            val response = chain.proceed(request)

            response.body?.let {
                val t2 = System.nanoTime()
                val contentType = it.contentType()
                val content = it.string()
                Log.e("${response.request.url}","${String.format("%.1f", (t2 - t1) / 1e6)}ms $content")

                val wrappedBody: ResponseBody = content.toResponseBody(contentType)
                return response.newBuilder().body(wrappedBody).build()
            }
            return chain.proceed(request)
        }
    }

    private fun httpGet(url: String, resOfT: Class<*>? = null){
        Log.e("httpGet", url)
        val req = Request.Builder().url(url).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(resOfT == null) return

                try{
                    val json = response.body?.string() as String
                    val res = Gson().fromJson(json, resOfT)

                    notifyChanged(res)
                }catch (e: Exception){
                    Log.e(url, "$e")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("onFailure", "$e")
            }
        })
    }

    fun notifyChanged(res: Any) {
        setChanged()
        notifyObservers(res)
    }

    /**
     * function
     */
    fun doRoomInfo() = httpGet("${ServerUrl}:8000/api/roomInfo", RoomInfoRes::class.java)
}