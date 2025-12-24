package com.example.elderlink.view_medication_v2;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AddMedicationActivity_v2 extends AppCompatActivity {
    private static final String TAG = "AddMedication_v2";

    private EditText editBrandName, editMedName, editTimesPerDay;
    private ImageView selectedMedicationImage;
    private Button selectMedicationImageBtn, saveMedicationBtn, deleteMedicationBtn;

    private String selectedImageBase64 = "";
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore firestore;
    private String caregiverUid;
    private String OwnerUid;
    private String personUid;
    private String personName;
    private String medId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_add_page_v2);

        initializeViews();

        // Firestore
        firestore = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Intent extras
        personUid = getIntent().getStringExtra("personUid");
        personName = getIntent().getStringExtra("personName");
        medId = getIntent().getStringExtra("medId");
        isEditMode = (medId != null && !medId.isEmpty());

        // Determine OwnerUid
        String ownerFromIntent = getIntent().getStringExtra("caregiverUid");
        OwnerUid = (ownerFromIntent != null && !ownerFromIntent.isEmpty()) ? ownerFromIntent : caregiverUid;

        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "OwnerUid=" + OwnerUid + ", personUid=" + personUid + ", medId=" + medId);

        // Setup image picker
        setupImagePicker();

        // Hide delete button if adding new
        if (!isEditMode) {
            deleteMedicationBtn.setVisibility(Button.GONE);
        }

        setupButtonListeners();

        // Set up image click listener for enlargement
        setupImageClickListener();

        // If editing, load existing data
        if (isEditMode) {
            loadMedicationForEdit();
            setTitle("Edit Medication");
        } else {
            setTitle("Add Medication");
        }
    }

    private void initializeViews() {
        editBrandName = findViewById(R.id.editBrandName);
        editMedName = findViewById(R.id.editMedName);
        editTimesPerDay = findViewById(R.id.editTimesPerDay);
        selectedMedicationImage = findViewById(R.id.selectedMedicationImage);
        selectMedicationImageBtn = findViewById(R.id.selectMedicationImageBtn);
        saveMedicationBtn = findViewById(R.id.saveMedicationBtn);
        deleteMedicationBtn = findViewById(R.id.deleteMedicationBtn);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                selectedImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                selectedMedicationImage.setImageBitmap(bitmap);
                Log.d(TAG, "Image selected, bytes=" + baos.size());

                // Ensure click listener is set after new image is selected
                setupImageClickListener();
            } catch (Exception e) {
                Log.e(TAG, "Image picker error", e);
                Toast.makeText(AddMedicationActivity_v2.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });
        selectMedicationImageBtn.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupImageClickListener() {
        selectedMedicationImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEnlargedImage();
            }
        });

        // Make sure the image view is clickable
        selectedMedicationImage.setClickable(true);
    }

    private void showEnlargedImage() {
        if (selectedImageBase64 == null || selectedImageBase64.isEmpty()) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a fullscreen dialog
            Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.dialog_fullscreen_image);

            // Cast to ZoomImageView
            ZoomImageView enlargedImage = (ZoomImageView) dialog.findViewById(R.id.enlargedImageView);
            Button closeButton = dialog.findViewById(R.id.btnClose);

            // Decode and display the image
            byte[] decoded = Base64.decode(selectedImageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            enlargedImage.setImageBitmap(bitmap);

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing enlarged image: " + e.getMessage());
            Toast.makeText(this, "Failed to display image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonListeners() {
        saveMedicationBtn.setOnClickListener(v -> saveMedication());
        deleteMedicationBtn.setOnClickListener(v -> deleteMedication());
    }

    private void loadMedicationForEdit() {
        firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications_v2")
                .document(medId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Toast.makeText(this, "Medication not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    Model_medication_v2 med = snap.toObject(Model_medication_v2.class);
                    if (med == null) return;

                    editBrandName.setText(med.getBrandname());
                    editMedName.setText(med.getMedname());
                    editTimesPerDay.setText(med.getTimesperday());

                    // Load image if exists
                    if (med.getImageBase64() != null && !med.getImageBase64().isEmpty()) {
                        selectedImageBase64 = med.getImageBase64();
                        byte[] decoded = Base64.decode(selectedImageBase64, Base64.DEFAULT);
                        selectedMedicationImage.setImageBitmap(
                                BitmapFactory.decodeByteArray(decoded, 0, decoded.length)
                        );
                    }

                    // Ensure click listener is set after loading image
                    setupImageClickListener();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadMedicationForEdit failed", e);
                    Toast.makeText(this, "Error loading medication: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void saveMedication() {
        String brandname = editBrandName.getText().toString().trim();
        String medname = editMedName.getText().toString().trim();
        String timesperday = editTimesPerDay.getText().toString().trim();

        if (brandname.isEmpty() || medname.isEmpty() || timesperday.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = isEditMode && medId != null ? medId : UUID.randomUUID().toString();

        // Create medication object
        Model_medication_v2 medication = new Model_medication_v2();
        medication.setId(docId);
        medication.setBrandname(brandname);
        medication.setMedname(medname);
        medication.setTimesperday(timesperday);
        medication.setImageBase64(selectedImageBase64); // Add image to model

        // Save to Firestore
        firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications_v2")
                .document(docId)
                .set(medication)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Medication saved successfully!");
                    Toast.makeText(this, "Medication saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Save failed: " + e.getMessage());
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteMedication() {
        if (!isEditMode || medId == null) {
            Toast.makeText(this, "No medication to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications_v2")
                .document(medId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medication deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete failed", e);
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}