package com.example.sic_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Boot completed — scheduling worker");

            PeriodicWorkRequest workRequest =
                    new PeriodicWorkRequest.Builder(HiWorker.class, 15, TimeUnit.MINUTES)
                            .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "HiNotificationWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
            );
        }
    }
}