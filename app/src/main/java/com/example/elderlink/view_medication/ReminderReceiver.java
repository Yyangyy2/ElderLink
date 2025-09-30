//This java controls the notification:
//Gets triggered by the AlarmManager at the scheduled time.
//
//Reads the medId, medInfo, and retry count from the Intent

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
import androidx.core.content.ContextCompat;

import com.example.elderlink.R;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "med_channel";
    private static final long RETRY_DELAY_MS = 1 * 60 * 1000L; // 5 minutes (For testing purpose 1 min)
    private static final int MAX_RETRIES = 3; //loop 3 times if ignored or Not Taken is pressed

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String role = intent.getStringExtra("role");  //(elder/caregiver)
            String medId = intent.getStringExtra("medId");
            String medInfo = intent.getStringExtra("medInfo");
            int retryCount = intent.getIntExtra("retryCount", 0);


            Log.d(TAG, "onReceive medId=" + medId + " retry=" + retryCount + " medInfo=" + medInfo);

            if (medId == null || medInfo == null) {
                Log.w(TAG, "Missing medId or medInfo");
                return;
            }

            int notifId = medId.hashCode();
            ensureChannel(context);




            //Here, create the buttons which connects to the above PendingIntent below-----------------------------------

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.logoelderlink_new)
                    .setContentTitle("Medication Reminder")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);


            // Elder only sees this--------------------------------------------------------
            if ("elder".equals(role)) {
                int notifID_role = notifId + role.hashCode(); //notification ID (based on role)

                // Action: Taken------------------------------------------------------------------------------------
                Intent takenIntent = new Intent(context, ReminderActionReceiver.class);
                takenIntent.setAction("ACTION_TAKEN");
                takenIntent.putExtra("medId", medId);

                PendingIntent takenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role, // unique request code for taken
                        takenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Action: Not taken------------------------------------------------------------------------------------
                Intent notTakenIntent = new Intent(context, ReminderActionReceiver.class);
                notTakenIntent.setAction("ACTION_NOT_TAKEN");
                notTakenIntent.putExtra("medId", medId);
                notTakenIntent.putExtra("medInfo", medInfo);
                notTakenIntent.putExtra("retryCount", retryCount);

                PendingIntent notTakenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role  + 1, // unique request code for not taken. +1 is for counts of retries but until 3
                        notTakenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // No Action------------------------------------------------------------------------------------------------
                // deleteIntent — fired when user dismisses / swipes the notification
                Intent deleteIntent = new Intent(context, ReminderActionReceiver.class);
                deleteIntent.setAction("ACTION_DISMISSED");
                deleteIntent.putExtra("medId", medId);
                deleteIntent.putExtra("medInfo", medInfo);
                deleteIntent.putExtra("retryCount", retryCount);

                PendingIntent deletePI = PendingIntent.getBroadcast(
                        context,
                        notifID_role  + 2, // unique requestCode for delete
                        deleteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Actions(buttons) for elder's notification-----------------------------------------------------
                builder.setContentText("Remember to eat " + medInfo)
                        .addAction(android.R.drawable.ic_menu_my_calendar, "Taken", takenPI)
                        .addAction(android.R.drawable.ic_menu_revert, "Not taken", notTakenPI)
                        .setDeleteIntent(deletePI);

            }else{
                // Caregiver only sees info-------------------------------------------------------------------------
                String personName = intent.getStringExtra("personName");    // must write this so that system remembers which elder even when app is closed/exited
                builder.setContentText("Reminder for "+ personName +" : " + medInfo);

            }

            //Here, displays the notification out----------------------------------------------------------------------------
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted.");
                return;
            }

            NotificationManagerCompat.from(context).notify(notifId, builder.build());


            // Automatically schedule the next retry, no matter if user pressed Not Taken,swiped away or did nothing (until MAX_RETRIES of 3)
            if (retryCount < MAX_RETRIES) {
                scheduleRetry(context, medId, medInfo, retryCount);
            }


        } catch (Exception e) {
            Log.e(TAG, "onReceive error", e);
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

    // schedules a retry after x minutes (called from ReminderActionReceiver when user taps NOT TAKEN)-------------------------------------------------------------
    public static void scheduleRetry(Context context, String medId, String medInfo, int currentRetry) {
        try {
            int nextRetry = currentRetry + 1;
            if (nextRetry > MAX_RETRIES) {                              //Increase count until reach max_retries
                Log.d(TAG, "Max retries reached for " + medId);
                return;
            }

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                Log.w(TAG, "AlarmManager null");
                return;
            }

            Intent i = new Intent(context, ReminderReceiver.class);
            i.putExtra("medId", medId);
            i.putExtra("medInfo", medInfo);
            i.putExtra("retryCount", nextRetry);
            i.putExtra("role", "elder"); // got retry code,therefore this is for elder


            // Unique requestCode per med+retry to avoid collisions, the 7919 is random, can be 12345
            int requestCode = medId.hashCode() ^ (nextRetry * 7919);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = System.currentTimeMillis() + RETRY_DELAY_MS;
            Log.d(TAG, "scheduleRetry medId=" + medId + " nextRetry=" + nextRetry + " at=" + triggerAt + " req=" + requestCode);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } catch (Exception e) {
            Log.e(TAG, "scheduleRetry error", e);
        }
    }
}
