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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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
            String personUid = intent.getStringExtra("personUid");
            String caregiverUid = intent.getStringExtra("caregiverUid");
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

                // Action: Taken-----------------------------------------------------------------------
                Intent takenIntent = new Intent(context, ReminderActionReceiver.class);
                takenIntent.setAction("ACTION_TAKEN");
                takenIntent.putExtra("medId", medId);
                takenIntent.putExtra("personName", personName);
                takenIntent.putExtra("personUid", personUid);
                takenIntent.putExtra("caregiverUid", caregiverUid);

                PendingIntent takenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role,
                        takenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Action: Not Taken-----------------------------------------------------------------------
                Intent notTakenIntent = new Intent(context, ReminderActionReceiver.class);
                notTakenIntent.setAction("ACTION_NOT_TAKEN");
                notTakenIntent.putExtra("medId", medId);
                notTakenIntent.putExtra("medInfo", medInfo);
                notTakenIntent.putExtra("personName", personName);
                notTakenIntent.putExtra("personUid", personUid);
                notTakenIntent.putExtra("caregiverUid", caregiverUid);
                notTakenIntent.putExtra("retryCount", retryCount);

                PendingIntent notTakenPI = PendingIntent.getBroadcast(
                        context,
                        notifID_role + 1,
                        notTakenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Action: Dismissed-----------------------------------------------------------------------
                Intent deleteIntent = new Intent(context, ReminderActionReceiver.class);
                deleteIntent.setAction("ACTION_DISMISSED");
                deleteIntent.putExtra("medId", medId);
                deleteIntent.putExtra("medInfo", medInfo);
                deleteIntent.putExtra("personName", personName);
                deleteIntent.putExtra("personUid", personUid);
                deleteIntent.putExtra("caregiverUid", caregiverUid);
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
                    scheduleRetry(context, medId, medInfo, retryCount, personName, personUid, caregiverUid);
                    // Update Firestore medications with status = "Pending" before retryCount reached max
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users")
                            .document(caregiverUid)
                            .collection("people")
                            .document(personUid)
                            .collection("medications")
                            .document(medId)
                            .update("status", "Pending")
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Status updated to Missed for medId=" + medId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));
                } else if (retryCount >= MAX_RETRIES) {
                    // Update Firestore medications with status = "Missed" once retryCount reached max
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users")
                            .document(caregiverUid)
                            .collection("people")
                            .document(personUid)
                            .collection("medications")
                            .document(medId)
                            .update("status", "Missed")
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Status updated to Missed for medId=" + medId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));

                }

            } else {
                // Caregiver notification-----------------------------------------------------------------------
                personName = intent.getStringExtra("personName");    // must write this so that system remembers which elder even when app is closed/exited
                builder.setContentTitle("Medication Reminder").setContentText("Reminder for " + personName + " : " + medInfo);


                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                }
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

    public static void scheduleRetry(Context context, String medId, String medInfo, int currentRetry, String personName,String personUid, String caregiverUid) {
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
            i.putExtra("personUid", personUid);
            i.putExtra("caregiverUid", caregiverUid);
            i.putExtra("retryCount", nextRetry);

            if (nextRetry > MAX_RETRIES) {
                //  After last retry, escalate to caregiver
                i.putExtra("role", "caregiver");
            } else {
                // Normal elder retry
                i.putExtra("role", "elder");
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
