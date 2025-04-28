package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CardActivity extends AppCompatActivity {

    private static final String TAG = "CardActivity";
    private String cardId;
    private String originalOwnerId;
    private String currentUserId;
    private DatabaseReference cardRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);
        Log.d(TAG, "onCreate");

        // Initialize Firebase user
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Retrieve card ID and original owner ID from intent
        cardId = getIntent().getStringExtra("cardId");
        originalOwnerId = getIntent().getStringExtra("originalOwnerId");

        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(this, "Card ID is missing", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Card ID is missing");
            finish();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User not authenticated");
            finish();
            return;
        }

        // Log the received IDs
        Log.d(TAG, "Card ID: " + cardId);
        Log.d(TAG, "Original Owner ID: " + (originalOwnerId != null ? originalOwnerId : "null"));
        Log.d(TAG, "Current User ID: " + currentUserId);

        // Initialize Firebase reference
        String cardOwnerId = originalOwnerId != null ? originalOwnerId : currentUserId;
        cardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardOwnerId)
                .child(cardId);

        // Initialize back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Verify card exists before proceeding
        verifyCardExists();

        // Load the default fragment (CardFragment)
        loadInitialFragment();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void verifyCardExists() {
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(CardActivity.this, "Card not found", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Card does not exist in database");
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CardActivity.this, "Error verifying card", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error verifying card existence", error.toException());
                finish();
            }
        });
    }

    private void loadInitialFragment() {
        Fragment initialFragment = CardFragment.newInstance(cardId, originalOwnerId);
        loadFragment(initialFragment, false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_main) {
                    selectedFragment = CardFragment.newInstance(cardId, originalOwnerId);
                } else if (itemId == R.id.nav_chat) {
                    selectedFragment = ChatFragment.newInstance(cardId, originalOwnerId);
                } else if (itemId == R.id.nav_parameters) {
                    selectedFragment = ParametersFragment.newInstance("param1", "param2", cardId, originalOwnerId);
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, true);
                }
                return true;
            });

            // Set the default selected item
            bottomNavigationView.setSelectedItemId(R.id.nav_main);
        } else {
            Log.e(TAG, "BottomNavigationView is null. Check your layout file.");
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // Use fragment tag for better management
            String fragmentTag = fragment.getClass().getSimpleName();
            transaction.replace(R.id.fragment_container, fragment, fragmentTag);

            if (addToBackStack) {
                transaction.addToBackStack(fragmentTag);
            }
            transaction.commit();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error loading fragment", e);
        }
    }

    public void onCardDeleted(String cardId) {
        // Notify all fragments that might need to handle the deletion
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).onCardDeleted(cardId);
        }

        // Close the activity after deletion
        finish();
    }

    public void reloadHomeFragmentData() {
        try {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof HomeFragment) {
                runOnUiThread(() -> {
                    try {
                        ((HomeFragment) fragment).reloadData();
                    } catch (Exception e) {
                        Log.e(TAG, "Error reloading HomeFragment data", e);
                    }
                });
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Fragment manager not ready", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}