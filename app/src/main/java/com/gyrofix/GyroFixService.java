package com.gyrofix;

import static com.gyrofix.MainActivity.SampleRatePref;
import static com.gyrofix.MainActivity.SampleRatePrefDefValue;
import static com.gyrofix.MainActivity.UpdateIntervalPref;
import static com.gyrofix.MainActivity.UpdateIntervalPrefDefValue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import android.text.TextPaint;
import android.widget.Toast;

import java.util.Locale;


public class GyroFixService extends Service {


    private SensorManager mSensorMgr;// 声明一个传感管理器对象
    private boolean isGyroEnabled = false;

    private boolean isUpdateNotificationEnabled = false;
    private int counter = 0;

    private final Handler handler = new Handler();

    // 定义任务循环间隔时间（以毫秒为单位）
    private int interval = UpdateIntervalPrefDefValue;
    private Bitmap bitmap;
    private Canvas canvas;
    private TextPaint textPaint1, textPaint2, textPaint3;

    // 定义一个Runnable，表示要执行的任务
    private final Runnable taskRunnable = new Runnable() {
        @Override
        public void run() {
            // 执行任务逻辑
            int counterPerSec = counter / interval;
            notification.setContentText(getString(R.string.noti_realtime_text) + counterPerSec + "Hz");


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawCurrentText(counterPerSec);
                notification.setSmallIcon(Icon.createWithBitmap(bitmap));
            }


            notificationManager.notify(1, notification.build());
            counter = 0;
            // 任务执行完后，再次调用 postDelayed 来延迟下一次执行
            handler.postDelayed(this, interval * 1000);
        }
    };

    private Notification.Builder notification;
    private NotificationManager notificationManager;

    public static final String BroadcastIntentFilter = "intent.gyrofix.exit";


    private final SensorEventListener mGyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (isUpdateNotificationEnabled) {
                counter++;
            }
        }

        //当传感器精度改变时回调该方法，一般无需处理
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener myListener = (sharedPreferences, key) -> {
        switch (key) {
            case SampleRatePref:
//                    int sampleRate = sharedPreferences.getInt(SampleRatePref, SampleRatePrefDefValue);
//                    if (isGyroEnabled) {
//                        mSensorMgr.unregisterListener(mGyroListener);
//                        mSensorMgr.registerListener(mGyroListener, mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sampleRate);
//                    }
                break;
            case UpdateIntervalPref:
                interval = sharedPreferences.getInt(UpdateIntervalPref, UpdateIntervalPrefDefValue);
                break;
            default:
                return;
        }

    };


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this,new ComponentName(this, StartFixTileService.class));
        }
        //读取用户的设置项
        SharedPreferences sp = getSharedPreferences("data", 0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mBroadcastReceiver, new IntentFilter(BroadcastIntentFilter), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mBroadcastReceiver, new IntentFilter(BroadcastIntentFilter));
        }

        int sampleRate = sp.getInt(SampleRatePref, SampleRatePrefDefValue);
        //如果用户开启了”使用前台通知“，则发送前台通知
        if (sp.getBoolean("foreground", true)) {

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


            notification = new Notification.Builder(getApplication())
                    .setAutoCancel(true)
                    .setContentText(getString(R.string.noti_text) + (sampleRate > 200 ? getString(R.string.max) : (sampleRate + "Hz")))
                    .setContentTitle(getString(R.string.noti_title))
                    .addAction(android.R.drawable.ic_delete, getString(R.string.noti_stop), PendingIntent.getBroadcast(this, 0, new Intent(BroadcastIntentFilter), PendingIntent.FLAG_IMMUTABLE))
                    .setWhen(System.currentTimeMillis())
                    .setSound(null) // 静音通知
                    .setVibrate(null) // 禁用震动
                    .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.setSmallIcon(Icon.createWithResource(this, R.drawable.tile));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel("daemon", "陀螺仪服务", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableLights(false);
                notificationChannel.setShowBadge(false);
                notificationChannel.setSound(null,null);
                notificationChannel.enableVibration(false);

                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

                notificationManager.createNotificationChannel(notificationChannel);
                notification.setChannelId("daemon");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(1, notification.build());
            }

            if (sp.getBoolean("update_notification", true)) {
                interval = sp.getInt(UpdateIntervalPref,UpdateIntervalPrefDefValue);
                isUpdateNotificationEnabled = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(bitmap);

                    //textPaint1负责三位数电流显示
                    textPaint1 = new TextPaint();
                    textPaint1.setAntiAlias(true);
                    textPaint1.setTextSize(40);
                    textPaint1.setTypeface(Typeface.DEFAULT_BOLD);
                    textPaint1.setTextAlign(Paint.Align.CENTER);
                    textPaint1.setStyle(Paint.Style.FILL);
                    textPaint1.setFakeBoldText(true);
                    textPaint1.setSubpixelText(true);
                    textPaint1.setLetterSpacing(0);

                    //textPaint2负责显示单位，如mA、A、-mA、-A
                    textPaint2 = new TextPaint();
                    textPaint2.setAntiAlias(true);
                    textPaint2.setTextSize(27.0f);
                    textPaint2.setTypeface(Typeface.DEFAULT_BOLD);
                    textPaint2.setTextAlign(Paint.Align.CENTER);
                    textPaint2.setStyle(Paint.Style.FILL);
                    textPaint2.setFakeBoldText(true);
                    textPaint2.setSubpixelText(true);

                    //textPaint3负责两位数电流显示
                    textPaint3 = new TextPaint();
                    textPaint3.setAntiAlias(true);
                    textPaint3.setTextSize(50);
                    textPaint3.setTypeface(Typeface.DEFAULT_BOLD);
                    textPaint3.setTextAlign(Paint.Align.CENTER);
                    textPaint3.setStyle(Paint.Style.FILL);
                    textPaint3.setFakeBoldText(true);
                    textPaint3.setSubpixelText(true);
                    textPaint3.setLetterSpacing(0);

                }


                handler.postDelayed(taskRunnable, interval * 1000);
            }

        }


        //注册传感器监听器
        mSensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //boolean hasGyroSope = mSensorMgr.registerListener(gyrpSopeListener, mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        int sampleInterval =  sampleRate > 200 ? 0 : (1000000 / sampleRate);
        boolean hasGyroSope = mSensorMgr.registerListener(mGyroListener, mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sampleInterval);
        if (!hasGyroSope) {
            Toast.makeText(this, R.string.gyro_unsupport, Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        isGyroEnabled = true;

        sp.registerOnSharedPreferenceChangeListener(myListener);
    }

    public void drawCurrentText(int counter) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        String fo1 = String.valueOf(counter);

        //大于999则使用K为单位，使用大号字体显示
        if (counter > 999) {
            String fo2 = String.format(Locale.getDefault(), "%.1f", counter / 1000f);
            canvas.drawText(fo2, 31f, 40f, textPaint3);
            canvas.drawText( "K", 31f, 64f, textPaint2);
            //大于99则说明是三位数字，使用mA单位即可
        } else if (counter > 99) {
            canvas.drawText(fo1, 31f, 48f, textPaint1);
            //小于等于99则是两位数字，需要使用大号字体显示
        } else {
            canvas.drawText(fo1, 31f, 50f, textPaint3);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isGyroEnabled) mSensorMgr.unregisterListener(mGyroListener);
        if (isUpdateNotificationEnabled) handler.removeCallbacksAndMessages(null);
        unregisterReceiver(mBroadcastReceiver);

        SharedPreferences sp = getSharedPreferences("data", 0);
        sp.unregisterOnSharedPreferenceChangeListener(myListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this,new ComponentName(this, StartFixTileService.class));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
