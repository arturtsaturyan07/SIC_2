package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import java.util.Objects;

/**
 * ChatFragment: Chat UI for a card, with swipe-to-reply, long-press menu for edit, react, and delete actions.
 * This version hides the top bar from activity_chat.xml when used as a fragment.
 */
public class ChatFragment extends Fragment implements OnMessageLongClickListener {

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

    // --- Swipe-to-reply state & UI ---
    private ChatMessage replyToMessage = null;
    private LinearLayout replyLayout;
    private TextView replySender;
    private TextView replyText;
    private ImageButton cancelReplyButton;

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
        // Inflate the activity_chat.xml layout (which includes the top bar)
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        // Option 1: Hide the top bar if present (when used as a fragment)
        View topBar = view.findViewById(R.id.top_bar);
        if (topBar != null) {
            topBar.setVisibility(View.GONE);
        }

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
        if (chatRecyclerView == null) {
            throw new RuntimeException("activity_chat.xml must have a RecyclerView with id 'chat_recycler_view'");
        }
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), chatMessages, currentUserId, userNames, userProfilePics, this);

        chatAdapter.setOnReactionClickListener(new ChatAdapter.OnReactionClickListener() {
            @Override
            public void onReactionClicked(ChatMessage msg, String emoji, int position) {
                toggleReaction(msg, emoji);
            }
            @Override
            public void onReactionLongClicked(ChatMessage msg, String emoji, int position) {
                showReactionUsersDialog(msg, emoji);
            }
            @Override
            public void onAddReaction(ChatMessage msg, int position, View anchor) {
                showReactionPopup(msg, position, anchor);
            }
        });

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // --- Swipe to reply ---
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatMessage message = chatMessages.get(position);
                showReply(message);
                chatAdapter.notifyItemChanged(position); // Reset swipe state
            }
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.3f;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(chatRecyclerView);

        // --- Reply Preview UI ---
        replyLayout = view.findViewById(R.id.reply_layout);
        replySender = view.findViewById(R.id.reply_sender);
        replyText = view.findViewById(R.id.reply_text);
        cancelReplyButton = view.findViewById(R.id.cancel_reply);

        replyLayout.setVisibility(View.GONE);
        cancelReplyButton.setOnClickListener(v -> clearReply());

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
                ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
                if (chatMessage == null) return;
                chatMessage.setId(snapshot.getKey());
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
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String changedId = snapshot.getKey();
                ChatMessage updatedMessage = snapshot.getValue(ChatMessage.class);
                if (changedId != null && updatedMessage != null) {
                    for (int i = 0; i < chatMessages.size(); i++) {
                        ChatMessage msg = chatMessages.get(i);
                        if (msg.getId() != null && msg.getId().equals(changedId)) {
                            updatedMessage.setId(changedId);
                            chatMessages.set(i, updatedMessage);
                            chatAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String deletedId = snapshot.getKey();
                for (int i = 0; i < chatMessages.size(); i++) {
                    ChatMessage msg = chatMessages.get(i);
                    if (msg.getId() != null && msg.getId().equals(deletedId)) {
                        chatMessages.remove(i);
                        chatAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }

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

        // Add reply info if replying
        if (replyToMessage != null) {
            chatMessage.setReplyToMessageId(replyToMessage.getId());
            chatMessage.setReplyToMessageText(replyToMessage.getMessage());
            chatMessage.setReplyToSenderName(replyToMessage.getSenderName());
            clearReply();
        }

        chatRef.child(messageId).setValue(chatMessage)
                .addOnSuccessListener(aVoid -> {
                    saveChatReference(cardId, currentUserId); // Save chat in user_chats
                    sendNotificationsToChatParticipants(message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    showToast("Failed to send message");
                });
    }

    private void saveChatReference(String chatId, String currentUserId) {
        DatabaseReference userChatRefA = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(this.currentUserId)
                .child(chatId);

        DatabaseReference userChatRefB = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(Objects.requireNonNull(userNames.getOrDefault(this.currentUserId, "User")))
                .child(chatId);

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("read", false);
        chatData.put("lastMessage", chatMessages);
        chatData.put("timestamp", System.currentTimeMillis());

        userChatRefA.updateChildren(chatData);
        userChatRefB.updateChildren(chatData);
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
        DatabaseReference chatRef = userChatsRef.child(currentUserId).child(cardId);
        chatRef.child("read").setValue(true);
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

    // ---- REACTIONS: Telegram-style, per-user, per-emoji ----

    private void toggleReaction(ChatMessage chatMessage, String emoji) {
        String uid = currentUserId;
        DatabaseReference msgRef = chatRef.child(chatMessage.getId()).child("reactions").child(emoji);

        Map<String, Map<String, Boolean>> reactions = chatMessage.getReactions();
        boolean alreadyReacted = reactions != null
                && reactions.containsKey(emoji)
                && reactions.get(emoji) != null
                && reactions.get(emoji).containsKey(uid);

        if (alreadyReacted) {
            msgRef.child(uid).removeValue();
        } else {
            msgRef.child(uid).setValue(true);
        }
    }

    private void showReactionUsersDialog(ChatMessage msg, String emoji) {
        Map<String, Map<String, Boolean>> reactions = msg.getReactions();
        if (reactions == null || !reactions.containsKey(emoji)) return;
        Map<String, Boolean> users = reactions.get(emoji);

        String[] names = users.keySet().stream()
                .map(uid -> userNames.getOrDefault(uid, uid))
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Users reacted with " + emoji)
                .setItems(names, null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showReactionPopup(ChatMessage msg, int position, View anchor) {
        ReactionPopupFragment popup = new ReactionPopupFragment();
        popup.setReactionSelectListener(emoji -> toggleReaction(msg, emoji));
        popup.show(getParentFragmentManager(), "reactions_popup");
    }

    // ---- Long-press support for message actions: Edit, React, Delete ----
    @Override
    public void onMessageLongClick(ChatMessage chatMessage, int position) {
        boolean isOwnMessage = currentUserId.equals(chatMessage.getSenderId());

        ArrayList<String> options = new ArrayList<>();
        if (isOwnMessage) options.add("Edit");
        options.add("React");
        if (isOwnMessage) options.add("Delete");

        String[] actions = options.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Action")
                .setItems(actions, (dialog, which) -> {
                    String selected = actions[which];
                    switch (selected) {
                        case "Edit":
                            showEditMessageDialog(chatMessage); break;
                        case "React":
                            showReactionPopup(chatMessage, position, null); break;
                        case "Delete":
                            confirmAndDeleteMessage(chatMessage); break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditMessageDialog(ChatMessage chatMessage) {
        if (!currentUserId.equals(chatMessage.getSenderId())) {
            showToast("You can only edit your own messages");
            return;
        }
        final EditText editText = new EditText(requireContext());
        editText.setText(chatMessage.getMessage());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Message")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = editText.getText().toString().trim();
                    if (!newText.isEmpty() && !newText.equals(chatMessage.getMessage())) {
                        chatRef.child(chatMessage.getId()).child("message").setValue(newText)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        showToast("Message updated");
                                    } else {
                                        showToast("Failed to update message");
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmAndDeleteMessage(ChatMessage chatMessage) {
        if (!currentUserId.equals(chatMessage.getSenderId())) {
            showToast("You can only delete your own messages");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete message?")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (chatMessage.getId() != null) {
                        chatRef.child(chatMessage.getId()).removeValue()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        showToast("Message deleted");
                                    } else {
                                        showToast("Failed to delete message");
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Swipe-to-reply helpers ---
    private void showReply(ChatMessage message) {
        replyToMessage = message;
        replySender.setText(message.getSenderName());
        replyText.setText(message.getMessage().isEmpty() ? "[Media]" : message.getMessage());
        replyLayout.setVisibility(View.VISIBLE);
    }

    private void clearReply() {
        replyToMessage = null;
        replyLayout.setVisibility(View.GONE);
    }
}