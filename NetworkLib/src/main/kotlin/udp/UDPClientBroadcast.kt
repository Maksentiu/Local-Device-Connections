package org.example.udp

import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class UDPClientBroadcast(
    private val socket: DatagramSocket,
    private val listenYourIP: Boolean = false
): UDPSocket(socket) {
    private val serverIPs = ConcurrentHashMap<String, Long>()
    private val expressionTime = 10000

    override fun run() {
        socket.soTimeout = 1000
        clearIPAddress()

        try {
            while (true) {
                val packet = receiveMessage()
                val ip = convertToString(packet)

                if(ip.isNotEmpty()) {
                    if(!listenYourIP || ip != InetAddress.getLocalHost().hostAddress) {
                        serverIPs[ip] = System.currentTimeMillis()
                    }
                }
            }
        } catch (e: IOException) {
            throw e
        }
    }

    private fun clearIPAddress() {
        thread {
            while (true) {
                val currentTime = System.currentTimeMillis()
                serverIPs.entries.removeIf { (_, timestamp) ->
                    currentTime - timestamp > expressionTime
                }
                sleep(1000)
            }
        }
    }

    fun getIPsList(): List<String> {
        return serverIPs.keys.toList()
    }
}

fun main() {
    val socket = DatagramSocket(8081)
    val client = UDPClientBroadcast(socket)
    client.start()
    while(true) {
        println(client.getIPsList())
        Thread.sleep(100)
    }
}
