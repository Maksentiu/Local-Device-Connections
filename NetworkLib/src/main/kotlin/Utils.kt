import java.io.*
import javax.swing.JTextArea

fun readFileLines(filePath: String): List<String> {
    return try {
        File(filePath).readLines()
    } catch (e: IOException) {
        println("Failed to read file: ${e.message}")
        emptyList()
    }
}

fun writeFileLines(filePath: String, lines: List<String>) {
    try {
        File(filePath).writeText(lines.joinToString("\n"))
    } catch (e: IOException) {
        println("Failed to write to file: ${e.message}")
    }
}
