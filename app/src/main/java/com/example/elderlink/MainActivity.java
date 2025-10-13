package com.example.elderlink;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton addPersonBtn;
    private List<Person> personList;
    private List<Person> filteredPersonList; // For search results
    private PersonAdapter adapter;
    private FirebaseFirestore firestore;
    private String uid;
    private String selectedImageBase64 = "";
    private ImageView imagePreview;
    private ListenerRegistration peopleListener;
    private String username = "";

    // Search variables
    private EditText searchBar;
    private ImageButton btnClearSearch;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.peopleRecyclerView);
        addPersonBtn = findViewById(R.id.addPersonBtn);
        TextView userNameTextView = findViewById(R.id.userName);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Initialize search views
        searchBar = findViewById(R.id.searchBar);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        personList = new ArrayList<>();
        filteredPersonList = new ArrayList<>();


        adapter = new PersonAdapter(this, filteredPersonList, false, uid,"");
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore = FirebaseFirestore.getInstance();
        Log.d("MainActivity", "Current UID: " + uid);

        // Setup search functionality
        setupSearch();

        // Safe snapshot listener
        peopleListener = firestore.collection("users")
                .document(uid)
                .collection("people")
                .addSnapshotListener(this::onPeopleSnapshot);

        addPersonBtn.setOnClickListener(v -> showAddPersonDialog());

        // Caregiver username-------------------------------------------------------------------------------------------------------
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        username = documentSnapshot.getString("username");
                        userNameTextView.setText(username != null ? username : "No Name");

                        // Update the adapter with the actual username
                        if (adapter != null) {
                            // Create a new adapter with the correct username
                            adapter = new PersonAdapter(MainActivity.this, filteredPersonList, false, uid, username);
                            recyclerView.setAdapter(adapter);
                        }
                    } else {
                        username = "No Name";
                        userNameTextView.setText(username);
                        adapter = new PersonAdapter(MainActivity.this, filteredPersonList, false, uid, username);
                        recyclerView.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    username = "No Name";
                    userNameTextView.setText(username);
                    adapter = new PersonAdapter(MainActivity.this, filteredPersonList, false, uid, username);
                    recyclerView.setAdapter(adapter);
                });


        // Info Button (to ProfilePageCaregiver)--------------------------------------------------------------------------------------------------------------
        Button infoButton = findViewById(R.id.infobtn);
        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfilePageCaregiver.class);

            startActivity(intent);
            finish();
        });

        // Log out Caregiver----------------------------------------------------------------------------------------------------------
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Search Bar-------------------------------------------------------------------------------------------------------------------------------
    private void setupSearch() {
        // Text change listener for real-time search
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                updateClearButtonVisibility();
                applySearchFilter(); // Apply search filter
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            searchBar.setText("");
            currentSearchQuery = "";
            applySearchFilter();
        });
    }

    private void updateClearButtonVisibility() {
        if (currentSearchQuery.isEmpty()) {
            btnClearSearch.setVisibility(View.GONE);
        } else {
            btnClearSearch.setVisibility(View.VISIBLE);
        }
    }

    private void applySearchFilter() {
        // Clear the filtered list
        filteredPersonList.clear();

        if (currentSearchQuery.isEmpty()) {
            // No search query, show all people
            filteredPersonList.addAll(personList);
        } else {
            // Apply search filter
            String searchLower = currentSearchQuery.toLowerCase();
            for (Person person : personList) {
                if (matchesSearchQuery(person, searchLower)) {
                    filteredPersonList.add(person);
                }
            }
        }

        // Update the adapter with filtered results
        adapter.notifyDataSetChanged();
        showEmptyStateIfNeeded();
    }

    private boolean matchesSearchQuery(Person person, String searchQuery) {
        if (person == null) return false;

        // Search in person name
        if (person.getName() != null &&
                person.getName().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in person ID (if you want to search by ID as well)
        if (person.getId() != null &&
                person.getId().toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    private void showEmptyStateIfNeeded() {
        if (filteredPersonList.isEmpty()) {
            if (currentSearchQuery.isEmpty()) {
                // No people at all
                Toast.makeText(this, "No people under your care", Toast.LENGTH_SHORT).show();
            } else {
                // No search results
                Toast.makeText(this, "No people found for: " + currentSearchQuery, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onPeopleSnapshot(@NonNull QuerySnapshot snapshots, @NonNull FirebaseFirestoreException e) {
        if (e != null) {
            Log.e("FirestoreListener", "Error loading people", e);
            Toast.makeText(this, "Error loading people", Toast.LENGTH_SHORT).show();
            return;
        }

        personList.clear();
        for (DocumentSnapshot doc : snapshots) {
            Person p = doc.toObject(Person.class);
            if (p != null) {
                p.setId(doc.getId());
                personList.add(p);
            }
        }

        //Apply search filter after loading data to update the displayed list
        applySearchFilter();
    }

    // Add person dialog----------------------------------------------------------------------------------------------------------------------------
    private void showAddPersonDialog() {
        selectedImageBase64 = ""; // Reset image every time

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Person");

        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_person, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editPersonName);
        ImageView selectedImage = dialogView.findViewById(R.id.selectedImage);
        EditText editPIN = dialogView.findViewById(R.id.editPIN);
        dialogView.findViewById(R.id.selectImageBtn).setOnClickListener(v -> pickImage(selectedImage));

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String pin = editPIN.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pin.length() != 6 || !pin.matches("\\d{6}")) {
                Toast.makeText(this, "PIN must be 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            // Hash the PIN
            String hashedPin = HashPIN.hashPin(pin);

            // Generate a new document reference with an auto ID
            DocumentReference newPersonRef = firestore.collection("users")
                    .document(uid)
                    .collection("people")
                    .document();

            // Get the generated ID
            String generatedId = newPersonRef.getId();

            // Create Person object with the ID
            Person newPerson = new Person(name, selectedImageBase64, hashedPin, generatedId);

            // Save to Firestore at that document path
            newPersonRef.set(newPerson)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FirestoreAdd", "Saved person with ID: " + generatedId);
                        Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(err -> {
                        Log.e("FirestoreAdd", "Failed to save person", err);
                        Toast.makeText(this, "Error saving: " + err.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void pickImage(ImageView preview) {
        imagePreview = preview;
        imagePickerLauncher.launch("image/*");
    }

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        // Compress image
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
                        byte[] imageBytes = baos.toByteArray();

                        selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                        if (imagePreview != null) {
                            imagePreview.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        Log.e("ImagePicker", "Failed to load image", e);
                    }
                }
            }
    );

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (peopleListener != null) {
            peopleListener.remove(); // Clean up listener
        }
    }
}