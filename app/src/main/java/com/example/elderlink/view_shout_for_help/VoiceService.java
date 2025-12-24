package com.example.elderlink.view_shout_for_help;

import com.example.elderlink.view_shout_for_help.Help_MainActivity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.elderlink.R;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;

public class VoiceService extends Service {

    private static final String TAG = "EmergencyVoiceApp";
    private static final String CHANNEL_ID = "voice_service_channel";
    private static final int NOTIF_ID = 1;
    private PorcupineManager porcupineManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceService onCreate() called");

        // Defensive permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted — stopping VoiceService to avoid SecurityException");
            stopSelf();
            return;
        }

        createNotificationChannel();
        acquireWakeLock();

        Notification notification = buildNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIF_ID, notification);
        }

        initPorcupine();
    }

    private void createNotificationChannel() {
        Log.d(TAG, "Creating notification channel");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Emergency Voice Detection",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Listens for emergency voice commands");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                getSystemService(NotificationManager.class).createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification channel", e);
        }
    }

    private Notification buildNotification() {
        Log.d(TAG, "Building foreground notification");
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Help-Help Listener")
                .setContentText("Listening for 'Help Help'...")
                .setSmallIcon(R.drawable.mic_icon)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void acquireWakeLock() {
        Log.d(TAG, "Acquiring wake lock");
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "VoiceService::WakeLock"
                );
                wakeLock.acquire();
                Log.d(TAG, "Wake lock acquired successfully");
            } else {
                Log.e(TAG, "PowerManager is null, cannot acquire wake lock");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake lock", e);
        }
    }

    private void initPorcupine() {
        Log.d(TAG, "Initializing Porcupine wake word detection");
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(Help_MainActivity.ACCESS_KEY)
                    .setKeywordPath("porcupine/Help-Help_en_android_v3_0_0.ppn")
                    .setSensitivity(0.7f)
                    .build(this, this::onKeywordDetected);

            porcupineManager.start();
            Log.i(TAG, "Porcupine started successfully - Listening for 'Help Help'");

        } catch (PorcupineException e) {
            Log.e(TAG, "Porcupine initialization failed: " + e.getMessage(), e);
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Porcupine initialization", e);
            stopSelf();
        }
    }

    private void onKeywordDetected(int index) {
        Log.i(TAG, "KEYWORD DETECTED! 'Help Help' heard! Index: " + index);

        // Log detection details
        Log.d(TAG, "Detection details:");
        Log.d(TAG, "  - Timestamp: " + System.currentTimeMillis());
        Log.d(TAG, "  - Service state: " + (isServiceRunning() ? "Running" : "Stopped"));
        Log.d(TAG, "  - Thread: " + Thread.currentThread().getName());

        // Acquire full wake lock to wake up device screen
        acquireFullWakeLock();

        // Show emergency notification
        showEmergencyNotification();

        // Make the call directly
        makeEmergencyCallDirectly();
    }

    private boolean isServiceRunning() {
        return porcupineManager != null;
    }

    private void acquireFullWakeLock() {     // Wakes up the device screen
        Log.d(TAG, "Attempting to acquire full wake lock");
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock fullWakeLock = pm.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "VoiceService::FullWakeLock"
                );
                fullWakeLock.acquire(10000); // 10 seconds
                Log.d(TAG, "Full wake lock acquired - Device should wake up");
            } else {
                Log.e(TAG, "PowerManager is null, cannot acquire full wake lock");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire full wake lock", e);
        }
    }

    private void showEmergencyNotification() {
        Log.d(TAG, "Showing emergency notification");
        try {
            Intent intent = new Intent(this, Help_MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.mic_icon)
                    .setContentTitle("EMERGENCY DETECTED")
                    .setContentText("Calling emergency contact...")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setTimeoutAfter(5000);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(101, builder.build());
                Log.d(TAG, "Emergency notification shown");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show emergency notification", e);
        }
    }

    private void makeEmergencyCallDirectly() {
        Log.d(TAG, "Attempting to make emergency call directly");
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);      // Direct call action
            callIntent.setData(Uri.parse(Help_MainActivity.EMERGENCY_NUMBER));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);      // Required when starting activity from service
            callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |    // Clear existing tasks
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            Log.d(TAG, "Starting call activity with number: " + Help_MainActivity.EMERGENCY_NUMBER);
            startActivity(callIntent);
            Log.i(TAG, "Emergency call initiated successfully");

        } catch (SecurityException e) {
            Log.e(TAG, "CALL_PHONE permission denied, falling back to dial", e);
            makeEmergencyDial();
        } catch (Exception e) {
            Log.e(TAG, "Failed to make emergency call", e);
            makeEmergencyDial();
        }
    }

    private void makeEmergencyDial() {        // Fallback to dialer if direct call fails, will not auto call anymore but user must press call
        Log.d(TAG, "Attempting emergency dial fallback");
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse(Help_MainActivity.EMERGENCY_NUMBER));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
            Log.i(TAG, "Emergency dial initiated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to make emergency dial", e);
        }
    }

    // Clean up resources------------------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {
        Log.d(TAG, "VoiceService onDestroy() called");
        super.onDestroy();

        if (porcupineManager != null) {
            try {
                Log.d(TAG, "Stopping Porcupine manager");
                porcupineManager.stop();
                porcupineManager.delete();
                Log.d(TAG, "Porcupine manager stopped and deleted");
            } catch (PorcupineException e) {
                Log.e(TAG, "Error stopping Porcupine", e);
            }
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }

        Log.i(TAG, "VoiceService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VoiceService onStartCommand() - Service is running");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "App task removed from recents, scheduling service restart");

        Intent restartService = new Intent(getApplicationContext(), VoiceService.class);
        restartService.setPackage(getPackageName());

        PendingIntent pendingIntent = PendingIntent.getService(
                this, 1, restartService,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 2000, pendingIntent);
            Log.d(TAG, "Service restart scheduled via AlarmManager");
        } else {
            Log.e(TAG, "AlarmManager is null, cannot schedule restart");
        }

        super.onTaskRemoved(rootIntent);
    }
}
