package com.example.sic_2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private static final String CHANNEL_ID = "chat_notifications";
    private LinearLayout notificationContainer;
    private DatabaseReference notificationsRef;
    private NotificationManager notificationManager;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        notificationContainer = view.findViewById(R.id.notification_container);

        try {
            notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUid();
                notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);
                loadNotifications();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fragment", e);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        clearNotificationsFromBar();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Chat Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notifications for new chat messages");
                notificationManager.createNotificationChannel(channel);
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    private void loadNotifications() {
        if (notificationsRef == null) return;

        notificationsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                List<CustomNotification> notifications = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        CustomNotification notification = data.getValue(CustomNotification.class);
                        if (notification != null && !notification.isRead()) {
                            notification.setId(data.getKey());
                            notifications.add(notification);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notification", e);
                    }
                }
                updateNotificationUI(notifications);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading notifications", error.toException());
            }
        });
    }

    private void updateNotificationUI(List<CustomNotification> notifications) {
        if (!isAdded() || notificationContainer == null) return;

        notificationContainer.removeAllViews();

        if (notifications.isEmpty()) {
            try {
                TextView emptyView = new TextView(requireContext());
                emptyView.setText("No new notifications");
                emptyView.setTextSize(18);
                emptyView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                emptyView.setGravity(Gravity.CENTER);
                notificationContainer.addView(emptyView);
            } catch (Exception e) {
                Log.e(TAG, "Error creating empty view", e);
            }
        } else {
            for (CustomNotification notification : notifications) {
                try {
                    View notificationView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.notification_item, notificationContainer, false);

                    TextView messageView = notificationView.findViewById(R.id.notification_message);
                    TextView cardView = notificationView.findViewById(R.id.notification_card_name);
                    TextView timeView = notificationView.findViewById(R.id.notification_time);

                    messageView.setText(notification.getMessage());
                    cardView.setText("From: " + notification.getCardName());
                    timeView.setText(formatTime(notification.getTimestamp()));

                    notificationView.setOnClickListener(v -> {
                        markNotificationAsRead(notification.getId());
                        openChatActivity(notification.getCardId());
                    });

                    notificationContainer.addView(notificationView);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating notification view", e);
                }
            }
        }
    }

    private String formatTime(long timestamp) {
        try {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time", e);
            return "";
        }
    }

    private void markNotificationAsRead(String notificationId) {
        if (notificationId == null || notificationsRef == null) return;

        notificationsRef.child(notificationId).child("isRead").setValue(true)
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read", e));
    }

    private void openChatActivity(String cardId) {
        try {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening chat activity", e);
        }
    }

    private void clearNotificationsFromBar() {
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    public static void sendNotification(Context context, String cardId, String cardName, String message, String senderId) {
        if (context == null || cardId == null || cardName == null || message == null || senderId == null) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || auth.getCurrentUser().getUid().equals(senderId)) {
            return; // Don't notify yourself
        }

        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(currentUserId);

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        CustomNotification notification = new CustomNotification(
                notificationId,
                cardId,
                cardName,
                "New message in " + cardName + ": " + message,
                System.currentTimeMillis(),
                false
        );

        notificationsRef.child(notificationId).setValue(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSystemNotification(context, cardName, message, cardId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error sending notification", e));
    }

    private static void showSystemNotification(Context context, String cardName, String message, String cardId) {
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                    .setContentTitle("New message in " + cardName)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("cardId", cardId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    flags
            );
            builder.setContentIntent(pendingIntent);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing system notification", e);
        }
    }
}