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

import java.util.Objects;

public class UserAddActivity extends AppCompatActivity {
    private EditText userIdInput;
    private DatabaseReference cardsRef;
    private String currentUserId;
    private String cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_add);

        userIdInput = findViewById(R.id.userIdInput);
        Button addUserButton = findViewById(R.id.addUserButton);

        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        cardsRef = FirebaseDatabase.getInstance().getReference("cards");

        cardId = getIntent().getStringExtra("cardId");

        addUserButton.setOnClickListener(view -> checkUserExists());
    }

    private void checkUserExists() {
        String newUserId = userIdInput.getText().toString().trim();

        if (newUserId.isEmpty()) {
            Toast.makeText(this, "Enter user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user exists in the database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(newUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    shareCardWithUser(newUserId);
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
        // Create a reference to the specific card
        DatabaseReference cardRef = cardsRef.child(currentUserId).child(cardId);

        // Create a new entry for the shared card
        cardRef.child("sharedWith").child(newUserId).setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UserAddActivity.this, "Card shared with user!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(UserAddActivity.this, "Failed to share card", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
