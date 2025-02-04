package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class Card extends AppCompatActivity {
    private DatabaseReference database;
    private TextView cardMessage;
    private String cardId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Get cardId from Intent
        cardId = getIntent().getStringExtra("cardId");

        // Initialize UI components
        cardMessage = findViewById(R.id.card_message);
        ImageButton backButton = findViewById(R.id.backbutton);
        Button addUserButton = findViewById(R.id.addUserButton);

        // Load card data only if the user has access
        if (cardId != null) {
            checkUserAccess();
        }

        // Handle back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(Card.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Open AddUserActivity when "Add User" button is clicked
        addUserButton.setOnClickListener(view -> {
            Intent intent = new Intent(Card.this, UserAddActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });
    }

    private void checkUserAccess() {
        database.child(cardId).child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    loadCardData();  // User has access, load card
                } else {
                    Toast.makeText(Card.this, "Access denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to check access", error.toException());
            }
        });
    }


    private void loadCardData() {
        database.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        cardMessage.setText(message);
                    }
                } else {
                    Log.e("Firebase", "Card not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load card", error.toException());
            }
        });
    }
}
