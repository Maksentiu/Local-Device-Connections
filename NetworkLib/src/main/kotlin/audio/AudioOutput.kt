package org.example.audio

abstract class AudioOutput() {
    abstract fun initAudioOutput()
    abstract fun write(buffer: ByteArray, off: Int, length: Int)
    abstract fun stopAudioOutput()
}