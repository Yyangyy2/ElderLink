//Reads location from Firestore & displays on map

package com.example.elderlink.view_gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.elderlink.CheckOnElderlyActivity;
import com.example.elderlink.MainActivityElder;
import com.example.elderlink.ProfilePageElder_ElderSide;
import com.example.elderlink.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GPSActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker elderMarker;
    private Marker caregiverMarker;
    private ListenerRegistration elderListener;
    private FusedLocationProviderClient fusedLocationClient;

    // Status UI elements
    private TextView tvLocationStatus, tvBatteryStatus, tvLastSeen, tvAccuracy;
    private ImageView ivBatteryIcon, ivAccuracyIcon;
    private LinearLayout statusDashboard;

    private LocationCallback locationCallback;

    // Safe Zone variables
    private FloatingActionButton fabSafeZone;
    private Polygon safeZoneCircle;
    private boolean isSafeZoneEnabled = false;
    private double safeZoneRadius = 100.0; // default meters
    private double safeZoneCenterLat = 0;
    private double safeZoneCenterLon = 0;

    private String caregiverUid;
    private String personUid;
    private String personName;

    // Location Age tracking variables
    private long currentLocationStartTime = 0;
    private double lastKnownLatitude = 0;
    private double lastKnownLongitude = 0;
    private static final double LOCATION_CHANGE_THRESHOLD = 50.0; // meters

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

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);

        loadSafeZoneFromFirestore();

        startElderLocationListener();
        startElderBatteryListener(); // Listen for elder's battery status
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);



        // Status dashboard views
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvLastSeen = findViewById(R.id.tvLastSeen);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        ivBatteryIcon = findViewById(R.id.ivBatteryIcon);
        ivAccuracyIcon = findViewById(R.id.ivAccuracyIcon);
        statusDashboard = findViewById(R.id.statusDashboard);

        // Safe Zone FAB
        fabSafeZone = findViewById(R.id.fabSafeZone);
        fabSafeZone.setOnClickListener(v -> showSafeZoneDialog());


        // Back button
        FloatingActionButton fabBack = findViewById(R.id.fabBack);
        fabBack.setOnClickListener(v -> {
            Intent intent = new Intent(GPSActivity.this, CheckOnElderlyActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });
    }

    // Map setup--------------------------------------------------------------------------------------------------------------------------------
    private void setupMap() {
        // Set tile source programmatically
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(5.4141, 100.3288)); // Start at Penang default

        // Set zoom limits
        mapView.setMinZoomLevel(5.0);
        mapView.setMaxZoomLevel(19.0);
    }


    // Status dashboard setup--------------------------------------------------------------------------------------------------------------------------------
    private void setupStatusDashboard() {
        // Initial status as loading state
        updateLocationStatus("Finding location...", "#2962FF");
        updateElderBatteryStatus(-1, false, 0); // Unknown initially
        updateLocationAge(System.currentTimeMillis()); // Initialize with current time
        updateAccuracy(10.0f); // Default high accuracy
    }


    //Retrieves previously saved safe zone settings from Firestore------------------------------------------------------------------------------------
    //Fetches enabled status, radius, and center coordinates. If enabled, automatically draws the zone circle on map
    private void loadSafeZoneFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean enabled = documentSnapshot.getBoolean("enabled");
                        Double radius = documentSnapshot.getDouble("radius");
                        Double centerLat = documentSnapshot.getDouble("centerLat");
                        Double centerLon = documentSnapshot.getDouble("centerLon");

                        if (enabled != null) isSafeZoneEnabled = enabled;
                        if (radius != null) safeZoneRadius = radius;
                        if (centerLat != null) safeZoneCenterLat = centerLat;
                        if (centerLon != null) safeZoneCenterLon = centerLon;

                        if (isSafeZoneEnabled && safeZoneCenterLat != 0 && safeZoneCenterLon != 0) {
                            // Draw the safe zone if enabled and center coordinates are set
                            drawSafeZone();
                        }

                        Log.d("GPSActivity", "Safe zone loaded from Firestore: " +
                                (isSafeZoneEnabled ? "Enabled" : "Disabled"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GPSActivity", "Failed to load safe zone: " + e.getMessage());
                });
    }


    // Location Age--------------------------------------------------------------------------------------------------------------------------------------------
    private void updateLocationAge(Location newLocation) {
        if (tvLastSeen == null) return;

        double newLat = newLocation.getLatitude();
        double newLon = newLocation.getLongitude();

        // Check if this is a new location (moved more than threshold)
        if (isNewLocation(newLat, newLon)) {
            // Reset timer for new location
            currentLocationStartTime = System.currentTimeMillis();
            lastKnownLatitude = newLat;
            lastKnownLongitude = newLon;
            Log.d("GPSActivity", "New location detected, resetting timer");
        }

        // Calculate how long they've been at this location
        long currentTime = System.currentTimeMillis();
        long timeAtLocation = currentTime - currentLocationStartTime;

        String locationAgeText = formatLocationAge(timeAtLocation);
        tvLastSeen.setText(locationAgeText);
    }

    private boolean isNewLocation(double newLat, double newLon) {
        if (currentLocationStartTime == 0) {
            return true; // First location
        }

        float[] results = new float[1];
        Location.distanceBetween(
                lastKnownLatitude, lastKnownLongitude,
                newLat, newLon,
                results
        );

        float distanceMoved = results[0];
        return distanceMoved > LOCATION_CHANGE_THRESHOLD;
    }

    //Converts milliseconds to readable time format
    private String formatLocationAge(long timeAtLocationMs) {
        long seconds = timeAtLocationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String ageText;
        int color;

        if (minutes < 5) {
            ageText = "Just arrived";
            color = android.graphics.Color.parseColor("#4CAF50"); // Green
        } else if (minutes < 30) {
            ageText = minutes + " min here";
            color = android.graphics.Color.parseColor("#2196F3"); // Blue
        } else if (hours < 2) {
            ageText = minutes + " min";
            color = android.graphics.Color.parseColor("#FF9800"); // Orange
        } else if (hours < 24) {
            ageText = hours + " hr" + (hours > 1 ? "s" : "") + " here";
            color = android.graphics.Color.parseColor("#FF5722"); // Deep Orange
        } else {
            ageText = days + " day" + (days > 1 ? "s" : "") + " here";
            color = android.graphics.Color.parseColor("#F44336"); // Red
        }

        tvLastSeen.setTextColor(color);
        return ageText;
    }

    // Keep this method for backward compatibility with battery updates
    private void updateLocationAge(long timestamp) {
        if (tvLastSeen == null) return;

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        String timeText;
        if (diff < 60000) { // Less than 1 minute
            timeText = "Just arrived";
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if (diff < 3600000) { // Less than 1 hour
            long minutes = diff / 60000;
            timeText = minutes + " min here";
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#2196F3"));
        } else {
            long hours = diff / 3600000;
            timeText = hours + " hr" + (hours > 1 ? "s" : "") + " here";
            tvLastSeen.setTextColor(android.graphics.Color.parseColor("#FF5722"));
        }

        tvLastSeen.setText(timeText);
    }

    //Converts GPS coordinates to readable physical address
    private void getAddressFromLocation(Location location) {
        // Only show loading if we don't already have an address
        if (tvLocationStatus.getText().toString().contains("Updating") ||
                tvLocationStatus.getText().toString().contains("Getting") ||
                tvLocationStatus.getText().toString().contains("Refreshing")) {
            updateLocationStatus("Getting address...", "#FF9800");
        }

        // Using Android's built-in Geocoder API
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1 // Maximum number of results
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = formatAddress(address);
                updateLocationStatus(addressText, "#4CAF50"); // Green color for success
                Log.d("GPSActivity", "Address found: " + addressText);
            } else {
                // Fallback to coordinates if no address found
                String coordinates = String.format(Locale.getDefault(),
                        "%.6f, %.6f", location.getLatitude(), location.getLongitude());
                updateLocationStatus(coordinates, "#FF9800"); // Orange for coordinates
                Log.d("GPSActivity", "No address found for coordinates, showing coordinates instead");
            }

        } catch (IOException e) {
            Log.e("GPSActivity", "Geocoder error: " + e.getMessage());
            // Fallback to coordinates on error
            String coordinates = String.format(Locale.getDefault(),
                    "%.6f, %.6f", location.getLatitude(), location.getLongitude());
            updateLocationStatus(coordinates, "#F44336"); // Red for error
        }
    }

    //Formats address components into clean readable string
    private String formatAddress(Address address) {
        StringBuilder addressText = new StringBuilder();

        // Add thoroughfare (street name and number)
        if (address.getThoroughfare() != null) {
            addressText.append(address.getThoroughfare());
            if (address.getSubThoroughfare() != null) {
                addressText.append(" ").append(address.getSubThoroughfare()); // Street number
            }
        }

        // Add locality (city) - most important for identification
        if (address.getLocality() != null) {
            if (addressText.length() > 0) addressText.append(", ");
            addressText.append(address.getLocality());
        }

        // Add admin area (state) if no locality found
        if (addressText.length() == 0 && address.getAdminArea() != null) {
            addressText.append(address.getAdminArea());
        }

        // Add postal code for more precision
        if (address.getPostalCode() != null && addressText.length() > 0) {
            addressText.append(" ").append(address.getPostalCode());
        }

        // If still no address, show coordinates as fallback
        if (addressText.length() == 0) {
            addressText.append(String.format(Locale.getDefault(),
                    "Coordinates: %.4f, %.4f", address.getLatitude(), address.getLongitude()));
        }

        // Limit address length for display but keep it readable
        String result = addressText.toString();
        if (result.length() > 50) {
            // Try to keep the important parts (street and city)
            if (result.contains(",")) {
                String[] parts = result.split(",");
                if (parts.length >= 2) {
                    result = parts[0].trim() + ", " + parts[1].trim();
                    if (result.length() > 50) {
                        result = result.substring(0, 47) + "...";
                    }
                } else {
                    result = result.substring(0, 47) + "...";
                }
            } else {
                result = result.substring(0, 47) + "...";
            }
        }

        return result;
    }

    // Status update methods--------------------------------------------------------------------------------------------------------------------------------------------
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
        String batteryText = batteryPercent + "%" + (isCharging ? " (charging)" : "");
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

        // Update location age based on battery update time if it's more recent
        if (lastUpdate > 0) {
            updateLocationAge(lastUpdate);
        }
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


    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //Real-time listener for the elderly person's location updates from Firestore----------------------------------------------------------------------------------------------------------------------------------------------
    //Attaches a Firestore snapshot listener that automatically triggers whenever the elder's location data changes in the database. Updates the map marker and checks for safe zone violations.
    //This method only extracts data from database
    private void startElderLocationListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        elderListener = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .addSnapshotListener((snapshot, e) -> {         //addSnapshotListener to listen for real-time updates
                    if (e != null) {
                        Log.e("GPSActivity", "Listen failed for elder location", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get("location");   // Retrieves the nested location field from the document
                        if (locationData != null) {
                            Double lat = (Double) locationData.get("latitude");                     // Extract latitude
                            Double lon = (Double) locationData.get("longitude");                    // Extract longitude
                            Long timestamp = (Long) locationData.get("timestamp");                  // Extract timestamp
                            if (lat != null && lon != null) {
                                // Create location object
                                Location elderLocation = new Location("elder");
                                elderLocation.setLatitude(lat);
                                elderLocation.setLongitude(lon);
                                elderLocation.setTime(timestamp != null ? timestamp : System.currentTimeMillis());

                                // Check safe zone
                                checkSafeZone(elderLocation);

                                updateMarker(lat, lon, true);
                                updateLocationAge(elderLocation); // Update location age

                                // Also get and display address for elder's location
                                getAddressFromLocation(elderLocation);

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



    //Handle map lifecycle (Resume/pause map rendering map when activity comes to foreground/background to save battery)--------------------------------------------------
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

    // Cleanup when activity closes on destroy to prevent memory leaks
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (elderListener != null) elderListener.remove();

        // Stop location updates if callback exists
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        Log.d("GPSActivity", "GPSActivity destroyed");
    }

    // Safe Zone methods
    private void showSafeZoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_safe_zone, null);
        builder.setView(dialogView);

        TextInputEditText etRadius = dialogView.findViewById(R.id.etRadius);
        SwitchMaterial switchEnable = dialogView.findViewById(R.id.switchEnable);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Set current values
        etRadius.setText(String.valueOf((int) safeZoneRadius));
        switchEnable.setChecked(isSafeZoneEnabled);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            try {
                int radius = Integer.parseInt(etRadius.getText().toString());
                if (radius < 10 || radius > 5000) {
                    Toast.makeText(this, "Radius must be between 10-5000 meters", Toast.LENGTH_SHORT).show();
                    return;
                }

                safeZoneRadius = radius;
                isSafeZoneEnabled = switchEnable.isChecked();

                if (isSafeZoneEnabled && elderMarker != null) {
                    // Use elder's current location as center
                    GeoPoint elderPosition = elderMarker.getPosition();
                    safeZoneCenterLat = elderPosition.getLatitude();
                    safeZoneCenterLon = elderPosition.getLongitude();
                    drawSafeZone();
                } else {
                    removeSafeZone();
                }

                saveSafeZoneToFirestore();
                dialog.dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    //Visualization of the circle--------------------------------------------------------------------------------------------------------
    private void drawSafeZone() {
        removeSafeZone(); // Remove existing circle

        // Create a polygon that approximates a circle
        safeZoneCircle = new Polygon();
        safeZoneCircle.setPoints(createCirclePoints(safeZoneCenterLat, safeZoneCenterLon, safeZoneRadius));
        safeZoneCircle.setFillColor(0x224CAF50);
        safeZoneCircle.setStrokeColor(0xFF4CAF50);
        safeZoneCircle.setStrokeWidth(2.0f);

        mapView.getOverlays().add(safeZoneCircle);
        mapView.invalidate();
    }

    private List<GeoPoint> createCirclePoints(double centerLat, double centerLon, double radiusMeters) {
        List<GeoPoint> points = new ArrayList<>();
        int pointsCount = 36; // Number of points to approximate circle

        for (int i = 0; i < pointsCount; i++) {
            double angle = Math.toRadians(i * (360.0 / pointsCount));

            // Convert meters to degrees (approximate)
            double latOffset = (radiusMeters / 111320.0) * Math.cos(angle);
            double lonOffset = (radiusMeters / (111320.0 * Math.cos(Math.toRadians(centerLat)))) * Math.sin(angle);

            double lat = centerLat + latOffset;
            double lon = centerLon + lonOffset;

            points.add(new GeoPoint(lat, lon));
        }

        // Close the circle
        points.add(points.get(0));
        return points;
    }

    private void removeSafeZone() {
        if (safeZoneCircle != null) {
            mapView.getOverlays().remove(safeZoneCircle);
            safeZoneCircle = null;
            mapView.invalidate();
        }
    }


    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------
    // Detects if elder has exited safe zone and triggers alert-----------[THE SAFEZONE LOGIC HERE]---------------------------------------------------------------------------
    private void checkSafeZone(Location elderLocation) {
        if (!isSafeZoneEnabled || safeZoneCenterLat == 0) return;     //If safeZoneCenterLat == 0, the safe-zone has not been set yet

        float[] results = new float[1];                   //Create array to hold distance result
        Location.distanceBetween(                          //Built in method Location.distanceBetween
                safeZoneCenterLat, safeZoneCenterLon,       //The set safe zone center coordinates
                elderLocation.getLatitude(), elderLocation.getLongitude(),   //The elder's current coordinates
                results
        );

        float distanceFromCenter = results[0];                          //Result show distance in meters, by comparing those two coordinates above
        boolean isOutsideZone = distanceFromCenter > safeZoneRadius;     //Check if distance is greater than the set safe zone radius,
                                                                        // e.g. safeZoneRadius is 10, if distanceFromCenter is 15, then isOutsideZone = true

        if (isOutsideZone) {
            triggerSafeZoneAlert(distanceFromCenter);
        }
    }

    private void triggerSafeZoneAlert(float distance) {
        // Show enhanced local notification with vibration
        showEnhancedLocalNotification(distance);
        // Save alert to Firestore for record keeping
        saveAlertToFirestore(distance);

        // Show local alert in app (map) as toast
        Toast.makeText(this, "ALERT: " + personName + " left safe zone!", Toast.LENGTH_LONG).show();
        // Log the alert
        Log.d("GPSActivity", "Safe zone alert triggered - Distance: " + distance + " meters");

    }


    // Sends urgent alert notification to caregiver------------------------------------------------------------------------
    private void showEnhancedLocalNotification(float distance) {
        String title = "SAFE ZONE ALERT";
        String message = personName + " has left the safe zone. Current distance: " +
                String.format("%.0f", distance) + " meters away. Time: " +
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Show urgent notification
        NotificationHelper.showUrgentNotification(this, title, message, GPSActivity.class);

        // Vibrate for attention
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Vibrate pattern: wait 0, vibrate 1000ms, wait 500ms, vibrate 1000ms
                long[] pattern = {0, 1000, 500, 1000};
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.e("GPSActivity", "Vibration error: " + e.getMessage());
        }
    }

    private void saveAlertToFirestore(float distance) {
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("personName", personName);
        alertData.put("distance", Math.round(distance));
        alertData.put("timestamp", System.currentTimeMillis());
        alertData.put("safeZoneCenter", new HashMap<String, Object>() {{
            put("latitude", safeZoneCenterLat);
            put("longitude", safeZoneCenterLon);
        }});
        alertData.put("radius", safeZoneRadius);

        FirebaseFirestore.getInstance()
                .collection("alerts")
                .add(alertData)
                .addOnSuccessListener(documentReference ->
                        Log.d("GPSActivity", "Alert saved to Firestore: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e("GPSActivity", "Failed to save alert: " + e.getMessage()));
    }

    private void saveSafeZoneToFirestore() {
        Map<String, Object> safeZoneData = new HashMap<>();
        safeZoneData.put("enabled", isSafeZoneEnabled);
        safeZoneData.put("radius", safeZoneRadius);
        safeZoneData.put("centerLat", safeZoneCenterLat);
        safeZoneData.put("centerLon", safeZoneCenterLon);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .set(safeZoneData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("GPSActivity", "Safe zone saved to Firestore"))
                .addOnFailureListener(e ->
                        Log.e("GPSActivity", "Failed to save safe zone: " + e.getMessage()));
    }
}