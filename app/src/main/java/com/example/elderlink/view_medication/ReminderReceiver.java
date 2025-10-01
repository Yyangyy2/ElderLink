package com.example.elderlink.view_medication;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.elderlink.R;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "med_channel";
    private static final long RETRY_DELAY_MS = 1 * 60 * 1000L; // 1 minute (for testing)
    private static final int MAX_RETRIES = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String role = intent.getStringExtra("role");  // "elder" or "caregiver"
            String medId = intent.getStringExtra("medId");
            String medInfo = intent.getStringExtra("medInfo");
            String personName = intent.getStringExtra("personName");
            int retryCount = intent.getIntExtra("retryCount", 0);

            Log.d(TAG, "onReceive medId=" + medId + " retry=" + retryCount + " medInfo=" + medInfo + " role=" + role);

            if (medId == null || medInfo == null) {
                Log.w(TAG, "Missing medId or medInfo");
                return;
            }

            int notifId = medId.hashCode();
            ensureChannel(context);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.logoelderlink_new)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            if ("elder".equals(role)) {
                int notifID_role = notifId + role.hashCode();

                // Action: Taken
                Intent takenIntent = new Intent(context, ReminderActionReceiver.class);
                takenIntent.setAction("ACTION_TAKEN");
                takenIntent.putExtra("medId", medId);

                PendingIntent takenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role,
                        takenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Action: Not Taken
                Intent notTakenIntent = new Intent(context, ReminderActionReceiver.class);
                notTakenIntent.setAction("ACTION_NOT_TAKEN");
                notTakenIntent.putExtra("medId", medId);
                notTakenIntent.putExtra("medInfo", medInfo);
                notTakenIntent.putExtra("retryCount", retryCount);

                PendingIntent notTakenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role + 1,
                        notTakenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Action: Dismissed
                Intent deleteIntent = new Intent(context, ReminderActionReceiver.class);
                deleteIntent.setAction("ACTION_DISMISSED");
                deleteIntent.putExtra("medId", medId);
                deleteIntent.putExtra("medInfo", medInfo);
                deleteIntent.putExtra("retryCount", retryCount);

                PendingIntent deletePI = PendingIntent.getBroadcast(
                        context,
                        notifID_role + 2,
                        deleteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                builder.setContentTitle("Medication Reminder")
                        .setContentText("Remember to eat " + medInfo)
                        .addAction(android.R.drawable.ic_menu_my_calendar, "Taken", takenPI)
                        .addAction(android.R.drawable.ic_menu_revert, "Not taken", notTakenPI)
                        .setDeleteIntent(deletePI);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                }

                // Retry loop for elder
                if (retryCount < MAX_RETRIES) {
                    scheduleRetry(context, medId, medInfo, retryCount, personName);
                }

            } else if ("caregiver".equals(role))  {
                // Caregiver notification
                if (retryCount >= MAX_RETRIES) {
                    // Once elder hit MAX_RETRIES → Trigger caregiver alert
                    notifyCaregiver(context, personName, medInfo, medId);
                } else {
                    builder.setContentTitle("Medication Reminder").setContentText("Reminder for " + personName + " : " + medInfo);
                }

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "onReceive error", e);
        }
    }

    private void notifyCaregiver(Context context, String personName, String medInfo, String medId) {
        NotificationCompat.Builder caregiverBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logoelderlink_new)
                .setContentTitle("Caregiver Alert")
                .setContentText(personName + " has not taken all medicines for 3 mins")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(medId.hashCode() + 999, caregiverBuilder.build());
        }
    }


    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }
    }

    public static void scheduleRetry(Context context, String medId, String medInfo, int currentRetry, String personName) {
        try {
            int nextRetry = currentRetry + 1;
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                Log.w(TAG, "AlarmManager null");
                return;
            }

            Intent i = new Intent(context, ReminderReceiver.class);
            i.putExtra("medId", medId);
            i.putExtra("medInfo", medInfo);
            i.putExtra("personName", personName);

            if (nextRetry > MAX_RETRIES) {
                // 👉 After last retry, escalate to caregiver
                i.putExtra("role", "caregiver");
                i.putExtra("retryCount", nextRetry);
            } else {
                // Normal elder retry
                i.putExtra("role", "elder");
                i.putExtra("retryCount", nextRetry);
            }

            int requestCode = medId.hashCode() ^ (nextRetry * 7919);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = System.currentTimeMillis() + RETRY_DELAY_MS;
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);

            Log.d(TAG, "scheduleRetry medId=" + medId + " nextRetry=" + nextRetry + " role=" + i.getStringExtra("role"));

        } catch (Exception e) {
            Log.e(TAG, "scheduleRetry error", e);
        }
    }

}
