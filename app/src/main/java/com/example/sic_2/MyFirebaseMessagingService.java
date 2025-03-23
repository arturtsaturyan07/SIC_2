package com.example.sic_2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();

            // Display the notification
            sendNotification(title, message);
        }

        // Check if the message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            // Handle data payload (if needed)
            String senderId = remoteMessage.getData().get("senderId");
            String message = remoteMessage.getData().get("message");

            // Save the notification to Firebase (optional)
            saveNotificationToFirebase(senderId, message);
        }
    }

    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class); // Open MainActivity when clicked
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel"; // Notification channel ID
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.baseline_circle_notifications_24) // Set your notification icon
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }

    private void saveNotificationToFirebase(String senderId, String message) {
        // Save the notification to Firebase Realtime Database
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);

        String notificationId = notificationsRef.push().getKey();
        if (notificationId != null) {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("message", message);
            notificationData.put("senderId", senderId);
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("isRead", false);

            notificationsRef.child(notificationId).setValue(notificationData);
        }
    }
}