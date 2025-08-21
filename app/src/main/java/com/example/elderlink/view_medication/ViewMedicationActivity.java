package com.example.elderlink.view_medication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.MainActivity;
import com.example.elderlink.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewMedicationActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    private MedicationAdapter adapter;
    private List<Model_medication> medicationList = new ArrayList<>();
    private FirebaseFirestore db;
    private String personUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_view_page);


        db = FirebaseFirestore.getInstance();
        personUid = getIntent().getStringExtra("personUid");


        recyclerView = findViewById(R.id.medicationRecyclerView);
        // Use 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);


        adapter = new MedicationAdapter(this, medicationList, medication -> {
            // handle edit click → open AddMedicationActivity in edit mode
            Intent intent = new Intent(ViewMedicationActivity.this, AddMedicationActivity.class);
            intent.putExtra("personUid", personUid);
            intent.putExtra("medId", medication.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadMedications();





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



    }

    //Out of onCreate boundary--------------------------------------------------------------------------
    //Display medications-------------------------------------------------------------------------------
    // do not use .get(), it is not realtime and i have to reenter the page to only see the changes (add/delete), instead use SnapshotListener() to listen for updates
    private void loadMedications() {
        String userUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        medicationList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Model_medication medication = doc.toObject(Model_medication.class);
                            if (medication != null) {
                                medication.setId(doc.getId()); // set Firestore doc id
                                medicationList.add(medication);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }


}