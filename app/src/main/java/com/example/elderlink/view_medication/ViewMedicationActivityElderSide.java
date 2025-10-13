package com.example.elderlink.view_medication;
import com.example.elderlink.DrawerMenu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
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
import com.example.elderlink.MainActivityElder;
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


public class ViewMedicationActivityElderSide extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicationAdapter adapter;
    private List<Model_medication> medicationList = new ArrayList<>();
    private List<Model_medication> allMedications = new ArrayList<>();
    private List<Model_medication> filteredMedications = new ArrayList<>(); // For search results
    private FirebaseFirestore db;
    private String personUid;
    private String caregiverUid; // intent-provided owner (may be null)
    private String OwnerUid; //  original owner

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

        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        String imageBase64 = getIntent().getStringExtra("personImageBase64");
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");

        recyclerView = findViewById(R.id.medicationRecyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new MedicationAdapter(this, medicationList, medication -> {
            // handle edit click 10 open AddMedicationActivity in edit mode----- change to same acitivity due to not allowing edit
        });
        recyclerView.setAdapter(adapter);

        // Initialize search functionality
        setupSearch();

        setupCalendar();
        resolveOwnerAndLoadMedications();
        listenForMedicationReminders();

        // Bottom Navigation Bar
        ImageButton navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivityElderSide.this, MainActivityElder.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", name);
            intent.putExtra("personImageBase64", imageBase64);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        // Open Left navigation menu
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Hide Floating Action Button
        FloatingActionButton addMedicationFab = findViewById(R.id.addMedicationFab);
        addMedicationFab.setVisibility(View.GONE);

    }

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

        // Search in repeat type (if exists)
        if (medication.getRepeatType() != null &&
                medication.getRepeatType().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in time (if exists and is string format)
        if (medication.getTime() != null &&
                medication.getTime().toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    private void showEmptyStateIfNeeded() {
        if (medicationList.isEmpty()) {
            if (currentSearchQuery.isEmpty()) {
                // No medications for selected date
                Toast.makeText(this, "No medications for selected date", Toast.LENGTH_SHORT).show();
            } else {
                // No search results
                Toast.makeText(this, "No medications found for: " + currentSearchQuery, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resolveOwnerAndLoadMedications() {
        if (personUid == null || personUid.isEmpty()) return;

        if (caregiverUid != null && !caregiverUid.isEmpty()) {
            OwnerUid = caregiverUid;
            attachMedicationsListener(OwnerUid);
        } else {
            String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users").document(currentUser).collection("people").document(personUid).get()
                    .addOnSuccessListener(doc -> {
                        String owner = null;
                        if (doc.exists()) owner = doc.getString("ownerUid");
                        if (owner == null || owner.isEmpty()) owner = currentUser;
                        OwnerUid = owner;
                        attachMedicationsListener(OwnerUid);
                    })
                    .addOnFailureListener(e -> {
                        Log.w("ViewMedElder", "Failed to resolve owner, default to current user", e);
                        OwnerUid = currentUser;
                        attachMedicationsListener(OwnerUid);
                    });
        }
    }

    private void attachMedicationsListener(String ownerUid) {
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

                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
                        currentSelectedDate = today;
                        applyFilters();

                        loadMedicationStatusForCalendar(ownerUid);
                    }
                });
    }

    private void setupCalendar() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // Generate 015 days around today
        dateList = new ArrayList<>(); // Initialize the class variable
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

    // Remove the dateList parameter since it's now a class variable
    private void loadMedicationStatusForCalendar(String caregiverUid) {
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

                        for (Map.Entry<String, List<String>> entry : dateMedicationsMap.entrySet()) {
                            String date = entry.getKey();
                            List<String> statuses = entry.getValue();

                            String overallStatus = calculateOverallStatus(statuses);
                            dateStatusMap.put(date, overallStatus);
                        }

                        if (calendarAdapter != null) {
                            calendarAdapter.updateDateStatus(dateStatusMap);
                        }
                    } else {
                        Log.e("ViewMedicationActivityElderSide", "Error loading medication status: ", task.getException());
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

    // Elder listens for caregiver-scheduled meds and schedules alarms locally
    private void listenForMedicationReminders() {
        // Ensure we have OwnerUid (if not, try to resolve quickly)
        if (OwnerUid == null) {
            if (caregiverUid != null && !caregiverUid.isEmpty()) {
                OwnerUid = caregiverUid;
            } else {
                String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                OwnerUid = currentUser; // fallback; ideally resolve earlier
            }
        }

        final String owner = OwnerUid;
        db.collection("users")
                .document(owner)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ElderReminder", "Listen error", e);
                        return;
                    }
                    if (snapshots == null) return;

                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        Model_medication med = dc.getDocument().toObject(Model_medication.class);
                        med.setId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                scheduleMedication(this, med, owner);
                                break;
                            case MODIFIED:
                                cancelMedication(this, med.getId());
                                scheduleMedication(this, med, owner);
                                break;
                            case REMOVED:
                                cancelMedication(this, med.getId());
                                break;
                        }
                    }
                });
    }

    private void scheduleMedication(Context context, Model_medication med, String ownerUid) {
        try {
            long triggerAt = med.getTimeMillis(); // get milliseconds from model
            long now = System.currentTimeMillis();

            // Prevent scheduling for past meds
            if (triggerAt < now) {
                Log.d("ElderReminder", "Skipped past medication: " + med.getName());
                return;
            }

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("medId", med.getId());
            intent.putExtra("medInfo", med.getName() + " " + med.getDosage() + " " + med.getUnit());
            intent.putExtra("retryCount", 0);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", ownerUid);
            intent.putExtra("role", "elder");

            int requestCode = med.getId().hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            String repeatType = med.getRepeatType() != null ? med.getRepeatType().toLowerCase() : "once";

            switch (repeatType) {
                case "daily":
                    am.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            AlarmManager.INTERVAL_DAY,
                            pi
                    );
                    break;

                case "weekly":
                    am.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            AlarmManager.INTERVAL_DAY * 7,
                            pi
                    );
                    break;

                default: // once (only as needed)
                    am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                    );
                    break;
            }

        } catch (Exception e) {
            Log.e("ElderReminder", "scheduleMedication error", e);
        }
    }

    private void cancelMedication(Context context, String medId) {
        int requestCode = medId.hashCode();
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pi != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }
}