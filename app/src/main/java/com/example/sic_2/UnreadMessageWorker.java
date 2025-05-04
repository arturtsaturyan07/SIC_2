package com.example.sic_2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.CountDownLatch;

public class UnreadMessageWorker extends Worker {

    public UnreadMessageWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) return Result.success();

        DatabaseReference userChatsRef = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(userId);

        final CountDownLatch latch = new CountDownLatch(1);

        userChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = (Boolean) chatSnapshot.child("read").getValue();
                    if (isRead == null || !isRead) {
                        // Trigger local notification or update badge
                        Log.d("UnreadWorker", "Unread message in chat: " + chatSnapshot.getKey());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await();
            return Result.success();
        } catch (InterruptedException e) {
            return Result.retry();
        }
    }
}