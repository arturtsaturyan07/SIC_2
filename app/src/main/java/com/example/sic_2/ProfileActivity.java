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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private String userId;
    private String currentUserId;
    private DatabaseReference userRef;
    private DatabaseReference directChatsRef;

    private TextView nameText, bioText;
    private ImageView profileImage;
    private Button directChatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // UI Components
        nameText = findViewById(R.id.profile_name);
        bioText = findViewById(R.id.profile_bio);
        profileImage = findViewById(R.id.profile_image);
        directChatButton = findViewById(R.id.direct_chat_button);

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null || userId.isEmpty() || currentUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase references
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        directChatsRef = FirebaseDatabase.getInstance().getReference("direct_chats");

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
                    String bio = snapshot.child("about").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    nameText.setText(name != null ? name : "Not Found");
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
        if (userId.equals(currentUserId)) {
            Toast.makeText(this, "You can't chat with yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = "direct_chat_" + sortUids(currentUserId, userId);

        // First, create the direct chat in Firebase if it doesn't exist
        DatabaseReference chatRef = directChatsRef.child(chatId);
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Create chat with both participants
                    Map<String, Object> chatData = new HashMap<>();
                    Map<String, Boolean> participants = new HashMap<>();
                    participants.put(currentUserId, true);
                    participants.put(userId, true);
                    chatData.put("participants", participants);
                    chatData.put("lastMessage", "");
                    chatData.put("timestamp", System.currentTimeMillis());
                    chatRef.setValue(chatData)
                            .addOnSuccessListener(aVoid -> openChat(chatId))
                            .addOnFailureListener(e -> {
                                Toast.makeText(ProfileActivity.this, "Failed to start chat", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    openChat(chatId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChat(String chatId) {
        Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
        intent.putExtra("cardId", chatId);
        startActivity(intent);
    }

    private String sortUids(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}