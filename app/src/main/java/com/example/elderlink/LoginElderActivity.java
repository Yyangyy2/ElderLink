package com.example.elderlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class LoginElderActivity extends AppCompatActivity {

    private EditText loginElder;
    private Button enterButton, caregiverRedirectButton;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_elder);

        loginElder = findViewById(R.id.login_elder);
        enterButton = findViewById(R.id.enter_button);
        caregiverRedirectButton = findViewById(R.id.caregiver_redirect_Button);

        db = FirebaseFirestore.getInstance();

        // caregiver redirect dropdown menu----------------------------------------------------------------------------------------
        caregiverRedirectButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(LoginElderActivity.this, caregiverRedirectButton);
            popup.getMenuInflater().inflate(R.menu.login_elder_redirect_caregiver_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_signup) {
                    startActivity(new Intent(LoginElderActivity.this, SignupActivity.class));
                    return true;
                } else if (id == R.id.menu_login) {
                    startActivity(new Intent(LoginElderActivity.this, LoginActivity.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // Elder login check------------------------------------------------------------------------------------------------------
        enterButton.setOnClickListener(v -> {
            String caregiverUsername = loginElder.getText().toString().trim();

            if (caregiverUsername.isEmpty()) {
                Toast.makeText(this, "Please enter caregiver username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check caregiver existence
            db.collection("users")
                    .whereEqualTo("username", caregiverUsername)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result != null && !result.isEmpty()) {
                                // Caregiver exists
                                Toast.makeText(this, "Caregiver found!", Toast.LENGTH_SHORT).show();

                                // Fetch "people" under this caregiver
                                // adjust path according your data model — here we assume a top-level "people" collection that has a field 'caregiverId' or 'username'
                                // Example below assumes people documents either have 'caregiverUsername' or you have a subcollection under users/<uid>/people
                                // In your earlier code you stored people as subcollection under users/{uid}/people
                                // So we first need the caregiver's uid from the user doc we found:
                                DocumentSnapshot caregiverDoc = result.getDocuments().get(0);
                                String caregiverUid = caregiverDoc.getId();

                                // Now read the people subcollection for that caregiver:
                                db.collection("users")
                                        .document(caregiverUid)
                                        .collection("people")
                                        .get()
                                        .addOnCompleteListener(peopleTask -> {
                                            if (peopleTask.isSuccessful()) {
                                                QuerySnapshot peopleResult = peopleTask.getResult();
                                                if (peopleResult != null && !peopleResult.isEmpty()) {
                                                    // Inflate popup layout (you need to create activity_login_elder_people_item_list.xml)
                                                    LayoutInflater inflater = getLayoutInflater();
                                                    View popupView = inflater.inflate(R.layout.activity_login_elder_people_item_list, null);

                                                    RecyclerView recyclerView = popupView.findViewById(R.id.recyclerPeople);
                                                    recyclerView.setLayoutManager(new LinearLayoutManager(this));

                                                    // Convert Firestore documents to List<Person>
                                                    List<Person> personList = new ArrayList<>();
                                                    for (DocumentSnapshot doc : peopleResult.getDocuments()) {
                                                        String id = doc.getId();
                                                        String name = doc.getString("name");
                                                        String imageBase64 = doc.getString("imageBase64");
                                                        String pin = doc.getString("pin");
                                                        personList.add(new Person(name == null ? "" : name, imageBase64, pin == null ? "" : pin, id)); //Ai told me to change the last imageBase64 to pin
                                                    }

                                                    // Attach adapter - pass context + list (PersonAdapter constructor in your code expects Context + List<Person>)
                                                    PersonAdapter adapter = new PersonAdapter(LoginElderActivity.this, personList, true, caregiverUid);
                                                    recyclerView.setAdapter(adapter);

                                                    // Build AlertDialog popup
                                                    AlertDialog dialog = new AlertDialog.Builder(this)
                                                            .setView(popupView)
                                                            .setCancelable(true)
                                                            .create();

                                                    // Close button in popup layout (if present)
                                                    View closeBtn = popupView.findViewById(R.id.closePopupButton);
                                                    if (closeBtn != null) {
                                                        closeBtn.setOnClickListener(v2 -> dialog.dismiss());
                                                    }

                                                    dialog.show();
                                                } else {
                                                    Toast.makeText(this, "No people found under this caregiver", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(this, "Error fetching people list", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            } else {
                                Toast.makeText(this, "Caregiver does not exist", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Error checking caregiver", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
