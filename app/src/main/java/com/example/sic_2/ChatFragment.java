package com.example.sic_2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String ARG_CARD_ID = "cardId";
    private static final String ARG_OWNER_ID = "originalOwnerId";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private DatabaseReference usersRef;
    private DatabaseReference tokensRef;
    private DatabaseReference userChatsRef;
    private String cardId;
    private String currentUserId;
    private ChildEventListener chatListener;

    private Map<String, String> userNames = new HashMap<>();
    private Map<String, String> userProfilePics = new HashMap<>();

    private boolean isCurrentCardActive = false;
    private boolean isInForeground = false; // Tracks if user is in this chat

    public static ChatFragment newInstance(String cardId, String originalOwnerId) {
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId);
        args.putString(ARG_OWNER_ID, originalOwnerId);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isInForeground = true;
        isCurrentCardActive = true;
        markChatAsRead();
    }

    @Override
    public void onPause() {
        super.onPause();
        isInForeground = false;
        isCurrentCardActive = false;
    }

    private void setupViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatMessages = new ArrayList<>();

        // ✅ Pass context here to avoid errors
        chatAdapter = new ChatAdapter(requireContext(), chatMessages, currentUserId, userNames, userProfilePics);

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
    }

    private void setupChatListener() {
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Object rawValue = snapshot.getValue();

                if (!(rawValue instanceof Map)) {
                    Log.e(TAG, "Unexpected data type: " + rawValue.getClass());
                    return;
                }

                Map<String, Object> messageMap = (Map<String, Object>) rawValue;

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(snapshot.getKey());

                chatMessage.setSenderId((String) messageMap.get("senderId"));
                chatMessage.setMessage((String) messageMap.get("message"));

                Object timestampObj = messageMap.get("timestamp");
                if (timestampObj instanceof Long) {
                    chatMessage.setTimestamp((Long) timestampObj);
                } else {
                    chatMessage.setTimestamp(System.currentTimeMillis()); // fallback
                }

                chatMessage.setSenderName((String) messageMap.get("senderName"));
                chatMessage.setProfileImageUrl((String) messageMap.get("profileImageUrl"));

                // Safely parse delivered/read maps
                chatMessage.parseDelivered(messageMap.get("delivered"));
                chatMessage.parseRead(messageMap.get("read"));

                chatMessages.add(chatMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

                String senderId = chatMessage.getSenderId();
                if (senderId != null && !senderId.equals(currentUserId)) {
                    if (!isInForeground) {
                        showUnreadNotification(chatMessage.getMessage(), senderId);
                    }
                    if (!userNames.containsKey(senderId)) {
                        fetchUserInfo(senderId);
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
        DatabaseReference cardRef = FirebaseDatabase.getInstance().getReference("cards").child(cardId);
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
                    String title = userNames.getOrDefault(currentUserId, "Someone");
                    String body = message.length() > 50 ? message.substring(0, 50) + "..." : message;

                    Map<String, String> data = new HashMap<>();
                    data.put("title", title);
                    data.put("body", body);
                    data.put("cardId", cardId); // Important: pass card ID
                    data.put("click_action", "OPEN_CHAT_ACTIVITY"); // Optional custom action

                    FCMNotificationSender.sendNotification(token, data);
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
        DatabaseReference chatRef = userChatsRef.child(currentUserId).child(cardId);
        chatRef.child("read").setValue(true);
    }

    private void updateMessageStatus(String cardId, String messageId, String userId, String statusType, boolean value) {
        DatabaseReference msgRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages")
                .child(messageId);

        Map<String, Object> update = new HashMap<>();
        update.put(statusType + "." + userId, value);
        msgRef.updateChildren(update);
    }

    private void showUnreadNotification(String message, String senderId) {
        DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String senderName = snapshot.child("name").getValue(String.class);
                if (senderName == null) senderName = "User";

                String title = senderName;
                String body = message.length() > 50 ? message.substring(0, 50) + "..." : message;
                NotificationHelper.showUnreadChatNotification(requireContext(), title, body, cardId.hashCode());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupFirebaseForUserChatTracking() {
        if (currentUserId == null || cardId == null) return;
        DatabaseReference userChatRef = userChatsRef.child(currentUserId).child(cardId);
        userChatRef.child("read").onDisconnect().removeValue(); // Optional cleanup
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
    }
}