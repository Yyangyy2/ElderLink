package com.example.elderlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.CheckBox;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {
    EditText signupEmail, signupUsername, signupPassword;
    Button signupButton;
    TextView loginRedirectText;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_caregiver);

        signupEmail = findViewById(R.id.signup_email);
        signupUsername = findViewById(R.id.signup_username);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        CheckBox checkboxTerms = findViewById(R.id.checkbox_terms);
        TextView linkTerms = findViewById(R.id.link_terms);


        auth = FirebaseAuth.getInstance();


        // Checkbox logic for Terms and Conditions---------------------------------------------------------------------------------------------------

        // Initially disable the Sign Up button if checkbox is not checked
        signupButton.setEnabled(false);
        signupButton.setAlpha(0.5f); // visual feedback

        // Enable button only when checkbox is ticked
        checkboxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            signupButton.setEnabled(isChecked);
            signupButton.setAlpha(isChecked ? 1f : 0.5f);
        });

        // Show Terms and Conditions dialog
        linkTerms.setOnClickListener(v -> {
            new AlertDialog.Builder(SignupActivity.this)
                    .setTitle("Terms and Conditions")
                    .setMessage("By using this app, you agree to our policy on data privacy, " +
                            "location tracking, and caregiver-elder data sharing. " +
                            "Please ensure you have consent from the elderly before enabling GPS tracking.")
                    .setPositiveButton("OK", null)
                    .show();
        });
        //--------------------------------------------------------------------------------------------------------------------------------------------


        signupButton.setOnClickListener(view -> {
            String email = signupEmail.getText().toString().trim();
            String username = signupUsername.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }


            FirebaseFirestore db = FirebaseFirestore.getInstance();

            //Check if username already exists to avoid duplication
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(checkTask -> {
                        if (checkTask.isSuccessful() && !checkTask.getResult().isEmpty()) {
                            // Username already taken
                            Toast.makeText(SignupActivity.this, "Username already exists. Choose another.", Toast.LENGTH_SHORT).show();
                        } else {

                            // Register user with FirebaseAuth
                            auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = auth.getCurrentUser();
                                            String uid = user.getUid();

                                            // Save additional user data in Realtime DB under /users/<uid>
                                            HashMap<String, String> userData = new HashMap<>();
                                            userData.put("email", email);
                                            userData.put("username", username);

                                            FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(uid)
                                                    .set(userData)
                                                    .addOnCompleteListener(dbTask -> {
                                                        if (dbTask.isSuccessful()) {
                                                            Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(SignupActivity.this, "Database write failed.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                        } else {
                                            Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
        });
        loginRedirectText.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));


        //Back Button (to LoginElderActivityPage)------------------------------------------------
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginElderActivity.class);
                startActivity(intent);
                finish();
            }
        });



    }
}

