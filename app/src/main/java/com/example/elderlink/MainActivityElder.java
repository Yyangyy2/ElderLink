package com.example.elderlink;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.elderlink.DrawerMenu;
import com.example.elderlink.LoginElderActivity;
import com.example.elderlink.R;
import com.example.elderlink.view_Ask_Ai.ChatActivityElder;
import com.example.elderlink.view_gps.ElderLocationService;
import com.example.elderlink.view_medication.Model_medication;
import com.example.elderlink.view_medication.ViewMedicationActivityElderSide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivityElder extends AppCompatActivity {

    private final List<Model_medication> medicationList = new ArrayList<>();   //keep meds in Model_medication list for date picker
    private final List<DateGroup> dateGroupList = new ArrayList<>();          // keep grouped meds by date
    private DashboardAdapter dashboardAdapter;                                // adapter for dashboard
    private TextView tvOverallProgress, tvTodayProgress;
    private String caregiverUid;
    private String personUid;
// progress labels

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

        RecyclerView dashboardRecyclerView = findViewById(R.id.dashboardRecyclerView);
        dashboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dashboardAdapter = new DashboardAdapter(dateGroupList);
        dashboardRecyclerView.setAdapter(dashboardAdapter);



        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");

        nameText.setText(name);


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


        // Call this after successful PIN verification
        startLocationService();



    }

    // Start location service when elder logs in
    private void startLocationService() {
        Intent serviceIntent = new Intent(this, ElderLocationService.class);
        serviceIntent.putExtra("caregiverUid", caregiverUid);
        serviceIntent.putExtra("personUid", personUid);

        try {
            startService(serviceIntent);
            Log.d("LocationService", "Location service started");
        } catch (Exception e) {
            Log.e("LocationService", "Failed to start service: " + e.getMessage());
        }
    }





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
    }


    //Gray out dates without meds in date picker
    private void showDatePickerWithMedDates(List<Model_medication> allMedications) {
        List<String> medDates = new ArrayList<>();
        for (Model_medication med : allMedications) {
            if (!medDates.contains(med.getDate())) {
                medDates.add(med.getDate());
            }
        }
        if (medDates.isEmpty()) return;
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


}