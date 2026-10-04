package com.youwa.violationguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** 触发警报：高优先级全屏通知（锁屏也能弹）+ 前台时直接拉起警报界面 */
object AlarmHelper {
    const val CHANNEL_ID = "violation_alarm"
    const val NOTIF_ID = 1001

    fun trigger(context: Context, sender: String, body: String) {
        ensureChannel(context)
        val intent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("sender", sender)
            putExtra("body", body)
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("违章短信警报")
            .setContentText("收到疑似违章短信，发件人：$sender")
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notification)

        // App 在前台时直接拉起警报界面（后台时靠上面的全屏通知）
        try {
            val i = Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("sender", sender)
                putExtra("body", body)
            }
            context.startActivity(i)
        } catch (_: Exception) {
        }
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "违章警报", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "收到违章短信时的全屏警报"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }
}
