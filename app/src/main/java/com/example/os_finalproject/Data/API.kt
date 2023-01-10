package com.example.os_finalproject.Data


data class RoomInfoRes(val result: Result) {
    data class Result(val roomInfoList: ArrayList<RoomInfoList>)
    data class RoomInfoList(val roomID: String, val roomUser: ArrayList<RoomUser>)
    data class RoomUser(val userName: String, val socketID: String, val refreshTime: Long)
}