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
import com.google.firebase.auth.FirebaseUser;
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
    private DatabaseReference tokensRef;
    private DatabaseReference userChatsRef;
    private String cardId;
    private String currentUserId;
    private ChildEventListener chatListener;
    private Map<String, String> userNames = new HashMap<>();
    private Map<String, String> userProfilePics = new HashMap<>();

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            storeUserInfo(currentUser);
        } else {
            showToast("Please sign in first");
            requireActivity().finish();
        }
    }

    private void storeUserInfo(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        // Check if name already exists
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChild("name")) {
                    String displayName = user.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        userRef.child("name").setValue(displayName);
                    } else {
                        String emailPrefix = user.getEmail() != null ?
                                user.getEmail().split("@")[0] : "User";
                        userRef.child("name").setValue(emailPrefix);
                    }
                }

                if (!snapshot.hasChild("profileImageUrl") && user.getPhotoUrl() != null) {
                    userRef.child("profileImageUrl").setValue(user.getPhotoUrl().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check user info", error.toException());
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        markChatAsRead();
    }

    private void setupViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, currentUserId, userNames, userProfilePics);
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
        tokensRef = FirebaseDatabase.getInstance().getReference("fcmTokens");
        userChatsRef = FirebaseDatabase.getInstance().getReference("user_chats");
        chatRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages");

        setupChatListener();
        fetchUserInfo(currentUserId);
        markChatAsRead();
    }

    private void setupChatListener() {
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setId(snapshot.getKey());
                    chatMessages.add(message);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

                    if (!userNames.containsKey(message.getSenderId())) {
                        fetchUserInfo(message.getSenderId());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chat listener cancelled: " + error.getMessage());
                showToast("Failed to load messages");
            }
        };

        chatRef.addChildEventListener(chatListener);
    }

    private void fetchUserInfo(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                if (name != null && !name.isEmpty()) {
                    userNames.put(userId, name);
                } else {
                    String email = snapshot.child("email").getValue(String.class);
                    if (email != null) {
                        userNames.put(userId, email.split("@")[0]);
                    }
                }

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    userProfilePics.put(userId, profileImageUrl);
                }

                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch user info", error.toException());
            }
        });
    }

    private void sendMessage(String message) {
        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        String senderName = userNames.getOrDefault(currentUserId, "You");
        String profileImageUrl = userProfilePics.getOrDefault(currentUserId, "");

        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                senderName,
                message,
                System.currentTimeMillis(),
                profileImageUrl
        );

        chatRef.child(messageId).setValue(chatMessage)
                .addOnSuccessListener(aVoid -> sendNotificationsToChatParticipants(message))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    showToast("Failed to send message");
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
                        sendNotificationToUser(userId, message);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat participants", error.toException());
            }
        });
    }

    private void sendNotificationToUser(String userId, String message) {
        tokensRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String token = snapshot.getValue(String.class);
                if (token != null) {
                    String title = "New message in chat";
                    String senderName = userNames.getOrDefault(currentUserId, "Someone");
                    String body = senderName + ": " + message;

                    Map<String, String> notificationData = new HashMap<>();
                    notificationData.put("title", title);
                    notificationData.put("body", body);
                    notificationData.put("cardId", cardId);
                    notificationData.put("senderId", currentUserId);
                    notificationData.put("senderName", senderName);

                    FCMNotificationSender.sendNotification(token, notificationData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get FCM token for user: " + userId, error.toException());
            }
        });
    }

    private void markChatAsRead() {
        if (cardId == null || currentUserId == null) return;
        userChatsRef.child(currentUserId).child(cardId).child("read").setValue(true);
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