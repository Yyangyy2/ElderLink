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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.view_Ask_Ai.ChatActivity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckOnElderlyActivity extends AppCompatActivity {

    // Dashboard variables
    private RecyclerView dashboardRecyclerView;
    private DashboardAdapter dashboardAdapter;
    private List<DateGroup> dateGroupList = new ArrayList<>();

    private TextView tvTodayProgress, tvOverallProgress;
    private String caregiverUid, personUid, elderName;
    private FirebaseFirestore db;
    private List<Model_medication> medicationList = new ArrayList<>();   //keep meds in Model_medication list for date picker

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
        elderName = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);

        nameText.setText(elderName);
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

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        // Info Button (to ProfilePageElder)---------------------------------------------
        Button infoButton = findViewById(R.id.infobtn);

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ProfilePageElder.class);
                intent.putExtra("personUid", personUid);
                //intent.putExtra("personImageBase64", imageBase64);
                intent.putExtra("personName", elderName);
                startActivity(intent);
                finish();
            }
        });





        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
//        ImageButton navNotifications = findViewById(R.id.navNotifications);
//
//        navNotifications.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
//                intent.putExtra("personUid", personUid);
//                startActivity(intent);
//                finish();
//            }
//        });



        //Open Left navigation menu------------------------------------------------ rmb add DrawerMenu.setupMenu(this); on top
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);

        // When clicking the button, open the drawer
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // START is opens from left; END opens from right
        });




        //View Medication Button (to Medication)------------------------------------------------

        ImageButton btnMedication = findViewById(R.id.btnMedication);

        btnMedication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ViewMedicationActivity.class);

                // Pass along personUid so next activity knows which elderly
                String personUid = getIntent().getStringExtra("personUid");
                String caregiverUid = getIntent().getStringExtra("caregiverUid");
                intent.putExtra("personUid", personUid);
                intent.putExtra("caregiverUid", caregiverUid);
                intent.putExtra("personName", elderName);
                startActivity(intent);
                finish();
            }
        });


        //View Ask Ai Button (to Ai chatbot)------------------------------------------------

        ImageButton btnAibot = findViewById(R.id.btnAibot);

        btnAibot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
                intent.putExtra("personUid", personUid);
                intent.putExtra("personName", elderName);
                startActivity(intent);
                finish();
            }
        });


        //Filter by Date Button (to DatePicker)------------------------------------------------
        Button btnFilterDate = findViewById(R.id.btnFilterDate);
        btnFilterDate.setOnClickListener(v -> {
            // Pass your current medications list
            showDatePickerWithMedDates(medicationList);
        });


    }


    //Dashboard --------------------------------------------------------------------------------------
    private void initializeDashboardViews() {
        tvTodayProgress = findViewById(R.id.tvTodayProgress);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        dashboardRecyclerView = findViewById(R.id.dashboardRecyclerView);
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
                    } else {
                        Log.e("Dashboard", "Error getting medications: ", task.getException());
                    }
                });
    }

    private void groupMedicationsByDate(List<Model_medication> medications) {

        // Get Today's date
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Filter medications only for today to show
        List<Model_medication> medsForToday = new ArrayList<>();
        for (Model_medication medication : medications) {
            if (todayStr.equals(medication.getDate())) {
                medsForToday.add(medication);
            }
        }

        // Clear old data and add only today's group
        dateGroupList.clear();
        if (!medsForToday.isEmpty()) {
            dateGroupList.add(new DateGroup(todayStr, medsForToday));
        }

        dashboardAdapter.notifyDataSetChanged();
    }


    //Gray out dates without meds in date picker
    private void showDatePickerWithMedDates(List<Model_medication> allMedications) {
        // Collect all dates with meds
        List<String> medDates = new ArrayList<>();
        for (Model_medication med : allMedications) {
            if (!medDates.contains(med.getDate())) {
                medDates.add(med.getDate());
            }
        }

        if (medDates.isEmpty()) {
            return; // no meds, do nothing
        }

        // Get min and max date from medication list
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date minDate = sdf.parse(Collections.min(medDates));
            Date maxDate = sdf.parse(Collections.max(medDates));

            final Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                        //  Only allow if this date exists in medDates
                        if (medDates.contains(selectedDate)) {
                            filterByDate(selectedDate);
                        } else {
                            // Show warning or ignore
                            Log.w("DatePicker", "No medication on " + selectedDate);
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // Restrict visible date range
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

                        // Filter only selected date
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

        // Get start and end of this week
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()); // start of week (Monday or Sunday depending on locale)
        Date weekStart = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6); // end of week
        Date weekEnd = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int totalMeds = 0;
        int totalTaken = 0;

        int todayTotal = 0;
        int todayTaken = 0;

        String todayStr = sdf.format(new Date());

        for (Model_medication med : medicationList) {
            try {
                Date medDate = sdf.parse(med.getDate());
                if (medDate != null && !medDate.before(weekStart) && !medDate.after(weekEnd)) {
                    // inside this week
                    totalMeds++;
                    if ("Taken".equals(med.getStatus())) {
                        totalTaken++;
                    }
                }

                // Today’s progress separately
                if (todayStr.equals(med.getDate())) {
                    todayTotal++;
                    if ("Taken".equals(med.getStatus())) {
                        todayTaken++;
                    }
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


    // Data model for date groups
    public static class DateGroup {
        private String date;
        private List<Model_medication> medications;
        private String progress;

        public DateGroup(String date, List<Model_medication> medications) {
            this.date = date;
            this.medications = medications;
            this.progress = calculateProgress();
        }

        private String calculateProgress() {
            if (medications == null || medications.isEmpty()) {
                return "0%";
            }

            int takenCount = 0;
            for (Model_medication med : medications) {
                // Check if medication is taken based on status
                if ("Taken".equals(med.getStatus())) {
                    takenCount++;
                }
            }

            int percentage = medications.size() > 0 ? (int) ((takenCount * 100.0f) / medications.size()) : 0;
            return percentage + "%";
        }

        // Getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public List<Model_medication> getMedications() { return medications; }
        public void setMedications(List<Model_medication> medications) {
            this.medications = medications;
            this.progress = calculateProgress();
        }
        public String getProgress() { return progress; }
    }
}