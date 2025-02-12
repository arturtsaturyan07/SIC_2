package com.example.sic_2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserAddActivity extends AppCompatActivity {

    private EditText userIdInput;
    private DatabaseReference cardsRef;
    private String cardId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_add);



        // Initialize views
        userIdInput = findViewById(R.id.userIdInput);
        Button addUserButton = findViewById(R.id.addUserButton);

        // Get current user ID
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Initialize Firebase reference to the cards node
        cardsRef = FirebaseDatabase.getInstance().getReference("cards");

        // Retrieve card ID from intent
        cardId = getIntent().getStringExtra("cardId");
        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(this, "Invalid card ID", Toast.LENGTH_SHORT).show();
            finish(); // Exit the activity if card ID is missing
            return;
        }

        // Set click listener for the button
        addUserButton.setOnClickListener(view -> checkUserExists());
    }

    private void checkUserExists() {
        String newUserId = userIdInput.getText().toString().trim();

        // Validate input
        if (newUserId.isEmpty()) {
            Toast.makeText(this, "Enter user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user exists in the database
        cardsRef.child(newUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    shareCardWithUser(newUserId); // Proceed to share the card with the user
                } else {
                    Toast.makeText(UserAddActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserAddActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareCardWithUser(String newUserId) {
        // Reference to the current user's card
        DatabaseReference currentCardRef = cardsRef.child(currentUserId).child(cardId);

        // Reference to the target user's cards node
        DatabaseReference targetUserCardsRef = cardsRef.child(newUserId).child(cardId);

        // Fetch the card's information
        currentCardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the card's information
                    Map<String, Object> cardInfo = (Map<String, Object>) snapshot.getValue();

                    // Share the card with the target user
                    targetUserCardsRef.setValue(cardInfo).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(UserAddActivity.this, "Card shared successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Close the activity after successful sharing
                        } else {
                            Toast.makeText(UserAddActivity.this, "Failed to share card", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(UserAddActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(UserAddActivity.this, "Card does not exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserAddActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}