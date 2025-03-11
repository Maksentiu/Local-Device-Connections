package org.example.udp

import org.example.DesktopAudioOutput
import org.example.audio.AudioOutput
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress

class UDPAudioClient(
    private val socket: DatagramSocket,
    private val audioOutput: AudioOutput,
    private val listenYourIP: Boolean = false
): UDPSocket(socket) {
    override fun run() {
        socket.soTimeout = 1000

        val inetAddress = getWifiInetAddress()?:InetAddress.getLocalHost()
        audioOutput.initAudioOutput()

        try {
            while (true) {
                val packet = receiveMessage()
            if(packet.length == 0 || !listenYourIP || packet.address.hostAddress != inetAddress.hostAddress)
                audioOutput.write(packet.data, 0, packet.length)
            }
        } catch (e: IOException) {
            throw e
        }
    }
}

fun main() {
    val audioOutput = DesktopAudioOutput()

    val socket = DatagramSocket(5006)
    UDPAudioClient(socket, audioOutput, true).start()
}