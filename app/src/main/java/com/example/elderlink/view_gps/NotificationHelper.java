package com.example.elderlink.view_gps;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.elderlink.R;

public class NotificationHelper {
    private static final String CHANNEL_ID = "elderlink_safe_zone_alerts";
    private static final String CHANNEL_NAME = "Safe Zone Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for safe zone breaches";
    private static final int NOTIFICATION_ID = 1001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showUrgentNotification(Context context, String title, String message, Class<?> targetActivity) {
        try {
            // Create intent for when notification is tapped
            Intent intent = new Intent(context, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.safe_zone_alert_icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setLights(Color.RED, 1000, 1000)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setFullScreenIntent(pendingIntent, true) // Show on locked screen
                    .setTimeoutAfter(60000); // Auto cancel after 1 minute

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d("NotificationHelper", "Urgent notification shown: " + title);
            }
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error showing notification: " + e.getMessage());
        }
    }

    public static void showNormalNotification(Context context, String title, String message, Class<?> targetActivity) {
        try {
            Intent intent = new Intent(context, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.safe_zone_alert_icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
                Log.d("NotificationHelper", "Normal notification shown: " + title);
            }
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error showing normal notification: " + e.getMessage());
        }
    }

    public static void cancelAllNotifications(Context context) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancelAll();
                Log.d("NotificationHelper", "All notifications cancelled");
            }
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error cancelling notifications: " + e.getMessage());
        }
    }

    public static void cancelNotification(Context context, int notificationId) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(notificationId);
                Log.d("NotificationHelper", "Notification cancelled: " + notificationId);
            }
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error cancelling notification: " + e.getMessage());
        }
    }
}