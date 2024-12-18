package com.gyrofix;

import android.app.Activity;
import android.os.Bundle;

public class EmptyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoDisplay);
        super.onCreate(savedInstanceState);
        finish();
    }
}
