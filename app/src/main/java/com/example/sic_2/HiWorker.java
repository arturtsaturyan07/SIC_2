package com.example.sic_2;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HiWorker extends Worker {
    private static final String CHANNEL_ID = "chat_notifications";
    private DatabaseReference userChatsRef;
    private String currentUserId;

    public HiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (currentUserId == null) {
            Log.w("HiWorker", "User not authenticated. Skipping notification check.");
            return Result.success();
        }

        createNotificationChannel();
        checkUnreadMessages();
        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void checkUnreadMessages() {
        userChatsRef = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(currentUserId);

        userChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = chatSnapshot.child("read").getValue(Boolean.class);
                    Object messageObj = chatSnapshot.child("lastMessage").getValue();
                    String lastMessage = "";

                    if (messageObj instanceof String) {
                        lastMessage = (String) messageObj;
                    } else if (messageObj instanceof ArrayList<?>) {
                        ArrayList<?> list = (ArrayList<?>) messageObj;
                        if (!list.isEmpty() && list.get(0) instanceof String) {
                            lastMessage = (String) list.get(list.size() - 1); // Get latest message
                        }
                    }

                    String cardId = chatSnapshot.child("cardId").getValue(String.class);
                    String senderId = chatSnapshot.child("senderId").getValue(String.class);

                    // 🔒 Validate senderId before proceeding
                    if (isNonEmptyString(senderId)) {
                        if (isRead != null && !isRead && !lastMessage.isEmpty()) {
                            showChatNotification(cardId, senderId, lastMessage);
                            markAsRead(chatSnapshot.getKey());
                        }
                    } else {
                        Log.e("HiWorker", "Invalid or missing senderId");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HiWorker", "Failed to check unread messages", error.toException());
            }
        });
    }

    private void showChatNotification(String cardId, String senderId, String message) {
        if (!isNonEmptyString(senderId)) {
            Log.e("HiWorker", "Cannot show notification: Invalid sender ID");
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(senderId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String senderName = snapshot.getValue(String.class);
                if (senderName == null) senderName = "Someone";

                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("New message from " + senderName)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HiWorker", "Failed to get sender name", error.toException());
            }
        });
    }

    private void markAsRead(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            Log.e("HiWorker", "Cannot mark as read: Invalid chat ID");
            return;
        }
        userChatsRef.child(chatId).child("read").setValue(true);
    }

    // 🔒 Helper method to safely validate strings
    private boolean isNonEmptyString(String str) {
        return str != null && !str.trim().isEmpty();
    }
}