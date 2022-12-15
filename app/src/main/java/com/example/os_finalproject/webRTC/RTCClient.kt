package lab.italkutalk.tool.webRTC

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import com.example.os_finalproject.Data.Answer
import com.example.os_finalproject.Data.Offer
import com.example.os_finalproject.signaling.Constants
import com.example.os_finalproject.tool.SocketManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer

class RTCClient(context: Application, observer: PeerConnection.Observer) {

    val Tag = "RTCClient"

    val db = Firebase.firestore

    private val context = context
    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val peerConnection by lazy { buildPeerConnection(observer) }
    private val iceServers = listOf (
        PeerConnection.IceServer.builder(listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun3.l.google.com:19302",
            "stun:stun4.l.google.com:19302")).createIceServer(),
        PeerConnection.IceServer.builder("turn:numb.viagenie.ca").setUsername("italkutalktw@gmail.com").setPassword("italkutalktw").createIceServer())
    private var dataChannel: DataChannel? = null
    private val rootEglBase: EglBase = EglBase.create() //video設備
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private val audioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var localVideoSource: VideoSource
    private lateinit var surfaceTextureHelper: SurfaceTextureHelper

    private var cameraSupportFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>? = null

    init {
        initPeerConnectionFactory(context)
    }

    //初始化
    private fun initPeerConnectionFactory(context: Application) {
        val option = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/") //試用特性
            .setFieldTrials("WebRTC-IntelVP8/Enabled/")
            .setFieldTrials("WebRTC-SupportVP9SVC/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(option)
    }

    //建立PeerConnectionFactory
    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
                networkIgnoreMask = 0
            })
            .createPeerConnectionFactory()
    }

    //影片採集，使用Camera2
    private fun getVideoCapturer(context: Context): CameraVideoCapturer {
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it) //相機是否與手機畫面同方向
            }?.let {
                Log.e(Tag, "getSupportedFormats: ${getSupportedFormats(it)}")
                cameraSupportFormats = getSupportedFormats(it)
                return createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    //建立點對點連線
    private fun buildPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.run {
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            sdpSemantics = PeerConnection.SdpSemantics.PLAN_B

        /**
         * android端 UNIFIED_PLAN 無法調用 addStream，改調用 addTrack
         * 但 IOS端 UNIFIED_PLAN 能調用 addStream，所以 android端改用PLAN_B，不然IOS端無法解析出Stream
        */
        }

        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver, roomID: String, socketID: String, targetSocketID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) //允許音訊
            mandatory.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) //允許影片
        }

        createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                //設定localSDP
                setLocalDescription(object: SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.e(Tag, "[offer/localDesc] onCreateSuccess: Description ${p0?.description}")
                    }

                    override fun onSetSuccess() {
                        Log.e(Tag, "[offer/localDesc] onSetSuccess")
                        //撥打者傳送本地SDP -> socket on "offer"
                        Log.e(Tag, "[Caller SDP] type: ${desc?.type} \n ${desc?.description}")
                        val jsonObject = JSONObject().also {
                            it.put("roomID", roomID)
                            it.put("socketID", socketID)
                            it.put("targetSocketID", targetSocketID)
                            it.put("sdp", desc?.description)
                            it.put("type", desc?.type?.ordinal)
                        }
                        SocketManager.instance.emit("offer", jsonObject)

//                        val offer = hashMapOf(
//                            "sdp_offer" to desc?.description,
//                            "type_offer" to desc?.type,
//                            "state" to "caller"
//                        )
//                        db.collection("calls").document(roomID)
//                            .set(offer)
//                            .addOnSuccessListener {
//                                Log.e(Tag, "[offer/localDesc] DocumentSnapshot added")
//                            }
//                            .addOnFailureListener { e ->
//                                Log.e(Tag, "[offer/localDesc] Error adding document", e)
//                            }
                        Log.e(Tag, "[offer/localDesc] onSetSuccess")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.e(Tag, "[offer/localDesc] onCreateFailure: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e(Tag, "[offer/localDesc] onSetFailure: $p0")
                    }

                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
            override fun onCreateFailure(p0: String?) {
                Log.e(Tag, "[offer] onCreateFailure: $p0")
            }
            override fun onSetFailure(p0: String?) {
                Log.e(Tag, "[offer] onSetFailure: $p0")
            }

        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver, roomID: String, socketID: String, targetSocketID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) //允許音訊
            mandatory.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) //允許影片
        }

        createAnswer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
