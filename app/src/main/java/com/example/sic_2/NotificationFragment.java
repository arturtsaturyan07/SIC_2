package com.example.sic_2;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private LinearLayout notificationContainer;
    private DatabaseReference notificationsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        notificationContainer = view.findViewById(R.id.notification_container);

        // Initialize Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);

        // Load notifications
        loadNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        clearNotificationsFromBar(); // Clear notifications from the notification bar
    }

    private void loadNotifications() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);

        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CustomNotification> notificationList = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    CustomNotification notification = data.getValue(CustomNotification.class);
                    if (notification != null && !notification.isRead()) {
                        notificationList.add(notification);
                    }
                }
                updateNotificationUI(notificationList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationFragment", "Error loading notifications", error.toException());
            }
        });
    }

    private void updateNotificationUI(List<CustomNotification> notificationList) {
        notificationContainer.removeAllViews();

        if (notificationList.isEmpty()) {
            // Show a "No new notifications" message
            TextView noNotificationsText = new TextView(requireContext());
            noNotificationsText.setText("No new notifications");
            noNotificationsText.setTextSize(18);
            noNotificationsText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            noNotificationsText.setGravity(Gravity.CENTER);
            notificationContainer.addView(noNotificationsText);
        } else {
            // Display the notifications
            for (CustomNotification notification : notificationList) {
                View notificationView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.notification_item, notificationContainer, false);
                TextView messageText = notificationView.findViewById(R.id.notification_message);
                TextView cardNameText = notificationView.findViewById(R.id.notification_card_name);

                messageText.setText(notification.getMessage());
                cardNameText.setText("Card: " + notification.getCardName());

                // Mark as read when clicked
                notificationView.setOnClickListener(v -> {
                    markNotificationAsRead(notification.getId());
                    openChat(notification.getCardId());
                });

                notificationContainer.addView(notificationView);
            }
        }
    }

    private void markNotificationAsRead(String notificationId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .child(notificationId);

        notificationRef.child("isRead").setValue(true);
    }

    private void openChat(String cardId) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("cardId", cardId);
        startActivity(intent);
    }

    private void clearNotificationsFromBar() {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll(); // Clear all notifications
    }
}