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
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String ARG_CARD_ID = "cardId";

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

    private boolean isInForeground = false;

    public static ChatFragment newInstance(String cardId, String originalOwnerId) {
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isInForeground = true;
        markChatAsRead();
    }

    @Override
    public void onPause() {
        super.onPause();
        isInForeground = false;
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
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        usersRef = database.getReference("users");
        tokensRef = database.getReference("fcmTokens");
        userChatsRef = database.getReference("user_chats");

        chatRef = database.getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages");

        setupChatListener();
        fetchUserInfo(currentUserId);
        setupFirebaseForUserChatTracking();
    }

    private void setupChatListener() {
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message == null) return;

                message.setId(snapshot.getKey());
                chatMessages.add(message);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

                String senderId = message.getSenderId();
                if (senderId != null && !senderId.equals(currentUserId)) {
                    if (!isInForeground) {
                        showUnreadNotification(message.getMessage(), senderId);
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
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        userNames.put(userId, name);
                    } else if (email != null) {
                        userNames.put(userId, email.split("@")[0]);
                    }

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        userProfilePics.put(userId, profileImageUrl);
                    }

                    chatAdapter.notifyDataSetChanged();
                }
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
                    data.put("cardId", cardId);
                    data.put("click_action", "OPEN_CHAT_ACTIVITY");

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
        userChatsRef.child(currentUserId).child(cardId).child("read").setValue(true);
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