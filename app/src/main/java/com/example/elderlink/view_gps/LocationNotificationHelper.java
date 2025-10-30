//shows foreground notification when location tracking is active

package com.example.elderlink.view_gps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.elderlink.R;
import com.example.elderlink.MainActivityElder;

public class LocationNotificationHelper {

    public static void startForegroundNotification(
            Context context,
            LocationTrackingService service) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context);
        }

        Intent notificationIntent = new Intent(context, MainActivityElder.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, "location_channel")
                .setContentTitle("Location Tracking Active")
                .setContentText("Your location is being tracked for safety")
                .setSmallIcon(R.drawable.accuracy_icon_pin)
                .setContentIntent(pendingIntent)
                .build();

        service.startForeground(1, notification);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "location_channel",
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
