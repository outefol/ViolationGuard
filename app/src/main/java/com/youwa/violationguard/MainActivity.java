package com.youwa.violationguard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 主界面：绑定多个车牌、总开关、测试警报、权限申请 */
public class MainActivity extends Activity {

private Prefs prefs;
private ArrayAdapter<String> adapter;
private final List<String> plates = new ArrayList<>();

@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_main);
prefs = new Prefs(this);

EditText etPlate = findViewById(R.id.et_plate);
Button btnAdd = findViewById(R.id.btn_add);
ListView listView = findViewById(R.id.lv_plates);
Switch swEnable = findViewById(R.id.sw_enable);
Button btnTest = findViewById(R.id.btn_test);
TextView tvPerm = findViewById(R.id.tv_perm);
Button btnPerm = findViewById(R.id.btn_perm);

plates.addAll(prefs.getPlates());
Collections.sort(plates);
adapter = new ArrayAdapter<>(this, R.layout.item_plate, R.id.tv_plate_text, plates);
listView.setAdapter(adapter);

btnAdd.setOnClickListener(v -> {
String p = etPlate.getText().toString().trim().toUpperCase().replace(" ", "");
if (p.isEmpty()) {
toast("请输入车牌号");
return;
}
if (plates.contains(p)) {
toast("该车牌已添加");
return;
}
plates.add(p);
prefs.setPlates(new java.util.HashSet<>(plates));
adapter.notifyDataSetChanged();
etPlate.getText().clear();
toast("已添加 " + p);
});

listView.setOnItemClickListener((parent, view, pos, id) -> {
String p = plates.get(pos);
new AlertDialog.Builder(this)
.setTitle("删除车牌")
.setMessage("确定删除 " + p + " 吗？")
.setPositiveButton("删除", (d, w) -> {
plates.remove(pos);
prefs.setPlates(new java.util.HashSet<>(plates));
adapter.notifyDataSetChanged();
})
.setNegativeButton("取消", null)
.show();
});

swEnable.setChecked(prefs.isAlarmEnabled());
swEnable.setOnCheckedChangeListener((v, checked) -> {
prefs.setAlarmEnabled(checked);
toast(checked? "警报已开启": "警报已关闭");
});

Spinner spSound = findViewById(R.id.sp_sound);
ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(
this, R.array.sound_names, android.R.layout.simple_spinner_item);
soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
spSound.setAdapter(soundAdapter);
spSound.setSelection(prefs.getSoundIndex());
spSound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
@Override
public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
prefs.setSoundIndex(pos);
}

@Override
public void onNothingSelected(AdapterView<?> parent) {
}
});

btnTest.setOnClickListener(v -> {
String sample = plates.isEmpty()? "浙A12345": plates.get(0);
AlarmHelper.trigger(this, "1063912123",
"您名下号牌为" + sample + "的机动车有交通违法未处理（测试短信）。");
});

btnPerm.setOnClickListener(v -> requestPerms());
refreshPerm(tvPerm, btnPerm);
if (checkSelfPermission(Manifest.permission.RECEIVE_SMS)
!= PackageManager.PERMISSION_GRANTED) {
requestPerms();
}

TextView tvOverlay = findViewById(R.id.tv_overlay);
Button btnOverlay = findViewById(R.id.btn_overlay);
btnOverlay.setOnClickListener(v -> {
Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
android.net.Uri.parse("package:" + getPackageName()));
startActivity(intent);
});
refreshOverlay(tvOverlay, btnOverlay);
}

@Override
protected void onResume() {
super.onResume();
TextView tvOverlay = findViewById(R.id.tv_overlay);
Button btnOverlay = findViewById(R.id.btn_overlay);
if (tvOverlay != null) refreshOverlay(tvOverlay, btnOverlay);
}

private void refreshOverlay(TextView tvOverlay, Button btnOverlay) {
boolean ok = Settings.canDrawOverlays(this);
tvOverlay.setText("后台弹出界面权限：" + (ok? "已授予": "未授予（警报可能只响通知声）"));
btnOverlay.setEnabled(!ok);
}

private void refreshPerm(TextView tvPerm, Button btnPerm) {
boolean smsOk = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
== PackageManager.PERMISSION_GRANTED;
boolean notifOk = Build.VERSION.SDK_INT < 33
|| checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
== PackageManager.PERMISSION_GRANTED;
tvPerm.setText("短信权限：" + (smsOk? "已授予": "未授予")
+ "\n通知权限：" + (notifOk? "已授予": "未授予"));
btnPerm.setEnabled(!smsOk ||!notifOk);
}

private void requestPerms() {
List<String> need = new ArrayList<>();
need.add(Manifest.permission.RECEIVE_SMS);
if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS);
requestPermissions(need.toArray(new String[0]), 100);
}

@Override
public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
super.onRequestPermissionsResult(code, perms, results);
recreate();
}

private void toast(String s) {
Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
}
}
