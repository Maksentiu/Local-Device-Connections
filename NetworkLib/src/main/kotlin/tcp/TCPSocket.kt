package org.example.tcp

import java.io.*
import java.lang.Long.min
import java.net.Socket

abstract class TCPSocket(
    socket: Socket
): Thread() {
    private var dataInputStream = DataInputStream(socket.getInputStream())
    private var dataOutputStream = DataOutputStream(socket.getOutputStream())

    open var progress = 0

    fun sendFile(filePath: String) {
        try {
            val file = File(filePath)
            val fileInputStream = FileInputStream(file)

            dataOutputStream.writeLong(file.length())
            dataOutputStream.writeInt(file.name.length)
            dataOutputStream.write(file.name.toByteArray(), 0, file.name.length)
            dataOutputStream.flush()

            val buffer = ByteArray(4096)
            var send = 0

            while(true) {
                val bytes = fileInputStream.read(buffer)
                if(bytes == -1) break
                dataOutputStream.write(buffer, 0, bytes)
                dataOutputStream.flush()
                send += bytes
                progress = (((file.length() - (file.length() - send)) * 100) / file.length()).toInt()
            }

            fileInputStream.close()
        } catch (e: IOException) {
            throw e
        }
    }

    fun receiveFile(folderPath: String) {
        try {
            var size = dataInputStream.readLong()

            val nameSize = dataInputStream.readInt()
            val nameBuffer = ByteArray(nameSize)
            dataInputStream.read(nameBuffer, 0, nameSize)
            val fileName = String(nameBuffer)

            val folder = File(folderPath)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val fileOutputStream = FileOutputStream(File(folder, fileName))

            val buffer = ByteArray(4096)
            var bytes: Int

            while (true) {
                if (size <= 0) break
                bytes = dataInputStream.read(buffer, 0, min(buffer.size.toLong(), size).toInt())
                if (bytes == -1) break
                fileOutputStream.write(buffer, 0, bytes)
                size -= bytes
            }

            fileOutputStream.close()
            println("File $fileName received successfully.")
        } catch (e: IOException) {
            throw e
        }
    }
}