package com.example.os_finalproject.Data

const val ServerUrl = "http://140.124.73.7"

const val ACTION_END_CALL = 102

data class RoomInfoRes(val result: Result) {
    data class Result(val roomInfoList: ArrayList<RoomInfoList>)
    data class RoomInfoList(val roomID: String, val roomUser: ArrayList<RoomUser>)
    data class RoomUser(val caller: String, val callee: String)
}