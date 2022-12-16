package com.example.os_finalproject.Data

val URL = "http://192.168.0.102"

data class RoomInfoRes(val result: Result) {
    data class Result(val roomInfoList: ArrayList<RoomInfoList>)
    data class RoomInfoList(val roomID: String, val roomUser: ArrayList<RoomUser>)
    data class RoomUser(val caller: String, val callee: String)
}