package org.example.udp

import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress

class UDPServerBroadcast(
    socket: DatagramSocket,
    private val clientsPort: Int
): UDPSocket(socket) {
    override fun run() {
        try {
            val inetAddress = getWifiInetAddress()?: InetAddress.getLocalHost()
            val broadcastAddress = getBroadcast(inetAddress)

            println(inetAddress?.hostAddress)
            println(broadcastAddress?.hostAddress)

            while (true) {
                sendMessage(inetAddress?.hostAddress!!.toByteArray(), broadcastAddress!!, clientsPort)
                sleep(5000)
            }
        } catch (e: IOException) {
            throw e
        }
    }
}

fun main() {
    val socket = DatagramSocket(1000)
    UDPServerBroadcast(socket, 8081).start()
}