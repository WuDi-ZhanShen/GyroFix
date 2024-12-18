package com.gyrofix;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;


@TargetApi(Build.VERSION_CODES.N)
public class StartFixTileService extends TileService {


    @Override
    public void onTileAdded() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isGyroFixServiceRunning() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isGyroFixServiceRunning() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (tile.getState() == Tile.STATE_ACTIVE) {
            stopService(new Intent(this, GyroFixService.class));
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getSharedPreferences("data", 0).getBoolean("foreground", true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled())
                    return;
                startForegroundService(new Intent(this, GyroFixService.class));
            } else
                startService(new Intent(this, GyroFixService.class));
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            try {
                Object statusBarManager = getSystemService("statusbar");
                Method collapse = statusBarManager.getClass().getMethod("collapsePanels");
                collapse.setAccessible(true);
                collapse.invoke(statusBarManager);
            } catch (Exception ignored) {
                Intent intent = new Intent(this,EmptyActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
            }
        } else {
            Intent intent = new Intent(this,EmptyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_IMMUTABLE));
            }else {
                startActivityAndCollapse(intent);
            }
        }

        super.onClick();


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
