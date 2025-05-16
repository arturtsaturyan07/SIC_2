package com.example.sic_2;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;

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
                int unreadCount = 0;
                String lastUnreadCardId = null;
                String lastMessage = null;
                String senderId = null;
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = chatSnapshot.child("read").getValue(Boolean.class);

                    // Defensive type check for lastMessage
                    Object msgObj = chatSnapshot.child("lastMessage").getValue();
                    String msg = null;
                    if (msgObj instanceof String) {
                        msg = (String) msgObj;
                    } else if (msgObj instanceof List) {
                        List<?> list = (List<?>) msgObj;
                        if (!list.isEmpty()) {
                            Object last = list.get(list.size() - 1);
                            if (last instanceof String) {
                                msg = (String) last;
                            }
                        }
                    }

                    String cardId = chatSnapshot.child("cardId").getValue(String.class);
                    String sId = chatSnapshot.child("senderId").getValue(String.class);

                    if (isRead != null && !isRead && msg != null && !msg.isEmpty()) {
                        unreadCount++;
                        lastUnreadCardId = cardId;
                        lastMessage = msg;
                        senderId = sId;
                    }
                }
                if (unreadCount > 0 && lastUnreadCardId != null && senderId != null) {
                    showChatNotification(lastUnreadCardId, senderId, lastMessage, unreadCount);
                } else {
                    // Cancel notification if all read
                    NotificationManagerCompat.from(getApplicationContext()).cancel(1001);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HiWorker", "Failed to check unread messages", error.toException());
            }
        });
    }

    private void showChatNotification(String cardId, String senderId, String message, int unreadCount) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(senderId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String senderName = snapshot.getValue(String.class);
                if (senderName == null) senderName = "Someone";

                Context context = getApplicationContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("cardId", cardId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1001,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat_notification)
                        .setContentTitle("Unread messages: " + unreadCount)
                        .setContentText("Latest from " + senderName + ": " + message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(1001, builder.build());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HiWorker", "Failed to get sender name", error.toException());
            }
        });
    }
}