package com.example.elderlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.elderlink.view_Ask_Ai.ChatActivity;
import com.example.elderlink.view_collaborators.ViewCollaborators;
import com.example.elderlink.view_gps.GPSActivity;
import com.example.elderlink.view_medication_v2.ViewMedicationActivity_v2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckOnElderlyActivity extends AppCompatActivity {

    private String caregiverUid, username, personUid, personName;
    private FirebaseFirestore db;

    // Shared Notes variables
    private LinearLayout notesContainer;
    private EditText noteEditText;
    private Button addNoteButton;
    private List<SharedNote> notesList;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_on_elderly_page);

        DrawerMenu.setupMenu(this); // Add the Left side menu

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get data from intent
        personName = getIntent().getStringExtra("personName");
        personUid = getIntent().getStringExtra("personUid");
        username = getIntent().getStringExtra("username");
        String caregiverUidIntent = getIntent().getStringExtra("caregiverUid");
        if (caregiverUidIntent != null) {
            caregiverUid = caregiverUidIntent;
        }

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);

        nameText.setText(personName);
        imageView.setImageResource(R.drawable.profile_placeholder); // placeholder initially

        // Initialize Shared Notes
        initializeSharedNotes();

        // Fetch elder profile from Firestore
        DocumentReference elderRef = db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid);

        elderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageBase64 = documentSnapshot.getString("imageBase64");
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    imageView.setImageBitmap(bitmap);
                }
            }
        });

        // Load shared notes from Firestore
        loadSharedNotesFromFirestore();

        //Back Button (to MainPage)------------------------------------------------
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Info Button (to ProfilePageElder)---------------------------------------------
        Button infoButton = findViewById(R.id.infobtn);
        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ProfilePageElder.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        //Open Left navigation menu------------------------------------------------ rmb add DrawerMenu.setupMenu(this); on top
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // START is opens from left; END opens from right
        });

        //View Medication Button (to Medication)------------------------------------------------
        ImageButton btnMedication = findViewById(R.id.btnMedication);
        btnMedication.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ViewMedicationActivity_v2.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);
            startActivity(intent);
            finish();
        });

        //View Ask Ai Button (to Ai chatbot)------------------------------------------------
        ImageButton btnAibot = findViewById(R.id.btnAibot);
        btnAibot.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        //View Collaborators Button (to Collaborators)------------------------------------------------
        ImageButton btnCollaborators = findViewById(R.id.btnCollaborators);
        btnCollaborators.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, ViewCollaborators.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });

        //View GPS (to GPS)------------------------------------------------
        ImageButton btnGPS = findViewById(R.id.btnGPS);
        btnGPS.setOnClickListener(v -> {
            Intent intent = new Intent(CheckOnElderlyActivity.this, GPSActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("caregiverUid", caregiverUid);
            startActivity(intent);
            finish();
        });
    }

    // Dashboard ---------[Shared Notes] ----------------------------------------------------------------------------------------
    private void initializeSharedNotes() {
        notesContainer = findViewById(R.id.notesContainer);
        noteEditText = findViewById(R.id.noteEditText);
        addNoteButton = findViewById(R.id.addNoteButton);

        notesList = new ArrayList<>();

        // Set up click listener for adding notes
        addNoteButton.setOnClickListener(v -> addNewNote());
    }

    private void loadSharedNotesFromFirestore() {
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("shared_notes")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("SharedNotes", "Listen failed", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        notesList.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            String authorName = document.getString("authorName");
                            String content = document.getString("content");
                            String timestamp = document.getString("timestamp");
                            String noteId = document.getId();

                            if (authorName != null && content != null && timestamp != null) {
                                notesList.add(new SharedNote(authorName, content, timestamp, noteId));
                            }
                        }
                        refreshNotesDisplay();
                    }
                });
    }

    private void addNewNote() {
        String noteContent = noteEditText.getText().toString().trim();

        if (noteContent.isEmpty()) {
            Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = getCurrentTimestamp();

        // Create note data for Firestore
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("authorName", username);
        noteData.put("content", noteContent);
        noteData.put("timestamp", timestamp);
        noteData.put("createdAt", FieldValue.serverTimestamp());

        // Add to Firestore
        db.collection("users")
                .document(caregiverUid)
                .collection("people")
                .document(personUid)
                .collection("shared_notes")
                .add(noteData)
                .addOnSuccessListener(documentReference -> {
                    // Clear input
                    noteEditText.setText("");

                    // Reload notes to show the new one
                    loadSharedNotesFromFirestore();

                    Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add note", Toast.LENGTH_SHORT).show();
                    Log.e("SharedNotes", "Error adding note: ", e);
                });
    }

    //auto refresh notes display
    private void refreshNotesDisplay() {
        int childCount = notesContainer.getChildCount();
        for (int i = childCount - 2; i >= 0; i--) {
            notesContainer.removeViewAt(i);
        }

        // Show empty state if no notes
        if (notesList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No notes yet. Be the first to share something!");
            emptyText.setTextSize(14);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 32, 0, 32);
            notesContainer.addView(emptyText, 0);
        } else {
            // Add all notes
            for (int i = 0; i < notesList.size(); i++) {
                SharedNote note = notesList.get(i);
                View noteView = createNoteView(note);
                notesContainer.addView(noteView, i);
            }
        }
    }

    private View createNoteView(SharedNote note) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View noteView = inflater.inflate(R.layout.item_dashboard_note, notesContainer, false);

        TextView authorName = noteView.findViewById(R.id.authorName);
        TextView noteContent = noteView.findViewById(R.id.noteContent);
        TextView timestamp = noteView.findViewById(R.id.timestamp);

        authorName.setText(note.getAuthorName());
        noteContent.setText(note.getContent());
        timestamp.setText(note.getTimestamp());

        return noteView;
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Note model class
    public static class SharedNote {
        private String authorName;
        private String content;
        private String timestamp;
        private String noteId;

        public SharedNote(String authorName, String content, String timestamp, String noteId) {
            this.authorName = authorName;
            this.content = content;
            this.timestamp = timestamp;
            this.noteId = noteId;
        }

        public String getAuthorName() { return authorName; }
        public String getContent() { return content; }
        public String getTimestamp() { return timestamp; }
        public String getNoteId() { return noteId; }
    }
}