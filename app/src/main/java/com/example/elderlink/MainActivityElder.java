package com.example.elderlink;

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

import com.example.elderlink.view_medication.ViewMedicationActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivityElder extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_elder);

        DrawerMenu.setupMenu(this); // Add the Left side menu

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        String imageBase64 = getIntent().getStringExtra("personImageBase64");
        String personUid = getIntent().getStringExtra("personUid");

        nameText.setText(name);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } else {
            imageView.setImageResource(R.drawable.profile_placeholder);
        }






        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivityElder.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //Bottom Navigation Bar-----------------------------------------------------------------------------------------
        ImageButton navNotifications = findViewById(R.id.navNotifications);

        navNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivityElder.this, ChatActivity.class);
                intent.putExtra("personUid", personUid);
                startActivity(intent);
                finish();
            }
        });



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
                Intent intent = new Intent(MainActivityElder.this, ViewMedicationActivity.class);

                // Pass along personUid so next activity knows which elderly
                String personUid = getIntent().getStringExtra("personUid");
                intent.putExtra("personUid", personUid);

                startActivity(intent);
                finish();
            }
        });




        //Log out Elder----------------------------------------------------------------------------
        logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            //Redirect to SignupActivity
            Intent intent = new Intent(MainActivityElder.this, LoginElderActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Clear back stack so user can't press back to return
            startActivity(intent);
            finish(); //ensures current activity is finished
        });


    }
}
