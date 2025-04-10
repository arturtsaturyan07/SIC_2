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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        notificationContainer = view.findViewById(R.id.notification_container);
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);

        loadNotifications();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        clearNotificationsFromBar();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for new chat messages");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void loadNotifications() {
        notificationsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CustomNotification> notifications = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    CustomNotification notification = data.getValue(CustomNotification.class);
                    if (notification != null && !notification.isRead()) {
                        notification.setId(data.getKey());
                        notifications.add(notification);
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
        notificationContainer.removeAllViews();

        if (notifications.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText("No new notifications");
            emptyView.setTextSize(18);
            emptyView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            emptyView.setGravity(Gravity.CENTER);
            notificationContainer.addView(emptyView);
        } else {
            for (CustomNotification notification : notifications) {
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
            }
        }
    }

    private String formatTime(long timestamp) {
        // Implement your time formatting logic here
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    private void markNotificationAsRead(String notificationId) {
        notificationsRef.child(notificationId).child("isRead").setValue(true);
    }

    private void openChatActivity(String cardId) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("cardId", cardId);
        startActivity(intent);
    }

    private void clearNotificationsFromBar() {
        notificationManager.cancelAll();
    }

    // Call this method from your ChatFragment when a new message is sent
    public static void sendNotification(Context context, String cardId, String cardName, String message, String senderId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId.equals(senderId)) return; // Don't notify yourself

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(currentUserId);

        String notificationId = notificationsRef.push().getKey();
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
                });
    }

    private static void showSystemNotification(Context context, String cardName, String message, String cardId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                .setContentTitle("New message in " + cardName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("cardId", cardId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}