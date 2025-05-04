package com.example.sic_2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTIFICATIONS = 0;

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private FirebaseUser user;
    private String userId;
    private LinearLayout cardContainer;
    private TextView badge;
    private int unreadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show app-started notification
        createNotificationChannel();
        showAppStartedNotification();

        badge = findViewById(R.id.badge);

        // Load unread count from Firebase or local logic
        loadUnreadMessageCount();

        // Initialize views
        initializeViews();

        // Request notification permissions for Android 13+
        requestNotificationPermission();

        // Handle deep link to open chat fragment
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_chat", false)) {
            String cardId = intent.getStringExtra("cardId");
            if (cardId != null) {
                openChatFragment(cardId);
            }
        }

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Check guest mode
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

        // Apply dark mode settings
        applyDarkMode();

        // Set up bottom navigation
        setupBottomNavigationView();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "App Notifications";
            String description = "Notifications for App Events";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("app_channel_id", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void showAppStartedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "app_channel_id")
                .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                .setContentTitle("SIC App Running")
                .setContentText("Your app is now active.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false); // Prevent swiping away

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        cardContainer = findViewById(R.id.card_container);
    }

    private void openChatFragment(String cardId) {
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
        if (userId == null) return;
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
        if (user == null) return;
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
        if (userId == null || cardContainer == null) return;
        DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");
        cardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATIONS);
            } else {
                scheduleNotificationWorker();
            }
        } else {
            scheduleNotificationWorker();
        }
    }

    private void scheduleNotificationWorker() {
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(HiWorker.class, 1, TimeUnit.HOURS)
                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ChatNotificationWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleNotificationWorker();
            } else {
                Toast.makeText(this, "Notifications permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void updateUnreadCount(int count) {
        TextView badge = bottomNavigationView.findViewById(R.id.badge);
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void scheduleUnreadCheckWorker() {
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(UnreadMessageWorker.class, 15, TimeUnit.MINUTES)
                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "UnreadMessageWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    private void loadUnreadMessageCount() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userChatsRef = FirebaseDatabase.getInstance().getReference("user_chats").child(currentUserId);

        userChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = chatSnapshot.child("read").getValue(Boolean.class);
                    if (isRead == null || !isRead) {
                        count++;
                    }
                }
                updateBadge(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load unread counts", error.toException());
            }
        });
    }

    private void updateBadge(int count) {
        unreadCount = count;
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }
}