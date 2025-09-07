package com.example.elderlink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginElderActivity extends AppCompatActivity {

    private EditText loginElder;
    private Button enterButton, caregiverRedirectButton;;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_elder);

        loginElder = findViewById(R.id.login_elder);
        enterButton = findViewById(R.id.enter_button);
        caregiverRedirectButton = findViewById(R.id.caregiver_redirect_Button);


        db = FirebaseFirestore.getInstance();


        // Dropdown for caregiver options
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



        // Elder login check (based on if caregiver exist or not)------------------------------------------------
        enterButton.setOnClickListener(v -> {
            String caregiverUsername = loginElder.getText().toString().trim();

            if (caregiverUsername.isEmpty()) {
                Toast.makeText(this, "Please enter caregiver username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check does caregiver with this username exist?
            db.collection("users")
                    .whereEqualTo("username", caregiverUsername)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result != null && !result.isEmpty()) {
                                // Caregiver exists → Go to Main Activity
                                Toast.makeText(this, "Caregiver found!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginElderActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Caregiver not found
                                Toast.makeText(this, "Caregiver does not exist", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Error checking caregiver", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
