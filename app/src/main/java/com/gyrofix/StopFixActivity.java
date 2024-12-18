package com.gyrofix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StopFixActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        stopService(new Intent(this, GyroFixService.class));
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}