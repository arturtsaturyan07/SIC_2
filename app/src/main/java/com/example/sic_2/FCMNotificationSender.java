package com.example.sic_2;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMNotificationSender {
    private static final String TAG = "FCMNotificationSender";

    public static void sendNotification(String token, Map<String, String> data) {
        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(token)
                .setMessageId(Integer.toString((int) System.currentTimeMillis()))
                .addData("title", data.get("title"))
                .addData("body", data.get("body"))
                .addData("cardId", data.get("cardId"))
                .addData("senderId", data.get("senderId"))
                .build());

        Log.d(TAG, "Notification sent to token: " + token);
    }
}