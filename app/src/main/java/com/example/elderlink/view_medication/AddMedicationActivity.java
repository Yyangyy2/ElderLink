package com.example.elderlink.view_medication;


import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddMedicationActivity extends AppCompatActivity {
    private static final String TAG = "AddMedicationActivity";

    private EditText editMedicationName, editMedicationDate, editMedicationTime;
    private ImageView selectedMedicationImage;
    private Button selectMedicationImageBtn, saveMedicationBtn;
    private Spinner spinnerMedicationUnit;

    private String selectedImageBase64 = "";
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore firestore;
    private String caregiverUid; // current signed-in caregiver uid
    private String personUid;    // required: the person doc id under users/{caregiverUid}/people/{personUid}

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_add_page); // <-- ensure this layout file exists

        // Views
        editMedicationName = findViewById(R.id.editMedicationName);
        editMedicationDate = findViewById(R.id.editMedicationDate);
        editMedicationTime = findViewById(R.id.editMedicationTime);
        selectedMedicationImage = findViewById(R.id.selectedMedicationImage);
        selectMedicationImageBtn = findViewById(R.id.selectMedicationImageBtn);
        saveMedicationBtn = findViewById(R.id.saveMedicationBtn);
        spinnerMedicationUnit = findViewById(R.id.spinnerMedicationUnit);

        // Spinner - units
        ArrayAdapter<String> unitsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Tablets", "ml", "Drops", "Dose", "Capsules", "Other"}
        );
        unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedicationUnit.setAdapter(unitsAdapter);

        // Firestore & auth
        firestore = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Read required personUid from Intent
        personUid = getIntent().getStringExtra("personUid");
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified. Cannot save medication.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Saving for caregiverUid=" + caregiverUid + " personUid=" + personUid);

        // Date/time pickers
        editMedicationDate.setOnClickListener(v -> showDatePicker());
        editMedicationDate.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(); });

        editMedicationTime.setOnClickListener(v -> showTimePicker());
        editMedicationTime.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showTimePicker(); });

        // Image picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                // compress to reduce size
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                selectedImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                selectedMedicationImage.setImageBitmap(bitmap);
                Log.d(TAG, "Image selected, bytes=" + baos.size());
            } catch (Exception e) {
                Log.e(TAG, "Image picker error", e);
                Toast.makeText(AddMedicationActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });
        selectMedicationImageBtn.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        saveMedicationBtn.setOnClickListener(v -> saveMedication());
    }

    private void showDatePicker() {
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            editMedicationDate.setText(sdfDate.format(calendar.getTime()));
        }, y, m, d).show();
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute1);
            editMedicationTime.setText(sdfTime.format(calendar.getTime()));
        }, hour, minute, true).show();
    }

    private void saveMedication() {
        String med_name = editMedicationName.getText().toString().trim();
        String med_date = editMedicationDate.getText().toString().trim();
        String med_time = editMedicationTime.getText().toString().trim();
        String med_unit = (String) spinnerMedicationUnit.getSelectedItem();

        if (med_name.isEmpty()) {
            editMedicationName.setError("Name required");
            editMedicationName.requestFocus();
            return;
        }
        if (med_date.isEmpty()) {
            editMedicationDate.setError("Date required");
            editMedicationDate.requestFocus();
            return;
        }
        if (med_time.isEmpty()) {
            editMedicationTime.setError("Time required");
            editMedicationTime.requestFocus();
            return;
        }

        String docId = UUID.randomUUID().toString();
        Model_medication medication = new Model_medication(docId, med_name, med_date, med_time, med_unit, selectedImageBase64);


        // SAVE to users/{caregiverUid}/people/{personUid}/medications/{docId}
        firestore.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .document(docId)
                .set(medication)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saved medication for personUid=" + personUid + " docId=" + docId);
                    Toast.makeText(AddMedicationActivity.this, "Medication saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save medication", e);
                    Toast.makeText(AddMedicationActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


}
