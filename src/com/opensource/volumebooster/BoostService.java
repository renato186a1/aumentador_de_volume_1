package com.opensource.volumebooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

public class BoostService extends Service {
    private static final String CHANNEL_ID = "volume_booster_channel";
    private static final int NOTIF_ID = 101;
    private AudioEngine audioEngine;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("booster_prefs", Context.MODE_PRIVATE);
        audioEngine = new AudioEngine();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_BOOST".equals(intent.getAction())) {
            prefs.edit().putInt("boost_percent", 0).apply();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        int boost = prefs.getInt("boost_percent", 0);
        if (boost <= 0) {
            audioEngine.disableAll();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification(boost));
        audioEngine.applySettings(prefs);
        return START_STICKY;
    }

    private Notification buildNotification(int boost) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Aumentador de Volume Ativo",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mostra quando o amplificador de áudio está ligado");
            nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_speaker)
               .setContentTitle("Aumentador de Volume Ativo")
               .setContentText("Boost atual: +" + boost + "% (Toque para ajustar)")
               .setContentIntent(contentIntent)
               .setOngoing(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        if (audioEngine != null) {
            audioEngine.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
