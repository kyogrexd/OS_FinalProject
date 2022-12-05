package lab.italkutalk.tool.webRTC

import org.webrtc.DataChannel

open class DataChanelObserver: DataChannel.Observer {
    override fun onBufferedAmountChange(p0: Long) {
    }

    override fun onStateChange() {
    }

    override fun onMessage(p0: DataChannel.Buffer?) {
    }
}