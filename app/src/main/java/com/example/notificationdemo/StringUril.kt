package com.example.notificationdemo

object StringUril {
    val Hour = 60*60*1000
    val Min = 60*1000
    val Sec = 1000
    fun parseDuration(process: Int):String{
        val hour = process/ Hour
        val min = process% Hour/ Min
        val sec = process%Min/ Sec

        if (hour ==0){
            return String.format("%02d:%02d",min,sec)
        }else{
            return String.format("%02d:%02d:%02d",hour,min,sec)
        }
    }
}