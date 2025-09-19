package com.example.elderlink.view_Ask_Ai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlink.CheckOnElderlyActivity;
import com.example.elderlink.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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


        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        String imageBase64 = getIntent().getStringExtra("personImageBase64");
        String personUid = getIntent().getStringExtra("personUid");
        String uid = getIntent().getStringExtra("caregiverUid");
        loadMedications(personUid);

        btnSend.setOnClickListener(v -> {
            String userQuestion = inputMessage.getText().toString();
            if (userQuestion.isEmpty()) return;


            // Get today's date to help Ai know
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date());


            appendChat("You: " + userQuestion);

            // Join all Firestore data into one string
            String firestoreData = medicationArray.toString();

            String prompt =
                    "You are a helpful and friendly medical assistant. "
                            + "Today’s date is " + today + ". "
                            + "You are given a list of medications in JSON format. "
                            + "Each record may include: name, dosage, unit, time (24-hour format), date, endDate, repeatType, and switchReminder. "
                            + "Answer the user’s question in a short, clear, and polite way.\n\n"

                            + "JSON Data:\n" + firestoreData + "\n\n"

                            + "User Question: " + userQuestion + "\n\n"

                            + "Guidelines:\n"
                            + "- Keep answers 1–3 sentences max.\n"
                            + "- Be direct, but warm and supportive.\n"
                            + "- If multiple medications apply, show them as a simple bullet list.\n"
                            + "- For 'when' questions, always show both formats like: '14:00 (2:00 PM)'.\n"
                            + "- For 'when' questions, use time/date/repeat info.\n"
                            + "- For 'how much' questions, use dosage + unit.\n"
                            + "- If the info isn’t in the data, say: 'I don’t know based on the data.'\n"
                            + "- Never invent details not in the JSON.\n\n"

                            + "Answer:";






            callGemini(prompt);
            inputMessage.setText("");
        });


        //Back Button (to MainPage)------------------------------------------------
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, CheckOnElderlyActivity.class);
                intent.putExtra("personUid", personUid);
                intent.putExtra("personName", name);
                intent.putExtra("personImageBase64", imageBase64);
                intent.putExtra("caregiverUid", uid);
                startActivity(intent);
                finish();
            }
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
