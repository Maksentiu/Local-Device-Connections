import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

class TCPServer(private val port: Int) {

    private val serverSocket = ServerSocket(port)
    private val bannedIps = mutableSetOf<String>()
    private val activeClients = Collections.synchronizedMap(mutableMapOf<String, Socket>())
    private val fileDirectory = "server_files"

    @Volatile
    private var running = true
    private var logTextArea: JTextArea? = null
    private var statsTextArea: JTextArea? = null
    private var IPsTextArea: JTextArea? = null
    private var commandTextField: JTextField? = null
    private var bannedIpsTextArea: JTextArea? = null

    init {
        loadBannedIps()
        File(fileDirectory).mkdirs()
    }

    // Методы для работы с UI
    fun setLogTextArea(area: JTextArea) {
        logTextArea = area
    }

    fun setStatsTextArea(area: JTextArea) {
        statsTextArea = area
    }

    fun setIPsTextArea(area: JTextArea) {
        IPsTextArea = area
    }

    fun setCommandTextField(field: JTextField) {
        commandTextField = field
    }

    fun setBannedIpsTextArea(area: JTextArea) {
        bannedIpsTextArea = area
    }

    fun start() {
        logToLogArea("Server started on port $port")
        try {
            while (running) {
                val clientSocket = serverSocket.accept()
                val clientIp = clientSocket.inetAddress.hostAddress

                if (bannedIps.contains(clientIp)) {
                    logToLogArea("Blocked client tried to connect: $clientIp")
                    clientSocket.close()
                    continue
                }

                clientSocket.keepAlive = true
                logToLogArea("Client connected: $clientIp")
                activeClients[clientIp] = clientSocket
                updateIPsList()

                thread {
                    handleClient(clientSocket, clientIp)
                    activeClients.remove(clientIp)
                    updateIPsList()
                    logToLogArea("Client disconnected: $clientIp")
                }
            }
        } catch (e: SocketException) {
            if (!running) {
                logToLogArea("Server stopped.")
            } else {
                logToLogArea("Server error: ${e.message}")
            }
        } finally {
            stopServer()
        }
    }

    fun stopServer() {
        running = false
        try {
            activeClients.values.forEach { it.close() }
            activeClients.clear()
            serverSocket.close()
            logToLogArea("Server socket closed.")
        } catch (e: IOException) {
            logToLogArea("Error closing server socket: ${e.message}")
        }
    }

    fun processCommand(command: String) {
        when {
            command.startsWith("ban") -> {
                val ip = command.removePrefix("ban").trim()
                if (ip.isNotEmpty()) {
                    bannedIps.add(ip)
                    disconnectClient(ip)
                    saveBannedIps()
                    logToLogArea("Banned IP: $ip")
                    updateBannedIpsList()
                } else {
                    logToLogArea("Invalid IP address to ban")
                }
            }
            command.startsWith("unban") -> {
                val ip = command.removePrefix("unban").trim()
                if (bannedIps.remove(ip)) {
                    saveBannedIps()
                    logToLogArea("Unbanned IP: $ip")
                    updateBannedIpsList()
                } else {
                    logToLogArea("IP address $ip was not banned")
                }
            }
            command.startsWith("disconnect") -> {
                val ip = command.removePrefix("disconnect").trim()
                if (activeClients.containsKey(ip)) {
                    disconnectClient(ip)
                    logToLogArea("Disconnected client with IP: $ip")
                } else {
                    logToLogArea("No active client with IP: $ip")
                }
            }
            else -> {
                logToLogArea("Unknown command")
            }
        }
    }

    private fun handleClient(socket: Socket, clientIp: String) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            while (true) {
                val command = reader.readLine() ?: break
                logToLogArea("Received from $clientIp: $command")

                when {
                    command.startsWith("ECHO") -> {
                        val response = command.removePrefix("ECHO").trim()
                        writer.write("$response\r\n")
                        writer.flush()
                    }
                    command.startsWith("UPLOAD") -> {
                        val filename = command.removePrefix("UPLOAD").trim()
                        handleUpload(socket, filename)
                    }
                    command.startsWith("DOWNLOAD") -> {
                        val filename = command.removePrefix("DOWNLOAD").trim()
                        handleDownload(socket, filename)
                    }
                    command == "TIME" -> {
                        val time = SimpleDateFormat("HH:mm:ss").format(Date())
                        writer.write("$time\r\n")
                        writer.flush()
                    }
                    command == "CLOSE" -> {
                        writer.write("Goodbye!\r\n")
                        writer.flush()
                        break
                    }
                    else -> {
                        writer.write("Unknown command\r\n")
                        writer.flush()
                    }
                }
            }
        } catch (e: IOException) {
            logToLogArea("Error with client $clientIp: ${e.message}")
        } finally {
            socket.close()
        }
    }

    private fun handleUpload(socket: Socket, filename: String) {
        // Загрузка файла
    }

    private fun handleDownload(socket: Socket, filename: String) {
        // Скачивание файла
    }

    private fun disconnectClient(ip: String) {
        activeClients[ip]?.let { socket ->
            try {
                socket.close()
                activeClients.remove(ip)
                logToLogArea("Client $ip has been forcibly disconnected")
            } catch (e: IOException) {
                logToLogArea("Error disconnecting client $ip: ${e.message}")
            }
        }
    }

    private fun logToLogArea(message: String) {
        SwingUtilities.invokeLater {
            val time = SimpleDateFormat("HH:mm:ss").format(Date())
            logTextArea?.append("[$time] $message\n")
        }
    }

    private fun updateIPsList() {
        SwingUtilities.invokeLater {
            IPsTextArea?.text = activeClients.keys.joinToString("\n")
        }
    }

    private fun updateBannedIpsList() {
        SwingUtilities.invokeLater {
            bannedIpsTextArea?.text = bannedIps.joinToString("\n")
        }
    }

    private fun loadBannedIps() {
        bannedIps.addAll(readFileLines(ServerConstants.BANNED_IPS_FILE))
    }

    private fun saveBannedIps() {
        writeFileLines(ServerConstants.BANNED_IPS_FILE, bannedIps.toList())
    }
}
