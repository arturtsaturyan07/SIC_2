package com.example.sic_2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private Map<String, Notification> activeNotifications = new HashMap<>();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            String cardId = data.get("cardId");
            String cardName = data.get("cardName");
            String senderName = data.get("senderName");
            String message = data.get("body");
            CustomNotification customNotification = new CustomNotification(cardId, cardName, message, senderName);
            showNotification(customNotification);
        }
    }

    private void showNotification(CustomNotification customNotification) {

        String cardId = customNotification.cardId;

        if (activeNotifications.containsKey(cardId)) {
            Log.d("MyFirebaseMessagingService", "Notification for cardId " + cardId + " already exists.");
            return; // Do not send a new notification
        }

        if(customNotification.message == null){
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_chat", true);
        intent.putExtra("cardId", cardId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "chat_notification_channel")
                .setSmallIcon(R.drawable.baseline_chat_24)
                .setContentTitle(customNotification.cardName)
                .setContentText(customNotification.senderName+": "+customNotification.message)
                .setAutoCancel(true).setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = "Chat Notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel("chat_notification_channel", name, importance);
            channel.setDescription("Channel for chat notifications");
            // Register the channel with the system
            manager.createNotificationChannel(channel);
        }
        Notification notification = builder.build();
        activeNotifications.put(cardId, notification);
        manager.notify(cardId.hashCode(), notification);

    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);
        // Upload token to Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            FirebaseDatabase.getInstance().getReference("fcmTokens").child(userId).setValue(token);
        }
    }
}