package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private String userId;
    private DatabaseReference userRef;

    private TextView nameText, surnameText, bioText;
    private ImageView profileImage;
    private Button directChatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // UI Components
        nameText = findViewById(R.id.profile_name);
        surnameText = findViewById(R.id.profile_surname);
        bioText = findViewById(R.id.profile_bio);
        profileImage = findViewById(R.id.profile_image);
        directChatButton = findViewById(R.id.direct_chat_button);

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase references
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        fetchAndDisplayUserProfile();

        // Set click listener
        directChatButton.setOnClickListener(v -> startDirectChat());
    }

    private void fetchAndDisplayUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String surname = snapshot.child("surname").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    nameText.setText(name != null ? name : "Not Found");
                    surnameText.setText(surname != null ? surname : "Not Found");
                    bioText.setText(bio != null ? bio : "No bio available");

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_profile)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.default_profile);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void startDirectChat() {
        Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
        intent.putExtra("cardId", "direct_chat_" + userId + "_" + userId); // or use sorted key
        startActivity(intent);
    }
}