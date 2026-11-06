package com.opensource.volumebooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("booster_prefs", Context.MODE_PRIVATE);
            boolean autoBoot = prefs.getBoolean("auto_boot", false);
            int boost = prefs.getInt("boost_percent", 0);
            if (autoBoot && boost > 0) {
                Intent serviceIntent = new Intent(context, BoostService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