//                val answer = hashMapOf(
//                    "sdp_answer" to desc?.description,
//                    "type_answer" to desc?.type,
//                    "state" to "callee"
//                )
//                db.collection("calls").document(roomID)
//                    .set(answer)
//                    .addOnSuccessListener {
//                        Log.e(Tag, "[answer] DocumentSnapshot added")
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e(Tag, "[answer] Error adding document", e)
//                    }

                //設定localSDP
                setLocalDescription(object: SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.e(Tag, "[answer/localDesc] onCreateSuccess: ${p0?.description}")
                    }

                    override fun onSetSuccess() {
                        Log.e(Tag, "[answer/localDesc] onSetSuccess")
                        //接收者傳送本地SDP -> socket server -> 撥打者
                        Log.e(Tag, "[Callee SDP] type: ${desc?.type} \n${desc?.description}")
                        val jsonObject = JSONObject().also {
                            it.put("roomID", roomID)
                            it.put("socketID", socketID)
                            it.put("targetSocketID", targetSocketID)
                            it.put("sdp", desc?.description)
                            it.put("type", desc?.type?.ordinal)
                        }
                        SocketManager.instance.emit("answer", jsonObject)
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.e(Tag, "[answer/localDesc] onCreateFailure: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e(Tag, "[answer/localDesc] onCreateFailure: $p0")
                    }

                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(Tag, "[answer] onCreateFailureRemote: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.e(Tag, "[answer] onSetFailure: $p0")
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver, roomID: String, socketID: String, targetSocketID: String)
            = peerConnection?.call(sdpObserver, roomID, socketID, targetSocketID)

    fun answer(sdpObserver: SdpObserver, roomID: String, socketID: String, targetSocketID: String)
            = peerConnection?.answer(sdpObserver, roomID, socketID, targetSocketID)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object: SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e(Tag, "[remote] onCreateSuccess: $p0")
            }

            override fun onSetSuccess() {
                Log.e(Tag, "[remote] onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(Tag, "[remote] onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.e(Tag, "[remote] onSetFailure: $p0")
            }

        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun endCall(roomID: String) {
//        db.collection("calls").document(roomID).collection("candidates")
//            .get().addOnSuccessListener {
//                val iceCandidateArray: MutableList<IceCandidate> = mutableListOf()
//                for ( dataSnapshot in it) {
//                    if (dataSnapshot.contains("type") && dataSnapshot["type"]=="offerCandidate") {
//                        val offerCandidate = dataSnapshot
//                        iceCandidateArray.add(IceCandidate(offerCandidate["sdpMid"].toString(), Math.toIntExact(offerCandidate["sdpMLineIndex"] as Long), offerCandidate["sdp"].toString()))
//                    } else if (dataSnapshot.contains("type") && dataSnapshot["type"]=="answerCandidate") {
//                        val answerCandidate = dataSnapshot
//                        iceCandidateArray.add(IceCandidate(answerCandidate["sdpMid"].toString(), Math.toIntExact(answerCandidate["sdpMLineIndex"] as Long), answerCandidate["sdp"].toString()))
//                    }
//                }
//                peerConnection?.removeIceCandidates(iceCandidateArray.toTypedArray())
//            }
//        val endCall = hashMapOf(
//            "state" to "END_CALL"
//        )
//        db.collection("calls").document(roomID)
//            .set(endCall)
//            .addOnSuccessListener {
//                Log.e(Tag, "[endCall] DocumentSnapshot added")
//            }
//            .addOnFailureListener { e ->
//                Log.e(Tag, "[endCall] Error adding document", e)
//            }

        Constants.isIntiatedNow = true
        peerConnection?.close()
        //closedAudio()
        closedVideo()
        if (rootEglBase.hasSurface()) rootEglBase.release()
    }

    fun setDataChanel() {
        val dcInit = DataChannel.Init()
        dcInit.id = 1
        dataChannel = peerConnection?.createDataChannel("1", dcInit)
    }

    fun sendData(data: String) {
        val buffer = ByteBuffer.wrap(data.toByteArray())
        dataChannel?.send(DataChannel.Buffer(buffer, false))
    }

    fun enableVideo(videoEnable: Boolean) {
        if (localVideoTrack != null) localVideoTrack?.setEnabled(videoEnable)
    }

    fun enableAudio(isMute: Boolean) {
        if (localAudioTrack != null) localAudioTrack?.setEnabled(!isMute)
    }

    fun switchCamera() {
        (videoCapturer as CameraVideoCapturer).switchCamera(null)
    }

    //初始化視訊介面
    fun initSurfaceView(view: SurfaceViewRenderer, string: String, isMirror: Boolean = true) {
        try {
            view.run {
                setMirror(isMirror)
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                init(rootEglBase.eglBaseContext, object: RendererCommon.RendererEvents {
                    override fun onFirstFrameRendered() {}

                    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                        Log.e(Tag, "$string: w = $videoWidth, h = $videoHeight, rotation = $rotation")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(Tag, e.toString())
        }
    }
    //設定視訊
    fun setVideo(localVideoOutput: SurfaceViewRenderer, uuid: String, isToggle: Boolean) {
        if (!isToggle) { //初始設定
            setVideoStream(localVideoOutput, uuid)
            peerConnection?.addTrack(localVideoTrack)
        } else { //切回視訊
            videoCapturer.stopCapture()
            setVideoStream(localVideoOutput, uuid)
            peerConnection?.senders?.find { it.track()?.kind() == "video" }?.setTrack(localVideoTrack, true)
        }
    }

    //設定視訊媒體流
    fun setVideoStream(localVideoOutput: SurfaceViewRenderer, uuid: String) {
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCapturer = getVideoCapturer(context)
        localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)

        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)

        //取得 camera 最高解析度及其fps (Samsung M11 會報Error: HardwareVideoEncoder: initEncodeInternal failed)
//        if (!cameraSupportFormats.isNullOrEmpty()) {
//            cameraSupportFormats?.sortedByDescending { it.width } //高到低
//            cameraSupportFormats?.let {
//                val width = it[0].width
//                val height = it[0].height
//                val fps = it[0].framerate.max.div(1000)
//                Method.logE(Tag, "width: $width, height: $height, fps: $fps")
//                videoCapturer.startCapture(width, height, 30)
//            }
//        } else {
            videoCapturer.startCapture(1280, 720, 15)
//        }

        localVideoTrack = peerConnectionFactory.createVideoTrack("track_${uuid}_video", localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        localVideoTrack?.setEnabled(!RTCConstants.instance.isVideoPaused)
    }

    //設定音訊
    fun setAudioStream(uuid: String) {
        localAudioTrack = peerConnectionFactory.createAudioTrack("track_${uuid}_audio", audioSource)
        localAudioTrack?.setEnabled(!RTCConstants.instance.isMute)
        peerConnection?.addTrack(localAudioTrack)
    }

    //切換分享畫面
    fun toggleShareScreen(localVideoOutput: SurfaceViewRenderer, uuid: String, data: Intent?, width: Int, height: Int) {
        videoCapturer.stopCapture()
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCapturer = ScreenCapturerAndroid(data, object: MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
            }
        })
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer.startCapture(720, 1280, 15)
        val videoTrack = peerConnectionFactory.createVideoTrack("track_${uuid}_share_screen", videoSource)
        videoTrack?.addSink(localVideoOutput)

        peerConnection?.senders?.find { it.track()?.kind() == "video" }?.setTrack(videoTrack, true)
    }

    //釋放視訊相關資源
    fun closedVideo() {
        videoCapturer.stopCapture()
        //surfaceTextureHelper.dispose()
        videoCapturer.dispose()
    }

//    fun closedAudio() {
//        localAudioTrack?.dispose()
//        audioSource.dispose()
//    }
}