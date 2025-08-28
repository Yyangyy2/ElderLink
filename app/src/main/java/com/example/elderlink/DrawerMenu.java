package com.example.elderlink;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;

import com.example.elderlink.view_medication.ViewMedicationActivity;

public class DrawerMenu {

    public static void setupMenu(Activity activity) {
        LinearLayout btnleftHome = activity.findViewById(R.id.btnleftHome);
        LinearLayout btnleftMedication = activity.findViewById(R.id.btnleftMedication);

        btnleftHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        });

        btnleftMedication.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ViewMedicationActivity.class);
            activity.startActivity(intent);
        });


    }
}

