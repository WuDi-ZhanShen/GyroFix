package com.gyrofix;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {


    public static final String SampleRatePref = "sample_rate";
    public static final int SampleRatePrefDefValue = 200;

    public static final String UpdateIntervalPref = "update_interval";
    public static final int UpdateIntervalPrefDefValue = 1;

    private void showPrivacy() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.privacy_title)
                .setPositiveButton(R.string.agree, (dialogInterface, i) -> getSharedPreferences("data", 0).edit().putBoolean("first", false).apply())
                .setCancelable(false)
                .setMessage(R.string.privacy_content)
                .setNegativeButton(R.string.exit, (dialogInterface, i) -> finish())
                .create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //限定一下横屏时的窗口宽度,让其不铺满屏幕。否则太丑
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            getWindow().getAttributes().width = (getWindowManager().getDefaultDisplay().getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!((PowerManager) getSystemService(Service.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()))
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled()) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 0);
        }
        Button B = findViewById(R.id.b);
        float density = getResources().getDisplayMetrics().density;
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density}, null, null));
        oval.getPaint().setColor(getResources().getColor(R.color.a));
        B.setBackground(oval);
        B.setOnClickListener(v -> showHelp());
        SharedPreferences sp = getSharedPreferences("data", 0);

        //读取用户设置，决定是否隐藏本APP的后台卡片
        ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(sp.getBoolean("hide", true));

        if (sp.getBoolean("first", true)) {
            showPrivacy();
        }
        Switch s1 = findViewById(R.id.s1);
        s1.setChecked(isGyroFixServiceRunning());
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!((PowerManager) getSystemService(Service.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()))
                        startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                }
                if (isChecked)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sp.getBoolean("foreground", true)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled())
                            return;
                        startForegroundService(new Intent(MainActivity.this, GyroFixService.class));
                    } else
                        startService(new Intent(MainActivity.this, GyroFixService.class));
                else
                    stopService(new Intent(MainActivity.this, GyroFixService.class));
            }

        });

        Switch s2 = findViewById(R.id.s2);
        s2.setChecked(sp.getBoolean("foreground", true));
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                sp.edit().putBoolean("foreground", isChecked).apply();
                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled()) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 0);
                }
                Toast.makeText(MainActivity.this, R.string.restart_service, Toast.LENGTH_SHORT).show();
            }
        });

        Switch s3 = findViewById(R.id.s3);
        s3.setChecked(sp.getBoolean("update_notification", true));
        s3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                sp.edit().putBoolean("update_notification", isChecked).apply();
                Toast.makeText(MainActivity.this, R.string.restart_service, Toast.LENGTH_SHORT).show();
            }
        });


        EditText e2 = findViewById(R.id.e2);
        SeekBar sb2 = findViewById(R.id.sb2);
        e2.setText(String.format(Locale.getDefault(), "%d", sp.getInt(UpdateIntervalPref, UpdateIntervalPrefDefValue)));
        sb2.setProgress(sp.getInt(UpdateIntervalPref, UpdateIntervalPrefDefValue));
        sb2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                e2.setText(String.format(Locale.getDefault(), "%d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sp.edit().putInt(UpdateIntervalPref, seekBar.getProgress()).apply();
            }
        });
        e2.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && e2.getText().length() > 0) {
                int value = Integer.parseInt(e2.getText().toString());
                if (value >= 1 && value <= 5) {
                    sp.edit().putInt(UpdateIntervalPref, value).apply();
                    sb2.setProgress(value);
                } else {
                    Toast.makeText(MainActivity.this, R.string.input_5, Toast.LENGTH_SHORT).show();
                    e2.setText(String.format(Locale.getDefault(), "%d", sp.getInt(UpdateIntervalPref, UpdateIntervalPrefDefValue)));
                }
            }
            return false;
        });
        e2.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                int value = Integer.parseInt(e2.getText().toString());
                if (value >= 1 && value <= 5) {
                    sp.edit().putInt(UpdateIntervalPref, value).apply();
                    sb2.setProgress(value);
                } else {
                    Toast.makeText(MainActivity.this, R.string.input_5, Toast.LENGTH_SHORT).show();
                    e2.setText(String.format(Locale.getDefault(), "%d", sp.getInt(UpdateIntervalPref, UpdateIntervalPrefDefValue)));
                }
            }
        });


        EditText e1 = findViewById(R.id.e1);
        SeekBar sb1 = findViewById(R.id.sb1);
        int sampleRate = sp.getInt(SampleRatePref, SampleRatePrefDefValue);
        e1.setText(sampleRate > 200 ? getString(R.string.max) : String.format(Locale.getDefault(), "%d", sampleRate));
        sb1.setProgress(sampleRate > 200 ? 250 : sampleRate);
        sb1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 200) {
                    seekBar.setProgress(250);
                    e1.setText(getString(R.string.max));

                } else {
                    e1.setText(String.format(Locale.getDefault(), "%d", progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                sp.edit().putInt(SampleRatePref, progress > 200 ? 250 : progress).apply();
            }
        });
        e1.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && e1.getText().length() > 0) {
                int value = Integer.parseInt(e1.getText().toString());
                if (value > 200) {
                    sp.edit().putInt(SampleRatePref, 250).apply();
                    sb1.setProgress(250);
                    e1.setText(getString(R.string.max));
                } else if (value >= 10) {
                    sp.edit().putInt(SampleRatePref, value).apply();
                    sb1.setProgress(value);
                } else {
                    Toast.makeText(MainActivity.this, R.string.input_200, Toast.LENGTH_SHORT).show();
                    e1.setText(String.format(Locale.getDefault(), "%d", sp.getInt(SampleRatePref, SampleRatePrefDefValue)));
                }
            }
            return false;
        });
        e1.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                try {
                    int value = Integer.parseInt(e1.getText().toString());
                    if (value > 200) {
                        sp.edit().putInt(SampleRatePref, 250).apply();
                        sb1.setProgress(250);
                        e1.setText(getString(R.string.max));
                    } else if (value >= 10) {
                        sp.edit().putInt(SampleRatePref, value).apply();
                        sb1.setProgress(value);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.input_200, Toast.LENGTH_SHORT).show();
                        e1.setText(String.format(Locale.getDefault(), "%d", sp.getInt(SampleRatePref, SampleRatePrefDefValue)));
                    }
                } catch (Exception ignored) {
                }

            }
        });

        Switch s4 = findViewById(R.id.s4);
        s4.setChecked(sp.getBoolean("hide", true));
        s4.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            sp.edit().putBoolean("hide", isChecked).apply();
            ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(isChecked);
        });

    }


    private void showHelp() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.help_title)
                .setPositiveButton(R.string.enderstand, null)
                .setCancelable(true)
                .setMessage(R.string.help_content)
                .create().show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Switch s1 = findViewById(R.id.s1);
        s1.setChecked(isGyroFixServiceRunning());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            Switch s1 = findViewById(R.id.s1);
            s1.setChecked(isGyroFixServiceRunning());
        }
    }

    public boolean isGyroFixServiceRunning() {

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices.isEmpty()) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
//            Log.d("TAG", "isGyroFixServiceRunning: "+serviceInfo.toString());
//            Log.d("TAG", "GyroFixService.class.getName(): "+GyroFixService.class.getName());
//            Log.d("TAG", "serviceInfo.service.getClassName(): "+serviceInfo.service.getClassName());
            if (GyroFixService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

}