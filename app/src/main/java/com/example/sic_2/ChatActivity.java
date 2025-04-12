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

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, currentUserId, userNames);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Firebase references
        chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(cardId)
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
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = messageSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        chatMessage.setId(messageSnapshot.getKey());
                        chatMessages.add(chatMessage);

                        // Fetch sender name if not already cached
                        if (!userNames.containsKey(chatMessage.getSenderId())) {
                            fetchUserName(chatMessage.getSenderId());
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (!chatMessages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading chat messages: " + error.getMessage());
                showToast("Failed to load chat messages");
            }
        });
    }

    private void fetchUserName(String userId) {
        usersRef.child(userId).child("name").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            userNames.put(userId, name);
                            chatAdapter.notifyDataSetChanged();
                        } else {
                            // Fallback to email if name not available
                            fetchUserEmail(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Failed to fetch user name: " + error.getMessage());
                    }
                }
        );
    }

    private void fetchUserEmail(String userId) {
        usersRef.child(userId).child("email").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String email = snapshot.getValue(String.class);
                        if (email != null) {
                            userNames.put(userId, email.split("@")[0]);
                            chatAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Failed to fetch user email: " + error.getMessage());
                    }
                }
        );
    }

    private void sendMessage(String message) {
        if (currentUserId == null || cardId == null) {
            showToast("User authentication failed");
            return;
        }

        String messageId = chatRef.push().getKey();
        if (messageId != null) {
            ChatMessage chatMessage = new ChatMessage(
                    currentUserId,
                    message,
                    System.currentTimeMillis()
            );

            chatRef.child(messageId).setValue(chatMessage)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e("FirebaseError", "Failed to send message", task.getException());
                            showToast("Failed to send message");
                        }
                    });
        }
    }

    private void showToast(String message) {
        Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners if needed
    }
}