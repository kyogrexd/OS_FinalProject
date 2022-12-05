package lab.italkutalk.tool.webRTC

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import lab.italkutalk.data.webRTC.Answer
import lab.italkutalk.data.webRTC.Offer
import lab.italkutalk.tool.Method
import org.webrtc.*
import java.nio.ByteBuffer

class RTCClient(context: Application, observer: PeerConnection.Observer) {

    val Tag = "RTCClient"

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
                Method.logE(Tag, "getSupportedFormats: ${getSupportedFormats(it)}")
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

    private fun PeerConnection.call(sdpObserver: SdpObserver, roomID: String, userID: String,
                                    targetID: String, targetSocketID: String, isAudioCall: Boolean) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) //允許音訊
            mandatory.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true")) //加密
            mandatory.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
            if (!isAudioCall) mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) //允許影片
        }

        createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                //設定localSDP
                setLocalDescription(object: SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Method.logE(Tag, "[offer/localDesc] onCreateSuccess: Description ${p0?.description}")
                    }

                    override fun onSetSuccess() {
                        Method.logE(Tag, "[offer/localDesc] onSetSuccess")
                        //撥打者傳送本地SDP -> socket on "offer"
                        Method.logE(Tag, "[Caller SDP] type: ${desc?.type} \n ${desc?.description}")
                        val data = Offer(desc?.description, desc?.type?.ordinal, roomID, targetSocketID, userID, userID, targetID)
                        RTCSocketManager.instance.sendSDPOffer(data)
                    }

                    override fun onCreateFailure(p0: String?) {
                        Method.logE(Tag, "[offer/localDesc] onCreateFailure: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Method.logE(Tag, "[offer/localDesc] onSetFailure: $p0")
                    }

                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
            override fun onCreateFailure(p0: String?) {
                Method.logE(Tag, "[offer] onCreateFailure: $p0")
            }
            override fun onSetFailure(p0: String?) {
                Method.logE(Tag, "[offer] onSetFailure: $p0")
            }

        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver, roomID: String, userID: String,
                                      targetID: String, targetSocketID: String, isAudioCall: Boolean) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) //允許音訊
            mandatory.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement","true")) //加密
            mandatory.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
            if (!isAudioCall) mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) //允許影片
        }

        createAnswer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                //設定localSDP
                setLocalDescription(object: SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Method.logE(Tag, "[answer/localDesc] onCreateSuccess: ${p0?.description}")
                    }

                    override fun onSetSuccess() {
                        Method.logE(Tag, "[answer/localDesc] onSetSuccess")
                        //接收者傳送本地SDP -> socket server -> 撥打者
                        Method.logE(Tag, "[Callee SDP] type: ${desc?.type} \n${desc?.description}")
                        val data = Answer(desc?.description, desc?.type?.ordinal, roomID, targetSocketID, targetID, userID ,targetID)
                        RTCSocketManager.instance.sendSDPAnswer(data)
                    }

                    override fun onCreateFailure(p0: String?) {
                        Method.logE(Tag, "[answer/localDesc] onCreateFailure: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Method.logE(Tag, "[answer/localDesc] onCreateFailure: $p0")
                    }

                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }

            override fun onCreateFailure(p0: String?) {
                Method.logE(Tag, "[answer] onCreateFailureRemote: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Method.logE(Tag, "[answer] onSetFailure: $p0")
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver, roomID: String, userID: String, targetID: String, targetSocketID: String, isAudioCall: Boolean)
            = peerConnection?.call(sdpObserver, roomID, userID, targetID, targetSocketID, isAudioCall)

    fun answer(sdpObserver: SdpObserver, roomID: String, userID: String, targetID: String, targetSocketID: String, isAudioCall: Boolean)
            = peerConnection?.answer(sdpObserver, roomID, userID, targetID, targetSocketID, isAudioCall)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        Method.logE(Tag, "[remote] ${sessionDescription.description}")
        peerConnection?.setRemoteDescription(object: SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Method.logE(Tag, "[remote] onCreateSuccess: $p0")
            }

            override fun onSetSuccess() {
                Method.logE(Tag, "[remote] onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Method.logE(Tag, "[remote] onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Method.logE(Tag, "[remote] onSetFailure: $p0")
            }

        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun endCall(isAudioCall: Boolean) {
        peerConnection?.close()
        //closedAudio()
        if (!isAudioCall) closedVideo()
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

    fun enableAudio(audioEnable: Boolean) {
        if (localAudioTrack != null) localAudioTrack?.setEnabled(audioEnable)
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
                        Method.logE(Tag, "$string: w = $videoWidth, h = $videoHeight, rotation = $rotation")
                    }
                })
            }
        } catch (e: Exception) {
            Method.logE(Tag, e.toString())
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