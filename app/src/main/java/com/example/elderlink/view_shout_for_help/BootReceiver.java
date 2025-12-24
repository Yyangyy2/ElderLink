// Ensures voice wake-word detection service (VoiceService) automatically restarts after the phone reboots.

package com.example.elderlink.view_shout_for_help;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {            //BroadcastReceiver listens to system broadcasts
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            Log.i("BootReceiver", "Device booted, starting VoiceService");

            // Restart service on boot
            Intent serviceIntent = new Intent(context, VoiceService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
