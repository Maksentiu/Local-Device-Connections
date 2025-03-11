package org.example.udp

import java.io.IOException
import java.net.*

abstract class UDPSocket(
    private val socket: DatagramSocket
) : Thread() {
    protected fun sendMessage(array: ByteArray, inetAddress: InetAddress, port: Int = 8080) {
        try{
            val packet = DatagramPacket(array, array.size, inetAddress, port)
            socket.send(packet)
        } catch(e: IOException) {
            throw e
        }
    }

    protected fun receiveMessage(): DatagramPacket {
        return try{
            val byteArray = ByteArray(4096)
            val packet = DatagramPacket(byteArray, byteArray.size)
            socket.receive(packet)
            packet
        } catch (e: SocketTimeoutException) {
            DatagramPacket(ByteArray(0), 0)
        }
        catch(e: IOException) {
            throw e
        }
    }

    protected fun convertToString(packet: DatagramPacket): String{
        return String(packet.data, 0, packet.length)
    }

    protected fun getBroadcast(inetAddress: InetAddress?): InetAddress? {
        val networkInterface = NetworkInterface.getByInetAddress(inetAddress)
        return networkInterface.interfaceAddresses[0].broadcast
    }

    protected fun getWifiInetAddress(): InetAddress? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while(interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if(networkInterface.isUp && !networkInterface.isLoopback && networkInterface.name.startsWith("wlan")) {
                val address = networkInterface.interfaceAddresses
                for (inetAddress in address.asIterable()) {
                    return inetAddress.address
                }
            }
        }
        return null
    }
}
