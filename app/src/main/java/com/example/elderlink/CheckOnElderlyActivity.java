package com.example.elderlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.elderlink.view_Ask_Ai.ChatActivity;
import com.example.elderlink.view_medication.ViewMedicationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CheckOnElderlyActivity extends AppCompatActivity {

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_on_elderly_page);

        DrawerMenu.setupMenu(this); // Add the Left side menu

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);

        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        //String imageBase64 = getIntent().getStringExtra("personImageBase64");
        String personUid = getIntent().getStringExtra("personUid");

        nameText.setText(name);
        imageView.setImageResource(R.drawable.profile_placeholder); // placeholder initially


        // Fetch elder profile from Firestore
        String caregiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference elderRef = db.collection("users")
                .document(caregiverId)
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




        //Back Button (to MainPage)------------------------------------------------
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        // Info Button (to ProfilePageElder)---------------------------------------------
        Button infoButton = findViewById(R.id.infobtn);

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ProfilePageElder.class);
                intent.putExtra("personUid", personUid);
                //intent.putExtra("personImageBase64", imageBase64);
                intent.putExtra("personName", name);
                startActivity(intent);
                finish();
            }
        });





        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
//        ImageButton navNotifications = findViewById(R.id.navNotifications);
//
//        navNotifications.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
//                intent.putExtra("personUid", personUid);
//                startActivity(intent);
//                finish();
//            }
//        });



        //Open Left navigation menu------------------------------------------------ rmb add DrawerMenu.setupMenu(this); on top
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton navMenu = findViewById(R.id.navMenu);

        // When clicking the button, open the drawer
        navMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // START is opens from left; END opens from right
        });




        //View Medication Button (to Medication)------------------------------------------------

        ImageButton btnMedication = findViewById(R.id.btnMedication);

        btnMedication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ViewMedicationActivity.class);

                // Pass along personUid so next activity knows which elderly
                String personUid = getIntent().getStringExtra("personUid");
                String caregiverUid = getIntent().getStringExtra("caregiverUid");
                intent.putExtra("personUid", personUid);
                intent.putExtra("caregiverUid", caregiverUid);
                intent.putExtra("personName", name);
                startActivity(intent);
                finish();
            }
        });


        //View Ask Ai Button (to Ai chatbot)------------------------------------------------

        ImageButton btnAibot = findViewById(R.id.btnAibot);

        btnAibot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, ChatActivity.class);
                intent.putExtra("personUid", personUid);
                intent.putExtra("personName", name);
                startActivity(intent);
                finish();
            }
        });




    }
}