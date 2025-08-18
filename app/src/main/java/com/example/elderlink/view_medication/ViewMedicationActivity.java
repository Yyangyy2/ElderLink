package com.example.elderlink.view_medication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.MainActivity;
import com.example.elderlink.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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


    private void loadMedications() {
        db.collection("people").document(personUid)
                .collection("medications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    medicationList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Model_medication med = doc.toObject(Model_medication.class);
                        if (med != null) {
                            med.setId(doc.getId()); // keep Firestore doc id
                            medicationList.add(med);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }


}