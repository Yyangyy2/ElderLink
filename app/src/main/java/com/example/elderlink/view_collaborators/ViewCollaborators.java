package com.example.elderlink.view_collaborators;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.Caregiver;
import com.example.elderlink.CaregiverAdapter;
import com.example.elderlink.CheckOnElderlyActivity;
import com.example.elderlink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewCollaborators extends AppCompatActivity {

    private RecyclerView collaboratorsRecyclerView;
    private TextView titleText;
    private String personUid;
    private String personName;
    private String caregiverUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborators_view_page);

        // Get data from intent
        Intent intent = getIntent();
        personUid = intent.getStringExtra("personUid");
        personName = intent.getStringExtra("personName");
        caregiverUid = intent.getStringExtra("caregiverUid");

        // If caregiverUid is not provided, try to get from FirebaseAuth
        if (caregiverUid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize UI
        collaboratorsRecyclerView = findViewById(R.id.collaboratorsRecyclerView);
        titleText = findViewById(R.id.titleText);

        // Set title with person's name
        if (personName != null) {
            titleText.setText("Caregivers for " + personName);
        } else {
            titleText.setText("Caregivers");
        }

        // Setup RecyclerView
        collaboratorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load caregivers
        loadCaregivers();

        // Back Button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent backIntent = new Intent(ViewCollaborators.this, CheckOnElderlyActivity.class);
            backIntent.putExtra("personUid", personUid);
            backIntent.putExtra("personName", personName);
            backIntent.putExtra("caregiverUid", caregiverUid);
            startActivity(backIntent);
            finish();
        });
    }

    private void loadCaregivers() {
        List<Caregiver> caregiverList = new ArrayList<>();
        findAllCaregiversWithAccess(caregiverList);
    }

    private void findAllCaregiversWithAccess(List<Caregiver> caregiverList) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collectionGroup("people")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("ViewCollaborators", "Found " + querySnapshot.size() + " documents");

                    Set<String> caregiverUids = new HashSet<>();

                    for (DocumentSnapshot personDoc : querySnapshot) {
                        if (personDoc.getId().equals(personUid)) {
                            String docPath = personDoc.getReference().getPath();
                            String[] pathSegments = docPath.split("/");
                            if (pathSegments.length >= 2) {
                                String foundCaregiverUid = pathSegments[1];
                                caregiverUids.add(foundCaregiverUid);
                            }
                        }
                    }

                    if (caregiverUids.isEmpty()) {
                        showNoCaregiversMessage(caregiverList);
                    } else {
                        fetchCaregiverDetails(caregiverUids, caregiverList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewCollaborators", "Error finding caregivers", e);
                    showNoCaregiversMessage(caregiverList);
                });
    }

    private void fetchCaregiverDetails(Set<String> caregiverUids, List<Caregiver> caregiverList) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int totalCaregivers = caregiverUids.size();
        final int[] processedCount = {0};

        for (String caregiverUid : caregiverUids) {
            db.collection("users")
                    .document(caregiverUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phone");

                            boolean isCurrentUser = caregiverUid.equals(this.caregiverUid);

                            String displayName = username != null ? username : "Caregiver";
                            String displayEmail = email != null ? email : "No email";
                            String displayPhone = phone != null ? phone : "Not provided";

                            caregiverList.add(new Caregiver(displayName, displayEmail, displayPhone, isCurrentUser));
                        } else {
                            caregiverList.add(new Caregiver("Caregiver", "No email", "Not provided", false));
                        }

                        processedCount[0]++;
                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter(caregiverList);
                        }
                    })
                    .addOnFailureListener(e -> {
                        caregiverList.add(new Caregiver("Caregiver", "No email", "Not provided", false));
                        processedCount[0]++;

                        if (processedCount[0] >= totalCaregivers) {
                            updateCaregiverAdapter(caregiverList);
                        }
                    });
        }
    }

    private void showNoCaregiversMessage(List<Caregiver> caregiverList) {
        caregiverList.add(new Caregiver("No caregivers assigned", "Contact administrator", "N/A", false));
        updateCaregiverAdapter(caregiverList);
    }

    private void updateCaregiverAdapter(List<Caregiver> caregiverList) {
        caregiverList.sort((c1, c2) -> {
            if (c1.isCurrentUser() && !c2.isCurrentUser()) return -1;
            if (!c1.isCurrentUser() && c2.isCurrentUser()) return 1;
            return c1.getName().compareTo(c2.getName());
        });

        CaregiverAdapter caregiverAdapter = new CaregiverAdapter(caregiverList);
        collaboratorsRecyclerView.setAdapter(caregiverAdapter);
    }
}