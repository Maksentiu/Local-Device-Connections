package org.example.tcp

import java.io.*
import java.net.ServerSocket
import java.net.Socket

class TCPServer(
    private val port: Int,
    private val folderPath: String
) {
    private var serverSocket: ServerSocket? = null

    fun start() {
        try {
            serverSocket = ServerSocket(port)
            while (true) ClientHandler(serverSocket!!.accept(), folderPath).start()
        } catch (e: IOException) {
            serverSocket?.close()
            throw e
        }
    }

    class ClientHandler(
        clientSocket: Socket,
        private val folderPath: String
    ) : TCPSocket(clientSocket) {
        override fun run() {
            receiveFile(folderPath)
        }
    }
}

fun main() {
    val server = TCPServer(port = 8080, folderPath = "D:/server_files")
    server.start()
}