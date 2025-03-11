package org.example.audio

abstract class AudioInput() {
    abstract fun initAudioInput()
    abstract fun read(buffer: ByteArray, off: Int, length: Int): Int
    abstract fun stopAudioInput()
}