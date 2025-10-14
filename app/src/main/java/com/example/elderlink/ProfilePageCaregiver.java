package com.example.elderlink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfilePageCaregiver extends AppCompatActivity {

    private Button btnSave, btnBack;
    private EditText username, email, phone;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page_caregiver);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        // UI references
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        // Disable username and email editing
        username.setEnabled(false);
        email.setEnabled(false);

        // Set phone hint for Malaysian format
        phone.setHint("e.g., 012-345 6789");

        // Load user data from Firestore
        loadUserData();

        // Save button: update phone only
        btnSave.setOnClickListener(v -> {
            if (validatePhoneNumber()) {
                saveUserData();
            }
        });

        // Back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProfilePageCaregiver.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userName = documentSnapshot.getString("username");
                String userEmail = documentSnapshot.getString("email");
                String userPhone = documentSnapshot.getString("phone");

                username.setText(userName);
                email.setText(userEmail);
                if (userPhone != null) phone.setText(userPhone);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show());
    }

    private boolean validatePhoneNumber() {
        String phoneNum = phone.getText().toString().trim();

        // Check if phone number is empty
        if (phoneNum.isEmpty()) {
            phone.setError("Phone number is required");
            return false;
        }

        // Remove all non-digit characters
        String cleanPhone = phoneNum.replaceAll("[^\\d]", "");

        // Malaysian phone number validation - exactly 10 digits starting with 01
        if (cleanPhone.matches("^01\\d{8}$")) {
            return true;
        } else {
            phone.setError("Please enter a valid Malaysian phone number\n\nExamples:\n• 0123456789\n• 012-345 6789\n\nMust start with 01 and be exactly 10 digits");
            return false;
        }
    }

    private void saveUserData() {
        String phoneNum = phone.getText().toString().trim();

        // Format the phone number nicely
        String formattedPhone = formatMalaysianPhone(phoneNum);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", formattedPhone);

        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    // Update display with formatted number
                    phone.setText(formattedPhone);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
    }

    private String formatMalaysianPhone(String phone) {
        // Remove all non-digit characters
        String cleanPhone = phone.replaceAll("[^\\d]", "");

        // Format based on length
        if (cleanPhone.length() == 10) {
            // Format: 012-345 6789
            return cleanPhone.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2 $3");
        } else if (cleanPhone.length() == 11) {
            // Format: 011-1234 5678
            return cleanPhone.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2 $3");
        }

        // Return original if formatting doesn't apply
        return phone;
    }
}