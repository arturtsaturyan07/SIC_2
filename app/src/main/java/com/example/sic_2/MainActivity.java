package com.example.sic_2;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
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

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Request notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
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
        initializeViews();
        setupBottomNavigationView();

        loadFragment(new HomeFragment());
    }

    private void initializeFirebase(boolean isGuest, Intent intent) {
        if (isGuest) {
            Log.d("GuestMode", "Skipping Firebase user check for guest mode.");
            return;
        }

        String email_ = intent.getStringExtra("email");
        String name_ = RegisterActivity.name_;
        String surname_ = intent.getStringExtra("surname");

        if (email_ == null) email_ = user.getEmail();

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        checkIfUserExists(email_, name_, surname_);
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

    private void checkIfUserExists(String email, String name, String surname) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d("Firebase", "User does not exist, creating...");
                    createUser(email, name, surname);
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

    private void createUser(String email, String name, String surname) {
        name = Objects.requireNonNullElse(name, Objects.requireNonNullElse(user.getDisplayName(), "Unknown"));
        surname = Objects.requireNonNullElse(surname, "Unknown");
        email = Objects.requireNonNullElse(email, "no-email@example.com");

        User newUser = new User(userId, name, surname, email);
        String finalName = name;
        String finalSurname = surname;
        String finalEmail = email;
        usersRef.child(userId).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "User created successfully: " + finalName + " " + finalSurname + " " + finalEmail);
                    } else {
                        Log.e("Firebase", "User creation failed", task.getException());
                    }
                });
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        cardContainer = findViewById(R.id.card_container);
    }

    private Fragment getFragmentById(int id) {
        if (id == R.id.home) return new HomeFragment();
        if (id == R.id.notification) return new NotificationFragment();
        if (id == R.id.settings) return new SettingsFragment();
        return null;
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = getFragmentById(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void loadUserCards() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");

        cardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    DataSnapshot usersSnapshot = cardSnapshot.child("users");

                    if (usersSnapshot.hasChild(userId) && usersSnapshot.child(userId).getValue(Boolean.class)) {
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
}