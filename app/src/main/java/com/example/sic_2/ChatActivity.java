package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private DatabaseReference usersRef;
    private String cardId;
    private String currentUserId;
    private Map<String, String> userNames = new HashMap<>();
    private Map<String, String> userProfilePics = new HashMap<>();
    private ValueEventListener chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase and UI components
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        cardId = getIntent().getStringExtra("cardId");

        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            finish();
            return;
        }

        if (currentUserId == null) {
            showToast("User not authenticated");
            finish();
            return;
        }

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages, currentUserId, userNames, userProfilePics);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Firebase references - ✅ Fixed path from `/chats/...` to `/cards/...`
        chatRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages");

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadChatMessages();
        setupUI();
    }

    private void setupUI() {
        Button sendButton = findViewById(R.id.send_button);
        EditText messageInput = findViewById(R.id.message_input);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            } else {
                showToast("Message cannot be empty");
            }
        });
    }

    private void loadChatMessages() {
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear();

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = messageSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null && chatMessage.getSenderId() != null) {
                        chatMessage.setId(messageSnapshot.getKey());

                        // Use sender info from message or fetch from usersRef
                        if (chatMessage.getSenderName() != null && !chatMessage.getSenderName().isEmpty()) {
                            userNames.put(chatMessage.getSenderId(), chatMessage.getSenderName());
                        }

                        if (chatMessage.getProfileImageUrl() != null && !chatMessage.getProfileImageUrl().isEmpty()) {
                            userProfilePics.put(chatMessage.getSenderId(), chatMessage.getProfileImageUrl());
                        }

                        if (!userNames.containsKey(chatMessage.getSenderId())) {
                            fetchUserDetails(chatMessage.getSenderId());
                        }

                        chatMessages.add(chatMessage);
                    }
                }

                chatAdapter.notifyDataSetChanged();
                scrollToBottom();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading chat messages: " + error.getMessage());
                showToast("Failed to load chat messages");
            }
        };
        chatRef.addValueEventListener(chatListener);
    }

    private void fetchUserDetails(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profilePicUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (name == null || name.isEmpty()) {
                        name = email != null ? email.split("@")[0] : "User";
                    }

                    userNames.put(userId, name);
                    if (profilePicUrl != null) {
                        userProfilePics.put(userId, profilePicUrl);
                    }

                    chatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to fetch user details: " + error.getMessage());
            }
        });
    }

    private void sendMessage(String message) {
        if (currentUserId == null || cardId == null) {
            showToast("User authentication failed");
            return;
        }

        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        // Get cached name and profile URL
        String senderName = userNames.getOrDefault(currentUserId, "You");
        String profileImageUrl = userProfilePics.get(currentUserId);

        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                senderName,
                message,
                System.currentTimeMillis(),
                profileImageUrl
        );

        chatRef.child(messageId).setValue(chatMessage)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FirebaseError", "Failed to send message", task.getException());
                        showToast("Failed to send message");
                    }
                });
    }

    private void scrollToBottom() {
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private void showToast(String message) {
        Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
    }
}