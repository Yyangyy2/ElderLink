//Gets Gps coordinates from device hardware and sends them to firestore

package com.example.elderlink.view_gps;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String personUid;
    private String caregiverUid;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get person and caregiver UIDs from intent
        personUid = intent.getStringExtra("personUid");
        caregiverUid = intent.getStringExtra("caregiverUid");

        // Show foreground notification (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocationNotificationHelper.startForegroundNotification(this, this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();

        return START_STICKY; // Keep running even if app is closed
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        LocationRequest request = LocationRequest.create();
        request.setInterval(30000); // Update every 30 seconds
        request.setFastestInterval(15000); // Minimum 15 seconds between updates
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        request.setSmallestDisplacement(10); // Update every 10 meters

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        sendLocationToFirestore(location);
                        Log.d(TAG, "Location sent: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Location updates started");
    }

    private void sendLocationToFirestore(Location location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());
        locationData.put("accuracy", location.getAccuracy());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("location", locationData);

        // Update elder's location in Firestore
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .update(updateData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Location updated in Firestore"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update location", e));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
