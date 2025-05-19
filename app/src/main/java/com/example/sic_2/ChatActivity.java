package com.example.sic_2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.widget.ImageButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Add this interface to handle long-press in the adapter
interface OnMessageLongClickListener {
    void onMessageLongClick(ChatMessage chatMessage, int position);
}

public class ChatActivity extends AppCompatActivity implements OnMessageLongClickListener {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private DatabaseReference usersRef;
    private String chatId;
    private String currentUserId;
    private Map<String, String> userNames = new HashMap<>();
    private Map<String, String> userProfilePics = new HashMap<>();
    private ValueEventListener chatListener;
    private EditText messageInput; // For editing messages
    private static final int PICK_IMAGE_REQUEST = 1421;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ImageButton attachButton = findViewById(R.id.attach_button);
        attachButton.setOnClickListener(v -> pickImageFromGallery());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        chatId = getIntent().getStringExtra("cardId"); // still using cardId as chatId

        if (chatId == null || chatId.isEmpty()) {
            showToast("Chat ID is missing");
            finish();
            return;
        }

        if (currentUserId == null) {
            showToast("User not authenticated");
            finish();
            return;
        }

        chatMessages = new ArrayList<>();
        // Pass this as the OnMessageLongClickListener
        chatAdapter = new ChatAdapter(this, chatMessages, currentUserId, userNames, userProfilePics, this);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(chatId)
                .child("messages");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        markChatAsRead();

        loadChatMessages();
        setupUI();
    }

    private void setupUI() {
        Button sendButton = findViewById(R.id.send_button);
        messageInput = findViewById(R.id.message_input);

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
        if (currentUserId == null || chatId == null) {
            showToast("User authentication failed");
            return;
        }

        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

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
                    } else {
                        setUnreadForRecipients(message);
                    }
                });
    }

    private void setUnreadForRecipients(String message) {
        DatabaseReference participantsRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(chatId)
                .child("participants");

        long timestamp = System.currentTimeMillis();

        participantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    if (uid != null && !uid.equals(currentUserId)) {
                        DatabaseReference recipientChatRef = FirebaseDatabase.getInstance()
                                .getReference("user_chats")
                                .child(uid)
                                .child(chatId);

                        Map<String, Object> update = new HashMap<>();
                        update.put("read", false);
                        update.put("lastMessage", message);
                        update.put("chatId", chatId);
                        update.put("senderId", currentUserId);
                        update.put("timestamp", timestamp);

                        recipientChatRef.updateChildren(update);
                    }
                }
                DatabaseReference senderChatRef = FirebaseDatabase.getInstance()
                        .getReference("user_chats")
                        .child(currentUserId)
                        .child(chatId);

                Map<String, Object> updateSender = new HashMap<>();
                updateSender.put("read", true);
                updateSender.put("lastMessage", message);
                updateSender.put("chatId", chatId);
                updateSender.put("senderId", currentUserId);
                updateSender.put("timestamp", timestamp);
                senderChatRef.updateChildren(updateSender);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Failed to update unread status", error.toException());
            }
        });
    }

    private void markChatAsRead() {
        if (currentUserId == null || chatId == null) return;
        DatabaseReference myChatRef = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(currentUserId)
                .child(chatId);
        myChatRef.child("read").setValue(true);
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

    // Long-press message handler: show menu for edit, react, delete

    private void showEditMessageDialog(ChatMessage chatMessage) {
        if (!currentUserId.equals(chatMessage.getSenderId())) {
            showToast("You can only edit your own messages");
            return;
        }
        final EditText editText = new EditText(this);
        editText.setText(chatMessage.getMessage());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(this)
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

    private void showReactionDialog(ChatMessage chatMessage) {
        // Example reactions; you can expand this
        String[] reactions = {"👍", "❤️", "😂", "😮", "😢", "😡"};
        new AlertDialog.Builder(this)
                .setTitle("React to Message")
                .setItems(reactions, (dialog, which) -> {
                    String selectedReaction = reactions[which];
                    // Save reaction in Firebase (example: as a simple field, for advanced, store per user)
                    chatRef.child(chatMessage.getId()).child("reaction").setValue(selectedReaction)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    showToast("Reacted with " + selectedReaction);
                                } else {
                                    showToast("Failed to react");
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmAndDeleteMessage(ChatMessage chatMessage) {
        if (!currentUserId.equals(chatMessage.getSenderId())) {
            showToast("You can only delete your own messages");
            return;
        }
        new AlertDialog.Builder(this)
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

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            // Optionally show preview dialog
            showImageSendDialog(selectedImageUri);
        }
    }

    private void showImageSendDialog(Uri imageUri) {
        ImageView preview = new ImageView(this);
        Glide.with(this).load(imageUri).into(preview);
        new AlertDialog.Builder(this)
                .setTitle("Send Image?")
                .setView(preview)
                .setPositiveButton("Send", (d, w) -> uploadAndSendImageMessage(imageUri))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadAndSendImageMessage(Uri imageUri) {
        // Upload to Cloudinary or your provider, then send message with imageUrl
        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String imgUrl = (String) resultData.get("secure_url");
                        sendImageMessage(imgUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        showToast("Image upload failed: " + error.getDescription());
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void sendImageMessage(String imgUrl) {
        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        String senderName = userNames.getOrDefault(currentUserId, "You");
        String profileImageUrl = userProfilePics.get(currentUserId);

        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                senderName,
                "", // no text
                System.currentTimeMillis(),
                profileImageUrl
        );
        chatMessage.setImageUrl(imgUrl);

        chatRef.child(messageId).setValue(chatMessage)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to send image");
                    } else {
                        setUnreadForRecipients("[Image]");
                    }
                });
    }

    // --- Download image to gallery ---
    public void downloadImageToGallery(String url) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2001);
            Toast.makeText(this, "Permission required. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        String saved = MediaStore.Images.Media.insertImage(
                                getContentResolver(), resource, "ChatImage", "Downloaded from chat");
                        if (saved != null) showToast("Image saved to gallery");
                        else showToast("Failed to save image");
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    // --- Long-press menu improvement ---
    @Override
    public void onMessageLongClick(ChatMessage chatMessage, int position) {
        boolean isOwnMessage = currentUserId.equals(chatMessage.getSenderId());
        ArrayList<String> options = new ArrayList<>();
        if (isOwnMessage) options.add("Edit");
        options.add("React");
        if (chatMessage.getReaction() != null && isOwnMessage) options.add("Remove Reaction");
        if (isOwnMessage) options.add("Delete");
        if (chatMessage.getImageUrl() != null && !chatMessage.getImageUrl().isEmpty()) options.add("Download Image");

        String[] actions = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Action")
                .setItems(actions, (dialog, which) -> {
                    String selected = actions[which];
                    switch (selected) {
                        case "Edit":
                            showEditMessageDialog(chatMessage); break;
                        case "React":
                            showReactionDialog(chatMessage); break;
                        case "Remove Reaction":
                            chatRef.child(chatMessage.getId()).child("reaction").removeValue()
                                    .addOnCompleteListener(task -> showToast(task.isSuccessful() ? "Reaction removed" : "Failed"));
                            break;
                        case "Delete":
                            confirmAndDeleteMessage(chatMessage); break;
                        case "Download Image":
                            downloadImageToGallery(chatMessage.getImageUrl()); break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}