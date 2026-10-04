package com.youwa.violationguard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** SharedPreferences 封装：车牌集合 + 总开关 */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getSharedPreferences("vg", Context.MODE_PRIVATE);
    }

    public Set<String> getPlates() {
        return new HashSet<>(sp.getStringSet("plates", Collections.<String>emptySet()));
    }

    public void setPlates(Set<String> plates) {
        sp.edit().putStringSet("plates", plates).apply();
    }

    public boolean isAlarmEnabled() {
        return sp.getBoolean("enabled", true);
    }

    public void setAlarmEnabled(boolean enabled) {
        sp.edit().putBoolean("enabled", enabled).apply();
    }

    /** 选择的警报声音序号（0-4） */
    public int getSoundIndex() {
        return sp.getInt("sound", 0);
    }

    public void setSoundIndex(int index) {
        sp.edit().putInt("sound", index).apply();
    }

    /** 通知通道创建时用的声音序号，用于检测是否需要重建通道 */
    public int getChannelSoundIndex() {
        return sp.getInt("channel_sound", -1);
    }

    public void setChannelSoundIndex(int index) {
        sp.edit().putInt("channel_sound", index).apply();
    }
}
