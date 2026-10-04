package com.youwa.violationguard;

import android.app.Activity;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

/** 全屏警报界面：循环播放警报声 + 震动，点"停止"关闭并恢复音量 */
public class AlarmActivity extends Activity {

    private MediaPlayer player;
    private Vibrator vibrator;
    private int prevVolume = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 接管响铃：关掉通知的重复铃声，改由本界面用闹钟通道最大音量循环播放
        NotificationManager nm0 = getSystemService(NotificationManager.class);
        if (nm0 != null) nm0.cancel(AlarmHelper.NOTIF_ID);
        setContentView(R.layout.activity_alarm);

        String sender = getIntent().getStringExtra("sender");
        String body = getIntent().getStringExtra("body");
        ((TextView) findViewById(R.id.tv_alarm_sender))
                .setText("发件人：" + (sender != null ? sender : "未知"));
        ((TextView) findViewById(R.id.tv_alarm_body))
                .setText(body != null ? body : "");

        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopAlarm();
            finish();
        });
        startAlarm();
    }

    private void startAlarm() {
        AudioManager am = getSystemService(AudioManager.class);
        prevVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
        am.setStreamVolume(AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        int soundRes = AlarmHelper.getSoundResId(new Prefs(this).getSoundIndex());
        player = MediaPlayer.create(this, soundRes);
        if (player != null) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setLooping(true);
            player.start();
        }
        vibrator = getSystemService(Vibrator.class);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 400}, 0));
        }
    }

    private void stopAlarm() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (prevVolume >= 0) {
            getSystemService(AudioManager.class)
                    .setStreamVolume(AudioManager.STREAM_ALARM, prevVolume, 0);
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(AlarmHelper.NOTIF_ID);
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
