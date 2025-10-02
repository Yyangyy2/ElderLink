package com.example.elderlink.view_medication;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            String medId = intent.getStringExtra("medId");
            String medInfo = intent.getStringExtra("medInfo");
            String personName = intent.getStringExtra("personName");
            String personUid = intent.getStringExtra("personUid");
            String caregiverUid = intent.getStringExtra("caregiverUid");
            int retryCount = intent.getIntExtra("retryCount", 0);

            if (medId == null) {
                Log.w(TAG, "medId null in action");
                return;
            }

            int notifId = medId.hashCode();
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("med_prefs_v1", Context.MODE_PRIVATE);
            String handledKey = "handled_" + medId;

            FirebaseFirestore db = FirebaseFirestore.getInstance(); //connect to Firestore db

            if ("ACTION_TAKEN".equals(action)) {
                prefs.edit().putBoolean(handledKey, true).apply();
                if (nm != null) nm.cancel(notifId);
                cancelRetries(context, medId);

                // Update status to Taken
                db.collection("users")
                        .document(caregiverUid)
                        .collection("people")
                        .document(personUid)
                        .collection("medications")
                        .document(medId)
                        .update("status", "Taken")
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Status updated to Taken"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));

            } else if ("ACTION_NOT_TAKEN".equals(action)) {
                prefs.edit().putBoolean(handledKey, true).apply();
                if (nm != null) nm.cancel(notifId);

                // retry (elder only), no personName needed here
                ReminderReceiver.scheduleRetry(context, medId, medInfo, retryCount, personName, personUid, caregiverUid);

                // Update status to Pending
                db.collection("users")
                        .document(caregiverUid)
                        .collection("people")
                        .document(personUid)
                        .collection("medications")
                        .document(medId)
                        .update("status", "Pending")
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Status updated to Pending"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));

            } else if ("ACTION_DISMISSED".equals(action)) {
                boolean handled = prefs.getBoolean(handledKey, false);
                if (handled) {
                    prefs.edit().remove(handledKey).apply();
                } else {
                    ReminderReceiver.scheduleRetry(context, medId, medInfo, retryCount, personName, personUid, caregiverUid);
                }

                // If dismissed, leave it as Pending unless max retries later make it Missed
                db.collection("users")
                        .document(caregiverUid)
                        .collection("people")
                        .document(personUid)
                        .collection("medications")
                        .document(medId)
                        .update("status", "Pending")
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Status set to Pending (Dismissed)"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update status", e));

            } else {
                Log.w(TAG, "Unknown action: " + action);
            }

        } catch (Exception e) {
            Log.e(TAG, "ActionReceiver error", e);
        }
    }

    private void cancelRetries(Context context, String medId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int r = 1; r <= 5; r++) {
            int requestCode = medId.hashCode() ^ (r * 7919);
            Intent i = new Intent(context, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, i, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null && am != null) {
                am.cancel(pi);
                pi.cancel();
                Log.d(TAG, "Cancelled retry requestCode=" + requestCode);
            }
        }
    }
}
