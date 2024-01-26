package com.example.localproxyconnector;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.app.PendingIntent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ProxyService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ProxyServiceChannel";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());

            // Обработка действия кнопки "Stop Service"
            if (intent != null && "STOP_FOREGROUND".equals(intent.getAction())) {
                stopForeground(true);
                stopSelf();
                System.exit(0); // Этот вызов завершит приложение
                return START_NOT_STICKY;
            }

            // Здесь вызывайте методы для запуска серверов

            return START_STICKY;
        } catch (Exception e) {
            Log.e("ProxyService", "Error in onStartCommand", e);
            return START_NOT_STICKY;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Proxy Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Создаем Intent для действия кнопки
        Intent stopIntent = new Intent(this, ProxyService.class);
        stopIntent.setAction("STOP_FOREGROUND");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE  // Добавляем FLAG_IMMUTABLE
        );

// Добавляем кнопку "Stop Service"
        NotificationCompat.Action stopAction = new NotificationCompat.Action(
                R.drawable.ic_stop_service,
                "Stop Service",
                stopPendingIntent
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Proxy Service")
                .setContentText("Running in the background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .addAction(stopAction)  // Добавляем кнопку
                .build();
    }
}
