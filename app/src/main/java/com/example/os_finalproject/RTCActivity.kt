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
import com.example.os_finalproject.Data.ACTION_END_CALL
import com.example.os_finalproject.Data.Controller
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.Data.ServerUrl
import com.example.os_finalproject.databinding.ActivityRtcBinding
import com.example.os_finalproject.signaling.Constants
import com.example.os_finalproject.tool.SocketManager
import lab.italkutalk.tool.webRTC.AppSdpObserver
import lab.italkutalk.tool.webRTC.PeerConnectionObserver
import lab.italkutalk.tool.webRTC.RTCAudioManager
import lab.italkutalk.tool.webRTC.RTCClient
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class RTCActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRtcBinding

    private val Tag = "RTCActivity"

    private var rtcClient: RTCClient? = null

    private var signallingClient: SignalingClient? = null

    private lateinit var viewModel: DataViewModel

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private var roomID : String = "test-call"

    private var socketID : String = ""

    private var targetSocketID : String = ""

    private var userName : String = ""

    private var targetUserName : String = ""

    private var isCaller : Boolean = false

    private var uuid : String = ""

    private var isJoin = false

    private var isMute = false

    private var isVideo = true

    private var isSpeaker = true

    private var isBackCamera = true

    private var remoteVideoTrack: VideoTrack? = null

    private val sdpObserver = object : AppSdpObserver() {}

    private lateinit var callTimer: Timer

    private lateinit var callTimerTask: TimerTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val b = intent.extras ?: return
        roomID = b.getString("RoomID", "")
        isJoin = b.getBoolean("IsJoin", false)
        uuid = b.getString("Uuid", "")
        userName = b.getString("UserName", "")

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

        var count = 0
        callTimer = Timer()
        callTimerTask = object : TimerTask() {
            override fun run() {
                Log.e(Tag, "Timer: ${count ++}")
            }
        }
        callTimer.schedule(callTimerTask, 0, 1000)
    }

    private fun initSocket() {
        SocketManager.instance.run {
            connectUrl("${ServerUrl}:8500/")

            on("connected") {
                runOnUiThread {
                    socketID = it.getString("socketID")

                    val jsonObject = JSONObject().also {
                        it.put("roomID", roomID)
                        it.put("userName", userName)
                    }
                    emit("joinRoom", jsonObject)
                }
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
                //signallingClient?.sendIceCandidate(p0, isJoin)

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

            override fun onDataChannel(p0: DataChannel?) {
                Log.e(Tag, "onDataChannel: $p0")
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
        //signallingClient =  SignalingClient(roomID, createSignallingClientListener(), isJoin)
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

//    private fun createSignallingClientListener() = object : SignalingClientListener {
//        override fun onConnectionEstablished() {
//            Log.e(Tag, "[Signalling] onConnectionEstablished")
//            binding.imgEndCall.isClickable = true
//        }
//
//        override fun onOfferReceived(description: SessionDescription) {
//            Log.e(Tag, "[Signalling] onOfferReceived")
//            binding.remoteView.visibility = View.VISIBLE
//            rtcClient?.onRemoteSessionReceived(description)
//            Constants.isIntiatedNow = false
//            rtcClient?.answer(sdpObserver, roomID)
//        }
//
//        override fun onAnswerReceived(description: SessionDescription) {
//            Log.e(Tag, "[Signalling] onAnswerReceived")
//            binding.remoteView.visibility = View.VISIBLE
//            rtcClient?.onRemoteSessionReceived(description)
//            Constants.isIntiatedNow = false
//        }
//
//        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
//            Log.e(Tag, "[Signalling] onIceCandidateReceived")
//            rtcClient?.addIceCandidate(iceCandidate)
//        }
//
//        override fun onCallEnded() {
//            rtcClient?.endCall(roomID)
//            finish()
//            startActivity(Intent(this@RTCActivity, MainActivity::class.java))
//        }
//    }

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
                rtcClient?.switchCamera()
            }

            imgEndCall.setOnClickListener {
                //handleEndCall()
                finishAndRemoveTask()
                startActivity(Intent(this@RTCActivity, MainActivity::class.java))
            }

            imgPIP.setOnClickListener { enterPipMode() }
        }
    }

    private fun handleEndCall() {
        val jsonObject = JSONObject().also {
            it.put("roomID", roomID)
            it.put("userName", userName)
            it.put("socketID", socketID)

            rtcClient?.endCall(roomID)

            Constants.isCallEnded = true
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


    override fun onDestroy() {
        super.onDestroy()

        remoteVideoTrack?.removeSink(binding.remoteView)

        binding.remoteView.release()
        binding.localView.release()

        audioManager.stop()

        //signallingClient = null

        handleEndCall()

        SocketManager.instance.disconnect()

        callTimer.cancel()
        callTimer.purge()
        callTimerTask.cancel()

        //startActivity(Intent(this@RTCActivity, MainActivity::class.java))
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
}