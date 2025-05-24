package com.example.sic_2;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

interface OnMessageLongClickListener {
    void onMessageLongClick(ChatMessage chatMessage, int position);
}

public class ChatActivity extends AppCompatActivity implements OnMessageLongClickListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2002;
    private static final int PICK_IMAGE_REQUEST = 1421;
    private static final int REQUEST_CIRCLE_VIDEO_CAPTURE = 1999;
    private static final int REQUEST_CAMERA_PERMISSION = 2003;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
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
    private EditText messageInput;
    private Uri selectedImageUri;
    private Uri capturedCircleVideoUri;

    // Animation
    private LinearLayout voiceRecordingLayout;
    private ImageView voiceMicIcon;
    private TextView voiceRecordingText;
    private ObjectAnimator micAnimator;
    private ValueAnimator dotsAnimator;
    private boolean isMicAnimating = false;

    // Audio playback
    private MediaPlayer mediaPlayer = null;
    private int currentlyPlayingPosition = -1;

    // Reply
    private ChatMessage replyToMessage = null;
    private LinearLayout replyLayout;
    private TextView replySender;
    private TextView replyText;
    private ImageButton cancelReplyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        voiceRecordingLayout = findViewById(R.id.voice_recording_layout);
        voiceMicIcon = findViewById(R.id.voice_mic_icon);
        voiceRecordingText = findViewById(R.id.voice_recording_text);

        replyLayout = findViewById(R.id.reply_layout);
        replySender = findViewById(R.id.reply_sender);
        replyText = findViewById(R.id.reply_text);
        cancelReplyButton = findViewById(R.id.cancel_reply);

        replyLayout.setVisibility(View.GONE);
        cancelReplyButton.setOnClickListener(v -> clearReply());

        voiceRecordingLayout.setVisibility(View.GONE);

        ImageButton attachButton = findViewById(R.id.attach_button);
        attachButton.setOnClickListener(v -> pickImageFromGallery());

        ImageButton voiceButton = findViewById(R.id.voice_button);
        voiceButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecordingAndSend();
                    break;
            }
            return true;
        });

        ImageButton circleVideoButton = findViewById(R.id.circle_video_button);
        circleVideoButton.setOnClickListener(v -> checkCameraPermissionAndStartVideo());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        chatId = getIntent().getStringExtra("cardId");

        if (chatId == null || chatId.isEmpty()) {
            showToast("Chat ID is missing");
            finish();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        if (currentUserId == null) {
            showToast("User not authenticated");
            finish();
            return;
        }

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages, currentUserId, userNames, userProfilePics, this);

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

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // --- SWIPE TO REPLY ---
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

        // Add reply info if replying
        if (replyToMessage != null) {
            chatMessage.setReplyToMessageId(replyToMessage.getId());
            chatMessage.setReplyToMessageText(replyToMessage.getMessage());
            chatMessage.setReplyToSenderName(replyToMessage.getSenderName());
            clearReply();
        }

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
        stopMicAnimation();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ---- SWIPE REPLY UI ----
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

        new AlertDialog.Builder(this)
                .setTitle("Users reacted with " + emoji)
                .setItems(names, null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showReactionPopup(ChatMessage msg, int position, View anchor) {
        ReactionPopupFragment popup = new ReactionPopupFragment();
        popup.setReactionSelectListener(emoji -> toggleReaction(msg, emoji));
        popup.show(getSupportFragmentManager(), "reactions_popup");
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void checkCameraPermissionAndStartVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCircleVideoRecording();
        }
    }

    private void startCircleVideoRecording() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 20); // Limit to 20s
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_CIRCLE_VIDEO_CAPTURE);
        } else {
            showToast("No camera app available.");
        }
    }

    private void uploadAndSendCircleVideo(Uri videoUri) {
        MediaManager.get().upload(videoUri)
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String videoUrl = (String) resultData.get("secure_url");
                        sendCircleVideoMessage(videoUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        showToast("Video upload failed: " + error.getDescription());
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void sendCircleVideoMessage(String videoUrl) {
        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        String senderName = userNames.getOrDefault(currentUserId, "You");
        String profileImageUrl = userProfilePics.get(currentUserId);

        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                senderName,
                "", // No text
                System.currentTimeMillis(),
                profileImageUrl
        );
        chatMessage.setCircleVideoUrl(videoUrl);

        chatRef.child(messageId).setValue(chatMessage)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to send circle video");
                    } else {
                        setUnreadForRecipients("[Circle Video]");
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCircleVideoRecording();
            } else {
                showToast("Camera permission is required to record video");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            showImageSendDialog(selectedImageUri);
        } else if (requestCode == REQUEST_CIRCLE_VIDEO_CAPTURE && resultCode == RESULT_OK && data != null) {
            capturedCircleVideoUri = data.getData();
            uploadAndSendCircleVideo(capturedCircleVideoUri);
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

    @Override
    public void onMessageLongClick(ChatMessage chatMessage, int position) {
        boolean isOwnMessage = currentUserId.equals(chatMessage.getSenderId());
        ArrayList<String> options = new ArrayList<>();
        if (isOwnMessage) options.add("Edit");
        options.add("React");
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
                            showReactionPopup(chatMessage, position, null); break;
                        case "Delete":
                            confirmAndDeleteMessage(chatMessage); break;
                        case "Download Image":
                            downloadImageToGallery(chatMessage.getImageUrl()); break;
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

    private void startMicAnimation() {
        if (isMicAnimating) return;
        isMicAnimating = true;
        voiceRecordingLayout.setVisibility(View.VISIBLE);

        micAnimator = ObjectAnimator.ofFloat(voiceMicIcon, "scaleX", 1f, 1.4f, 1f);
        micAnimator.setDuration(900);
        micAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micAnimator.setRepeatMode(ValueAnimator.RESTART);
        micAnimator.setInterpolator(new LinearInterpolator());
        micAnimator.setPropertyName("scaleY");
        micAnimator.start();

        dotsAnimator = ValueAnimator.ofInt(0, 3);
        dotsAnimator.setDuration(900);
        dotsAnimator.setRepeatCount(ValueAnimator.INFINITE);
        dotsAnimator.addUpdateListener(animation -> {
            int dots = (int) animation.getAnimatedValue();
            StringBuilder sb = new StringBuilder("Recording");
            for (int i = 0; i < dots; i++) sb.append(".");
            voiceRecordingText.setText(sb.toString());
        });
        dotsAnimator.start();
    }

    private void stopMicAnimation() {
        isMicAnimating = false;
        if (micAnimator != null) micAnimator.cancel();
        if (dotsAnimator != null) dotsAnimator.cancel();
        if (voiceRecordingLayout != null)
            voiceRecordingLayout.setVisibility(View.GONE);
    }

    private void startRecording() {
        try {
            audioFilePath = getExternalCacheDir().getAbsolutePath() + "/voice_message_" + System.currentTimeMillis() + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            showToast("Recording...");
            isRecording = true;
            startMicAnimation();
        } catch (Exception e) {
            showToast("Recording failed: " + e.getMessage());
            e.printStackTrace();
            stopMicAnimation();
        }
    }

    private void stopRecordingAndSend() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                stopMicAnimation();
                File audioFile = new File(audioFilePath);
                if (audioFile.exists() && audioFile.length() > 0) {
                    uploadAndSendAudioMessage(audioFilePath);
                } else {
                    showToast("Recording was too short or failed.");
                }
            } catch (Exception e) {
                showToast("Error stopping recording: " + e.getMessage());
                stopMicAnimation();
            }
        } else {
            stopMicAnimation();
        }
    }

    private void uploadAndSendAudioMessage(String audioPath) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            showToast("Audio file missing or empty.");
            return;
        }
        Uri audioUri = Uri.fromFile(audioFile);
        MediaManager.get().upload(audioUri)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String audioUrl = (String) resultData.get("secure_url");
                        sendAudioMessage(audioUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        showToast("Audio upload failed: " + error.getDescription());
                        Log.e("Cloudinary", "Audio upload error: " + error.getDescription());
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void sendAudioMessage(String audioUrl) {
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
        chatMessage.setAudioUrl(audioUrl);

        chatRef.child(messageId).setValue(chatMessage)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to send audio");
                    } else {
                        setUnreadForRecipients("[Voice Message]");
                    }
                });
    }

    public void playAudio(String url, ImageButton playButton, int position, TextView durationText) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                currentlyPlayingPosition = -1;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                playButton.setImageResource(R.drawable.ic_pause);
                int duration = mp.getDuration() / 1000;
                durationText.setText(String.format(Locale.getDefault(), "%d:%02d", duration / 60, duration % 60));
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                playButton.setImageResource(R.drawable.ic_play);
                currentlyPlayingPosition = -1;
                mediaPlayer.release();
                mediaPlayer = null;
            });
            mediaPlayer.prepareAsync();
            currentlyPlayingPosition = position;
        } catch (IOException e) {
            showToast("Audio playback error");
            e.printStackTrace();
        }
    }
}