package com.gyrofix;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class StartFixActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getSharedPreferences("data", 0).getBoolean("foreground", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled())
                return;
            startForegroundService(new Intent(this, GyroFixService.class));
        } else
            startService(new Intent(this, GyroFixService.class));
    }


    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}