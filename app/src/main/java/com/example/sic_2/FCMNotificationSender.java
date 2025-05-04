package com.example.sic_2;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.SendChannelTokenRegistrationService;

import java.util.Map;

public class FCMNotificationSender {

    private static final String TAG = "FCMNotificationSender";

    public static void sendNotification(String token, Map<String, String> data) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder(token);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.addData(entry.getKey(), entry.getValue());
        }

        RemoteMessage message = builder.build();

        // Simulate delivery using FirebaseInstanceId (for testing only)
        // In production, use real FCM server or Firebase Cloud Functions
        Log.d(TAG, "Sending notification to " + token);
    }
}