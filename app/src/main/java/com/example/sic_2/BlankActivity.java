package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class BlankActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout); // Create this layout file

        // Inside your onCreate method in MainActivity
        ImageButton backButton = findViewById(R.id.backbutton);
        backButton.setOnClickListener(view -> {
            // Create an Intent to start the MainActivity
            Intent intent = new Intent(BlankActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Optional: Call this if you want to close the current activity
        });


    }
}

