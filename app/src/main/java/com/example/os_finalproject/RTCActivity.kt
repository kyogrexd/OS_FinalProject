package com.example.os_finalproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.developerspace.webrtcsample.SignalingClient
import com.developerspace.webrtcsample.SignalingClientListener
import com.example.os_finalproject.Data.Controller
import com.example.os_finalproject.Data.DataViewModel
import com.example.os_finalproject.databinding.ActivityRtcBinding
import com.example.os_finalproject.signaling.Constants
import lab.italkutalk.tool.webRTC.AppSdpObserver
import lab.italkutalk.tool.webRTC.PeerConnectionObserver
import lab.italkutalk.tool.webRTC.RTCAudioManager
import lab.italkutalk.tool.webRTC.RTCClient
import org.webrtc.*

class RTCActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRtcBinding

    private val Tag = "RTCActivity"

    private lateinit var rtcClient: RTCClient

    private lateinit var signallingClient: SignalingClient

    private lateinit var viewModel: DataViewModel

    private val audioManager by lazy { RTCAudioManager.create(this) }

    private var roomID : String = "test-call"

    private var uuid : String = ""

    private var isJoin = false

    private var isMute = false

    private var isVideo = false

    private var isSpeaker = true

    private var isBackCamera = true

    private var remoteVideoTrack: VideoTrack? = null

    private val sdpObserver = object : AppSdpObserver() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val b = intent.extras ?: return
        roomID = b.getString("RoomID", "")
        isJoin = b.getBoolean("IsJoin", false)
        uuid = b.getString("Uuid", "")

        audioManager.start(object : RTCAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: RTCAudioManager.AudioDevice?,
                availableAudioDevices: Set<RTCAudioManager.AudioDevice?>?
            ) {
                Log.e(Tag, "selectedAudioDevice: $selectedAudioDevice, availableAudioDevices: $availableAudioDevices") }
            })
        audioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        init()
        initDisplay()
        setListener()

        viewModel = ViewModelProvider(this)[DataViewModel::class.java]
        viewModel.controller.observe(this) {
            isMute = it.isMute
            isVideo = it.isVideo
            isSpeaker = it.isSpeakMode
            isBackCamera = it.isBackCamera
        }
    }

    private fun init() {
        rtcClient = RTCClient(application, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                signallingClient.sendIceCandidate(p0, isJoin)
                rtcClient.addIceCandidate(p0)
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

        rtcClient.initSurfaceView(binding.remoteView, "remote")
        rtcClient.initSurfaceView(binding.localView, "local")
        rtcClient.setVideo(binding.localView ,uuid, false)
        rtcClient.setAudioStream(uuid)
        signallingClient =  SignalingClient(roomID, createSignallingClientListener())
        if (!isJoin)
            rtcClient.call(sdpObserver, roomID)
    }

    private fun createSignallingClientListener() = object : SignalingClientListener {
        override fun onConnectionEstablished() {
            Log.e(Tag, "[Signalling] onConnectionEstablished")
            binding.imgEndCall.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            Log.e(Tag, "[Signalling] onOfferReceived")
            rtcClient.onRemoteSessionReceived(description)
            Constants.isIntiatedNow = false
            rtcClient.answer(sdpObserver, roomID)
        }

        override fun onAnswerReceived(description: SessionDescription) {
            Log.e(Tag, "[Signalling] onAnswerReceived")
            rtcClient.onRemoteSessionReceived(description)
            Constants.isIntiatedNow = false
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            Log.e(Tag, "[Signalling] onIceCandidateReceived")
            rtcClient.addIceCandidate(iceCandidate)
        }

        override fun onCallEnded() {
            if (!Constants.isCallEnded) {
                Constants.isCallEnded = true
                rtcClient.endCall(roomID)
                finish()
                startActivity(Intent(this@RTCActivity, MainActivity::class.java))
            }
        }
    }

    private fun initDisplay() {
        binding.run {
            Glide.with(this@RTCActivity).asGif().load(R.raw.loading).override(imgLoading.width, imgLoading.height).into(imgLoading)
        }
    }

    private fun setListener() {
        binding.run {
            imgMic.setOnClickListener {
                rtcClient.enableAudio(!isMute)
                viewModel.updateController(Controller(!isMute, isVideo, isSpeaker, isBackCamera))
            }

            imgEndCall.setOnClickListener {
                rtcClient.endCall(roomID)
                Constants.isCallEnded = true
                finish()
                startActivity(Intent(this@RTCActivity, MainActivity::class.java))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        remoteVideoTrack?.removeSink(binding.remoteView)

        binding.remoteView.release()
        binding.localView.release()

        audioManager.stop()
    }
}