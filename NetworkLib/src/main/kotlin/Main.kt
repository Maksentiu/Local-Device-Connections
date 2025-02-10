import java.awt.*
import java.io.*
import java.lang.management.ManagementFactory
import java.net.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.Timer
import kotlin.concurrent.thread
import com.sun.management.OperatingSystemMXBean
import java.text.DecimalFormat

class TCPServer(private val port: Int) {

    private val serverSocket = ServerSocket(port)
    private val bannedIps = mutableSetOf<String>()
    private val bannedIpsFile = "banned_ips.txt"
    private val activeClients = Collections.synchronizedMap(mutableMapOf<String, Socket>())
    private val fileDirectory = "server_files"
    private val inProgressFiles = mutableMapOf<String, Long>()

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

    private fun handleUpload(socket: Socket, filename: String) {
        try {
            logToLogArea("Uploading file: $filename")

            val file = File("$fileDirectory/$filename")
            val inputStream = socket.getInputStream()
            val outputStream = FileOutputStream(file, true) 

            val fileLength = if (file.exists()) file.length() else 0L
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.write("READY $fileLength\r\n") 
            writer.flush()

            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytes = fileLength
            val startTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            outputStream.flush()
            val elapsedTime = System.currentTimeMillis() - startTime
            val bitrate = (totalBytes * 8) / elapsedTime // в битах в секунду
            logToLogArea("Upload complete: $filename, $bitrate bps")
        } catch (e: Exception) {
            logToLogArea("Error uploading file: ${e.message}")
        }
    }

    private fun handleDownload(socket: Socket, filename: String) {
        try {
            val file = File("$fileDirectory/$filename")
            if (!file.exists()) {
                socket.getOutputStream().write("File not found\r\n".toByteArray())
                return
            }

            val inputStream = FileInputStream(file)
            val outputStream = socket.getOutputStream()

            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            outputStream.flush()
            val elapsedTime = System.currentTimeMillis() - startTime
            val bitrate = (totalBytes * 8) / elapsedTime // в битах в секунду
            logToLogArea("Download complete: $filename, $bitrate bps")
        } catch (e: Exception) {
            logToLogArea("Error downloading file: ${e.message}")
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
        try {
            val file = File(bannedIpsFile)
            if (file.exists()) {
                bannedIps.addAll(file.readLines())
            }
        } catch (e: IOException) {
            println("Failed to load banned IPs: ${e.message}")
        }
    }

    private fun saveBannedIps() {
        try {
            val file = File(bannedIpsFile)
            file.writeText(bannedIps.joinToString("\n"))
        } catch (e: IOException) {
            println("Failed to save banned IPs: ${e.message}")
        }
    }
}

class ServerApp : JFrame("TCP Server") {

    private val server = TCPServer(25565)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 500)

        val Client = JTextArea(20, 50).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val statsTextArea = JTextArea(5, 50).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val IPsTextArea = JTextArea(10, 10).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val bannedIpsTextArea = JTextArea(5, 10).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val commandTextField = JTextField(30).apply {
            preferredSize = Dimension(300, 25)
        }

        server.setLogTextArea(Client)
        server.setStatsTextArea(statsTextArea)
        server.setIPsTextArea(IPsTextArea)
        server.setCommandTextField(commandTextField)
        server.setBannedIpsTextArea(bannedIpsTextArea)

        thread {
            server.start()
        }

        val timer = Timer(1000) {
            updateStats(statsTextArea)
        }
        timer.start()

        commandTextField.addActionListener {
            val command = commandTextField.text.trim()
            commandTextField.text = ""
            server.processCommand(command)
        }

        val panel = JPanel().apply {
            layout = BorderLayout()

            val leftPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JScrollPane(statsTextArea).apply {
                    border = BorderFactory.createTitledBorder("Stats")
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    preferredSize = Dimension(300, 100)
                })

                add(JScrollPane(IPsTextArea).apply {
                    border = BorderFactory.createTitledBorder("Connected IPs")
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    preferredSize = Dimension(300, 150)
                })

                add(JScrollPane(bannedIpsTextArea).apply {
                    border = BorderFactory.createTitledBorder("Banned IPs")
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    preferredSize = Dimension(300, 100)
                })
            }
            add(leftPanel, BorderLayout.WEST)

            val rightPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JScrollPane(Client).apply {
                    border = BorderFactory.createTitledBorder("Client Activity")
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    preferredSize = Dimension(500, 440)
                })

                add(commandTextField.apply {
                    preferredSize = Dimension(300, 30)
                })
            }
            add(rightPanel, BorderLayout.CENTER)
        }

        add(panel)
        isVisible = true
    }

    private fun updateStats(statsTextArea: JTextArea) {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val cpuLoad = osBean.processCpuLoad * 100

        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val totalMemoryMB = totalMemory / 1024 / 1024
        val usedMemoryMB = usedMemory / 1024 / 1024
        val freeMemoryMB = freeMemory / 1024 / 1024

        val df = DecimalFormat("#.##")
        val cpuLoadFormatted = df.format(cpuLoad)

        SwingUtilities.invokeLater {
            statsTextArea.text = """
                CPU Load: $cpuLoadFormatted%
                Memory Usage: $usedMemoryMB MB used of $totalMemoryMB MB
                Free Memory: $freeMemoryMB MB
            """.trimIndent()
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        ServerApp()
    }
}
