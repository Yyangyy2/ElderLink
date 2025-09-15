//This java controls the action buttons on notification (handles what happened after pressed Taken or Not Taken)

package com.example.elderlink.view_medication;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            String medId = intent.getStringExtra("medId");
            String medInfo = intent.getStringExtra("medInfo");
            int retryCount = intent.getIntExtra("retryCount", 0);

            if (medId == null) {
                Log.w(TAG, "medId null in action");
                return;
            }

            int notifId = medId.hashCode();
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


            // small preferences flag to avoid double-handling when action + delete run
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("med_prefs_v1", Context.MODE_PRIVATE);
            String handledKey = "handled_" + medId;


            //Here, it shows if pressed Taken, it goes the Cancel method; if pressed Not Taken, it goes to ReminderReceiver's scheduleRetry method----------------------
            if ("ACTION_TAKEN".equals(action)) {
                // mark handled so deleteIntent won't schedule a retry
                prefs.edit().putBoolean(handledKey, true).apply();

                if (nm != null) nm.cancel(notifId);
                cancelRetries(context, medId);

                // keep flag until deleteIntent sees it, then it will remove the flag
            } else if ("ACTION_NOT_TAKEN".equals(action)) {
                prefs.edit().putBoolean(handledKey, true).apply();

                if (nm != null) nm.cancel(notifId);
                ReminderReceiver.scheduleRetry(context, medId, medInfo, retryCount);
                // keep flag until deleteIntent cleanup
            } else if ("ACTION_DISMISSED".equals(action)) {
                boolean handled = prefs.getBoolean(handledKey, false);
                if (handled) {
                    // somebody already handled the notif (action button), just clean up flag
                    prefs.edit().remove(handledKey).apply();
                } else {
                    // user dismissed/swiped without pressing actions — schedule retry
                    ReminderReceiver.scheduleRetry(context, medId, medInfo, retryCount);
                }
            } else {
                Log.w(TAG, "Unknown action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "ActionReceiver error", e);
        }
    }

    private void cancelRetries(Context context, String medId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // best-effort cancellation for a few likely request codes
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
