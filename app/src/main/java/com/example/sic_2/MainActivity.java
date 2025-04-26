package com.example.sic_2;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_NOTIFICATIONS = 0;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private FirebaseUser user;
    private String userId;
    private LinearLayout cardContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views first
        initializeViews();
        requestNotificationPermission();

        if (getIntent() != null && getIntent().getBooleanExtra("open_chat", false)) {
            String cardId = getIntent().getStringExtra("cardId");
            if (cardId != null) {
                // Open the chat fragment directly
                openChatFragment(cardId);
            }
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Request notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Check if this is guest mode
        Intent intent = getIntent();
        boolean isGuest = intent.getBooleanExtra("guest_mode", false);

        if (user == null && !isGuest) {
            redirectToLogin();
            return;
        }

        if (!isGuest) {
            userId = user.getUid();
            initializeFirebase(isGuest, intent);
            loadUserCards();
        }

        applyDarkMode();
        setupBottomNavigationView();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        cardContainer = findViewById(R.id.card_container);
    }

    private void openChatFragment(String cardId) {
        // Implement logic to open your chat fragment with the given cardId
        ChatFragment fragment = ChatFragment.newInstance(cardId, "");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }
    private void initializeFirebase(boolean isGuest, Intent intent) {
        if (isGuest) {
            Log.d("GuestMode", "Skipping Firebase user check for guest mode.");
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get user info from intent or Firebase user
        String email = intent.getStringExtra("email");
        if (email == null && user != null) {
            email = user.getEmail();
        }

        checkIfUserExists(email);
    }

    private void redirectToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void applyDarkMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkModeEnabled = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkModeEnabled ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void checkIfUserExists(String email) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d("Firebase", "User does not exist, creating...");
                    createUser(email);
                } else {
                    Log.d("Firebase", "User already exists in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void createUser(String email) {
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = "User";
        }

        User newUser = new User(userId, name, "", email != null ? email : "no-email@example.com");
        usersRef.child(userId).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "User created successfully");
                    } else {
                        Log.e("Firebase", "User creation failed", task.getException());
                    }
                });
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.notification) {
                selectedFragment = new NotificationFragment();
            } else if (itemId == R.id.profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    private void loadUserCards() {
        if (userId == null) return;

        DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");
        cardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (cardContainer == null) return;

                cardContainer.removeAllViews();
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    DataSnapshot usersSnapshot = cardSnapshot.child("users");
                    if (usersSnapshot.hasChild(userId) && Boolean.TRUE.equals(usersSnapshot.child(userId).getValue(Boolean.class))) {
                        String cardId = cardSnapshot.getKey();
                        String message = cardSnapshot.child("message").getValue(String.class);
                        if (message != null) {
                            addCardToUI(cardId, message);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load user cards", error.toException());
            }
        });
    }

    private void addCardToUI(String cardId, String message) {
        if (cardContainer == null) return;

        TextView cardView = new TextView(this);
        cardView.setText(message);
        cardView.setPadding(16, 16, 16, 16);
        cardView.setTextSize(18);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CardActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        cardContainer.addView(cardView);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATIONS);
            } else {
                Log.d("MainActivity", "Permission already granted");
                scheduleNotificationWorker();
            }
        } else {
            Log.d("MainActivity", "Permission not required (API < 33)");
            scheduleNotificationWorker();
        }
    }

//    private void scheduleNotificationWorker() {
//        Log.d("MainActivity", "Scheduling worker");
//
//        PeriodicWorkRequest workRequest =
//                new PeriodicWorkRequest.Builder(HiWorker.class, 15, TimeUnit.MINUTES)
//                        .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "HiNotificationWork",
//                ExistingPeriodicWorkPolicy.KEEP,
//                workRequest
//        );
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission granted by user");
                scheduleNotificationWorker();
            } else {
                Log.w("MainActivity", "Permission denied by user");
                Toast.makeText(this,
                        "Notifications permission denied.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // In MainActivity.java
    private void scheduleNotificationWorker() {
        Log.d("MainActivity", "Scheduling worker");

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(HiWorker.class, 1, TimeUnit.HOURS) // Check every hour
                        .setInitialDelay(10, TimeUnit.SECONDS) // Start after 10 seconds
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ChatNotificationWork",
                ExistingPeriodicWorkPolicy.REPLACE, // Replace existing work
                workRequest
        );
    }
}