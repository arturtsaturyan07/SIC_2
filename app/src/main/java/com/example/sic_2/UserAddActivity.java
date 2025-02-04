package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get card ID from Intent
        cardId = getIntent().getStringExtra("cardId");

        addUserButton.setOnClickListener(view -> addUser());
    }

    private void addUser() {
        String newUserId = userIdInput.getText().toString().trim();

        if (newUserId.isEmpty()) {
            Toast.makeText(this, "Enter user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firebase so both users can see the card
        Map<String, Object> updates = new HashMap<>();
        updates.put("cards/" + cardId + "/users/" + newUserId, true);
        updates.put("cards/" + cardId + "/users/" + currentUserId, true);

        usersRef.getRoot().updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UserAddActivity.this, "User added!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(UserAddActivity.this, "Failed to add user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
