package com.example.elderlink;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.example.elderlink.view_Ask_Ai.ChatActivity;
import com.example.elderlink.view_collaborators.ViewCollaborators;
import com.example.elderlink.view_gps.GPSActivity;
import com.example.elderlink.view_medication.Model_medication;
import com.example.elderlink.view_medication.ViewMedicationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckOnElderlyActivity extends AppCompatActivity {

    // Dashboard variables
    private RecyclerView dashboardRecyclerView;
    private DashboardAdapter dashboardAdapter;
    private final List<DateGroup> dateGroupList = new ArrayList<>();    //Get model from DateGroup

    private TextView tvTodayProgress, tvOverallProgress;
    private String caregiverUid, personUid, personName;
    private FirebaseFirestore db;
    private final List<Model_medication> medicationList = new ArrayList<>();   //keep meds in Model_medication list for date picker

    private TextView noMedsToday;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_on_elderly_page);

        DrawerMenu.setupMenu(this); // Add the Left side menu

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get data from intent
        personName = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        String caregiverUidIntent = getIntent().getStringExtra("caregiverUid");
        if (caregiverUidIntent != null) {
            caregiverUid = caregiverUidIntent;
        }

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);

        nameText.setText(personName);
        imageView.setImageResource(R.drawable.profile_placeholder); // placeholder initially

        // Initialize dashboard views
        initializeDashboardViews();
        setupDashboardRecyclerView();

        // Fetch elder profile from Firestore
        DocumentReference elderRef = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid);

        elderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageBase64 = documentSnapshot.getString("imageBase64");
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    imageView.setImageBitmap(bitmap);
                }
            }
        });

        // Load medications for dashboard
        loadMedicationsFromFirestore();

        //Back Button (to MainPage)------------------------------------------------
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Info Button (to ProfilePageElder)---------------------------------------------
        Button infoButton = findViewById(R.id.infobtn);
        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ProfilePageElder.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            startActivity(intent);
            finish();
        });

        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        //Open Left navigation menu------------------------------------------------ rmb add DrawerMenu.setupMenu(this); on top
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // START is opens from left; END opens from right
        });

        //View Medication Button (to Medication)------------------------------------------------
        ImageButton btnMedication = findViewById(R.id.btnMedication);
        btnMedication.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ViewMedicationActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);
            startActivity(intent);
            finish();
        });

        //View Ask Ai Button (to Ai chatbot)------------------------------------------------
        ImageButton btnAibot = findViewById(R.id.btnAibot);
        btnAibot.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            startActivity(intent);
            finish();
        });

        //View Collaborators Button (to Collaborators)------------------------------------------------
        ImageButton btnCollaborators = findViewById(R.id.btnCollaborators);
        btnCollaborators.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ViewCollaborators.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            startActivity(intent);
            finish();
        });


        //View GPS (to GPS)------------------------------------------------
        ImageButton btnGPS = findViewById(R.id.btnGPS);
        btnGPS.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, GPSActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });





        //Filter by Date Button (to DatePicker)------------------------------------------------
        Button btnFilterDate = findViewById(R.id.btnFilterDate);
        btnFilterDate.setOnClickListener(v -> {
            showDatePickerWithMedDates(medicationList);
        });
    }

    //Dashboard --------------------------------------------------------------------------------------
    private void initializeDashboardViews() {
        tvTodayProgress = findViewById(R.id.tvTodayProgress);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        dashboardRecyclerView = findViewById(R.id.dashboardRecyclerView);
        noMedsToday = findViewById(R.id.noMedsToday);
    }

    private void setupDashboardRecyclerView() {
        dashboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dashboardAdapter = new DashboardAdapter(dateGroupList);
        dashboardRecyclerView.setAdapter(dashboardAdapter);
    }

    private void loadMedicationsFromFirestore() {
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
        if (medDates.isEmpty()){
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
                            filterByDate(selectedDate);
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

    private void filterByDate(String selectedDate) {
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
                        updatenoMedsToday(); // Update empty state after filtering
                    }
                });
    }

    //Calculate overall progress for this week
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
}

