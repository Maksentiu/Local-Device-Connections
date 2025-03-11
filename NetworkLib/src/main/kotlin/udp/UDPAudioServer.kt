package org.example.udp

import org.example.DesktopAudioInput
import org.example.audio.AudioInput
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress

class UDPAudioServer(
    socket: DatagramSocket,
    private val clientsPort: Int,
    private val audioInput: AudioInput,
): UDPSocket(socket) {
    override fun run() {
        try {
            val inetAddress = getWifiInetAddress()?: InetAddress.getLocalHost()
            val broadcast = getBroadcast(inetAddress)
            audioInput.initAudioInput()

            while (true) {
                val outputBuffer = ByteArray(512)
                val bytesRead = audioInput.read(outputBuffer, 0, outputBuffer.size)
                sendMessage(outputBuffer.copyOf(bytesRead), broadcast!!, clientsPort)
            }
        } catch (e: IOException) {
            throw e
        }
    }
}

fun main() {
    val audioInput = DesktopAudioInput()
    val socket = DatagramSocket()
    UDPAudioServer(socket, 5006, audioInput).start()
}