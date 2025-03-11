package org.example

import org.example.audio.AudioInput
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

class DesktopAudioInput : AudioInput() {
    private lateinit var targetLine: TargetDataLine
    private val format = AudioFormat(16000f, 16, 1, true, false)

    override fun initAudioInput() {
        targetLine = AudioSystem.getTargetDataLine(format)
        targetLine.open(format)
        targetLine.start()
    }

    override fun read(buffer: ByteArray, off: Int, length: Int): Int {
        return targetLine.read(buffer, off, length)
    }

    override fun stopAudioInput() {
        targetLine.stop()
        targetLine.close()
    }
}