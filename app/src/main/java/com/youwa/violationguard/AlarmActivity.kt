package com.youwa.violationguard

import android.app.Activity
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/** 全屏警报界面：循环播放警报声 + 震动，点"停止"关闭并恢复音量 */
class AlarmActivity : Activity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var prevVolume = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_alarm)

        findViewById<TextView>(R.id.tv_alarm_sender).text =
            "发件人：${intent.getStringExtra("sender") ?: "未知"}"
        findViewById<TextView>(R.id.tv_alarm_body).text =
            intent.getStringExtra("body") ?: ""

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopAlarm()
            finish()
        }
        startAlarm()
    }

    private fun startAlarm() {
        val am = getSystemService(AudioManager::class.java)
        prevVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0
        )
        player = MediaPlayer.create(this, R.raw.siren)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            start()
        }
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
    }

    private fun stopAlarm() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        if (prevVolume >= 0) {
            getSystemService(AudioManager::class.java)
                .setStreamVolume(AudioManager.STREAM_ALARM, prevVolume, 0)
        }
        getSystemService(NotificationManager::class.java).cancel(AlarmHelper.NOTIF_ID)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
