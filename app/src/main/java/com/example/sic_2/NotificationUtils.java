package com.example.sic_2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    public static final String CHANNEL_ID = "camp_request_channel";

    public static void showCampRequestNotification(Context context, CampRequest request) {
        if (request == null) return;

        // Create channel (for Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Camp Requests", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent acceptIntent = new Intent(context, CampRequestActionReceiver.class);
        acceptIntent.setAction("CAMP_REQUEST_ACCEPT");
        acceptIntent.putExtra("ownerId", request.getOwnerId());
        acceptIntent.putExtra("requestId", request.getRequestId());
        acceptIntent.putExtra("cardId", request.getCardId());
        acceptIntent.putExtra("requesterId", request.getRequesterId());
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, ("ACCEPT"+request.getRequestId()).hashCode(), acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(context, CampRequestActionReceiver.class);
        rejectIntent.setAction("CAMP_REQUEST_REJECT");
        rejectIntent.putExtra("ownerId", request.getOwnerId());
        rejectIntent.putExtra("requestId", request.getRequestId());
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(context, ("REJECT"+request.getRequestId()).hashCode(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Camp Join Request")
                .setContentText(request.getRequesterName() + " wants to join: " + request.getCardTitle())
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(R.drawable.ic_accept, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_reject, "Reject", rejectPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(request.getRequestId().hashCode(), builder.build());
    }
}