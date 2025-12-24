package com.example.elderlink.view_medication_v2;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.DrawerMenu;
import com.example.elderlink.MainActivity;
import com.example.elderlink.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ViewMedicationActivity_v2 extends AppCompatActivity
        implements MedicationAdapter_v2.OnMedicationClickListener {

    private RecyclerView medicationRecyclerView;
    private MedicationAdapter_v2 adapter;
    private List<Model_medication_v2> medicationList = new ArrayList<>();

    private FirebaseFirestore db;
    private String personUid;
    private String caregiverUid;
    private String personName;

    // Search variables
    private EditText searchBar;
    private ImageButton btnClearSearch;
    private String currentSearchQuery = "";

    // Mic for search bar
    private ImageButton micButton;
    private final int SPEECH_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_view_page_v2);

        DrawerMenu.setupMenu(this);

        db = FirebaseFirestore.getInstance();
        personUid = getIntent().getStringExtra("personUid");
        caregiverUid = getIntent().getStringExtra("caregiverUid");
        personName = getIntent().getStringExtra("personName");

        // Setup RecyclerView
        medicationRecyclerView = findViewById(R.id.medicationRecyclerView);
        medicationRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Initialize NEW adapter v2
        adapter = new MedicationAdapter_v2(this, medicationList, this);
        medicationRecyclerView.setAdapter(adapter);

        // Initialize search functionality
        setupSearch();

        // Load medications
        loadMedications();

        // FAB button
        FloatingActionButton fab = findViewById(R.id.addMedicationFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivity_v2.this, AddMedicationActivity_v2.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
        });

        // Bottom Navigation Bar
        ImageButton navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ViewMedicationActivity_v2.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


        // Mic button for search bar
        micButton = findViewById(R.id.micButton);
        micButton.setOnClickListener(v -> {
            startSpeechToText();
        });
    }

    // Load medications from Firestore
    private void loadMedications() {
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Check if caregiverUid is provided, otherwise use current user
        if (caregiverUid == null || caregiverUid.isEmpty()) {
            // Try to get ownerUid from person document
            db.collection("users")
                    .document(currentUser)
                    .collection("people")
                    .document(personUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String owner = doc.contains("ownerUid") ? doc.getString("ownerUid") : currentUser;
                        if (owner == null || owner.isEmpty()) {
                            owner = currentUser;
                        }
                        caregiverUid = owner;
                        attachMedicationsListener();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("ViewMedication_v2", "Failed to get ownerUid, using current user", e);
                        caregiverUid = currentUser;
                        attachMedicationsListener();
                    });
        } else {
            attachMedicationsListener();
        }
    }

    private void attachMedicationsListener() {
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("medications_v2")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore_v2", "Listen failed.", error);
                        Toast.makeText(this, "Error loading medications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        medicationList.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Model_medication_v2 med = doc.toObject(Model_medication_v2.class);
                            if (med != null) {
                                med.setId(doc.getId());
                                medicationList.add(med);
                            }
                        }

                        // Apply search filter if there's a query
                        if (!currentSearchQuery.isEmpty()) {
                            filterMedications(currentSearchQuery);
                        } else {
                            adapter.updateData(medicationList);
                        }

                        showEmptyStateIfNeeded();
                    }
                });
    }

    // Search Bar Setup
    private void setupSearch() {
        searchBar = findViewById(R.id.searchBar);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // Text change listener for real-time search
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                updateClearButtonVisibility();
                filterMedications(currentSearchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            searchBar.setText("");
            currentSearchQuery = "";
            filterMedications("");
        });
    }

    private void updateClearButtonVisibility() {
        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void filterMedications(String query) {
        if (query.isEmpty()) {
            // Show all medications
            adapter.updateData(medicationList);
        } else {
            // Filter medications
            List<Model_medication_v2> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Model_medication_v2 med : medicationList) {
                if (med.getBrandname() != null && med.getBrandname().toLowerCase().contains(lowerCaseQuery) ||
                        med.getMedname() != null && med.getMedname().toLowerCase().contains(lowerCaseQuery) ||
                        med.getTimesperday() != null && med.getTimesperday().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(med);
                }
            }
            adapter.updateData(filteredList);
        }

        showEmptyStateIfNeeded();
    }

    private void showEmptyStateIfNeeded() {
        if (medicationList.isEmpty()) {
            Toast.makeText(this, "No medications found", Toast.LENGTH_SHORT).show();
        } else if (!currentSearchQuery.isEmpty() && adapter.getItemCount() == 0) {
            Toast.makeText(this, "No medications found for: " + currentSearchQuery, Toast.LENGTH_SHORT).show();
        }
    }

    // Speech to Text for search
    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak medication name...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                searchBar.setText(result.get(0));
                currentSearchQuery = result.get(0);
                filterMedications(currentSearchQuery);
            }
        }
    }

    // Implement MedicationAdapter_v2 click listeners
    @Override
    public void onMedicationClick(int position, Model_medication_v2 medication) {
        // Handle card click (view details)
        Toast.makeText(this, "Viewing: " + medication.getBrandname(), Toast.LENGTH_SHORT).show();
        // You can add a detail view activity here if needed
    }

    @Override
    public void onEditClick(int position, Model_medication_v2 medication) {
        // Handle edit button click
        Intent intent = new Intent(this, AddMedicationActivity_v2.class);
        intent.putExtra("personUid", personUid);
        intent.putExtra("personName", personName);
        intent.putExtra("caregiverUid", caregiverUid);
        intent.putExtra("medId", medication.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from edit/add activity
        if (medicationRecyclerView != null && adapter != null) {
            // The Firestore listener will automatically update
        }
    }
}