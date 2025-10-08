package com.example.elderlink.view_gps;

import static android.content.Intent.getIntent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.elderlink.R;
import com.google.android.gms.location.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GPSActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker elderMarker;
    private Marker caregiverMarker;
    private ListenerRegistration elderListener;
    private ListenerRegistration caregiverListener;
    private FusedLocationProviderClient fusedLocationClient;

    // Status UI elements
    private TextView tvLocationStatus, tvBatteryStatus, tvLastSeen, tvAccuracy;
    private ImageView ivBatteryIcon, ivAccuracyIcon;
    private FloatingActionButton fabRefresh;
    private LinearLayout statusDashboard;

    private String caregiverUid;
    private String personUid;
    private String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Important: Initialize osmdroid configuration
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.gps_page);

        personName = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");

        // Set activity title with person's name
        if (personName != null) {
            setTitle(personName + "'s Location");
        }

        initializeViews();
        setupMap();
        setupStatusDashboard();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get one-time location update
        getOneTimeLocation();

        startElderLocationListener();
        startCaregiverLocationListener();
        startElderBatteryListener(); // Listen for elder's battery status
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);

        // Status dashboard views//
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvLastSeen = findViewById(R.id.tvLastSeen);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        ivBatteryIcon = findViewById(R.id.ivBatteryIcon);
        ivAccuracyIcon = findViewById(R.id.ivAccuracyIcon);
        fabRefresh = findViewById(R.id.fabRefresh);
        statusDashboard = findViewById(R.id.statusDashboard);

        // Setup refresh FAB
        fabRefresh.setOnClickListener(v -> refreshLocation());
    }

    private void setupMap() {
        // Set tile source programmatically
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(5.4141, 100.3288)); // Penang default

        // Optional: Set zoom limits
        mapView.setMinZoomLevel(5.0);
        mapView.setMaxZoomLevel(19.0);
    }

    private void setupStatusDashboard() {
        // Initial status
        updateLocationStatus("Updating...", "#2962FF");
        updateElderBatteryStatus(-1, false, 0); // Unknown initially
        updateLastSeen(System.currentTimeMillis());
        updateAccuracy(10.0f); // Default high accuracy
    }

    @SuppressLint("MissingPermission")
    private void getOneTimeLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
            return;
        }

        updateLocationStatus("Getting location...", "#FF9800");
        Log.d("GPSActivity", "Getting one-time location update");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        handleLocationUpdate(location, "Last known");
                    } else {
                        updateLocationStatus("No last location", "#F44336");
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    updateLocationStatus("Location error", "#F44336");
                    Log.e("GPSActivity", "Failed to get last location: " + e.getMessage());
                    requestFreshLocation();
                });
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        updateLocationStatus("Getting fresh location...", "#FF9800");

        LocationRequest request = LocationRequest.create();
        request.setNumUpdates(1);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        request.setMaxWaitTime(10000);

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        handleLocationUpdate(location, "Live");
                    } else {
                        updateLocationStatus("No location found", "#F44336");
                    }
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper());
    }

    private void handleLocationUpdate(Location location, String source) {
        Log.d("GPSActivity", "Got " + source + " location: " + location.getLatitude() + ", " + location.getLongitude());

        // Update status
        updateLocationStatus("Live", "#4CAF50");
        updateAccuracy(location.getAccuracy());
        updateLastSeen(System.currentTimeMillis());

        // Update map and Firestore
        updateMarker(location.getLatitude(), location.getLongitude(), true);
        updateLocationInFirestore(location);
    }

    private void refreshLocation() {
        // Show refreshing state
        fabRefresh.setEnabled(false);
        updateLocationStatus("Refreshing...", "#FF9800");

        // Get new location
        getOneTimeLocation();

        // Re-enable FAB after a delay
        fabRefresh.postDelayed(() -> fabRefresh.setEnabled(true), 5000);
    }

    // Status update methods
    private void updateLocationStatus(String status, String colorHex) {
        if (tvLocationStatus != null) {
            tvLocationStatus.setText(status);
            tvLocationStatus.setTextColor(android.graphics.Color.parseColor(colorHex));
        }
    }

    private void updateElderBatteryStatus(int batteryPercent, boolean isCharging, long lastUpdate) {
        if (tvBatteryStatus == null || ivBatteryIcon == null) return;

        if (batteryPercent == -1) {
            tvBatteryStatus.setText("Unknown");
            ivBatteryIcon.setImageResource(R.drawable.battery_unknown_icon);
            tvBatteryStatus.setTextColor(android.graphics.Color.parseColor("#666666"));
            return;
        }

        // Add charging indicator if applicable
        String batteryText = batteryPercent + "%" + (isCharging ? " ⚡" : "");
        tvBatteryStatus.setText(batteryText);

        // Set appropriate battery icon and color based on level
        int batteryIcon;
        if (batteryPercent >= 80) {
            batteryIcon = R.drawable.battery_high_icon;
            tvBatteryStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if (batteryPercent >= 30) {
            batteryIcon = R.drawable.battery_medium_icon;
            tvBatteryStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            batteryIcon = R.drawable.battery_low_icon;
            tvBatteryStatus.setTextColor(android.graphics.Color.parseColor("#F44336"));
        }

        // If charging, you might want to use a different icon
        if (isCharging) {
            batteryIcon = R.drawable.battery_charging_icon;
        }

        ivBatteryIcon.setImageResource(batteryIcon);

        // Update last seen based on battery update time if it's more recent
        if (lastUpdate > 0) {
            updateLastSeen(lastUpdate);
        }
    }

    private void updateLastSeen(long timestamp) {
        if (tvLastSeen == null) return;

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        String timeText;
        if (diff < 60000) { // Less than 1 minute
            timeText = "Just now";
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if (diff < 3600000) { // Less than 1 hour
            long minutes = diff / 60000;
            timeText = minutes + " min ago";
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            timeText = sdf.format(new Date(timestamp));
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#F44336"));
        }

        tvLastSeen.setText(timeText);
    }

    private void updateAccuracy(float accuracy) {
        if (tvAccuracy == null || ivAccuracyIcon == null) return;

        if (accuracy < 20) {
            tvAccuracy.setText("High accuracy");
            tvAccuracy.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if (accuracy < 50) {
            tvAccuracy.setText("Medium accuracy");
            tvAccuracy.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            tvAccuracy.setText("Low accuracy");
            tvAccuracy.setTextColor(android.graphics.Color.parseColor("#F44336"));
        }
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
                updateLocationStatus("Permission denied", "#F44336");
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
                            Long timestamp = (Long) locationData.get("timestamp");
                            if (lat != null && lon != null) {
                                updateMarker(lat, lon, true);
                                if (timestamp != null) {
                                    updateLastSeen(timestamp);
                                }
                                Log.d("GPSActivity", "Elder location updated from Firestore: " + lat + ", " + lon);
                            }
                        }
                    }
                });
    }

    private void startElderBatteryListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Listen for elder's battery status updates
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("GPSActivity", "Listen failed for elder battery", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Get battery data from elder's device
                        Long batteryLevel = snapshot.getLong("batteryLevel");
                        Boolean isCharging = snapshot.getBoolean("isCharging");
                        Long batteryUpdate = snapshot.getLong("lastBatteryUpdate");

                        if (batteryLevel != null) {
                            updateElderBatteryStatus(
                                    batteryLevel.intValue(),
                                    isCharging != null ? isCharging : false,
                                    batteryUpdate != null ? batteryUpdate : 0
                            );
                            Log.d("GPSActivity", "Elder battery updated: " + batteryLevel + "%");
                        } else {
                            // No battery data available yet
                            updateElderBatteryStatus(-1, false, 0);
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
                marker.setIcon(getResources().getDrawable(R.drawable.caregiver_marker));
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
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (elderListener != null) elderListener.remove();
        if (caregiverListener != null) caregiverListener.remove();
        Log.d("GPSActivity", "GPSActivity destroyed");
    }
}