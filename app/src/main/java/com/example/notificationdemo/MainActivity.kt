package com.example.notificationdemo

import android.annotation.SuppressLint
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class MainActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private val MSG_PROGRESS = 0
    private var duration = 0
    private var init = true
    private var mode = 0

    private val connection by lazy { AudioConnection() }
    var iservice: Iservice? = null
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PROGRESS -> {
                    startUpdateProgress()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, PlayingMusicServices::class.java)
        intent.putExtra("from",PlayingMusicServices.FROM_CONTENT)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        startService(intent)

        button.setOnClickListener(this)
        button2.setOnClickListener(this)
        button3.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)

        EventBus.getDefault().register(this)
    }

    @Subscribe
    fun onEventMainThread(text: String) {
        textView.text = text
        //播放进度
        iservice?.let {
            duration = it.getDuration()
            mode = it.getMode()
            seekBar.max = duration
            startUpdateProgress()
        }
        updatePlayModeBtn()
        changeBtn()
    }


    fun startUpdateProgress() {
        //获取当前进度
        val progress = iservice?.getProgress() ?: 0
        //更新数据
        updateProgress(progress)
        //定时获取进度
        handler.sendEmptyMessageDelayed(MSG_PROGRESS, 1000)
    }

    fun updatePlayMode() {
        //修改service中的模式
        mode = iservice?.updatePlayMode()!!
        //修改图标
        updatePlayModeBtn()
    }

    fun updatePlayModeBtn(){
        button2.text = when (mode) {
            PlayingMusicServices.MODE_SINGLE -> "单曲循环"
            PlayingMusicServices.MODE_ALL -> "默认模式"
            PlayingMusicServices.MODE_RANDOM -> "随机播放"
            else -> "?"
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateProgress(progress: Int) {
        val n1 = StringUril.parseDuration(progress)
        val n2 = StringUril.parseDuration(duration)
        textView2.text = "$n1/$n2"
        seekBar.progress = progress
    }

    fun changeBtn(){
        iservice?.isPlaying()?.let {
            if (it) {
                button.text = "暂停"
                handler.sendEmptyMessageDelayed(MSG_PROGRESS, 1000)
            } else {
                button.text = "播放"
                handler.removeMessages(MSG_PROGRESS)
            }
        }
    }


    inner class AudioConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            iservice = service as Iservice
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //手动解绑服务
        unbindService(connection)
        //反注册Eventbus
        EventBus.getDefault().unregister(this)
        //清空消息，防止内存泄漏
        handler.removeCallbacksAndMessages(null)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button -> {
                iservice?.updatePlayState()
                changeBtn()
            }
            R.id.button2 -> {
                updatePlayMode()
            }
            R.id.button3 ->{
                iservice?.playNext()
            }
        }
    }

    //进度改变
    //fromUser:是否是用户手指拖动改变的进度
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        iservice?.seekToProgress(progress)
        updateProgress(progress)
    }

    //手指触摸
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


}
