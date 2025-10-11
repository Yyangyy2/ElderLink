package com.example.elderlink.view_medication;
import com.example.elderlink.DrawerMenu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.MainActivity;
import com.example.elderlink.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ViewMedicationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicationAdapter adapter;
    private List<Model_medication> medicationList = new ArrayList<>();
    private List<Model_medication> allMedications = new ArrayList<>();
    private FirebaseFirestore db;
    private String personUid;
    private String caregiverUid;
    private String personName;

    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;
    private List<String> dateList; // Make dateList a class variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_view_page);

        DrawerMenu.setupMenu(this);

        db = FirebaseFirestore.getInstance();
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");
        personName = getIntent().getStringExtra("personName");

        recyclerView = findViewById(R.id.medicationRecyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new MedicationAdapter(this, medicationList, medication -> {
            Intent intent = new Intent(ViewMedicationActivity.this, AddMedicationActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("medId", medication.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        setupCalendar();
        loadMedications(personUid);

        // FAB button
        FloatingActionButton fab = findViewById(R.id.addMedicationFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivity.this, AddMedicationActivity.class);
            String personUid = getIntent().getStringExtra("personUid");
            intent.putExtra("personUid", personUid);
            startActivity(intent);
        });

        // Bottom Navigation Bar
        ImageButton navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Open Left navigation menu
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void loadMedications(String personUid) {
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            return;
        }

        String userUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        db.collection("users")
                .document(userUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (querySnapshot != null) {
                        allMedications.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Model_medication med = doc.toObject(Model_medication.class);
                            if (med != null) {
                                med.setId(doc.getId());
                                allMedications.add(med);
                            }
                        }

                        // Default to today's date
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Calendar.getInstance().getTime());
                        filterMedicationsByDate(today);

                        // Load medication status for calendar AFTER loading medications
                        loadMedicationStatusForCalendar(caregiverUid, personUid);
                    }
                });
    }

    private void setupCalendar() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // Generate ±15 days around today
        dateList = new ArrayList<>(); // Initialize the class variable
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = -15; i <= 15; i++) {
            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.DAY_OF_MONTH, i);
            dateList.add(sdf.format(tempCal.getTime()));
        }

        String today = sdf.format(cal.getTime());

        calendarAdapter = new CalendarAdapter(dateList, today, selectedDate -> {
            filterMedicationsByDate(selectedDate);
        });
        calendarRecyclerView.setAdapter(calendarAdapter);

        // scroll to today's position
        int todayIndex = dateList.indexOf(today);
        if (todayIndex != -1) {
            calendarRecyclerView.scrollToPosition(todayIndex);
        }
    }

    private void filterMedicationsByDate(String date) {
        medicationList.clear();
        for (Model_medication med : allMedications) {
            if (med.getDate() != null && med.getDate().equals(date)) {
                medicationList.add(med);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Remove the dateList parameter since it's now a class variable
    private void loadMedicationStatusForCalendar(String caregiverUid, String personUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, String> dateStatusMap = new HashMap<>();

        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Group medications by date
                        Map<String, List<String>> dateMedicationsMap = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Model_medication medication = document.toObject(Model_medication.class);
                            String date = medication.getDate();
                            String status = medication.getStatus();

                            if (date != null) {
                                if (!dateMedicationsMap.containsKey(date)) {
                                    dateMedicationsMap.put(date, new ArrayList<>());
                                }
                                dateMedicationsMap.get(date).add(status);
                            }
                        }

                        // Determine overall status for each date
                        for (Map.Entry<String, List<String>> entry : dateMedicationsMap.entrySet()) {
                            String date = entry.getKey();
                            List<String> statuses = entry.getValue();

                            String overallStatus = calculateOverallStatus(statuses);
                            dateStatusMap.put(date, overallStatus);
                        }

                        // Update the calendar adapter
                        if (calendarAdapter != null) {
                            calendarAdapter.updateDateStatus(dateStatusMap);
                        }
                    } else {
                        Log.e("ViewMedicationActivity", "Error loading medication status: ", task.getException());
                    }
                });
    }

    private String calculateOverallStatus(List<String> statuses) {
        boolean hasMissed = false;
        boolean hasNotTaken = false;
        boolean hasTaken = false;

        for (String status : statuses) {
            if ("Missed".equals(status)) {
                hasMissed = true;
            }
            if (status == null || "Upcoming".equals(status) || "Pending".equals(status)) {
                hasNotTaken = true;
            }
            if ("Taken".equals(status)) {
                hasTaken = true;
            }
        }

        if (hasMissed) {
            return "RED"; // At least one medication missed
        } else if (!hasNotTaken && hasTaken && statuses.size() > 0) {
            return "GREEN"; // All medications taken (no upcoming/pending/missed, only taken)
        } else if (hasNotTaken) {
            return "BLUE"; // Some medications not taken yet (null, Upcoming, Pending)
        } else {
            return "BLUE"; // Default case
        }
    }
}