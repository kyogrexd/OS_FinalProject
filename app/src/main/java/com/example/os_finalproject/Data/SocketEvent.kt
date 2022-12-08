package com.example.os_finalproject.Data

data class StartCall(val roomID: String, val callerID: String, val callerName: String, val callerPhoto: String,
                     val callerSocketID: String, val calleeID: String, val uuid: String, val isAudio: Boolean)

data class Offer(val sdp: String?, val type: Int?, val roomID: String, val calleeSocketID: String , val callerID: String,
                 val sender: String, val receiver: String)

data class Join(val roomID: String, val calleeID: String, val callerSocketID: String,
                val calleeSocketID: String, val calleeVoIP: String, val calleeFCM: String)

data class Answer(val sdp: String?, val type: Int?, val roomID: String, val callerSocketID: String,
                  val callerID: String, val sender: String, val receiver: String)

data class ReceivedReject(val callerID: String)

data class Ice_candidates(val roomID: String, val sdpMid: String?, val sdpMLineIndex: Int?,
                          val candidateSdp: String?, val sender: String, val receiver: String)

data class Finish(val roomID: String, val callerID: String, val calleeID: String,
                  val callerSocketID: String, val calleeSocketID: String, val uuid: String)

data class Cancel(val roomID: String, val callerID: String, val uuid: String)