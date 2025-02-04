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
    private DatabaseReference usersRef;
    private String cardId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_add);

        userIdInput = findViewById(R.id.userIdInput);
        Button addUserButton = findViewById(R.id.addUserButton);

        // Get Firebase references
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get card ID from Intent
        cardId = getIntent().getStringExtra("cardId");

        addUserButton.setOnClickListener(view -> checkUserExists());
    }

    private void checkUserExists() {
        String newUserId = userIdInput.getText().toString().trim();

        if (newUserId.isEmpty()) {
            Toast.makeText(this, "Enter user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        usersRef.child(newUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    addUserToCard(newUserId);
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

    private void addUserToCard(String newUserId) {
        newUserId = userIdInput.getText().toString().trim();

        if (newUserId.isEmpty()) {
            Toast.makeText(this, "Enter user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalNewUserId = newUserId;
        usersRef.child(newUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DatabaseReference cardRef = FirebaseDatabase.getInstance().getReference("cards").child(cardId).child("users");

                    // 🔹 Add both users to the card
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(finalNewUserId, true);
                    updates.put(currentUserId, true);

                    cardRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(UserAddActivity.this, "User added!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(UserAddActivity.this, "Failed to add user", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(UserAddActivity.this, "User ID does not exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserAddActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
