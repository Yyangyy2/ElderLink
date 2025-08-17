package com.example.elderlink;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private PersonAdapter adapter;
    private FirebaseFirestore firestore;
    private String uid;
    private String selectedImageBase64 = "";
    private ImageView imagePreview;
    private ListenerRegistration peopleListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.peopleRecyclerView);
        addPersonBtn = findViewById(R.id.addPersonBtn);
        TextView userNameTextView = findViewById(R.id.userName); //Mainpage Caregiver Username
        Button logoutButton = findViewById(R.id.logoutButton); //Logout caregiver

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        personList = new ArrayList<>();
        adapter = new PersonAdapter(this, personList);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore = FirebaseFirestore.getInstance();
        Log.d("MainActivity", "Current UID: " + uid);

        // Safe snapshot listener
        peopleListener = firestore.collection("users")
                .document(uid)
                .collection("people")
                .addSnapshotListener(this::onPeopleSnapshot);

        addPersonBtn.setOnClickListener(v -> showAddPersonDialog());




        //Caregiver username-----------------------------------------------------------------------------
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("username"); // adjust path according to your database


        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String username = snapshot.getValue(String.class);
                userNameTextView.setText(username);
            } else {
                userNameTextView.setText("No Name");
            }
        }).addOnFailureListener(e -> {
            userNameTextView.setText("No Name");
        });


        //Log out Caregiver----------------------------------------------------------------------------
        logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            //Redirect to SignupActivity
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Clear back stack so user can't press back to return
            startActivity(intent);
            finish(); //ensures current activity is finished
        });









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
        adapter.notifyDataSetChanged();
    }

    private void showAddPersonDialog() {
        selectedImageBase64 = ""; // Reset image every time

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Person");

        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_person, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editPersonName);
        ImageView selectedImage = dialogView.findViewById(R.id.selectedImage);
        dialogView.findViewById(R.id.selectImageBtn).setOnClickListener(v -> pickImage(selectedImage));

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty() || selectedImageBase64.isEmpty()) {
                Toast.makeText(this, "Name and Image required", Toast.LENGTH_SHORT).show();
                return;
            }

            Person newPerson = new Person(name, selectedImageBase64);

            firestore.collection("users")
                    .document(uid)
                    .collection("people")
                    .add(newPerson)
                    .addOnSuccessListener(docRef -> {
                        Log.d("FirestoreAdd", "Saved person with ID: " + docRef.getId());
                        Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                        // No need to manually update personList; snapshot listener will handle it
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
