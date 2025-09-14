package com.example.elderlink.view_medication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.elderlink.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {
    // static in-memory storage for meds with the same time slot
    private static final Map<String, ArrayList<String>> groupedMeds = new HashMap<>();

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String medInfo = intent.getStringExtra("medInfo"); // already packed

        if (medInfo == null || date == null || time == null) return;

        String key = date + " " + time;

        // group medications that share the same date+time
        ArrayList<String> meds = groupedMeds.getOrDefault(key, new ArrayList<>());
        meds.add(medInfo);
        groupedMeds.put(key, meds);

        // create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "med_channel",
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // join all meds into one text
        StringBuilder bigText = new StringBuilder();
        for (String m : meds) {
            bigText.append("• ").append(m).append("\n");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "med_channel")
                .setSmallIcon(R.drawable.view_medication_logo)
                .setContentTitle("Medication Reminder (" + time + ")")
                .setContentText("You have " + meds.size() + " medications to take.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText.toString().trim()))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        // use the same notification ID for the same date+time so they merge
        manager.notify(key.hashCode(), builder.build());
    }
}
