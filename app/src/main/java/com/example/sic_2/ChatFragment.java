package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String ARG_CARD_ID = "cardId";
    private static final String ARG_OWNER_ID = "originalOwnerId";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private DatabaseReference usersRef;
    private String cardId;
    private String currentUserId;
    private ChildEventListener chatListener;
    private Map<String, String> userNames = new HashMap<>(); // Cache for user names

    public static ChatFragment newInstance(String cardId, String originalOwnerId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId);
        args.putString(ARG_OWNER_ID, originalOwnerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
            String originalOwnerId = getArguments().getString(ARG_OWNER_ID);
            Log.d(TAG, "Card ID: " + cardId + ", Owner ID: " + originalOwnerId);
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            showToast("Please sign in first");
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    private void setupViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, currentUserId, userNames);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        EditText messageInput = view.findViewById(R.id.message_input);
        Button sendButton = view.findViewById(R.id.send_button);

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

    private void setupFirebase() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages");

        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setId(snapshot.getKey());
                    chatMessages.add(message);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

                    // Fetch sender name if not already cached
                    if (!userNames.containsKey(message.getSenderId())) {
                        fetchUserName(message.getSenderId());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle message updates if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle message removal if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle message reordering if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chat listener cancelled: " + error.getMessage());
                showToast("Failed to load messages");
            }
        };

        chatRef.addChildEventListener(chatListener);

        // Pre-load current user's name
        fetchUserName(currentUserId);
    }

    private void fetchUserName(String userId) {
        usersRef.child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    userNames.put(userId, name);
                    chatAdapter.notifyDataSetChanged();
                } else {
                    // Fallback to email if name not available
                    usersRef.child(userId).child("email").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                            String email = emailSnapshot.getValue(String.class);
                            if (email != null) {
                                userNames.put(userId, email.split("@")[0]);
                                chatAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to fetch user email", error.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch user name", error.toException());
            }
        });
    }

    private void sendMessage(String message) {
        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        ChatMessage chatMessage = new ChatMessage(currentUserId, message, System.currentTimeMillis());

        chatRef.child(messageId).setValue(chatMessage)
                .addOnSuccessListener(aVoid -> {
                    sendNotificationsToChatParticipants(message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                });
    }

    private void sendNotificationsToChatParticipants(String message) {
        DatabaseReference cardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId);

        cardRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null && !userId.equals(currentUserId)) {
                        NotificationFragment.sendNotification(
                                requireContext(),
                                cardId,
                                "New message in chat",
                                userNames.containsKey(currentUserId)
                                        ? userNames.get(currentUserId) + ": " + message
                                        : "Someone: " + message,
                                currentUserId
                        );
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat participants", error.toException());
            }
        });
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
    }
}