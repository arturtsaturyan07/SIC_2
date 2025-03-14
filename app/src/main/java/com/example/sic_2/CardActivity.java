package com.example.sic_2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardActivity extends AppCompatActivity {

    private DatabaseReference database;
    private TextView cardMessage;
    private String cardId;
    private String currentUserId;
    private RecyclerView sharedCardsRecyclerView;
    private SharedCardsAdapter sharedCardsAdapter;
    private List<SharedCard> sharedCardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        sharedCardList = new ArrayList<>();
        sharedCardsAdapter = new SharedCardsAdapter(sharedCardList);
        sharedCardsRecyclerView = findViewById(R.id.shared_cards_recycler_view);
        sharedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sharedCardsRecyclerView.setAdapter(sharedCardsAdapter);

        // Initialize Firebase database and UI components
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        cardId = getIntent().getStringExtra("cardId");

        cardMessage = findViewById(R.id.card_message);
        ImageButton backButton = findViewById(R.id.back_button);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button createEventButton = findViewById(R.id.create_event_button);
        Button shareCardButton = findViewById(R.id.shareCardButton);
        sharedCardsRecyclerView = findViewById(R.id.shared_cards_recycler_view);

        // Initialize RecyclerView
        sharedCardList = new ArrayList<>();
        sharedCardsAdapter = new SharedCardsAdapter(sharedCardList);
        sharedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sharedCardsRecyclerView.setAdapter(sharedCardsAdapter);

        // Validate card ID and user authentication
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            finish();
            return;
        }
        if (currentUserId == null) {
            showToast("User authentication failed");
            finish();
            return;
        }

        // Check user access to the card
        checkUserAccess();

        // Load shared cards for the current user
        loadSharedCards();

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Add user button functionality
        addUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(CardActivity.this, UserAddActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        // Create event button functionality
        //createEventButton.setOnClickListener(v -> showCreateEventDialog());

        // Share card button functionality
        shareCardButton.setOnClickListener(v -> showShareDialog(cardId));

        // Bottom navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_main) {
                    navigateTo(MainActivity.class);
                    return true;
                } else if (item.getItemId() == R.id.nav_chat) {
                    navigateTo(ChatActivity.class);
                    return true;
                }
                return false;
            });
        } else {
            Log.e("CardActivity", "BottomNavigationView is null. Check your layout file.");
        }
    }

    /**
     * Checks if the current user has access to the specified card.
     */
    private void checkUserAccess() {
        if (currentUserId == null || cardId == null) {
            Log.e("FirebaseError", "Invalid user or card ID");
            showToast("Invalid user or card ID");
            finish();
            return;
        }

        // Reference to the card in the user's own cards
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);

        // Reference to the card in shared cards
        DatabaseReference sharedCardRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(currentUserId)
                .child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Card exists in the user's own cards
                    loadCardData();
                } else {
                    // Check if the card exists in shared cards
                    sharedCardRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot sharedSnapshot) {
                            if (sharedSnapshot.exists()) {
                                // Card exists in shared cards
                                loadSharedCardData(sharedSnapshot);
                            } else {
                                // Card not found in either location
                                showToast("Access denied: Card not found");
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("FirebaseError", "Error checking shared cards: " + error.getMessage());
                            showToast("Database error: " + error.getMessage());
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error checking user access: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
                finish();
            }
        });
    }


    private void loadSharedCardData(DataSnapshot sharedSnapshot) {
        String message = sharedSnapshot.child("message").getValue(String.class);
        String sharedBy = sharedSnapshot.child("sharedBy").getValue(String.class);

        if (message != null && !message.isEmpty()) {
            cardMessage.setText("[Shared] " + message); // Indicate shared card
        } else {
            showToast("Shared card data is incomplete");
            finish();
        }
    }

    /**
     * Loads the card data from Firebase.
     */
    private void loadCardData() {
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message = snapshot.child("message").getValue(String.class);
                if (message != null && !message.isEmpty()) {
                    cardMessage.setText(message);
                } else {
                    showToast("Card data is incomplete");
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to load card");
                Log.e("Firebase", "Failed to load card", error.toException());
                finish();
            }
        });
    }

    /**
     * Loads shared cards for the current user.
     */
    private void loadSharedCards() {
        if (currentUserId == null) {
            showToast("User authentication failed");
            return;
        }

        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(currentUserId);

        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sharedCardList.clear(); // Clear existing data
                if (snapshot.exists()) {
                    for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                        String cardId = cardSnapshot.getKey();
                        String message = cardSnapshot.child("message").getValue(String.class);
                        String sharedBy = cardSnapshot.child("sharedBy").getValue(String.class);

                        if (message != null && sharedBy != null) {
                            SharedCard sharedCard = new SharedCard(cardId, message, sharedBy);
                            sharedCardList.add(sharedCard);
                        }
                    }
                    sharedCardsAdapter.notifyDataSetChanged(); // Refresh RecyclerView
                } else {
                    Log.d("SharedCard", "No shared cards found");
                    showToast("No shared cards found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading shared cards: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Shows a dialog to share the card with another user.
     */
    private void showShareDialog(String cardId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Card");

        final AppCompatEditText userIdInput = new AppCompatEditText(this);
        userIdInput.setHint("Recipient User ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (!recipientUserId.isEmpty()) {
                shareCardWithUser(recipientUserId, cardId);
            } else {
                showToast("User ID cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Shares the card with another user by updating the sharedCards node in Firebase.
     */
    public void shareCardWithUser(String userId, String cardId) {
        if (userId == null || userId.isEmpty() || cardId == null || cardId.isEmpty()) {
            Log.e("ShareCard", "Invalid user ID or card ID");
            return;
        }

        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(userId)
                .child(cardId);

        DatabaseReference cardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(currentUserId)
                .child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        Map<String, Object> shareData = new HashMap<>();
                        shareData.put("sharedBy", currentUserId);
                        shareData.put("message", message);
                        shareData.put("timestamp", System.currentTimeMillis());

                        sharedCardsRef.setValue(shareData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("ShareCard", "Card successfully shared with user " + userId);
                                        showToast("Card shared successfully");
                                    } else {
                                        Log.e("ShareCard", "Failed to share card", task.getException());
                                        showToast("Failed to share card");
                                    }
                                });
                    } else {
                        Log.e("ShareCard", "Card message is missing");
                        showToast("Card message is missing");
                    }
                } else {
                    Log.e("ShareCard", "Card not found");
                    showToast("Card not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching card data: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(CardActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigates to the specified activity.
     */
    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(CardActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}