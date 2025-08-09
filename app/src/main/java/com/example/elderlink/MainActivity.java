package com.example.elderlink;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.peopleRecyclerView);
        addPersonBtn = findViewById(R.id.addPersonBtn);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        personList = new ArrayList<>();
        adapter = new PersonAdapter(this, personList);
        recyclerView.setAdapter(adapter);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore = FirebaseFirestore.getInstance();

        // Load people from Firestore
        firestore.collection("users").document(uid).collection("people")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error loading", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    personList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Person p = doc.toObject(Person.class);
                        if (p != null) {
                            personList.add(p);
                        }
                    }
                    adapter.notifyDataSetChanged();

                });



        addPersonBtn.setOnClickListener(v -> showAddPersonDialog());
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
            firestore.collection("users").document(uid).collection("people")
                    .add(newPerson)
                    .addOnSuccessListener(docRef -> Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(err -> Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show());
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

                        // Compress to smaller size
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        byte[] imageBytes = baos.toByteArray();

                        // Encode Base64
                        selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                        if (imagePreview != null) {
                            imagePreview.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );
}
