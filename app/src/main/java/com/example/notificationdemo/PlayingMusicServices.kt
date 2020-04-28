package com.example.notificationdemo

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus


@SuppressLint("Registered")
class PlayingMusicServices : Service() {
    companion object {
        const val MODE_RANDOM = 0
        const val MODE_ALL = 1
        const val MODE_SINGLE = 2
        const val FROM_NEXT = 4
        const val FROM_STATE = 5
        const val FROM_CONTENT = 6
    }

    val list = mutableListOf<Uri>()
    var init = false
    var notification:Notification? = null
    val musicList = listOf(R.raw.ten, R.raw.inviting, R.raw.lonely, R.raw.sky, R.raw.world)
    var mode = MODE_ALL
    var position = 0
    val sp by lazy { getSharedPreferences("config", Context.MODE_PRIVATE) }
    var manager: NotificationManager? = null

    private var mediaPlayer: MediaPlayer? = null
    private val binder by lazy { AudioBinder() }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        for (mp3 in musicList) {
            list.add(Uri.parse("android.resource://$packageName/$mp3"))
        }
        //从本地获取播放模式
        mode = sp.getInt("mode", 1)
        Log.d("modechange_sp", sp.getInt("mode", 1).toString())

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!init) {
            init = true
            binder.playItem()
            //notifyUpdateUI()
            Log.d("modechange+", mode.toString())
        } else {
            val from = intent?.getIntExtra("from", -1)
            Log.d("from+++===", from.toString())
            when (from) {
                FROM_CONTENT -> {
                    notifyUpdateUI()
                }
                FROM_STATE -> {
                    //重要重要！！！！！！！！！！！！！！！
                    //现在Android 10访问notification.contentView已经没用了
                    //现在的方法是，发送一条相同id的消息，大概就可以把原来的替换掉
                    //这样就可以动态更新消息
                    binder.updatePlayState()
                    notifyUpdateUI()
                }
                FROM_NEXT -> {
                    binder.playNext()
                    notifyUpdateUI()
                }
                else -> {
                    Log.d("error_in_content", mode.toString())
                }
            }
        }
        return START_NOT_STICKY
    }

    fun notifyUpdateUI() {
        EventBus.getDefault().post(getString(musicList[position]))
    }

    inner class AudioBinder : Binder(), MediaPlayer.OnPreparedListener, Iservice,
        MediaPlayer.OnCompletionListener {
        fun playItem() {
            if (mediaPlayer != null) {
                mediaPlayer?.let {
                    it.reset()
                    it.release()
                }
                mediaPlayer = null
            }
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let {
                it.setOnPreparedListener(this)
                it.setOnCompletionListener(this)
                it.setDataSource(this@PlayingMusicServices, list[position])
                it.prepareAsync()
            }
        }

        private fun autoPlayNext() {
            when (mode) {
                MODE_ALL -> {
                    position = (position + 1) % list.size
                }
                MODE_SINGLE -> {
                }
                MODE_RANDOM -> {
                    position = (0..4).random()
                }
            }
            playItem()
        }


         fun showNotification(text:String) {
            val channelId = createNotificationChannel()
            channelId?.let {
                notification = NotificationCompat.Builder(this@PlayingMusicServices, channelId)
                    .setTicker(getString(musicList[position]))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    //自定义view
                    .setCustomContentView(getRemoteView(text))
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setContentIntent(getPendingIntent())
                    .build()
                manager?.notify(1, notification)
            }

        }

        private fun getRemoteView(text: String): RemoteViews {
            val remoteViews = RemoteViews(packageName, R.layout.notificationlayout)
            //修改标题内容
            remoteViews.setTextViewText(R.id.button4,text)
            remoteViews.setTextViewText(R.id.textView3, getString(musicList[position]))
            remoteViews.setOnClickPendingIntent(R.id.button4, getStatePendingIntent())
            remoteViews.setOnClickPendingIntent(R.id.button5, getNextPendingIntent())
            return remoteViews
        }

        private fun getNextPendingIntent(): PendingIntent {
            val intent = Intent(this@PlayingMusicServices, PlayingMusicServices::class.java)
            intent.putExtra("from", FROM_NEXT)
            val pendingIntent = PendingIntent.getService(
                this@PlayingMusicServices,
                FROM_NEXT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            return pendingIntent
        }

        private fun getStatePendingIntent(): PendingIntent {
            val intent = Intent(this@PlayingMusicServices, PlayingMusicServices::class.java)
            intent.putExtra("from", FROM_STATE)
            val pendingIntent = PendingIntent.getService(
                this@PlayingMusicServices,
                FROM_STATE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            return pendingIntent
        }

        private fun getPendingIntent(): PendingIntent {
            val intent = Intent(this@PlayingMusicServices, MainActivity::class.java)
            intent.putExtra("from", FROM_CONTENT)
            val pendingIntent = PendingIntent.getActivity(
                this@PlayingMusicServices,
                FROM_CONTENT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            return pendingIntent
        }

        private fun createNotificationChannel(): String? {
            //这个在Android 10 环境下，需要通过创建渠道返回id
            manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelID = "my_channel_ID"
            val channel = NotificationChannel(
                channelID,
                "my_channel_NAME",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager!!.createNotificationChannel(channel)
            return channelID
        }


        override fun onPrepared(mp: MediaPlayer?) {
            //准备完成后，进行一系列操作，比如界面更新,比如下一首
            mediaPlayer?.start()
            notifyUpdateUI()
            showNotification("暂停")
        }

        override fun updatePlayState() {
            val playState = mediaPlayer?.isPlaying
            playState?.let {
                if (playState) {
                    mediaPlayer?.pause()
                    showNotification("播放")
                } else {
                    mediaPlayer?.start()
                    showNotification("暂停")

                }
            }
        }

        override fun isPlaying(): Boolean? = mediaPlayer?.isPlaying

        override fun getDuration(): Int = mediaPlayer?.duration ?: 0

        override fun getProgress(): Int = mediaPlayer?.currentPosition ?: 0

        override fun getMode(): Int = mode

        override fun seekToProgress(progress: Int) {
            mediaPlayer?.seekTo(progress)
        }

        override fun updatePlayMode(): Int {
            //修改播放模式
            Log.d("modechange", mode.toString())
            mode = (mode + 1) % 3
            Log.d("modechange", mode.toString())

            //保存播放模式
            sp.edit().putInt("mode", mode).apply()
            return mode
        }

        override fun playNext() {
            when (mode) {
                MODE_RANDOM -> {
                    position = (0..4).random()
                }
                else -> {
                    position = (position + 1) % list.size
                }
            }
            playItem()
        }


        override fun onCompletion(mp: MediaPlayer?) {
            autoPlayNext()
        }
    }
}