package com.example.elderlink.view_medication;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddMedicationActivity extends AppCompatActivity {
    private static final String TAG = "AddMedicationActivity";

    private EditText editMedicationName, editMedicationDate, editMedicationEndDate, editMedicationTime, editMedicationDosage;
    private ImageView selectedMedicationImage;
    private Button selectMedicationImageBtn, saveMedicationBtn, deleteMedicationBtn;
    private Spinner spinnerMedicationUnit, spinnerMedicationRepeatType, spinnerMedicationStatus;
    private LinearLayout statusLayout;

    private String selectedImageBase64 = "";
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore firestore;
    private String caregiverUid; // current signed-in user
    private String OwnerUid; // the owner for this person (primary caregiver)
    private String personUid;
    private String personName;
    @Nullable
    private String medId;
    private Switch switchReminder;
    private boolean isEditMode = false;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_add_page);

        // Initialize all views first
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

        // If caregiverUid (owner) is provided in intent (when the person is a shared reference), use it as OwnerUid
        String ownerFromIntent = getIntent().getStringExtra("caregiverUid");
        if (ownerFromIntent != null && !ownerFromIntent.isEmpty()) {
            OwnerUid = ownerFromIntent;
        } else {
            OwnerUid = caregiverUid; // default to current user
        }

        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "caregiverUid=" + caregiverUid + ", OwnerUid=" + OwnerUid + ", personUid=" + personUid + ", medId=" + medId);

        // Setup spinners
        setupSpinners();

        // Hide delete button if adding new
        if (!isEditMode) {
            deleteMedicationBtn.setVisibility(Button.GONE);
        }

        // Date/time pickers
        setupDateTimePickers();

        // Image picker
        setupImagePicker();

        // Button listeners
        setupButtonListeners();

        // If editing, load existing data------------------------------------------------------------------------
        if (isEditMode) {
            loadMedicationForEdit();
            setTitle("Edit Medication");
        } else {
            setTitle("Add Medication");
            // Hide status spinner for new medications (only if it exists)
            if (statusLayout != null) {
                statusLayout.setVisibility(View.GONE);
            }
        }
    }

    private void initializeViews() {
        editMedicationName = findViewById(R.id.editMedicationName);
        editMedicationDate = findViewById(R.id.editMedicationDate);
        editMedicationEndDate = findViewById(R.id.editMedicationEndDate);
        editMedicationTime = findViewById(R.id.editMedicationTime);
        editMedicationDosage = findViewById(R.id.editMedicationDosage);
        selectedMedicationImage = findViewById(R.id.selectedMedicationImage);
        selectMedicationImageBtn = findViewById(R.id.selectMedicationImageBtn);
        saveMedicationBtn = findViewById(R.id.saveMedicationBtn);
        spinnerMedicationUnit = findViewById(R.id.spinnerMedicationUnit);
        spinnerMedicationRepeatType = findViewById(R.id.spinnerMedicationRepeatType);
        deleteMedicationBtn = findViewById(R.id.deleteMedicationBtn);
        switchReminder = findViewById(R.id.switchReminder);

        // Initialize status views only if they exist in the layout
        try {
            statusLayout = findViewById(R.id.statusLayout);
            spinnerMedicationStatus = findViewById(R.id.spinnerMedicationStatus);
        } catch (Exception e) {
            Log.w(TAG, "Status layout or spinner not found in layout");
            statusLayout = null;
            spinnerMedicationStatus = null;
        }
    }

    private void setupSpinners() {
        // Spinner Unit setup---------------------------------------------------------------------------------------
        ArrayAdapter<String> unitsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Tablets", "ml", "Drops", "Dose", "Capsules", "Other"}
        );
        unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedicationUnit.setAdapter(unitsAdapter);

        // Spinner Repeat Type setup---------------------------------------------------------------------------------------
        ArrayAdapter<String> repeatTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Only as needed", "Daily", "Weekly"}
        );
        repeatTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedicationRepeatType.setAdapter(repeatTypeAdapter);

        // Status Spinner setup (only if it exists)---------------------------------------------------------------------------------------
        if (spinnerMedicationStatus != null) {
            ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                    R.array.medication_status_array, android.R.layout.simple_spinner_item);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMedicationStatus.setAdapter(statusAdapter);
        }
    }

    private void setupDateTimePickers() {
        editMedicationDate.setOnClickListener(v -> showDatePicker());
        editMedicationEndDate.setOnClickListener(v -> showEndDatePicker());
        editMedicationTime.setOnClickListener(v -> showTimePicker());
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
            } catch (Exception e) {
                Log.e(TAG, "Image picker error", e);
                Toast.makeText(AddMedicationActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });
        selectMedicationImageBtn.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupButtonListeners() {
        saveMedicationBtn.setOnClickListener(v -> saveMedication());
        deleteMedicationBtn.setOnClickListener(v -> deleteMedication());
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

    private void showEndDatePicker() {
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            editMedicationEndDate.setText(sdfDate.format(calendar.getTime()));
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

    private void loadMedicationForEdit() {
        // Show status spinner for editing (only if it exists)
        if (statusLayout != null) {
            statusLayout.setVisibility(View.VISIBLE);
        }

        // Use OwnerUid when reading the document when edit the medication
        DocumentReference docRef = firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .document(medId);

        docRef.get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Toast.makeText(this, "Medication not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    Model_medication med = snap.toObject(Model_medication.class);
                    if (med == null) return;

                    editMedicationName.setText(med.getName());
                    editMedicationDate.setText(med.getDate());
                    editMedicationEndDate.setText(med.getEndDate());
                    editMedicationTime.setText(med.getTime());

                    if (med.getDosage() != null) {
                        editMedicationDosage.setText(med.getDosage());
                    }

                    if (med.getUnit() != null) {
                        ArrayAdapter adapter = (ArrayAdapter) spinnerMedicationUnit.getAdapter();
                        int pos = adapter.getPosition(med.getUnit());
                        if (pos >= 0) spinnerMedicationUnit.setSelection(pos);
                    }

                    if (med.getRepeatType() != null) {
                        ArrayAdapter adapter = (ArrayAdapter) spinnerMedicationRepeatType.getAdapter();
                        int pos = adapter.getPosition(med.getRepeatType());
                        if (pos >= 0) spinnerMedicationRepeatType.setSelection(pos);
                    }

                    // Set status if it exists and status spinner is available
                    if (spinnerMedicationStatus != null) {
                        if (med.getStatus() != null && !med.getStatus().isEmpty()) {
                            ArrayAdapter adapter = (ArrayAdapter) spinnerMedicationStatus.getAdapter();
                            int pos = adapter.getPosition(med.getStatus());
                            if (pos >= 0) {
                                spinnerMedicationStatus.setSelection(pos);
                            }
                        } else {
                            // Default to "Upcoming" if status is null
                            spinnerMedicationStatus.setSelection(0); // "Upcoming" is first item
                        }
                    }

                    selectedImageBase64 = med.getImageBase64();
                    if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
                        byte[] decoded = Base64.decode(selectedImageBase64, Base64.DEFAULT);
                        selectedMedicationImage.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadMedicationForEdit failed", e);
                    Toast.makeText(this, "Error loading medication: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void saveMedication() {
        String med_name = editMedicationName.getText().toString().trim();
        String med_date = editMedicationDate.getText().toString().trim();
        String med_endDate = editMedicationEndDate.getText().toString().trim();
        String med_time = editMedicationTime.getText().toString().trim();
        String med_dosage = editMedicationDosage.getText().toString().trim();
        String med_unit = (String) spinnerMedicationUnit.getSelectedItem();
        String med_repeatType = (String) spinnerMedicationRepeatType.getSelectedItem();
        boolean med_switchReminder = switchReminder.isChecked();

        // Get status only if we're in edit mode and status spinner is available
        String med_status = null;
        if (isEditMode && spinnerMedicationStatus != null && statusLayout != null && statusLayout.getVisibility() == View.VISIBLE) {
            med_status = (String) spinnerMedicationStatus.getSelectedItem();
        }

        if (med_name.isEmpty() || med_date.isEmpty() || med_time.isEmpty() || med_endDate.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        // handle repeat types-----------------------------------------------------------------
        if ("Daily".equals(med_repeatType) || "Weekly".equals(med_repeatType)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(med_date));
                Calendar end = Calendar.getInstance();
                end.setTime(sdf.parse(med_endDate));

                int step = "Daily".equals(med_repeatType) ? 1 : 7; // 1 day for daily, 7 days for weekly

                while (!cal.getTime().after(end.getTime())) {
                    String currentDate = sdf.format(cal.getTime());
                    saveSingleMedication(
                            med_name, currentDate, med_endDate, med_time, med_dosage,
                            med_unit, med_repeatType, selectedImageBase64, med_switchReminder, med_status
                    );
                    cal.add(Calendar.DAY_OF_MONTH, step); // add 1 or 7 days
                }

                Toast.makeText(this, med_repeatType + " medications saved", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Only as needed----------------------------------------------------------
            saveSingleMedication(
                    med_name, med_date, med_endDate, med_time, med_dosage,
                    med_unit, med_repeatType, selectedImageBase64, med_switchReminder, med_status
            );
            Toast.makeText(this, "Medication saved", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveSingleMedication(String name, String date, String endDate, String time,
                                      String dosage, String unit, String repeatType, String imageBase64,
                                      boolean reminderEnabled, String status) {

        String docId = isEditMode && medId != null ? medId : UUID.randomUUID().toString();// Use existing medId if in edit mode, otherwise generate a new one

        Model_medication medication = new Model_medication(
                docId,
                name,
                date,
                endDate,
                time,
                dosage,
                unit,
                imageBase64 == null ? "" : imageBase64,
                repeatType,
                reminderEnabled,
                status    // (null for new medications, actual value for edits)
        );

        // Write to OwnerUid so shared persons' medications remain canonical and in sync
        firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .document(docId)
                .set(medication)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saved successfully " + date);

                    if (reminderEnabled) {
                        String medInfo = name + " : " + dosage + " " + unit;
                        scheduleReminder(docId, medInfo, personName, personUid, OwnerUid, date, time);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Save failed", e));
    }

    //Reminder (Initial start the reminder to fire to ReminderReceiver.java)------------------------------------------------------------------------------------------------------------------------------------
    private void scheduleReminder(String medId, String medInfo, String personName, String personUid, String caregiverUid, String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date d = sdf.parse(date + " " + time);
            if (d == null) return;
            long trigger = d.getTime();

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);   //Without AlarmManager, reminders won't survive when the app closes.
            Intent intent = new Intent(this, ReminderReceiver.class);     //Pass the below data to ReminderReceiver.java
            intent.putExtra("medId", medId);
            intent.putExtra("medInfo", medInfo);
            intent.putExtra("personName", personName);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("retryCount", 0);
            //intent.putExtra("role", "elder");//!!! Strangely this needs to be elder to work

            int requestCode = medId.hashCode() ^ 12345;                                 // medId.hashCode() with 12345, make sure alarm has a different requestCode from retries, so no collision
            PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (am != null) {                                                           // If AlarmManager am is not null, schedules the alarm in the Android system
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);     //RTC_WAKEUP Uses real-world clock time
                Log.d("AddMedication", "Scheduled initial alarm medId=" + medId + " at " + trigger + " req=" + requestCode);
            }
        } catch (Exception e) {
            Log.e("AddMedication", "scheduleReminder parse error", e);
        }
    }

    //Delete medication---------------------------------------------------------------------------------------------------------------------------------
    private void deleteMedication() {
        if (!isEditMode || medId == null) {
            Toast.makeText(this, "No medication to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete from OwnerUid so canonical medication removed
        firestore.collection("users")
                .document(OwnerUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
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