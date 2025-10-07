package com.example.elderlink.view_gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.elderlink.R;
import com.google.android.gms.location.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class GPSActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker elderMarker;
    private Marker caregiverMarker;
    private ListenerRegistration elderListener;
    private ListenerRegistration caregiverListener;
    private FusedLocationProviderClient fusedLocationClient;

    private String caregiverUid;
    private String personUid;
    private String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.gps_page);

        personName = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");

        // Set activity title with person's name
        if (personName != null) {
            setTitle(personName + "'s Location");
        }

        mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(5.4141, 100.3288)); // Penang default

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get one-time location update
        getOneTimeLocation();

        startElderLocationListener();
        startCaregiverLocationListener();
    }

    @SuppressLint("MissingPermission")
    private void getOneTimeLocation() {
        // First, check if we have permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if we don't have it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
            return;
        }

        Log.d("GPSActivity", "Getting one-time location update");

        // Try to get last known location first (fastest)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d("GPSActivity", "Got last known location: " + location.getLatitude() + ", " + location.getLongitude());
                        updateLocationInFirestore(location);
                        updateMarker(location.getLatitude(), location.getLongitude(), true);
                    } else {
                        // If no last location, request a fresh one
                        Log.d("GPSActivity", "No last known location, requesting fresh location");
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GPSActivity", "Failed to get last location: " + e.getMessage());
                    requestFreshLocation();
                });
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        LocationRequest request = LocationRequest.create();
        request.setNumUpdates(1); // We only want one location update
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        request.setMaxWaitTime(10000); // Maximum wait time 10 seconds

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.d("GPSActivity", "Got fresh location: " + location.getLatitude() + ", " + location.getLongitude());
                        updateLocationInFirestore(location);
                        updateMarker(location.getLatitude(), location.getLongitude(), true);
                    }
                }
                // Remove updates after getting one location
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper());
    }

    private void updateLocationInFirestore(Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("location", locationData);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("GPSActivity", "One-time location updated in Firestore"))
                .addOnFailureListener(e ->
                        Log.e("GPSActivity", "Failed to update location: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getOneTimeLocation();
            } else {
                Log.e("GPSActivity", "Location permission denied");
            }
        }
    }

    private void startElderLocationListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        elderListener = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("GPSActivity", "Listen failed for elder location", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get("location");
                        if (locationData != null) {
                            Double lat = (Double) locationData.get("latitude");
                            Double lon = (Double) locationData.get("longitude");
                            if (lat != null && lon != null) {
                                updateMarker(lat, lon, true);
                                Log.d("GPSActivity", "Elder location updated from Firestore: " + lat + ", " + lon);
                            }
                        }
                    }
                });
    }

    private void startCaregiverLocationListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        caregiverListener = db.collection("users")
                .document(caregiverUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("GPSActivity", "Listen failed for caregiver location", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get("location");
                        if (locationData != null) {
                            Double lat = (Double) locationData.get("latitude");
                            Double lon = (Double) locationData.get("longitude");
                            if (lat != null && lon != null) {
                                updateMarker(lat, lon, false);
                                Log.d("GPSActivity", "Caregiver location: " + lat + ", " + lon);
                            }
                        }
                    }
                });
    }

    private void updateMarker(double lat, double lon, boolean isElder) {
        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = isElder ? elderMarker : caregiverMarker;

        if (marker == null) {
            marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            // Use person's name for the marker title
            if (isElder) {
                String markerTitle = (personName != null) ? personName + "'s Location" : "Elder's Location";
                marker.setTitle(markerTitle);
            } else {
                marker.setTitle("Caregiver's Location");
                marker.setIcon(getResources().getDrawable(R.drawable.caregiver_marker, null));
            }
            mapView.getOverlays().add(marker);

            if (isElder) elderMarker = marker;
            else caregiverMarker = marker;
        } else {
            marker.setPosition(point);
        }

        mapView.getController().animateTo(point);
        mapView.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (elderListener != null) elderListener.remove();
        if (caregiverListener != null) caregiverListener.remove();

        // No need to stop service since we're not using it anymore
        Log.d("GPSActivity", "GPSActivity destroyed");
    }
}