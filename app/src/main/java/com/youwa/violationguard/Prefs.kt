package com.youwa.violationguard

import android.content.Context

/** 简单的 SharedPreferences 封装：车牌集合 + 总开关 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vg", Context.MODE_PRIVATE)

    var plates: Set<String>
        get() = sp.getStringSet("plates", emptySet()) ?: emptySet()
        set(v) { sp.edit().putStringSet("plates", v).apply() }

    var alarmEnabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) { sp.edit().putBoolean("enabled", v).apply() }
}
