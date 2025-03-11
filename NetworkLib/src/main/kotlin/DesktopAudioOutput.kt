package org.example

import org.example.audio.AudioOutput
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class DesktopAudioOutput : AudioOutput() {
    private lateinit var sourceLine: SourceDataLine
    private val format = AudioFormat(16000f, 16, 1, true, false)

    override fun initAudioOutput() {
        sourceLine = AudioSystem.getSourceDataLine(format)
        sourceLine.open(format)
        sourceLine.start()
    }

    override fun write(buffer: ByteArray, off: Int, length: Int) {
        sourceLine.write(buffer, off, length)
    }

    override fun stopAudioOutput() {
        sourceLine.stop()
        sourceLine.close()
    }
}