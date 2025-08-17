package com.example.elderlink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CheckOnElderlyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_on_elderly_page);

        TextView nameText = findViewById(R.id.personName);
        ImageView imageView = findViewById(R.id.personImage);

        // Get data from intent
        String name = getIntent().getStringExtra("personName");
        String imageBase64 = getIntent().getStringExtra("personImageBase64");

        nameText.setText(name);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } else {
            imageView.setImageResource(R.drawable.profile_placeholder);
        }


        //Back Button (to MainPage)------------------------------------------------
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckOnElderlyActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });



    }
}
