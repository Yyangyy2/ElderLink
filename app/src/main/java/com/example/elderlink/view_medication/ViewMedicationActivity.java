package com.example.elderlink.view_medication;
import com.example.elderlink.DrawerMenu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.MainActivity;
import com.example.elderlink.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class ViewMedicationActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    private MedicationAdapter adapter;
    // For view medicines shown in recyclerView but before selceting date
    private List<Model_medication> medicationList = new ArrayList<>();
    // For view medicines shown in recyclerView, then filter by selected date
    private List<Model_medication> allMedications = new ArrayList<>();
    private FirebaseFirestore db;
    private String personUid;
    private String personName;


    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_view_page);

        DrawerMenu.setupMenu(this); // Add the Left side menu


        db = FirebaseFirestore.getInstance();
        personUid = getIntent().getStringExtra("personUid");
        personName = getIntent().getStringExtra("personName");


        recyclerView = findViewById(R.id.medicationRecyclerView);
        // Use 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);


        adapter = new MedicationAdapter(this, medicationList, medication -> {
            // handle edit click → open AddMedicationActivity in edit mode
            Intent intent = new Intent(ViewMedicationActivity.this, AddMedicationActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("personName", personName);
            intent.putExtra("medId", medication.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        setupCalendar();
        loadMedications(personUid);








        //FAB button --------------------------------------------------------------------------
        FloatingActionButton fab = findViewById(R.id.addMedicationFab);
        fab.setOnClickListener(v -> {
            // start AddMedicationActivity
            Intent intent = new Intent(ViewMedicationActivity.this,
                    com.example.elderlink.view_medication.AddMedicationActivity.class);

            // Forward the personUid from intent
            String personUid = getIntent().getStringExtra("personUid");
            intent.putExtra("personUid", personUid);

            startActivity(intent);
        });






        //Bottom Navigation Bar--------------------------------------------------------------------------
        ImageButton navHome = findViewById(R.id.navHome);

        navHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewMedicationActivity.this, MainActivity.class);
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



    }

    //Out of onCreate boundary--------------------------------------------------------------------------
    //Display medications-------------------------------------------------------------------------------
    // do not use .get(), it is not realtime and i have to reenter the page to only see the changes (add/delete), instead use SnapshotListener() to listen for updates
    private void loadMedications(String personUid) {
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            return;
        }

        String userUid = com.google.firebase.auth.FirebaseAuth.getInstance()         //Check caregiver authentication, go through users then to people
                .getCurrentUser()
                .getUid();

        db.collection("users")
                .document(userUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (querySnapshot != null) {
                        allMedications.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Model_medication med = doc.toObject(Model_medication.class);
                            if (med != null) {
                                med.setId(doc.getId());
                                allMedications.add(med);
                            }
                        }

                        // Default to today’s date
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Calendar.getInstance().getTime());
                        filterMedicationsByDate(today);
                    }
                });
    }








    // Setup horizontal scrolling calendar ---------------------------------------------------------------
    private void setupCalendar() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // Generate ±15 days around today
        List<String> dateList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = -15; i <= 15; i++) {
            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.DAY_OF_MONTH, i);
            dateList.add(sdf.format(tempCal.getTime()));
        }

        String today = sdf.format(cal.getTime());

        calendarAdapter = new CalendarAdapter(dateList, today, selectedDate -> {
            // filter meds when a new date is selected
            filterMedicationsByDate(selectedDate);
        });
        calendarRecyclerView.setAdapter(calendarAdapter);
    }



    // Filter by date -----------------------------------------------------------------------------------
    private void filterMedicationsByDate(String date) {
        medicationList.clear();
        for (Model_medication med : allMedications) {
            if (med.getDate() != null && med.getDate().equals(date)) {
                medicationList.add(med);
            }
        }
        adapter.notifyDataSetChanged();
    }


}