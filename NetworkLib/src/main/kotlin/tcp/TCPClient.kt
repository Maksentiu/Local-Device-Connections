package org.example.tcp

import java.io.*
import java.net.Socket

class TCPClient(
    host: String,
    port: Int,
    private val folderPath: String
): TCPSocket(Socket(host, port)) {

    override fun run() {
        try {
            sendFile(folderPath)
        } catch (e: IOException) {
            throw e
        }
    }
}

fun main() {
    val client = TCPClient(host = "localhost", port = 8080, folderPath = "D:/client_files/civ6-setup-1.bin")
    client.start()
    while(client.isAlive) {
        println("Progress: ${client.progress}")
        Thread.sleep(100)
    }
}