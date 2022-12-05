package lab.italkutalk.tool.webRTC

import lab.italkutalk.tool.CallType

class RTCConstants private constructor() {
    companion object {
        val instance : RTCConstants = RTCConstants()
    }

    var isIntiateNow: Boolean = true
    var roomID: String = ""
    var callType: CallType = CallType.None
    var isMute: Boolean = false
    var isVideoPaused: Boolean = false
    var isSpeakMode: Boolean = true
}