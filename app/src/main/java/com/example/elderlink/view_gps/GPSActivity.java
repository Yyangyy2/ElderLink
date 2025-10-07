package com.example.elderlink.view_gps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlink.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Map;

public class GPSActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker elderMarker;
    private Marker caregiverMarker;
    private ListenerRegistration elderListener;
    private ListenerRegistration caregiverListener;

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


        Intent serviceIntent = new Intent(this, ElderLocationService.class);
        serviceIntent.putExtra("caregiverUid", caregiverUid);
        serviceIntent.putExtra("personUid", personUid);
        //serviceIntent.putExtra("personName", personName);
        startService(serviceIntent);


        startElderLocationListener();
        startCaregiverLocationListener();
    }

    private void startElderLocationListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        elderListener = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get("location");
                        if (locationData != null) {
                            double lat = (double) locationData.get("latitude");
                            double lon = (double) locationData.get("longitude");
                            updateMarker(lat, lon, true);
                            Log.d("GPSActivity", "Elder: " + lat + ", " + lon);
                        }
                    }
                });
    }

    private void startCaregiverLocationListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        caregiverListener = db.collection("users")
                .document(caregiverUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get("location");
                        if (locationData != null) {
                            double lat = (double) locationData.get("latitude");
                            double lon = (double) locationData.get("longitude");
                            updateMarker(lat, lon, false);
                            Log.d("GPSActivity", "Caregiver: " + lat + ", " + lon);
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
        if (elderListener != null) elderListener.remove();
        if (caregiverListener != null) caregiverListener.remove();
    }
}

