package com.example.os_finalproject

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.developerspace.webrtcsample.SignalingClient
import com.example.os_finalproject.Data.Controller
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.databinding.ActivityRtcBinding
import com.example.os_finalproject.tool.ActivityController
import com.example.os_finalproject.tool.SocketManager
import lab.italkutalk.tool.webRTC.*
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class RTCActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRtcBinding

    private val Tag = "RTCActivity"

    private var rtcClient: RTCClient? = null

    private lateinit var viewModel: DataViewModel

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private var roomID : String = "test-call"

    private var socketID : String = ""

    private var targetSocketID : String = ""

    private var userName : String = ""

    private var targetUserName : String = ""

    private var isCaller : Boolean = false

    private var uuid : String = ""

    private var isMute = false

    private var isVideo = true

    private var isSpeaker = true

    private var isBackCamera = true

    private var remoteVideoTrack: VideoTrack? = null

    private val sdpObserver = object : AppSdpObserver() {}

    private lateinit var callTimer: Timer

    private lateinit var callTimerTask: TimerTask

    //DataChannel 用到的字串
    enum class DataString {
        BackCamera,
        FrontCamera
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityController.addActivity(this)

        binding = ActivityRtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val b = intent.extras ?: return
        roomID = b.getString("RoomID", "")
        socketID = b.getString("SocketID", "")
        uuid = b.getString("Uuid", "")
        userName = b.getString("UserName", "")

        if (socketID.isEmpty()) finish()

        audioManager.start(object : RTCAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: RTCAudioManager.AudioDevice?,
                availableAudioDevices: Set<RTCAudioManager.AudioDevice?>?
            ) {
                Log.e(Tag, "selectedAudioDevice: $selectedAudioDevice, availableAudioDevices: $availableAudioDevices") }
            })
        audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        viewModel = ViewModelProvider(this)[DataViewModel::class.java]
        viewModel.updateController(Controller(isMute, isVideo, isSpeaker, isBackCamera))
        viewModel.controller.observe(this) {
            isMute = it.isMute
            isVideo = it.isVideo
            isSpeaker = it.isSpeakMode
            isBackCamera = it.isBackCamera

            binding.run {
                imgMic.setImageDrawable(getDrawable(if (isMute) R.drawable.mic_off else R.drawable.mic_on))
                imgVideo.setImageDrawable(getDrawable(if (isVideo) R.drawable.video_on else R.drawable.video_off))
                imgAudio.setImageDrawable(getDrawable(if (isSpeaker) R.drawable.sound_on else R.drawable.sound_off))
            }

            setController()
        }

        initSocket()
        initDisplay()
        setListener()
    }

    private fun initSocket() {
        SocketManager.instance.run {
//            connectUrl("${ServerUrl}:8500/")
//
//            on("connected") {
//                runOnUiThread {
//                    socketID = it.getString("socketID")
//
//                    val jsonObject = JSONObject().also {
//                        it.put("roomID", roomID)
//                        it.put("userName", userName)
//                    }
//                    emit("joinRoom", jsonObject)
//                }
//            }

            val jsonObject = JSONObject().also {
                it.put("roomID", roomID)
                it.put("userName", userName)
            }
            emit("joinRoom", jsonObject)

            //Caller 創建房間 需要取得roomID
            on("checkRoomID") {
                runOnUiThread {
                    roomID = it.getString("roomID")
                }
                startTimer()
            }

            on("startCall") {
                runOnUiThread {
                    isCaller = it.getBoolean("isCaller")
                    targetSocketID = it.getString("targetSocketID")
                    targetUserName = it.getString("targetUserName")

                    binding.remoteView.visibility = View.VISIBLE

                    Toast.makeText(this@RTCActivity, "$targetUserName join", Toast.LENGTH_LONG).show()

                    initRTCClient()
                }
            }

            on("offer") {
                runOnUiThread {
                    val type = when (it.getInt("type")) {
                        0 -> SessionDescription.Type.OFFER
                        1 -> SessionDescription.Type.PRANSWER
                        else -> SessionDescription.Type.ANSWER
                    }

                    //設定 RemoteDescription
                    rtcClient?.onRemoteSessionReceived(SessionDescription(type, it.getString("sdp")))
                    //發送 answer
                    rtcClient?.answer(sdpObserver, roomID, socketID, targetSocketID)
                }
            }

            on("answer") {
                runOnUiThread {
                    val type = when (it.getInt("type")) {
                        0 -> SessionDescription.Type.OFFER
                        1 -> SessionDescription.Type.PRANSWER
                        else -> SessionDescription.Type.ANSWER
                    }

                    //設定 RemoteDescription
                    rtcClient?.onRemoteSessionReceived(SessionDescription(type, it.getString("sdp")))
                }
            }

            on("ice_candidates") {
                runOnUiThread {
                    rtcClient?.addIceCandidate(IceCandidate(it.getString("sdpMid"),
                        it.getInt("sdpMLineIndex"), it.getString("candidateSdp")))
                }
            }

            on("targetLeave") {
                runOnUiThread {
                    Toast.makeText(this@RTCActivity, "$targetUserName has left", Toast.LENGTH_SHORT).show()

                    binding.remoteView.release()
                    binding.imgLoading.visibility = View.VISIBLE
                    binding.remoteView.visibility = View.GONE
                }
            }

            on("error") {
                runOnUiThread {
                    Toast.makeText(this@RTCActivity, it.getString("errMsg"), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun initRTCClient() {
        rtcClient = RTCClient(application, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)

                val jsonObject = JSONObject().also {
                    it.put("roomID", roomID)
                    it.put("socketID", socketID)
                    it.put("targetSocketID", targetSocketID)
                    it.put("sdpMid", p0?.sdpMid)
                    it.put("sdpMLineIndex", p0?.sdpMLineIndex)
                    it.put("candidateSdp", p0?.sdp)
                }
                SocketManager.instance.emit("ice_candidates", jsonObject)

                rtcClient?.addIceCandidate(p0)
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                Log.e(Tag, "onAddStream: $p0")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.e(Tag, "onIceConnectionChange: $p0")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.e(Tag, "onIceConnectionReceivingChange: $p0")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.e(Tag, "onConnectionChange: $newState")
            }

            override fun onDataChannel(dc: DataChannel?) {
                Log.e(Tag, "onDataChannel: $dc")
                dc?.registerObserver(object : DataChanelObserver() {
                    override fun onMessage(buffer: DataChannel.Buffer?) {
                        super.onMessage(buffer)

                        val data = buffer?.data
                        val bytes = data?.let { ByteArray(it.remaining()) }
                        data?.get(bytes)
                        val command = bytes?.let { String(it) }
                        Log.e(Tag, "[DataChannel] onMessage: $command")

                        try {
                            runOnUiThread {
                                command?.let {
                                    when (it) {
                                        DataString.FrontCamera.name -> binding.remoteView.setMirror(false)
                                        DataString.BackCamera.name -> binding.remoteView.setMirror(true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.message?.let { Log.e(Tag, it) }
                        }
                    }

                    override fun onStateChange() {
                        super.onStateChange()
                        Log.e(Tag, "[DataChannel] onStateChange: ${dc.state()}")
                    }
                })
            }

            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.e(Tag, "onStandardizedIceConnectionChange: $newState")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                Log.e(Tag, "onAddTrack: $p0 \n $p1")
                runOnUiThread {
                    try {
                        val track: MediaStreamTrack? = p0?.track()
                        if (track is VideoTrack) {
                            binding.imgLoading.visibility = View.GONE
                            binding.remoteView.visibility = View.VISIBLE

                            remoteVideoTrack = track
                            remoteVideoTrack?.addSink(binding.remoteView)
                            remoteVideoTrack?.setEnabled(true)
                        }
                    } catch (e: Exception) {
                        e.message?.let { Log.e(Tag, e.message.toString()) }
                    }
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                Log.e(Tag, "onTrack: $transceiver" )
            }
        })

        rtcClient?.setDataChanel()

        handleVideoAudioStream()
    }

    private fun handleVideoAudioStream() {
        rtcClient?.setAudioStream(uuid, isMute)

        binding.remoteView.release()
        binding.localView.release()
        binding.localView.setZOrderOnTop(true)

        rtcClient?.initSurfaceView(binding.remoteView, "remote")
        rtcClient?.initSurfaceView(binding.localView, "local")
        rtcClient?.setVideo(binding.localView ,uuid, false, isVideo)

        setController()

        if (isCaller) rtcClient?.call(sdpObserver, roomID, socketID, targetSocketID)
    }

    private fun initDisplay() {
        binding.run {
            Glide.with(this@RTCActivity).asGif().load(R.raw.loading).override(imgLoading.width, imgLoading.height).into(imgLoading)
            imgMic.setImageDrawable(getDrawable(if (isMute) R.drawable.mic_off else R.drawable.mic_on))
            imgVideo.setImageDrawable(getDrawable(if (isVideo) R.drawable.video_on else R.drawable.video_off))
            imgAudio.setImageDrawable(getDrawable(if (isSpeaker) R.drawable.sound_on else R.drawable.sound_off))
        }
    }

    private fun setController() {
        rtcClient?.enableAudio(isMute)
        rtcClient?.enableVideo(isVideo)
        audioManager.setDefaultAudioDevice(
            if (isSpeaker) RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else RTCAudioManager.AudioDevice.EARPIECE)
    }

    private fun setListener() {
        binding.run {
            imgMic.setOnClickListener {
                viewModel.updateController(Controller(!isMute, isVideo, isSpeaker, isBackCamera))
            }

            imgVideo.setOnClickListener {
                viewModel.updateController(Controller(isMute, !isVideo, isSpeaker, isBackCamera))
            }

            imgAudio.setOnClickListener {
                viewModel.updateController(Controller(isMute, isVideo, !isSpeaker, isBackCamera))
            }

            imgCamera.setOnClickListener {
                isBackCamera = !isBackCamera

                rtcClient?.sendData(if (isBackCamera) DataString.BackCamera.name else DataString.FrontCamera.name)
                rtcClient?.switchCamera()
            }

            imgEndCall.setOnClickListener {
                //handleEndCall()
                finishAndRemoveTask()
//                startActivity(Intent(this@RTCActivity, MainActivity::class.java))
            }

            imgPIP.setOnClickListener { enterPipMode() }
        }
    }

    private fun handleEndCall() {
        rtcClient?.endCall()

        val jsonObject = JSONObject().also {
            it.put("roomID", roomID)
            it.put("userName", userName)
            it.put("socketID", socketID)
        }
        SocketManager.instance.emit("endCall", jsonObject)
    }

    private fun getEndCallPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 102, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun enterPipMode() {
        val aspectRatio = Rational(9, 16)
        val endCallPendingIntent = getEndCallPendingIntent()
        val endCallAction = RemoteAction(
            Icon.createWithResource(this, R.drawable.icon_endcall),
            "End Call",
            "End Call Button",
            endCallPendingIntent
        )
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            //.setActions(listOf(endCallAction))
            .setAspectRatio(Rational(9, 16))
            .build()

        this.enterPictureInPictureMode(params)
    }

    private fun startTimer() {
        var count = 5
        callTimer = Timer()
        callTimerTask = object : TimerTask() {
            override fun run() {
                count --

                Log.e(Tag, "Timer: $count")

                if (count == 0) {
                    count = 5

                    val jsonObject = JSONObject().also { json ->
                        json.put("roomID", roomID)
                        json.put("socketID", socketID)
                    }
                    SocketManager.instance.emit("schedule_pairing_check", jsonObject)
                }
            }
        }
        callTimer.schedule(callTimerTask, 0, 1000)
    }


    override fun onDestroy() {
        super.onDestroy()
        ActivityController.removeActivity(this)

        remoteVideoTrack?.removeSink(binding.remoteView)

        binding.remoteView.release()
        binding.localView.release()

        audioManager.stop()

        //SocketManager.instance.disconnect()

        if (::callTimer.isInitialized && ::callTimerTask.isInitialized) {
            callTimer.cancel()
            callTimer.purge()
            callTimerTask.cancel()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (lifecycle.currentState == Lifecycle.State.CREATED)
            finishAndRemoveTask()
        else if (lifecycle.currentState == Lifecycle.State.STARTED)
            setUiVisible(isInPictureInPictureMode)
    }

    private fun setUiVisible(isInPictureInPictureMode: Boolean) {
        binding.run {
            gpUI.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun finishAndRemoveTask() {
        handleEndCall()

        SocketManager.instance.disconnect()

        //todo 在PIP下，按下 X 關閉後，setResult not working
        val b = Bundle()
        b.putString("UserName", userName)
        setResult(RESULT_OK, Intent().putExtras(b))

        super.finishAndRemoveTask()
    }
}