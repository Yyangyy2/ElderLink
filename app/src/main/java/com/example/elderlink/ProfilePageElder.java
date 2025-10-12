package com.example.elderlink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfilePageElder extends AppCompatActivity {

    private ImageView imgProfile;
    private EditText name, age, allergies, doctorInfo;
    private Button btnChangeImage, btnSave;

    private FirebaseFirestore db;
    private String caregiverUid, personUid;

    private static final int PICK_IMAGE_REQUEST = 1;
    private String imageBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page_elder);

        imgProfile = findViewById(R.id.imgProfile);
        name = findViewById(R.id.name);
        age = findViewById(R.id.age);
        allergies = findViewById(R.id.allergies);
        doctorInfo = findViewById(R.id.doctorInfo);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        personUid = getIntent().getStringExtra("personUid");
        if (personUid == null) {
            finish();
            return;
        }

        loadElderProfile();

        btnChangeImage.setOnClickListener(v -> openImageChooser());
        btnSave.setOnClickListener(v -> saveProfile());

        Button backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfilePageElder.this, CheckOnElderlyActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", name.getText().toString());
            startActivity(intent);
            finish();
        });
    }

    private void loadElderProfile() {
        DocumentReference elderRef = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid);

        elderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firestoreName = documentSnapshot.getString("name");
                String firestoreAge = documentSnapshot.getString("age");
                String firestoreAllergies = documentSnapshot.getString("allergies");
                String firestoreDoctorInfo = documentSnapshot.getString("doctorInfo");
                String firestoreImageBase64 = documentSnapshot.getString("imageBase64");

                if (firestoreName != null) name.setText(firestoreName);
                if (firestoreAge != null) age.setText(firestoreAge);
                if (firestoreAllergies != null) allergies.setText(firestoreAllergies);
                if (firestoreDoctorInfo != null) doctorInfo.setText(firestoreDoctorInfo);

                if (firestoreImageBase64 != null && !firestoreImageBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(firestoreImageBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    imgProfile.setImageBitmap(bitmap);
                    imageBase64 = firestoreImageBase64;
                } else {
                    imgProfile.setImageResource(R.drawable.profile_placeholder);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void saveProfile() {
        DocumentReference elderRef = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid);

        elderRef.update(
                "name", name.getText().toString(),
                "age", age.getText().toString(),
                "allergies", allergies.getText().toString(),
                "doctorInfo", doctorInfo.getText().toString(),
                "imageBase64", imageBase64
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(ProfilePageElder.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(ProfilePageElder.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imgProfile.setImageBitmap(bitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] b = baos.toByteArray();
                imageBase64 = Base64.encodeToString(b, Base64.DEFAULT);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
