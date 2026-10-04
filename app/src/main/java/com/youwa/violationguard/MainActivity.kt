package com.youwa.violationguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** 主界面：绑定多个车牌、总开关、测试警报、权限申请 */
class MainActivity: Activity() {

private lateinit var prefs: Prefs
private lateinit var adapter: ArrayAdapter<String>
private val plates = mutableListOf<String>()

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_main)
prefs = Prefs(this)

val etPlate = findViewById<EditText>(R.id.et_plate)
val btnAdd = findViewById<Button>(R.id.btn_add)
val listView = findViewById<ListView>(R.id.lv_plates)
val swEnable = findViewById<Switch>(R.id.sw_enable)
val btnTest = findViewById<Button>(R.id.btn_test)
val tvPerm = findViewById<TextView>(R.id.tv_perm)
val btnPerm = findViewById<Button>(R.id.btn_perm)

plates.addAll(prefs.plates.sorted())
adapter = ArrayAdapter(this, R.layout.item_plate, R.id.tv_plate_text, plates)
listView.adapter = adapter

btnAdd.setOnClickListener {
val p = etPlate.text.toString().trim().uppercase().replace(" ", "")
if (p.isEmpty()) {
toast("请输入车牌号")
return@setOnClickListener
}
if (plates.contains(p)) {
toast("该车牌已添加")
return@setOnClickListener
}
plates.add(p)
prefs.plates = plates.toSet()
adapter.notifyDataSetChanged()
etPlate.text.clear()
toast("已添加 $p")
}

listView.setOnItemClickListener { _, _, pos, _ ->
val p = plates[pos]
AlertDialog.Builder(this)
.setTitle("删除车牌")
.setMessage("确定删除 $p 吗？")
.setPositiveButton("删除") { _, _ ->
plates.removeAt(pos)
prefs.plates = plates.toSet()
adapter.notifyDataSetChanged()
}
.setNegativeButton("取消", null)
.show()
}

swEnable.isChecked = prefs.alarmEnabled
swEnable.setOnCheckedChangeListener { _, checked ->
prefs.alarmEnabled = checked
toast(if (checked) "警报已开启" else "警报已关闭")
}

btnTest.setOnClickListener {
val sample = plates.firstOrNull()?: "浙ACQ0397"
AlarmHelper.trigger(
this, "1063912123",
"您名下号牌为${sample}的机动车有交通违法未处理（测试短信）。"
)
}

fun refreshPerm() {
val smsOk = checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
PackageManager.PERMISSION_GRANTED
val notifOk = Build.VERSION.SDK_INT < 33 ||
checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
PackageManager.PERMISSION_GRANTED
tvPerm.text = "短信权限：${if (smsOk) "已授予" else "未授予"}\n" +
"通知权限：${if (notifOk) "已授予" else "未授予"}"
btnPerm.isEnabled =!smsOk ||!notifOk
}
btnPerm.setOnClickListener { requestPerms()}
refreshPerm()
if (checkSelfPermission(Manifest.permission.RECEIVE_SMS)!=
PackageManager.PERMISSION_GRANTED
) {
requestPerms()
}
}

private fun requestPerms() {
val need = mutableListOf(Manifest.permission.RECEIVE_SMS)
if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
requestPermissions(need.toTypedArray(), 100)
}

override fun onRequestPermissionsResult(
code: Int, perms: Array<out String>, results: IntArray
) {
super.onRequestPermissionsResult(code, perms, results)
recreate()
}

private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
