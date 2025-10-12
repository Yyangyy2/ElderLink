package com.example.elderlink.view_medication;
import com.example.elderlink.DrawerMenu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
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
    private List<Model_medication> filteredMedications = new ArrayList<>(); // For search results
    private FirebaseFirestore db;
    private String personUid;
    private String caregiverUid; // intent-provided caregiver/owner uid (may be original owner)
    private String personName;

    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;
    private List<String> dateList;

    // Search variables
    private EditText searchInput;
    private ImageButton btnClearSearch;
    private String currentSearchQuery = "";
    private String currentSelectedDate = "";

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

        // Initialize search functionality
        setupSearch();

        setupCalendar();
        loadMedications(personUid);

        // FAB button
        FloatingActionButton fab = findViewById(R.id.addMedicationFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivity.this, AddMedicationActivity.class);
            String personUid = getIntent().getStringExtra("personUid");
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
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

    //Search Bar------------------------------------------------------------------------------------------------------
    private void setupSearch() {
        searchInput = findViewById(R.id.searchInput);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // Text change listener for real-time search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                updateClearButtonVisibility();
                applyFilters(); // Apply both date and search filters
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            currentSearchQuery = "";
            applyFilters();
        });
    }

    private void updateClearButtonVisibility() {
        if (currentSearchQuery.isEmpty()) {
            btnClearSearch.setVisibility(View.GONE);
        } else {
            btnClearSearch.setVisibility(View.VISIBLE);
        }
    }

    private void applyFilters() {
        filteredMedications.clear();

        if (currentSearchQuery.isEmpty()) {
            // No search query, just filter by date
            for (Model_medication med : allMedications) {
                if (med.getDate() != null && med.getDate().equals(currentSelectedDate)) {
                    filteredMedications.add(med);
                }
            }
        } else {
            // Apply both date and search filters
            String searchLower = currentSearchQuery.toLowerCase();
            for (Model_medication med : allMedications) {
                boolean matchesDate = med.getDate() != null && med.getDate().equals(currentSelectedDate);
                boolean matchesSearch = matchesSearchQuery(med, searchLower);

                if (matchesDate && matchesSearch) {
                    filteredMedications.add(med);
                }
            }
        }

        // Update the displayed list
        medicationList.clear();
        medicationList.addAll(filteredMedications);
        adapter.notifyDataSetChanged();

        // Show empty state if no results
        showEmptyStateIfNeeded();
    }

    private boolean matchesSearchQuery(Model_medication medication, String searchQuery) {
        // Search in medication name
        if (medication.getName() != null &&
                medication.getName().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in dosage
        if (medication.getDosage() != null &&
                medication.getDosage().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in unit
        if (medication.getUnit() != null &&
                medication.getUnit().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in status
        if (medication.getStatus() != null &&
                medication.getStatus().toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    private void showEmptyStateIfNeeded() {
        // You can add an empty state TextView in your XML and show/hide it here
        if (medicationList.isEmpty()) {
            // Show empty state message
            if (currentSearchQuery.isEmpty()) {
                // No medications for selected date
                Toast.makeText(this, "No medications for selected date", Toast.LENGTH_SHORT).show();
            } else {
                // No search results
                Toast.makeText(this, "No medications found for: " + currentSearchQuery, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------

    private void loadMedications(String personUid) {
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            return;
        }

        // Resolve owner UID: prefer caregiverUid passed in intent (owner), otherwise check local person doc's ownerUid, else use current user
        String intentOwner = caregiverUid;
        if (intentOwner != null && !intentOwner.isEmpty()) {
            // directly listen to owner's medications
            attachMedicationsListener(intentOwner, personUid);
        } else {
            String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users")
                    .document(currentUser)
                    .collection("people")
                    .document(personUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String owner = null;
                        if (doc.exists()) {
                            owner = doc.getString("ownerUid");
                        }
                        if (owner == null || owner.isEmpty()) owner = currentUser;
                        attachMedicationsListener(owner, personUid);
                    })
                    .addOnFailureListener(e -> {
                        Log.w("ViewMedication", "Failed to resolve ownerUid, defaulting to current user", e);
                        attachMedicationsListener(currentUser, personUid);
                    });
        }
    }

    private void attachMedicationsListener(String ownerUid, String personUid) {
        this.caregiverUid = ownerUid; // remember owner

        db.collection("users")
                .document(ownerUid)
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
                        currentSelectedDate = today;
                        applyFilters(); // Apply both date and search filters

                        // Load medication status for calendar AFTER loading medications
                        loadMedicationStatusForCalendar(ownerUid, personUid);
                    }
                });
    }

    private void setupCalendar() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // Generate ±15 days around today
        dateList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = -15; i <= 15; i++) {
            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.DAY_OF_MONTH, i);
            dateList.add(sdf.format(tempCal.getTime()));
        }

        String today = sdf.format(cal.getTime());
        currentSelectedDate = today;

        calendarAdapter = new CalendarAdapter(dateList, today, selectedDate -> {
            currentSelectedDate = selectedDate;
            applyFilters(); // Apply both date and search filters when date changes
        });
        calendarRecyclerView.setAdapter(calendarAdapter);

        // scroll to today's position
        int todayIndex = dateList.indexOf(today);
        if (todayIndex != -1) {
            calendarRecyclerView.scrollToPosition(todayIndex);
        }
    }

    // Remove the old filterMedicationsByDate method and replace with applyFilters

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