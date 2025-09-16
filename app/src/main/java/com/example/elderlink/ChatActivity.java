package com.example.elderlink;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    FirebaseFirestore db;
    JsonArray medicationArray = new JsonArray();

    EditText inputMessage;
    Button btnSend;
    TextView chatHistory;

    // Replace with your Gemini API Key
    private static final String GEMINI_API_KEY = "AIzaSyBaTsP0Kd7QVg9FxrGe2Glx0lXe1oBSX0s";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();

        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        chatHistory = findViewById(R.id.chatHistory);

        String personUid = getIntent().getStringExtra("personUid");
        loadMedications(personUid);

        btnSend.setOnClickListener(v -> {
            String userQuestion = inputMessage.getText().toString();
            if (userQuestion.isEmpty()) return;

            appendChat("You: " + userQuestion);

            // Join all Firestore data into one string
            String firestoreData = medicationArray.toString();

            String prompt =
                    "You are a helpful medical assistant. "
                            + "You are given a list of medications in JSON format. "
                            + "Each record has these fields: name, dosage, unit, time, date, endDate, repeatType, switchReminder. "
                            + "Answer the user’s question ONLY using the JSON data.\n\n"
                            + "JSON Data:\n" + firestoreData + "\n\n"
                            + "User Question: " + userQuestion + "\n\n"
                            + "Rules:\n"
                            + "- If the question matches a medication name, use its details.\n"
                            + "- For 'when' questions, look at the 'time', 'date', 'endDate', and 'repeatType'.\n"
                            + "- For 'how much' questions, use 'dosage' and 'unit'.\n"
                            + "- If you cannot find the answer, say exactly: 'I don’t know based on the data.'\n"
                            + "Answer:";





            callGemini(prompt);
            inputMessage.setText("");
        });
    }

    private void loadMedications(String personUid) {
        if (personUid == null || personUid.isEmpty()) {
            Toast.makeText(this, "No person specified.", Toast.LENGTH_LONG).show();
            return;
        }

        String userUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        db.collection("users")
                .document(userUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        String dosage = doc.getString("dosage");
                        String unit = doc.getString("unit");
                        String time = doc.getString("time");
                        String date = doc.getString("date");
                        String endDate = doc.getString("endDate");
                        String repeatType = doc.getString("repeatType");
                        Boolean switchReminder = doc.getBoolean("switchReminder");

                        // Build JSON-style record
                        JsonObject record = new JsonObject();
                        record.addProperty("id", doc.getId());
                        record.addProperty("name", name);
                        record.addProperty("dosage", dosage);
                        record.addProperty("unit", unit);
                        record.addProperty("time", time);
                        record.addProperty("date", date);
                        record.addProperty("endDate", endDate);
                        record.addProperty("repeatType", repeatType);
                        record.addProperty("switchReminder", switchReminder != null ? switchReminder : false);

                        medicationArray.add(record);

                    }
                    Log.d("Firestore", "Loaded medications: " + medicationArray);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading meds", e));
    }


    private void callGemini(String prompt) {
        new Thread(() -> {
            try {
                // Build request JSON
                JsonObject requestJson = new JsonObject();
                JsonArray contents = new JsonArray();

                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);

                contents.add(content);
                requestJson.add("contents", contents);

                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(requestJson.toString(), JSON);

                Request request = new Request.Builder()
                        .url(GEMINI_URL)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    Log.d("Gemini", result);

                    // Parse with Gson
                    JsonObject json = new com.google.gson.JsonParser().parse(result).getAsJsonObject();
                    String aiText = json.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();

                    runOnUiThread(() -> appendChat("AI: " + aiText));
                } else {
                    runOnUiThread(() -> appendChat("AI: API Error " + response.code()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> appendChat("AI: Exception - " + e.getMessage()));
            }
        }).start();
    }


    private void appendChat(String message) {
        chatHistory.append(message + "\n\n");
    }
}
