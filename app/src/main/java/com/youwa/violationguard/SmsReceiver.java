package com.youwa.violationguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 监听收到的短信。命中规则：
 * 短信内容包含任一已绑定车牌，并且（发件人疑似官方 或 内容含违章关键词）
 */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        Context appCtx = context.getApplicationContext();
        Prefs prefs = new Prefs(appCtx);
        if (!prefs.isAlarmEnabled()) return;
        Set<String> plates = prefs.getPlates();
        if (plates.isEmpty()) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        String sender = parts[0].getOriginatingAddress() != null
                ? parts[0].getOriginatingAddress() : "";
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : parts) {
            if (m.getMessageBody() != null) sb.append(m.getMessageBody());
        }
        String body = sb.toString();
        if (body.isEmpty()) return;

        if (isViolationSms(sender, body, plates)) {
            AlarmHelper.trigger(appCtx, sender, body);
        }
    }

    private boolean isViolationSms(String sender, String body, Set<String> plates) {
        String upperBody = body.toUpperCase();
        boolean hitPlate = false;
        for (String p : plates) {
            if (!p.isEmpty() && upperBody.contains(p.toUpperCase())) {
                hitPlate = true;
                break;
            }
        }
        if (!hitPlate) return false;

        String s = sender.toLowerCase();
        boolean officialSender = s.contains("12123") || s.contains("1069")
                || sender.contains("交警") || sender.contains("公安") || sender.contains("交管");
        List<String> keywords = Arrays.asList("违章", "违法", "交通违法", "违停", "已被记录",
                "处罚", "扣分", "罚款", "违法停车", "未按规定停放", "违法行为",
                "额章", "移开", "挪车", "驶离", "拖移");
        boolean hitKeyword = false;
        for (String k : keywords) {
            if (body.contains(k)) {
                hitKeyword = true;
                break;
            }
        }
        return officialSender || hitKeyword;
    }
}
