package com.youwa.violationguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * 监听收到的短信。命中规则：
 * 短信内容包含任一已绑定车牌，并且（发件人疑似官方 或 内容含违章关键词）
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = Prefs(context.applicationContext)
        if (!prefs.alarmEnabled) return
        val plates = prefs.plates
        if (plates.isEmpty()) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val sender = parts[0].originatingAddress ?: ""
        val body = parts.joinToString("") { it.messageBody ?: "" }
        if (body.isEmpty()) return

        if (isViolationSms(sender, body, plates)) {
            AlarmHelper.trigger(context.applicationContext, sender, body)
        }
    }

    private fun isViolationSms(sender: String, body: String, plates: Set<String>): Boolean {
        val upperBody = body.uppercase()
        val hitPlate = plates.any { p -> p.isNotEmpty() && upperBody.contains(p.uppercase()) }
        if (!hitPlate) return false

        val s = sender.lowercase()
        val officialSender = s.contains("12123") || s.contains("1069") ||
                sender.contains("交警") || sender.contains("公安") || sender.contains("交管")
        val keywords = listOf(
            "违章", "违法", "交通违法", "违停", "已被记录", "处罚",
            "扣分", "罚款", "违法停车", "未按规定停放", "违法行为"
        )
        val hitKeyword = keywords.any { body.contains(it) }
        return officialSender || hitKeyword
    }
}
