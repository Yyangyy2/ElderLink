package com.example.elderlink;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.elderlink.DrawerMenu;
import com.example.elderlink.LoginElderActivity;
import com.example.elderlink.R;
import com.example.elderlink.view_Ask_Ai.ChatActivityElder;
import com.example.elderlink.view_medication.Model_medication;
import com.example.elderlink.view_medication.ViewMedicationActivityElderSide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivityElder extends AppCompatActivity {

    private final List<Model_medication> medicationList = new ArrayList<>();   //keep meds in Model_medication list for date picker
    private final List<DateGroup> dateGroupList = new ArrayList<>();          // keep grouped meds by date
    private DashboardAdapter dashboardAdapter;                                // adapter for dashboard
    private TextView tvOverallProgress, tvTodayProgress;
    private String caregiverUid;
    private String personUid;
    private BroadcastReceiver batteryReceiver;
    private TextView noMedsToday;

    // Dashboard variables
    private RecyclerView dashboardRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_elder);

        DrawerMenu.setupMenu(this); // Add the Left side menu

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);
        Button logoutButton = findViewById(R.id.logoutButton);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        tvTodayProgress = findViewById(R.id.tvTodayProgress);
        noMedsToday = findViewById(R.id.noMedsToday);

        dashboardRecyclerView = findViewById(R.id.dashboardRecyclerView);
        dashboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dashboardAdapter = new DashboardAdapter(dateGroupList);
        dashboardRecyclerView.setAdapter(dashboardAdapter);





        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");

        nameText.setText(name);


        //For display people caring for you section
        setupCaregiverRecyclerView();

        // Start battery monitoring
        startBatteryMonitoring();

        // Load medications and group by date
        loadMedicationsFromFirestore(caregiverUid, personUid, dateGroupList, dashboardAdapter);





        // Load imageBase64 from Firestore using personUid, because image too large to pass
        if (caregiverUid != null && personUid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference elderRef = db.collection("users")
                    .document(caregiverUid)
                    .collection("people")
                    .document(personUid);

            elderRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageBase64 = documentSnapshot.getString("imageBase64");
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        imageView.setImageBitmap(decodedBitmap);
                    } else {
                        imageView.setImageResource(R.drawable.profile_placeholder);
                    }
                }
            }).addOnFailureListener(e -> {
                imageView.setImageResource(R.drawable.profile_placeholder);
            });
        } else {
            imageView.setImageResource(R.drawable.profile_placeholder);
        }

        //Open Left navigation menu------------------------------------------------ rmb add DrawerMenu.setupMenu(this); on top
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);

        // When clicking the button, open the drawer
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // START is opens from left; END opens from right
        });

        //View Medication Button (to Medication)------------------------------------------------
        ImageButton btnMedication = findViewById(R.id.btnMedication);
        btnMedication.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityElder.this, ViewMedicationActivityElderSide.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", name);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        //View Ask Ai Button (to Ai chatbot)------------------------------------------------
        ImageButton btnAibot = findViewById(R.id.btnAibot);
        btnAibot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityElder.this, ChatActivityElder.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", name);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        //Log out Elder----------------------------------------------------------------------------
        logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            //Redirect to SignupActivity
            Intent intent = new Intent(MainActivityElder.this, LoginElderActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); //ensures current activity is finished
        });



        //Filter by Date Button (to DatePicker)------------------------------------------------
        Button btnFilterDate = findViewById(R.id.btnFilterDate);
        btnFilterDate.setOnClickListener(v -> {
            showDatePickerWithMedDates(medicationList);
        });










    }





    // Battery monitoring methods-------------[Elder send battery status to firestore then caregiver's dashboard reads it]------------------------------------------------------
    private void startBatteryMonitoring() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                if (level != -1 && scale != -1) {
                    int batteryPct = (int) ((level / (float) scale) * 100);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

                    updateBatteryStatusToFirestore(batteryPct, isCharging);
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Also update immediately
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            batteryReceiver.onReceive(this, batteryIntent);
        }
    }

    private void updateBatteryStatusToFirestore(int batteryPercent, boolean isCharging) {
        if (caregiverUid == null || personUid == null) return;

        Map<String, Object> batteryData = new HashMap<>();
        batteryData.put("batteryLevel", batteryPercent);
        batteryData.put("isCharging", isCharging);
        batteryData.put("lastBatteryUpdate", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .set(batteryData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("BatteryMonitor", "Elder battery updated: " + batteryPercent + "%"))
                .addOnFailureListener(e ->
                        Log.e("BatteryMonitor", "Failed to update battery: " + e.getMessage()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
    }







    // Dashboard (medication adherence) methods---------------------------------------------------------------------------------------------------------------------------------
    private void loadMedicationsFromFirestore(String caregiverUid, String personUid, List<DateGroup> dateGroupList, DashboardAdapter adapter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        medicationList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Model_medication medication = document.toObject(Model_medication.class);
                            medication.setId(document.getId());
                            medicationList.add(medication);
                        }
                        groupMedicationsByDate(medicationList);
                        calculateOverallProgress();
                        updatenoMedsToday();  // Show/hide empty state based on data

                    } else {
                        Log.e("Dashboard", "Error getting medications: ", task.getException());
                    }
                });
    }


    private void groupMedicationsByDate(List<Model_medication> medications) {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Model_medication> medsForToday = new ArrayList<>();
        for (Model_medication medication : medications) {
            if (todayStr.equals(medication.getDate())) {
                medsForToday.add(medication);
            }
        }
        dateGroupList.clear();
        if (!medsForToday.isEmpty()) {
            dateGroupList.add(new DateGroup(todayStr, medsForToday));
        }
        dashboardAdapter.notifyDataSetChanged();
        updatenoMedsToday(); // Update empty state after grouping
    }



    // Update empty state visibility
    private void updatenoMedsToday() {
        if (noMedsToday != null) {
            if (dateGroupList.isEmpty()) {
                noMedsToday.setVisibility(View.VISIBLE);
                dashboardRecyclerView.setVisibility(View.GONE);
            } else {
                noMedsToday.setVisibility(View.GONE);
                dashboardRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }






    //Gray out dates without meds in date picker
    private void showDatePickerWithMedDates(List<Model_medication> allMedications) {
        List<String> medDates = new ArrayList<>();
        for (Model_medication med : allMedications) {
            if (!medDates.contains(med.getDate())) {
                medDates.add(med.getDate());
            }
        }
        if (medDates.isEmpty()) {
            // Show message if no medications at all
            if (noMedsToday != null) {
                noMedsToday.setText("No medications available");
                noMedsToday.setVisibility(View.VISIBLE);
                dashboardRecyclerView.setVisibility(View.GONE);
            }
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date minDate = sdf.parse(Collections.min(medDates));
            Date maxDate = sdf.parse(Collections.max(medDates));
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        if (medDates.contains(selectedDate)) {
                            filterByDate(selectedDate, caregiverUid, personUid);
                        } else {
                            // Show message if no meds on selected date
                            if (noMedsToday != null) {
                                noMedsToday.setText("No medications on " + selectedDate);
                                noMedsToday.setVisibility(View.VISIBLE);
                                dashboardRecyclerView.setVisibility(View.GONE);
                            }
                            Log.w("DatePicker", "No medication on " + selectedDate);
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            if (minDate != null) datePickerDialog.getDatePicker().setMinDate(minDate.getTime());
            if (maxDate != null) datePickerDialog.getDatePicker().setMaxDate(maxDate.getTime());
            datePickerDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterByDate(String selectedDate, String caregiverUid, String personUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Model_medication> allMedications = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Model_medication medication = document.toObject(Model_medication.class);
                            medication.setId(document.getId());
                            allMedications.add(medication);
                        }
                        dateGroupList.clear();
                        List<Model_medication> medsForDate = new ArrayList<>();
                        for (Model_medication med : allMedications) {
                            if (selectedDate.equals(med.getDate())) {
                                medsForDate.add(med);
                            }
                        }
                        if (!medsForDate.isEmpty()) {
                            dateGroupList.add(new DateGroup(selectedDate, medsForDate));
                        }
                        dashboardAdapter.notifyDataSetChanged();
                    }
                });
    }






    //Calculate overall progress for today and this week
    private void calculateOverallProgress() {
        if (medicationList.isEmpty()) {
            tvOverallProgress.setText("0%");
            tvTodayProgress.setText("0%");
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date weekStart = calendar.getTime();
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date weekEnd = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int totalMeds = 0, totalTaken = 0, todayTotal = 0, todayTaken = 0;
        String todayStr = sdf.format(new Date());
        for (Model_medication med : medicationList) {
            try {
                Date medDate = sdf.parse(med.getDate());
                if (medDate != null && !medDate.before(weekStart) && !medDate.after(weekEnd)) {
                    totalMeds++;
                    if ("Taken".equals(med.getStatus())) totalTaken++;
                }
                if (todayStr.equals(med.getDate())) {
                    todayTotal++;
                    if ("Taken".equals(med.getStatus())) todayTaken++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int overallPercentage = totalMeds > 0 ? (int) ((totalTaken * 100.0f) / totalMeds) : 0;
        tvOverallProgress.setText(overallPercentage + "%");
        int todayPercentage = todayTotal > 0 ? (int) ((todayTaken * 100.0f) / todayTotal) : 0;
        tvTodayProgress.setText(todayPercentage + "%");
    }




    // People caring for you section, Caregiver RecyclerView methods---------------------------------------------------------------------------------------------------------------------------------
    private void setupCaregiverRecyclerView() {
        RecyclerView caregiverRecyclerView = findViewById(R.id.caregiverRecyclerView);
        caregiverRecyclerView.setLayoutManager(new LinearLayoutManager(this));   // Set how the items will be arranged (vertical list)

        List<String> caregiverList = new ArrayList<>();     // Create an empty list to store caregiver names
        findAllCaregiversWithAccess(caregiverList, caregiverRecyclerView);   // Start the process to find all caregivers
    }

    private void findAllCaregiversWithAccess(List<String> caregiverList, RecyclerView recyclerView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Search all "people" collections across all users
        // This is like searching every user's "people" folder at once
        db.collectionGroup("people")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("CaregiverRecyclerView", "Found " + querySnapshot.size() + " total people documents");

                    Set<String> caregiverUids = new HashSet<>();     // Use a Set to store unique caregiver IDs (no duplicates)

                    // Find documents that match the personUid
                    for (DocumentSnapshot personDoc : querySnapshot) {
                        // Check if this document has the same ID as the current elder
                        if (personDoc.getId().equals(personUid)) {
                            // If yes, this means some caregiver has access to the elder
                            // Figure out which caregiver owns this document
                            // Path format: "users/{caregiverUid}/people/{personUid}"
                            String docPath = personDoc.getReference().getPath();
                            String[] pathSegments = docPath.split("/");
                            if (pathSegments.length >= 2) {
                                String foundCaregiverUid = pathSegments[1];  // The caregiver UID is the second part of the path, pathSegments[1]
                                caregiverUids.add(foundCaregiverUid);
                                Log.d("CaregiverRecyclerView", "Found caregiver with access: " + foundCaregiverUid);
                            }
                        }
                    }

                    if (caregiverUids.isEmpty()) {     // If no caregivers found, just show the current user
                        addCurrentCaregiverOnly(caregiverList, recyclerView);
                    } else {      // If we found caregivers, get their usernames. fetchCaregiverUsernames
                        fetchCaregiverUsernames(caregiverUids, caregiverList, recyclerView);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CaregiverRecyclerView", "Error finding caregivers with access: ", e);
                    addCurrentCaregiverOnly(caregiverList, recyclerView);
                });
    }

    private void fetchCaregiverUsernames(Set<String> caregiverUids, List<String> caregiverList, RecyclerView recyclerView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int totalCaregivers = caregiverUids.size();
        final int[] processedCount = {0};            // Counter for completed requests


        // For each caregiver ID found, get their username
        for (String caregiverUid : caregiverUids) {
            db.collection("users")
                    .document(caregiverUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String displayName;

                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");     // Get the username from the user document
                            if (caregiverUid.equals(this.caregiverUid)) {
                                displayName = (username != null ? username : "You") + " (You)";   // If this is the current user, mark it as "(You)"
                            } else {
                                displayName = username != null ? username : "Caregiver";     // For other caregivers, just use their username
                            }
                        } else {
                            displayName = caregiverUid.equals(this.caregiverUid) ? "You (You)" : "Caregiver";    // If user document doesn't exist, use placeholder
                        }

                        caregiverList.add(displayName);   // Add the name to our list
                        processedCount[0]++;

                        // Check if processed all caregivers
                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter(caregiverList, recyclerView);   // When all usernames are fetched, update the display adapter
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CaregiverRecyclerView", "Error fetching username for " + caregiverUid + ": ", e);
                        String displayName = caregiverUid.equals(this.caregiverUid) ? "You (You)" : "Caregiver";
                        caregiverList.add(displayName);
                        processedCount[0]++;      // Mark as completed even if failed

                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter(caregiverList, recyclerView);
                        }
                    });
        }
    }

    private void addCurrentCaregiverOnly(List<String> caregiverList, RecyclerView recyclerView) {
        // Fallback: Only show the current caregiver (used when no others found or error occurs)
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(caregiverUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        caregiverList.add((username != null ? username : "You") + " (You)");
                    } else {
                        caregiverList.add("You (You)");
                    }
                    caregiverList.add("No other caregivers");
                    updateCaregiverAdapter(caregiverList, recyclerView);
                })
                .addOnFailureListener(e -> {   // Even if this fails, show something
                    caregiverList.add("You (You)");
                    caregiverList.add("No other caregivers");
                    updateCaregiverAdapter(caregiverList, recyclerView);
                });
    }

    private void updateCaregiverAdapter(List<String> caregiverList, RecyclerView recyclerView) {
        // Sort list to put current user first, organize and display the list
        Collections.sort(caregiverList, (name1, name2) -> {
            boolean isYou1 = name1.contains("(You)");   // Check if name1 contains "(You)" - this means it's the current user
            boolean isYou2 = name2.contains("(You)");   // Check if name2 contains "(You)" - this means it's the current user
            if (isYou1 && !isYou2) return -1;          // name1 is "You", name2 is not → name1 comes FIRST
            if (!isYou1 && isYou2) return 1;           // name1 is not "You", name2 is → name2 comes FIRST
            return name1.compareTo(name2);             // Both are same type (both "You" or both not) → sort alphabetically

        });

        CaregiverAdapter caregiverAdapter = new CaregiverAdapter(caregiverList);
        recyclerView.setAdapter(caregiverAdapter);
    }


}