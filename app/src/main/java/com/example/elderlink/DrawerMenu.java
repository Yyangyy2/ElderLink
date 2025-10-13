package com.example.elderlink;


import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;

import com.example.elderlink.view_Ask_Ai.ChatActivity;
import com.example.elderlink.view_collaborators.ViewCollaborators;
import com.example.elderlink.view_gps.GPSActivity;
import com.example.elderlink.view_medication.ViewMedicationActivity;
import com.google.firebase.auth.FirebaseAuth;

public class DrawerMenu {

    public static void setupMenu(Activity activity) {

        String caregiverUid = activity.getIntent().getStringExtra("caregiverUid");
        String personName = activity.getIntent().getStringExtra("personName");
        String personUid = activity.getIntent().getStringExtra("personUid");


        LinearLayout btnleftHome = activity.findViewById(R.id.btnleftHome);
        LinearLayout btnleftMedication = activity.findViewById(R.id.btnleftMedication);
        LinearLayout btnleftAskaibot = activity.findViewById(R.id.btnleftAskaibot);
        LinearLayout btnleftGPS = activity.findViewById(R.id.btnleftGPS);
        LinearLayout btnleftCollaborators = activity.findViewById(R.id.btnleftCollaborators);
        LinearLayout btnleftMyAccount = activity.findViewById(R.id.btnleftMyAccount);
        LinearLayout btnLogout = activity.findViewById(R.id.btnLogout);



        //Home button-------------------------------------------------------------------------------------------------
        btnleftHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        });



        //Medication button-------------------------------------------------------------------------------------------
        btnleftMedication.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ViewMedicationActivity.class);
            // Pass along personUid using this current activity holding it (that's why use activity.getIntent), so next activity knows which elderly
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });


        //Ask Ai button-------------------------------------------------------------------------------------------
        btnleftAskaibot.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ChatActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });


        //GPS button-------------------------------------------------------------------------------------------
        btnleftGPS.setOnClickListener(v -> {
            Intent intent = new Intent(activity, GPSActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });


        //Collaborators button-------------------------------------------------------------------------------------------
        btnleftCollaborators.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ViewCollaborators.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });


        //Profile pic button-------------------------------------------------------------------------------------------
        btnleftMyAccount.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ProfilePageCaregiver.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("caregiverUid", caregiverUid);
            intent.putExtra("personName", personName);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });



        //Logout button-------------------------------------------------------------------------------------------
        btnLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            //Redirect to SignupActivity
            Intent intent = new Intent(activity, SignupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Clear back stack so user can't press back to return
            activity.startActivity(intent);
            activity.finish(); //ensures current activity is finished
        });


    }
}

