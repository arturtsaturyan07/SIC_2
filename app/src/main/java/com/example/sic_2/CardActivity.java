package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
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
    private String currentUserId;
    private String originalOwnerId;
    private DatabaseReference cardRef;

    // Add a reference to your toolbar/appbar title
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);
        Log.d(TAG, "onCreate");

        // Reference to the title TextView (make sure it exists in your card_view_layout)
        titleTextView = findViewById(R.id.top_bar_title);

        // Initialize Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            showErrorAndFinish("User not authenticated");
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Retrieve card ID and original owner ID from intent
        cardId = getIntent().getStringExtra("cardId");
        originalOwnerId = getIntent().getStringExtra("originalOwnerId");

        if (cardId == null || cardId.isEmpty()) {
            showErrorAndFinish("Card ID is missing");
            return;
        }

        Log.d(TAG, "Attempting to open card: " + cardId +
                ", originalOwnerId: " + originalOwnerId +
                ", currentUserId: " + currentUserId);

        // Initialize back button
        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Check if this is a shared card (originalOwnerId exists and is different from current user)
        boolean isSharedCard = originalOwnerId != null && !originalOwnerId.equals(currentUserId);

        // Initialize Firebase reference based on card type
        if (isSharedCard) {
            // For shared cards, use allCards reference
            cardRef = FirebaseDatabase.getInstance()
                    .getReference("allCards")
                    .child(cardId);
        } else {
            // For own cards, use user's cards reference
            cardRef = FirebaseDatabase.getInstance()
                    .getReference("cards")
                    .child(currentUserId)
                    .child(cardId);
        }

        // Verify card access
        verifyCardAccess(isSharedCard);
    }

    private void verifyCardAccess(boolean isSharedCard) {
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showErrorAndFinish("Card not found");
                    return;
                }

                if (isSharedCard) {
                    // For shared cards, verify access in sharedCards node
                    DatabaseReference sharedRef = FirebaseDatabase.getInstance()
                            .getReference("sharedCards")
                            .child(currentUserId)
                            .child(cardId);

                    sharedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot sharedSnapshot) {
                            if (sharedSnapshot.exists()) {
                                Log.d(TAG, "User has access to shared card");
                                initializeActivity();
                            } else {
                                showErrorAndFinish("You don't have permission to access this card");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            showErrorAndFinish("Error verifying shared access");
                        }
                    });
                } else {
                    // For own cards, just verify authorId matches
                    String authorId = snapshot.child("authorId").getValue(String.class);
                    if (authorId == null || authorId.equals(currentUserId)) {
                        initializeActivity();
                    } else {
                        showErrorAndFinish("Card ownership mismatch");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorAndFinish("Error verifying card access");
            }
        });
    }

    private void initializeActivity() {
        // Load the default fragment
        loadInitialFragment();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void loadInitialFragment() {
        Fragment initialFragment = CardFragment.newInstance(cardId, originalOwnerId != null ? originalOwnerId : currentUserId);
        setToolbarTitleForFragment(initialFragment);
        loadFragment(initialFragment, false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView not found");
            return;
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_main) {
                selectedFragment = CardFragment.newInstance(cardId, originalOwnerId != null ? originalOwnerId : currentUserId);
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = ChatFragment.newInstance(cardId, originalOwnerId != null ? originalOwnerId : currentUserId);
            } else if (itemId == R.id.nav_parameters) {
                selectedFragment = ParametersFragment.newInstance("param1", "param2", cardId, originalOwnerId != null ? originalOwnerId : currentUserId);
            } else if (itemId == R.id.nav_members) {
                fetchAuthorIdForCard();
                return true;
            } else if (itemId == R.id.nav_events) {
                selectedFragment = CampCalendarFragment.newInstance(cardId);
            }

            if (selectedFragment != null) {
                setToolbarTitleForFragment(selectedFragment);
                loadFragment(selectedFragment, true);
            }
            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_main);
    }

    private void fetchAuthorIdForCard() {
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String authorId = snapshot.child("authorId").getValue(String.class);
                    if (authorId != null) {
                        MembersFragment membersFragment = MembersFragment.newInstance(cardId, authorId);
                        setToolbarTitleForFragment(membersFragment);
                        loadFragment(membersFragment, true);
                    } else {
                        showErrorAndFinish("Card has no author information");
                    }
                } else {
                    showErrorAndFinish("Card no longer exists");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorAndFinish("Failed to fetch card details");
            }
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

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

    /**
     * Updates the toolbar/appbar title based on the fragment type.
     */
    private void setToolbarTitleForFragment(Fragment fragment) {
        if (titleTextView == null) return;
        String title = "";
        if (fragment instanceof CardFragment) {
            title = "Publications";
        } else if (fragment instanceof ChatFragment) {
            title = "Chats";
        } else if (fragment instanceof ParametersFragment) {
            title = "Parameters";
        } else if (fragment instanceof MembersFragment) {
            title = "Members";
        } else {
            title = "Calendar and Events";
        }
        titleTextView.setText(title);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
        finish();
    }

    public void onCardDeleted(String cardId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).onCardDeleted(cardId);
        }
        finish();
        Toast.makeText(this, "Card deleted successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}