package com.example.sic_2;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.Map;

public class FCMNotificationSender {

    private static final String TAG = "FCMNotificationSender";

    /**
     * Sends a notification to a specific FCM token.
     *
     * @param token The target user's FCM token
     * @param data  The custom key-value data payload
     */
    public static void sendNotification(String token, Map<String, String> data) {
        // This doesn't actually send a notification from client side
        // It only builds the message object. Real sending must be done from server or cloud function.

        Log.d(TAG, "Built notification for token: " + token);
        Log.d(TAG, "Data payload: " + data.toString());

        // For real sending, use Firebase Cloud Functions or your backend
    }

    /**
     * Gets the current user's FCM token (to store in Firebase).
     */
    public static void getFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "Current FCM Token: " + token);

                    // Save this token in Firebase under /fcmTokens/{userId}
                    saveTokenToFirebase(token);
                });
    }

    /**
     * Save FCM token to Firebase under current user
     */
    private static void saveTokenToFirebase(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null || token == null) return;

        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("fcmTokens")
                .child(userId);

        tokenRef.setValue(token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved for user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }
}