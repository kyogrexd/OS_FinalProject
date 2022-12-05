package lab.italkutalk.tool.webRTC

import io.socket.client.IO
import io.socket.client.Socket
import lab.italkutalk.data.webRTC.*
import lab.italkutalk.tool.Method
import org.json.JSONObject
import java.net.URISyntaxException

class RTCSocketManager {
    val Tag = "RTCSocketManager"

    val localURL = "http://140.124.73.27:1234/"
    val itutURL = "https://nativewebrtc.italkutalk.com/"

    companion object {
        val instance : RTCSocketManager by lazy { RTCSocketManager() }
    }

    private var mSocket: io.socket.client.Socket? = null

    fun setSocket() {
        try {
            mSocket = IO.socket(itutURL)
            mSocket?.connect()
        } catch (e: URISyntaxException) {
            Method.logE(Tag, "error $e")
        }
    }

    fun closeConnection() {
        mSocket?.disconnect()
        mSocket = null
    }

    fun on(key: String, result: ((JSONObject) -> Unit)? = null) {
        mSocket?.on(key) { args ->
            val data = args[0] as JSONObject
            result?.let { it(data) }
        }
    }

    fun emit(key: String, arg: Any? = null) {
        mSocket?.emit(key, arg)
    }

    /**
     * emit
     */
    fun startCall(data: StartCall) {
        Method.logE(Tag, "[SocketEvent][emit] startCall")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("callerID", data.callerID)
        jsonObject.put("callerName", data.callerName)
        jsonObject.put("callerPhoto", data.callerPhoto)
        jsonObject.put("callerSocketID", data.callerSocketID)
        jsonObject.put("calleeID", data.calleeID)
        jsonObject.put("uuid", data.uuid)
        jsonObject.put("isAudio", data.isAudio)
        emit("startCall", jsonObject)
    }

    fun joinRoom(data: Join) {
        Method.logE(Tag, "[SocketEvent][emit] join")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("calleeID", data.calleeID)
        jsonObject.put("callerSocketID", data.callerSocketID)
        jsonObject.put("calleeSocketID", data.calleeSocketID)
        jsonObject.put("calleeVoIP", data.calleeVoIP)
        jsonObject.put("calleeFCM", data.calleeFCM)
        emit("join", jsonObject)
    }

    fun sendSDPOffer(data: Offer) {
        Method.logE(Tag, "[SocketEvent][emit] offer")
        val jsonObject = JSONObject()
        jsonObject.put("sdp", data.sdp)
        jsonObject.put("type", data.type)
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("calleeSocketID", data.calleeSocketID)
        jsonObject.put("callerID", data.callerID)
        jsonObject.put("sender", data.sender)
        jsonObject.put("receiver", data.receiver)
        emit("offer", jsonObject)
    }

    fun sendSDPAnswer(data: Answer) {
        Method.logE(Tag, "[SocketEvent][emit] answer")
        val jsonObject = JSONObject()
        jsonObject.put("sdp", data.sdp)
        jsonObject.put("type", data.type)
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("callerSocketID", data.callerSocketID)
        jsonObject.put("callerID", data.callerID)
        jsonObject.put("sender", data.sender)
        jsonObject.put("receiver", data.receiver)
        emit("answer", jsonObject)
    }

    fun receivedReject(data: ReceivedReject) {
        Method.logE(Tag, "[SocketEvent][emit] receivedReject")
        val jsonObject = JSONObject()
        jsonObject.put("callerID", data.callerID)
        emit("receivedReject", jsonObject)
    }

    fun sendICECandidates(data: Ice_candidates) {
        Method.logE(Tag,"[SocketEvent][emit] ice_candidates")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("sdpMid", data.sdpMid)
        jsonObject.put("sdpMLineIndex", data.sdpMLineIndex)
        jsonObject.put("candidateSdp", data.candidateSdp)
        jsonObject.put("sender",data.sender)
        jsonObject.put("receiver", data.receiver)
        emit("ice_candidates", jsonObject)
    }

    fun finishCall(data: Finish) {
        Method.logE(Tag, "[SocketEvent][emit] finish")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("callerID", data.callerID)
        jsonObject.put("calleeID", data.calleeID)
        jsonObject.put("callerSocketID", data.callerSocketID)
        jsonObject.put("calleeSocketID",data.calleeSocketID)
        jsonObject.put("uuid", data.uuid)
        emit("finish", jsonObject)
    }

    fun noReplyCall(roomID: String, userID: String) {
        Method.logE(Tag, "[SocketEvent][emit] noReply")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", roomID)
        jsonObject.put("callerID", userID)
        emit("noReply", jsonObject)
    }

    fun cancelCall(data: Cancel) {
        Method.logE(Tag, "[SocketEvent][emit] cancel")
        val jsonObject = JSONObject()
        jsonObject.put("roomID", data.roomID)
        jsonObject.put("callerID", data.callerID)
        jsonObject.put("uuid", data.uuid)
        emit("cancel", jsonObject)
    }

    /**
     * on
     */
    fun onFinish(result: (Array<Any>) -> Unit) {
        mSocket?.on("finish") {
            result(it)
        }
    }

    fun onBusy(result: (Array<Any>) -> Unit) {
        mSocket?.on("busy") {
            result(it)
        }
    }

    fun onError(result: (Array<Any>) -> Unit) {
        mSocket?.on("error") {
            result(it)
        }
    }

    fun onTimeOut(result: (Array<Any>) -> Unit) {
        mSocket?.on("connect_timeout") {
            result(it)
        }
    }
}