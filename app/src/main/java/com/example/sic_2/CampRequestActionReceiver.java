package com.example.sic_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;

public class CampRequestActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String ownerId = intent.getStringExtra("ownerId");
        String requestId = intent.getStringExtra("requestId");
        String cardId = intent.getStringExtra("cardId");
        String requesterId = intent.getStringExtra("requesterId");

        if (ownerId == null || requestId == null) return;

        if ("CAMP_REQUEST_ACCEPT".equals(action)) {
            // Set status to approved, add user to campMembers, share card
            FirebaseDatabase.getInstance().getReference("campRequests")
                    .child(ownerId).child(requestId).child("status").setValue("approved");
            if (cardId != null && requesterId != null) {
                FirebaseDatabase.getInstance().getReference("allCards")
                        .child(cardId).child("campMembers").child(requesterId).setValue(true);
                // Optionally: implement shareCardWithUser as in your HomeFragment
            }
        } else if ("CAMP_REQUEST_REJECT".equals(action)) {
            FirebaseDatabase.getInstance().getReference("campRequests")
                    .child(ownerId).child(requestId).child("status").setValue("rejected");
        }
        // Optionally: cancel notification here
    }
}