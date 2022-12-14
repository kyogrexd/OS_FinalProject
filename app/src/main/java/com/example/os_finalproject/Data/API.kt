package com.example.os_finalproject.Data

data class RoomInfoRes(val result: Result) {
    data class Result(val roomInfoList: ArrayList<RoomInfoList>)
    data class RoomInfoList(val roomID: String, val roomUser: RoomUser)
    data class RoomUser(val caller: String, val callee: String)
}