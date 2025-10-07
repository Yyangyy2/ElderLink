package com.example.elderlink.view_gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ElderLocationService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private LocationCallback locationCallback;

    private String caregiverUid;
    private String personUid;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ElderLocationService", "Service started");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        if (intent != null) {
            caregiverUid = intent.getStringExtra("caregiverUid");
            personUid = intent.getStringExtra("personUid");
            Log.d("ElderLocationService", "CaregiverUID: " + caregiverUid + ", PersonUID: " + personUid);
        } else {
            Log.e("ElderLocationService", "No intent provided!");
            stopSelf();
            return START_NOT_STICKY;
        }

        requestLocationPermission();
        return START_STICKY;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("ElderLocationService", "Location permission not granted!");
            stopSelf();
        } else {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(10000); // every 10 seconds (more reasonable)
        request.setFastestInterval(5000); // fastest update every 5 seconds
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("ElderLocationService", "LocationResult is null");
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("latitude", location.getLatitude());
                    locationData.put("longitude", location.getLongitude());
                    locationData.put("timestamp", System.currentTimeMillis());

                    Log.d("ElderLocationService", "Saving location to Firestore: " +
                            location.getLatitude() + ", " + location.getLongitude());

                    // Create a map with the location data
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("location", locationData);

                    db.collection("users")
                            .document(caregiverUid)
                            .collection("people")
                            .document(personUid)
                            .set(updateData, SetOptions.merge())
                            .addOnSuccessListener(aVoid ->
                                    Log.d("ElderLocationService", "Successfully uploaded location to Firestore"))
                            .addOnFailureListener(e ->
                                    Log.e("ElderLocationService", "Failed to upload location: " + e.getMessage()));
                } else {
                    Log.e("ElderLocationService", "Location is null");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
        Log.d("ElderLocationService", "Location updates started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d("ElderLocationService", "Location updates stopped");
        }
    }
}