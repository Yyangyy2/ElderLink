package com.example.elderlink;


import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;

import com.example.elderlink.view_medication.ViewMedicationActivity;
import com.google.firebase.auth.FirebaseAuth;

public class DrawerMenu {

    public static void setupMenu(Activity activity) {
        LinearLayout btnleftHome = activity.findViewById(R.id.btnleftHome);
        LinearLayout btnleftMedication = activity.findViewById(R.id.btnleftMedication);
        LinearLayout btnLogout = activity.findViewById(R.id.btnLogout);

        btnleftHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        });

        btnleftMedication.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ViewMedicationActivity.class);


            // Pass along personUid using this current activity holding it (that's why use activity.getIntent), so next activity knows which elderly
            String personUid = activity.getIntent().getStringExtra("personUid");
            intent.putExtra("personUid", personUid);

            activity.startActivity(intent);  //Just start new activity
            activity.finish();
        });

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

