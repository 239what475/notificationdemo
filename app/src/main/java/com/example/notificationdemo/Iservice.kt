package com.example.notificationdemo

interface Iservice {
    fun updatePlayState()
    fun isPlaying(): Boolean?
    fun getDuration(): Int
    fun getProgress():Int
    fun seekToProgress(progress: Int)
    fun updatePlayMode():Int
    fun playNext()
    fun getMode():Int
}