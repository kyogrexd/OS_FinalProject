package lab.italkutalk.tool.webRTC


class RTCConstants private constructor() {
    companion object {
        val instance : RTCConstants = RTCConstants()
    }

    var isIntiateNow: Boolean = true
    var roomID: String = ""
    var isMute: Boolean = false
    var isVideoPaused: Boolean = false
    var isSpeakMode: Boolean = true
}