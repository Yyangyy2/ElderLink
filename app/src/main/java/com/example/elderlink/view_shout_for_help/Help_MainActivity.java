// Main activity for the "Shout for Help" feature

package com.example.elderlink.view_shout_for_help;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.elderlink.Caregiver;
import com.example.elderlink.MainActivityElder;
import com.example.elderlink.R;
import com.example.elderlink.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Help_MainActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyVoiceApp";      // Tag for logging
    private static final int PERMISSIONS_REQUEST = 1;           // Request code for basic permissions
    private static final int OVERLAY_PERMISSION_REQUEST = 2;    // Request code for overlay permission
    public static final String ACCESS_KEY = BuildConfig.WAKEWORD_API_KEY;  // Wake word detection API key


    public static String EMERGENCY_NUMBER = null;    // No default number - will be null until selected
    private boolean isServiceEnabled = false;        // Voice detection service default disabled first

    private RecyclerView caregiversRecyclerView;
    private TextView currentContactText, noCaregiversText;
    private CaregiverAdapter_Help caregiverAdapter;
    private List<Caregiver> caregiverList;
    private FirebaseFirestore db;
    private String personUid, personName, caregiverUid;
    private Button switchEDButton, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shout_for_help_page);

        // Get personUid from Intent
        personUid = getIntent().getStringExtra("personUid");
        personName = getIntent().getStringExtra("personName");
        caregiverUid = getIntent().getStringExtra("caregiverUid");


        db = FirebaseFirestore.getInstance();
        caregiverList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadSavedEmergencyNumber();
        loadCaregivers();
        loadServiceState();

        Log.d(TAG, "Help_MainActivity created for person: " + personUid);


        //Back Button (to MainActivityElder)----------------------------------------------------------------
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(Help_MainActivity.this, MainActivityElder.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

    }

    private void initializeViews() {
        caregiversRecyclerView = findViewById(R.id.caregiversRecyclerView);
        currentContactText = findViewById(R.id.currentContactText);
        noCaregiversText = findViewById(R.id.noCaregiversText);
        switchEDButton = findViewById(R.id.switchEDButton);
        btnBack = findViewById(R.id.btnBack);

        // Set up enable/disable button
        switchEDButton.setOnClickListener(v -> toggleService());

        // Set up back button
        btnBack.setOnClickListener(v -> {
            finish();
        });

        updateButtonAppearance();
    }

    private void toggleService() {
        if (isServiceEnabled) {              // Disable the service (default=false)
            stopVoiceService();
            isServiceEnabled = false;
            Toast.makeText(this, "Voice detection disabled", Toast.LENGTH_SHORT).show();
            saveServiceState();
            updateButtonAppearance();
        } else {
            // Check if emergency number is set before enabling
            if (EMERGENCY_NUMBER == null) {
                Toast.makeText(this, "Please select a caregiver with phone number first", Toast.LENGTH_LONG).show();
                return;
            }

            checkPermissionsAndStartService();  // This will start the service if permissions are granted
        }
    }

    private void updateButtonAppearance() {
        if (isServiceEnabled) {
            switchEDButton.setText("Disable Voice Detection");
            switchEDButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            switchEDButton.setText("Enable Voice Detection");
            switchEDButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void loadServiceState() {
        SharedPreferences prefs = getSharedPreferences("EmergencySettings", MODE_PRIVATE);  // Load saved state from SharedPreferences
        isServiceEnabled = prefs.getBoolean("service_enabled", false);
        updateButtonAppearance();

        // If service was enabled, check if it's still running and update state accordingly
        if (isServiceEnabled) {
            // You might want to add logic here to check if the service is actually running
            Log.d(TAG, "Service state loaded: Enabled");
        } else {
            Log.d(TAG, "Service state loaded: Disabled");
        }
    }

    private void saveServiceState() {
        SharedPreferences prefs = getSharedPreferences("EmergencySettings", MODE_PRIVATE);  // Save current state into SharedPreferences
        prefs.edit().putBoolean("service_enabled", isServiceEnabled).apply();
    }

    private void setupRecyclerView() {    // Set up RecyclerView and its adapter (CaregiverAdapter_Help)
        caregiverAdapter = new CaregiverAdapter_Help(caregiverList, new CaregiverAdapter_Help.OnCaregiverSelectListener() {
            @Override
            public void onCaregiverSelected(Caregiver caregiver) {
                if (caregiver.hasPhone()) {
                    // Update the emergency number
                    String newEmergencyNumber = "tel:" + caregiver.getPhone();
                    EMERGENCY_NUMBER = newEmergencyNumber;

                    // Save to SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("EmergencySettings", MODE_PRIVATE);
                    prefs.edit().putString("emergency_number", newEmergencyNumber).apply();

                    updateCurrentContactDisplay();
                    Toast.makeText(Help_MainActivity.this,
                            "Emergency contact set to: " + caregiver.getName(),
                            Toast.LENGTH_SHORT).show();

                    Log.i(TAG, "Emergency number set to: " + caregiver.getPhone());

                    // If service is enabled, restart it with new number
                    if (isServiceEnabled) {
                        stopVoiceService();
                        startVoiceService();
                    }
                } else {
                    Toast.makeText(Help_MainActivity.this,
                            "This caregiver doesn't have a phone number",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        caregiversRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        caregiversRecyclerView.setAdapter(caregiverAdapter);
    }

    private void loadSavedEmergencyNumber() {
        SharedPreferences prefs = getSharedPreferences("EmergencySettings", MODE_PRIVATE);
        EMERGENCY_NUMBER = prefs.getString("emergency_number", null);
        updateCurrentContactDisplay();
    }

    private void updateCurrentContactDisplay() {
        if (EMERGENCY_NUMBER == null) {
            currentContactText.setText("No phone number selected");
            currentContactText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            String displayNumber = EMERGENCY_NUMBER.replace("tel:", "");
            currentContactText.setText(displayNumber);
            currentContactText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void loadCaregivers() {
        Log.d(TAG, "Loading caregivers for elderly person: " + personUid);

        // Search through all users' "people" collections to find who has access to this elderly person
        db.collectionGroup("people")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " total people documents");

                    Set<String> caregiverUids = new HashSet<>();

                    for (QueryDocumentSnapshot personDoc : querySnapshot) {
                        // Check if this person document matches the current elderly person
                        if (personDoc.getId().equals(personUid)) {
                            String docPath = personDoc.getReference().getPath();
                            String[] pathSegments = docPath.split("/");
                            if (pathSegments.length >= 2) {
                                String foundCaregiverUid = pathSegments[1]; // The user UID who owns this person document
                                caregiverUids.add(foundCaregiverUid);
                                Log.d(TAG, "Found caregiver with access: " + foundCaregiverUid);
                            }
                        }
                    }

                    if (caregiverUids.isEmpty()) {
                        Log.d(TAG, "No caregivers found with access to this elderly person");
                        showNoCaregiversMessage();
                    } else {
                        fetchCaregiverDetails(caregiverUids);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding caregivers with access: ", e);
                    showNoCaregiversMessage();
                });
    }

    private void fetchCaregiverDetails(Set<String> caregiverUids) {
        final int totalCaregivers = caregiverUids.size();
        final int[] processedCount = {0};

        Log.d(TAG, "Fetching details for " + totalCaregivers + " caregivers");

        for (String caregiverUid : caregiverUids) {
            db.collection("users")
                    .document(caregiverUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String phone = documentSnapshot.getString("phone");

                            // Use default values if data is missing
                            String displayName = username != null ? username : "Caregiver";
                            String displayPhone = phone; // Keep as null if not provided

                            Caregiver caregiver = new Caregiver(displayName, "", phone, false);
                            caregiverList.add(caregiver);
                            Log.d(TAG, "Added caregiver: " + displayName + " - Phone: " + (phone != null ? phone : "No phone"));
                        } else {
                            // Add placeholder if user document doesn't exist
                            caregiverList.add(new Caregiver("Caregiver","", null, false));
                        }

                        processedCount[0]++;
                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching caregiver details: ", e);
                        // Add placeholder on failure
                        caregiverList.add(new Caregiver("Caregiver","", null, false));    //email is "", because i do not want modify Caregiver.java, bcz this java is using it, and i do not want to pass email (cause problems)
                        processedCount[0]++;

                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter();
                        }
                    });
        }
    }

    private void showNoCaregiversMessage() {
        runOnUiThread(() -> {
            // Show the no caregivers text and hide the recyclerView
            noCaregiversText.setVisibility(View.VISIBLE);
            caregiversRecyclerView.setVisibility(View.GONE);
            Toast.makeText(this, "No caregivers found with access to your profile", Toast.LENGTH_LONG).show();
        });
    }

    private void updateCaregiverAdapter() {
        runOnUiThread(() -> {
            if (caregiverList.isEmpty()) {
                showNoCaregiversMessage();
                return;
            }

            // Hide no caregivers text and show recyclerView
            noCaregiversText.setVisibility(View.GONE);
            caregiversRecyclerView.setVisibility(View.VISIBLE);

            // Sort list alphabetically
            caregiverList.sort(Comparator.comparing(Caregiver::getName));

            caregiverAdapter.notifyDataSetChanged();
            Log.d(TAG, "Caregiver adapter updated with " + caregiverList.size() + " caregivers");
        });
    }

    // Permission handling and service start/stop logic-------------------------------------------------------------------------------------------
    private void checkPermissionsAndStartService() {
        Log.d(TAG, "Checking permissions");

        String[] perms = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE
        };

        boolean granted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + p);
                granted = false;
                break;
            }
        }

        if (!granted) {
            Log.d(TAG, "Requesting permissions from user");
            ActivityCompat.requestPermissions(this, perms, PERMISSIONS_REQUEST);
        } else {
            Log.d(TAG, "All basic permissions granted, requesting overlay permission");
            requestOverlayPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == PERMISSIONS_REQUEST) {
            boolean all = true;
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "Permission: " + permissions[i] + " = " +
                        (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) all = false;
            }

            if (all) {
                Log.d(TAG, "All basic permissions granted");
                requestOverlayPermission();
            } else {
                Log.e(TAG, "Some permissions were denied");
                Toast.makeText(this,
                        "Microphone and Call permissions are required",
                        Toast.LENGTH_LONG).show();
                // Don't finish, just disable the service
                isServiceEnabled = false;
                saveServiceState();
                updateButtonAppearance();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Log.d(TAG, "Overlay permission granted");
                } else {
                    Log.w(TAG, "Overlay permission not granted, but continuing anyway");
                }
            }
            promptIgnoreBatteryOptimizations();
        }
    }

    private void requestOverlayPermission() {            // Overlay Permission allows an app to  display content over any other application that's currently running on the screen.
        Log.d(TAG, "Checking overlay permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission not granted, requesting from user");
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            } else {
                Log.d(TAG, "Overlay permission already granted");
                promptIgnoreBatteryOptimizations();
            }
        } else {
            Log.d(TAG, "Android version < M, overlay permission not required");
            promptIgnoreBatteryOptimizations();
        }
    }

    private void promptIgnoreBatteryOptimizations() {
        Log.d(TAG, "Checking battery optimization settings");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Log.d(TAG, "Battery optimization not ignored, prompting user");
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Log.d(TAG, "Battery optimization already ignored or not applicable");
                startVoiceService();
            }
        } else {
            Log.d(TAG, "Android version < M, battery optimization not applicable");
            startVoiceService();
        }
    }

    private void startVoiceService() {
        Log.d(TAG, "Attempting to start VoiceService");
        try {
            Intent svc = new Intent(this, VoiceService.class);
            ContextCompat.startForegroundService(this, svc);
            Log.i(TAG, "VoiceService started successfully");
            // Only mark enabled after successful start
            isServiceEnabled = true;
            saveServiceState();
            updateButtonAppearance();
            Toast.makeText(this, "Voice detection enabled", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start VoiceService: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isServiceEnabled = false;
            saveServiceState();
            updateButtonAppearance();
        }
    }

    private void stopVoiceService() {
        Log.d(TAG, "Stopping VoiceService");
        try {
            Intent svc = new Intent(this, VoiceService.class);
            stopService(svc);
            Log.i(TAG, "VoiceService stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop VoiceService: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Help_MainActivity destroyed");
    }
}