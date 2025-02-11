import javax.swing.*
import java.awt.*
import kotlin.concurrent.thread

class ServerApp : JFrame("TCP Server") {

    private val server = TCPServer(25565)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 500)

        val client = JTextArea(20, 50)
        val statsTextArea = JTextArea(5, 50)
        val IPsTextArea = JTextArea(10, 10)
        val bannedIpsTextArea = JTextArea(5, 10)
        val commandTextField = JTextField(30)

        server.setLogTextArea(client)
        server.setStatsTextArea(statsTextArea)
        server.setIPsTextArea(IPsTextArea)
        server.setCommandTextField(commandTextField)
        server.setBannedIpsTextArea(bannedIpsTextArea)

        thread { server.start() }

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

                add(JScrollPane(statsTextArea))
                add(JScrollPane(IPsTextArea))
                add(JScrollPane(bannedIpsTextArea))
            }
            add(leftPanel, BorderLayout.WEST)

            val rightPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JScrollPane(client))
                add(commandTextField)
            }
            add(rightPanel, BorderLayout.CENTER)
        }

        add(panel)
        isVisible = true
    }

    private fun updateStats(statsTextArea: JTextArea) {
        // Обновление статистики
    }
}

fun main() {
    SwingUtilities.invokeLater {
        ServerApp()
    }
}
