//This java controls the action buttons on notification

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

            if ("ACTION_TAKEN".equals(action)) {
                if (nm != null) nm.cancel(notifId);
                // cancel any scheduled retries (best-effort)
                cancelRetries(context, medId);
                Log.d(TAG, "Taken - cancelled notification & retries: " + medId);
            } else if ("ACTION_NOT_TAKEN".equals(action)) {
                if (nm != null) nm.cancel(notifId);
                Log.d(TAG, "Not taken - scheduling retry for " + medId + " currentRetry=" + retryCount);
                // schedule retry (ReminderReceiver will show notification when fired)
                ReminderReceiver.scheduleRetry(context, medId, medInfo, retryCount);
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
