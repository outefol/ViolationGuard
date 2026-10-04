package com.youwa.violationguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;

/** 触发警报：高优先级全屏通知（锁屏也能弹）+ 前台时直接拉起警报界面 */
public class AlarmHelper {
    public static final String CHANNEL_ID = "violation_alarm_v2";
    private static final String OLD_CHANNEL_ID = "violation_alarm";
    public static final int NOTIF_ID = 1001;

    /** 声音序号 -> raw 资源 */
    public static int getSoundResId(int index) {
        switch (index) {
            case 1: return R.raw.siren_fire;
            case 2: return R.raw.siren_ambulance;
            case 3: return R.raw.siren_airraid;
            case 4: return R.raw.siren_beep;
            default: return R.raw.siren_police;
        }
    }

    public static void trigger(Context context, String sender, String body) {
        ensureChannel(context);
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra("sender", sender);
        intent.putExtra("body", body);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("违章短信警报")
                .setContentText("收到疑似违章短信，发件人：" + sender)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .build();
        // 让通知铃声重复播放，直到用户处理（点开或划掉）
        notification.flags |= Notification.FLAG_INSISTENT;
        nm.notify(NOTIF_ID, notification);

        // App 在前台时直接拉起警报界面（后台时靠上面的全屏通知）
        try {
            Intent i = new Intent(context, AlarmActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("sender", sender);
            i.putExtra("body", body);
            context.startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private static void ensureChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        // 删除无声的旧通道
        try {
            nm.deleteNotificationChannel(OLD_CHANNEL_ID);
        } catch (Exception ignored) {
        }
        Prefs prefs = new Prefs(context);
        int soundIndex = prefs.getSoundIndex();
        // 声音换了就重建通道（通道声音只在创建时生效）
        if (prefs.getChannelSoundIndex() != soundIndex) {
            try {
                nm.deleteNotificationChannel(CHANNEL_ID);
            } catch (Exception ignored) {
            }
        }
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            Uri sirenUri = Uri.parse("android.resource://" + context.getPackageName()
                    + "/" + getSoundResId(soundIndex));
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "违章警报", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("收到违章短信时的警报（重复响铃直到处理）");
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setSound(sirenUri, attrs);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 600, 400});
            nm.createNotificationChannel(ch);
            prefs.setChannelSoundIndex(soundIndex);
        }
    }
}
